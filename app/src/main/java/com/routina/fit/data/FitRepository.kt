package com.routina.fit.data

import android.content.Context
import com.routina.fit.model.Exercise
import com.routina.fit.model.FitBackup
import com.routina.fit.model.FitSettings
import com.routina.fit.model.Routine
import com.routina.fit.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Fit 的本地儲存：filesDir 底下三個 JSON 檔，加上放在 SharedPreferences 的偏好設定。
 *
 * 作法與 Routina Bite 一致：
 * - 資料量小，整份載進記憶體、整份寫回，不用資料庫
 * - 以 StateFlow 對外，UI 直接 collect
 * - 寫入排在 IO 執行緒、以 Mutex 序列化，並用 tmp + rename 讓檔案不會停在寫到一半的狀態
 * - 檔案損毀無法解析時以空清單啟動，不崩潰
 */
class FitRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val exercisesFile: File get() = File(appContext.filesDir, FILE_EXERCISES)
    private val routinesFile: File get() = File(appContext.filesDir, FILE_ROUTINES)
    private val sessionsFile: File get() = File(appContext.filesDir, FILE_SESSIONS)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _settings = MutableStateFlow(FitSettings())
    val settings: StateFlow<FitSettings> = _settings.asStateFlow()

    private val _expandedParts = MutableStateFlow<Set<String>>(emptySet())
    val expandedParts: StateFlow<Set<String>> = _expandedParts.asStateFlow()

    init {
        // 檔案只有幾十 KB，啟動時同步載入，讓第一幀就有正確資料
        val seededExercises = loadSeeded(exercisesFile, ASSET_EXERCISES, Exercise.serializer()) {
            _exercises.value = it
        }
        val seededRoutines = loadSeeded(routinesFile, ASSET_ROUTINES, Routine.serializer()) {
            _routines.value = it
        }
        _sessions.value = readList(sessionsFile, ListSerializer(Session.serializer()))
        _settings.value = readSettings()
        _expandedParts.value = readExpandedParts()

        // 種子資料要立刻落地，不然「使用者自己刪光」與「還沒載過」下次啟動就分不出來
        if (seededExercises && _exercises.value.isNotEmpty()) persistExercises()
        if (seededRoutines && _routines.value.isNotEmpty()) persistRoutines()
    }

    // ---------- 載入 ----------

    /**
     * 檔案存在就讀檔，不存在才載入內建種子。
     * 回傳 true 表示這次用的是種子，呼叫端要把它寫成檔案。
     */
    private fun <T> loadSeeded(
        file: File,
        asset: String,
        serializer: KSerializer<T>,
        assign: (List<T>) -> Unit
    ): Boolean {
        if (file.exists()) {
            assign(readList(file, ListSerializer(serializer)))
            return false
        }
        val seed = try {
            appContext.assets.open(asset).bufferedReader().use { reader ->
                json.decodeFromString(ListSerializer(serializer), reader.readText())
            }
        } catch (t: Throwable) {
            emptyList()
        }
        assign(seed)
        return true
    }

    private fun <T> readList(file: File, serializer: KSerializer<List<T>>): List<T> = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (t: Throwable) {
        emptyList()
    }

    private fun readSettings(): FitSettings {
        val fallback = FitSettings()
        return FitSettings(
            defaultRestSec = prefs.getInt(KEY_DEFAULT_REST, fallback.defaultRestSec),
            sound = prefs.getBoolean(KEY_SOUND, fallback.sound),
            vibrate = prefs.getBoolean(KEY_VIBRATE, fallback.vibrate),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, fallback.keepScreenOn)
        )
    }

    // 對外用的是部位名稱本身（未分類就是空字串）；存檔時未分類換成 NONE_PART_KEY，
    // 因為整個集合是用換行串成一個字串，空字串串進去就分不出來了。
    private fun readExpandedParts(): Set<String> =
        prefs.getString(KEY_EXPANDED_PARTS, null)
            ?.split('\n')
            ?.filter { it.isNotEmpty() }
            ?.map { if (it == NONE_PART_KEY) "" else it }
            ?.toSet()
            ?: emptySet()

    // ---------- 動作庫 ----------

    fun findExercise(id: String): Exercise? = _exercises.value.firstOrNull { it.id == id }

    fun upsertExercise(exercise: Exercise) {
        val current = _exercises.value
        val index = current.indexOfFirst { it.id == exercise.id }
        _exercises.value = if (index >= 0) {
            current.toMutableList().apply { this[index] = exercise }
        } else {
            current + exercise
        }
        persistExercises()
    }

    /**
     * 刪動作時把課表裡用到它的項目一起移除——留著會變成點不動的空項目。
     * 紀錄不動：紀錄存的是當時的名稱快照。
     */
    fun deleteExercise(id: String) {
        _exercises.value = _exercises.value.filterNot { it.id == id }
        persistExercises()

        val affected = _routines.value.any { routine -> routine.items.any { it.exerciseId == id } }
        if (affected) {
            _routines.value = _routines.value.map { routine ->
                routine.copy(items = routine.items.filterNot { it.exerciseId == id })
            }
            persistRoutines()
        }
    }

    fun setPartExpanded(part: String, expanded: Boolean) {
        val current = _expandedParts.value
        val next = if (expanded) current + part else current - part
        _expandedParts.value = next
        val stored = next.joinToString("\n") { it.ifEmpty { NONE_PART_KEY } }
        prefs.edit().putString(KEY_EXPANDED_PARTS, stored).apply()
    }

    // ---------- 課表 ----------

    fun findRoutine(id: String): Routine? = _routines.value.firstOrNull { it.id == id }

    fun upsertRoutine(routine: Routine) {
        val current = _routines.value
        val index = current.indexOfFirst { it.id == routine.id }
        _routines.value = if (index >= 0) {
            current.toMutableList().apply { this[index] = routine }
        } else {
            current + routine
        }
        persistRoutines()
    }

    fun deleteRoutine(id: String) {
        _routines.value = _routines.value.filterNot { it.id == id }
        persistRoutines()
    }

    // ---------- 紀錄 ----------

    fun findSession(id: String): Session? = _sessions.value.firstOrNull { it.id == id }

    fun addSession(session: Session) {
        _sessions.value = _sessions.value + session
        persistSessions()
    }

    fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
        persistSessions()
    }

    // ---------- 設定 ----------

    fun setSettings(settings: FitSettings) {
        _settings.value = settings
        prefs.edit()
            .putInt(KEY_DEFAULT_REST, settings.defaultRestSec)
            .putBoolean(KEY_SOUND, settings.sound)
            .putBoolean(KEY_VIBRATE, settings.vibrate)
            .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            .apply()
    }

    // ---------- 備份 ----------

    fun buildBackup(): FitBackup = FitBackup(
        exportedAt = System.currentTimeMillis(),
        exercises = _exercises.value,
        routines = _routines.value,
        sessions = _sessions.value,
        settings = _settings.value
    )

    /**
     * 依 id 合併匯入的備份：同 id 以匯入的為準，現有但檔案裡沒有的一律保留，
     * 所以重複匯入同一份不會長出重複資料。
     */
    fun applyBackup(backup: FitBackup) {
        _exercises.value = mergeBy(_exercises.value, backup.exercises) { it.id }
        _routines.value = mergeBy(_routines.value, backup.routines) { it.id }
        _sessions.value = mergeBy(_sessions.value, backup.sessions) { it.id }
        backup.settings?.let { setSettings(it) }
        persistExercises()
        persistRoutines()
        persistSessions()
    }

    private fun <T> mergeBy(current: List<T>, incoming: List<T>, key: (T) -> String): List<T> {
        val byKey = LinkedHashMap<String, T>()
        current.forEach { byKey[key(it)] = it }
        incoming.forEach { byKey[key(it)] = it }
        return byKey.values.toList()
    }

    // ---------- 寫入 ----------

    // 快照在取得鎖之後才讀：若在鎖外先取，兩次連續異動可能以相反順序寫入，
    // 讓較舊的內容蓋掉較新的內容。
    private fun persistExercises() {
        scope.launch {
            writeMutex.withLock {
                val content = json.encodeToString(ListSerializer(Exercise.serializer()), _exercises.value)
                writeAtomically(exercisesFile, content)
            }
        }
    }

    private fun persistRoutines() {
        scope.launch {
            writeMutex.withLock {
                val content = json.encodeToString(ListSerializer(Routine.serializer()), _routines.value)
                writeAtomically(routinesFile, content)
            }
        }
    }

    private fun persistSessions() {
        scope.launch {
            writeMutex.withLock {
                val content = json.encodeToString(ListSerializer(Session.serializer()), _sessions.value)
                writeAtomically(sessionsFile, content)
            }
        }
    }

    /** 呼叫端必須已持有 writeMutex */
    private fun writeAtomically(target: File, content: String) {
        try {
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.writeText(content)
            if (!tmp.renameTo(target)) {
                target.writeText(content)
                tmp.delete()
            }
        } catch (t: Throwable) {
            // 寫入失敗不影響 App 運作（記憶體內資料仍然正確）
        }
    }

    private companion object {
        const val FILE_EXERCISES = "exercises.json"
        const val FILE_ROUTINES = "routines.json"
        const val FILE_SESSIONS = "sessions.json"
        const val ASSET_EXERCISES = "seed_exercises.json"
        const val ASSET_ROUTINES = "seed_routines.json"

        const val PREFS_NAME = "routina_fit"
        const val KEY_DEFAULT_REST = "default_rest"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_EXPANDED_PARTS = "expanded_parts"
        const val NONE_PART_KEY = "__none__"
    }
}
