package com.routina.fit.data

import android.content.Context
import com.routina.fit.model.DayItem
import com.routina.fit.model.DayPlan
import com.routina.fit.model.Exercise
import com.routina.fit.model.FitBackup
import com.routina.fit.model.FitSettings
import com.routina.fit.model.Measure
import com.routina.fit.model.Session
import com.routina.fit.model.SetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
import java.util.UUID

/**
 * Fit 的本地儲存：filesDir 底下幾個 JSON 檔，加上放在 SharedPreferences 的偏好設定。
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
    private val sessionsFile: File get() = File(appContext.filesDir, FILE_SESSIONS)
    private val defaultsFile: File get() = File(appContext.filesDir, FILE_DEFAULTS)
    private val dayFile: File get() = File(appContext.filesDir, FILE_DAY)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _settings = MutableStateFlow(FitSettings())
    val settings: StateFlow<FitSettings> = _settings.asStateFlow()

    private val _expandedParts = MutableStateFlow<Set<String>>(emptySet())
    val expandedParts: StateFlow<Set<String>> = _expandedParts.asStateFlow()

    /** 每天的預設清單。換日時用它長出新的一天 */
    private val _defaults = MutableStateFlow<List<DayItem>>(emptyList())
    val defaults: StateFlow<List<DayItem>> = _defaults.asStateFlow()

    private val _today = MutableStateFlow(DayPlan(date = todayDate()))
    val today: StateFlow<DayPlan> = _today.asStateFlow()

    private var todayWrite: Job? = null

    init {
        // 檔案只有幾十 KB，啟動時同步載入，讓第一幀就有正確資料
        val seededExercises = loadSeeded(exercisesFile, ASSET_EXERCISES, Exercise.serializer()) {
            _exercises.value = it
        }
        val seededDefaults = loadSeeded(defaultsFile, ASSET_DEFAULTS, DayItem.serializer()) {
            _defaults.value = it
        }
        _sessions.value = readList(sessionsFile, ListSerializer(Session.serializer()))
        _settings.value = readSettings()
        _expandedParts.value = readExpandedParts()

        // 種子資料要立刻落地，不然「使用者自己刪光」與「還沒載過」下次啟動就分不出來
        if (seededExercises && _exercises.value.isNotEmpty()) persistExercises()
        if (seededDefaults && _defaults.value.isNotEmpty()) persistDefaults()

        val storedDay = readDay()
        _today.value = storedDay ?: freshDay()
        if (storedDay == null) persistToday()
        ensureToday()

        migrateSeed(freshInstall = seededExercises)
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
        assign(readAsset(asset, serializer))
        return true
    }

    /**
     * 種子欄位的一次性補齊。
     *
     * 舊版裝好之後才長出來的欄位（例如 0.2.0 的器材），檔案裡是空的，
     * 而種子只在首次啟動載入，所以要在這裡把內建動作缺的欄位補回去。
     * 只補「使用者沒填過」的，不覆蓋任何已有的值；跑過就把版本寫進 prefs，不再重跑。
     */
    private fun migrateSeed(freshInstall: Boolean) {
        val done = prefs.getInt(KEY_SEED_VERSION, 0)
        if (done >= SEED_VERSION) return
        // 全新安裝的資料就是最新的種子，不必補
        if (!freshInstall) backfillEquipment()
        prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()
    }

    private fun backfillEquipment() {
        val seed = readAsset(ASSET_EXERCISES, Exercise.serializer()).associateBy { it.id }
        if (seed.isEmpty()) return
        var changed = false
        _exercises.value = _exercises.value.map { exercise ->
            val fromSeed = seed[exercise.id]
            if (exercise.equipment.isEmpty() && fromSeed != null && fromSeed.equipment.isNotEmpty()) {
                changed = true
                exercise.copy(equipment = fromSeed.equipment)
            } else {
                exercise
            }
        }
        if (changed) persistExercises()
    }

    private fun <T> readAsset(asset: String, serializer: KSerializer<T>): List<T> = try {
        appContext.assets.open(asset).bufferedReader().use { reader ->
            json.decodeFromString(ListSerializer(serializer), reader.readText())
        }
    } catch (t: Throwable) {
        emptyList()
    }

    private fun <T> readList(file: File, serializer: KSerializer<List<T>>): List<T> = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (t: Throwable) {
        emptyList()
    }

    private fun readSettings(): FitSettings {
        val fallback = FitSettings()
        return FitSettings(
            sound = prefs.getBoolean(KEY_SOUND, fallback.sound),
            vibrate = prefs.getBoolean(KEY_VIBRATE, fallback.vibrate)
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

    private fun readDay(): DayPlan? = try {
        if (dayFile.exists()) json.decodeFromString(DayPlan.serializer(), dayFile.readText()) else null
    } catch (t: Throwable) {
        null
    }

    // ---------- 今天的清單 ----------

    /**
     * 確認手上這份清單就是今天的，跨過午夜就換一份新的。
     *
     * 畫面每次回到前景都會叫一次——App 開著過午夜是常態，不能只在啟動時算一次。
     *
     * 昨天有做卻沒結算的，在這裡自動收進紀錄：數出來的次數不該因為換日就消失。
     * 所以「結算」按鈕的作用是**現在就記下來**，不是「不按就不算」。
     */
    fun ensureToday() {
        val date = todayDate()
        val current = _today.value
        if (current.date == date) return
        if (current.touched && !current.settled) recordDay(current, System.currentTimeMillis())
        _today.value = freshDay(date)
        persistToday()
    }

    /** 用預設清單長出新的一天。id 重給，免得不同天的項目共用 key */
    private fun freshDay(date: String = todayDate()): DayPlan =
        DayPlan(date = date, items = _defaults.value.map { it.copy(id = newId(), done = 0) })

    /** 加或扣一下。回傳改完的那一項，呼叫端用它判斷要不要響 */
    fun countItem(id: String, delta: Int): DayItem? {
        val plan = _today.value
        val index = plan.items.indexOfFirst { it.id == id }
        if (index < 0) return null
        val item = plan.items[index]
        val next = item.copy(done = (item.done + delta).coerceAtLeast(0))
        if (next.done == item.done) return item
        _today.value = plan.copy(
            items = plan.items.toMutableList().apply { this[index] = next },
            startedAt = if (plan.startedAt == 0L) System.currentTimeMillis() else plan.startedAt
        )
        persistToday()
        return next
    }

    fun addTodayItem(name: String, target: Int, exerciseId: String = "") {
        val item = DayItem(id = newId(), name = name.trim(), target = target, exerciseId = exerciseId)
        _today.value = _today.value.let { it.copy(items = it.items + item) }
        persistToday()
    }

    fun updateTodayItem(item: DayItem) {
        val plan = _today.value
        val index = plan.items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        _today.value = plan.copy(items = plan.items.toMutableList().apply { this[index] = item })
        persistToday()
    }

    fun deleteTodayItem(id: String) {
        _today.value = _today.value.let { plan -> plan.copy(items = plan.items.filterNot { it.id == id }) }
        persistToday()
    }

    /** 結算：把今天寫成一筆紀錄。回傳記下來的總次數，0 表示今天還沒動過 */
    fun settleToday(): Int {
        val plan = _today.value
        if (!plan.touched) return 0
        val now = System.currentTimeMillis()
        recordDay(plan, now)
        _today.value = plan.copy(settledAt = now)
        persistToday()
        return plan.total
    }

    /**
     * 把一天寫成紀錄。id 由日期決定，所以結算兩次是更新同一筆，不是長出第二筆。
     * [Session.routineName] 留空，顯示的名字由畫面那層補——這一層不該碰字串資源。
     */
    private fun recordDay(plan: DayPlan, endedAt: Long) {
        val logs = plan.items.filter { it.done > 0 }.map { item ->
            SetLog(
                exerciseId = item.exerciseId,
                name = item.name,
                measure = Measure.REPS,
                target = item.target,
                done = item.done
            )
        }
        if (logs.isEmpty()) return
        upsertSession(
            Session(
                id = DAY_SESSION_PREFIX + plan.date,
                date = plan.date,
                startedAt = if (plan.startedAt > 0L) plan.startedAt else endedAt,
                endedAt = endedAt,
                logs = logs
            )
        )
    }

    // ---------- 預設清單 ----------

    fun setDefaults(items: List<DayItem>) {
        _defaults.value = items
        persistDefaults()
    }

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

    /** 紀錄與今天的清單都不動：它們存的是當時的名稱快照 */
    fun deleteExercise(id: String) {
        _exercises.value = _exercises.value.filterNot { it.id == id }
        persistExercises()
    }

    fun setPartExpanded(part: String, expanded: Boolean) {
        val current = _expandedParts.value
        val next = if (expanded) current + part else current - part
        _expandedParts.value = next
        val stored = next.joinToString("\n") { it.ifEmpty { NONE_PART_KEY } }
        prefs.edit().putString(KEY_EXPANDED_PARTS, stored).apply()
    }

    // ---------- 紀錄 ----------

    fun findSession(id: String): Session? = _sessions.value.firstOrNull { it.id == id }

    /** 同 id 就取代。每日清單的 id 由日期決定，結算兩次才不會長出兩筆 */
    fun upsertSession(session: Session) {
        val current = _sessions.value
        val index = current.indexOfFirst { it.id == session.id }
        _sessions.value = if (index >= 0) {
            current.toMutableList().apply { this[index] = session }
        } else {
            current + session
        }
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
            .putBoolean(KEY_SOUND, settings.sound)
            .putBoolean(KEY_VIBRATE, settings.vibrate)
            .apply()
    }

    // ---------- 備份 ----------

    fun buildBackup(): FitBackup = FitBackup(
        exportedAt = System.currentTimeMillis(),
        exercises = _exercises.value,
        sessions = _sessions.value,
        settings = _settings.value,
        defaults = _defaults.value,
        today = _today.value
    )

    /**
     * 依 id 合併匯入的備份：同 id 以匯入的為準，現有但檔案裡沒有的一律保留，
     * 所以重複匯入同一份不會長出重複資料。
     *
     * 兩個例外：
     * - **預設清單整份換掉**。它是一張有順序的樣板，依 id 合併會把使用者刪掉的項目又帶回來。
     * - **今天那份只在日期對得上時才收**。昨天的清單蓋到今天是錯的；那一天如果結算過，
     *   內容本來就在 `sessions` 裡。
     */
    fun applyBackup(backup: FitBackup) {
        _exercises.value = mergeBy(_exercises.value, backup.exercises) { it.id }
        _sessions.value = mergeBy(_sessions.value, backup.sessions) { it.id }
        backup.settings?.let { setSettings(it) }
        persistExercises()
        persistSessions()

        if (backup.defaults.isNotEmpty()) setDefaults(backup.defaults)
        backup.today?.takeIf { it.date == todayDate() }?.let {
            _today.value = it
            persistToday()
        }
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

    private fun persistSessions() {
        scope.launch {
            writeMutex.withLock {
                val content = json.encodeToString(ListSerializer(Session.serializer()), _sessions.value)
                writeAtomically(sessionsFile, content)
            }
        }
    }

    private fun persistDefaults() {
        scope.launch {
            writeMutex.withLock {
                val content = json.encodeToString(ListSerializer(DayItem.serializer()), _defaults.value)
                writeAtomically(defaultsFile, content)
            }
        }
    }

    /**
     * 今天這份要省著寫：長按連加一秒會按十幾下，每一下都寫檔太吵。
     * 已經有一次排隊中就不再排——它跑的時候讀到的是當下最新的狀態，
     * 所以中間這幾下不會漏，只是合併成一次寫入。
     */
    private fun persistToday() {
        if (todayWrite?.isActive == true) return
        todayWrite = scope.launch {
            delay(TODAY_WRITE_DELAY_MS)
            writeMutex.withLock {
                val content = json.encodeToString(DayPlan.serializer(), _today.value)
                writeAtomically(dayFile, content)
            }
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()

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
        const val FILE_SESSIONS = "sessions.json"
        const val FILE_DEFAULTS = "defaults.json"
        const val FILE_DAY = "day.json"
        const val ASSET_EXERCISES = "seed_exercises.json"
        const val ASSET_DEFAULTS = "seed_defaults.json"

        /** 每日清單那筆紀錄的 id 前綴，後面接日期 */
        const val DAY_SESSION_PREFIX = "day-"

        /** 連按時合併寫檔的間隔 */
        const val TODAY_WRITE_DELAY_MS = 400L

        /** 種子欄位的版本。內建資料多了新欄位時 +1，見 migrateSeed */
        const val SEED_VERSION = 2

        const val PREFS_NAME = "routina_fit"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_EXPANDED_PARTS = "expanded_parts"
        const val KEY_SEED_VERSION = "seed_version"
        const val NONE_PART_KEY = "__none__"
    }
}
