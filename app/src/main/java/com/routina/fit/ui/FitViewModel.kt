package com.routina.fit.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.routina.fit.FitApp
import com.routina.fit.data.BackupCodec
import com.routina.fit.model.DayItem
import com.routina.fit.model.DayPlan
import com.routina.fit.model.Exercise
import com.routina.fit.model.FitSettings
import com.routina.fit.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** 匯入的結果：成功時回目前的總筆數，失敗時回拒收原因 */
sealed interface ImportOutcome {
    data class Ok(val exercises: Int, val sessions: Int) : ImportOutcome
    data class Failed(val reason: BackupCodec.Reject) : ImportOutcome
}

class FitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FitApp).repository
    private val cues = Cues(application)

    val exercises: StateFlow<List<Exercise>> = repository.exercises
    val sessions: StateFlow<List<Session>> = repository.sessions
    val settings: StateFlow<FitSettings> = repository.settings
    val expandedParts: StateFlow<Set<String>> = repository.expandedParts
    val today: StateFlow<DayPlan> = repository.today
    val defaults: StateFlow<List<DayItem>> = repository.defaults

    /** 剛結算了幾下。首頁靠它講一次「已記下來」，講完就清掉 */
    private val _settled = MutableStateFlow<Int?>(null)
    val settled: StateFlow<Int?> = _settled.asStateFlow()

    // ---------- 今天的清單 ----------

    /** 回到前景時叫一次。App 開著過午夜，手上那份就不是今天的了 */
    fun ensureToday() = repository.ensureToday()

    /** 按一下就加一下。[delta] 給負數就是扣回去 */
    fun count(itemId: String, delta: Int) {
        val before = today.value.items.firstOrNull { it.id == itemId }
        val after = repository.countItem(itemId, delta) ?: return
        // 跨過目標的那一下響一聲，之後繼續加不再響
        if (after.reached && before?.reached == false) {
            val prefs = settings.value
            cues.done(prefs.sound, prefs.vibrate)
        }
    }

    fun addTodayItem(name: String, target: Int, exerciseId: String = "") =
        repository.addTodayItem(name, target, exerciseId)

    fun updateTodayItem(item: DayItem) = repository.updateTodayItem(item)

    fun deleteTodayItem(id: String) = repository.deleteTodayItem(id)

    /** 結算：把今天寫成一筆紀錄。沒動過就不記，也不報告 */
    fun settleToday() {
        _settled.value = repository.settleToday().takeIf { it > 0 }
    }

    fun clearSettled() {
        _settled.value = null
    }

    // ---------- 預設清單 ----------

    fun setDefaults(items: List<DayItem>) = repository.setDefaults(items)

    /** 設定頁的「用今天這份當預設」。次數歸零，存的是要做什麼、做幾下 */
    fun saveTodayAsDefaults() =
        repository.setDefaults(today.value.items.map { it.copy(done = 0) })

    // ---------- 動作庫 ----------

    fun findExercise(id: String): Exercise? = repository.findExercise(id)

    fun saveExercise(exercise: Exercise) = repository.upsertExercise(exercise)

    fun deleteExercise(id: String) = repository.deleteExercise(id)

    fun setPartExpanded(part: String, expanded: Boolean) = repository.setPartExpanded(part, expanded)

    // ---------- 紀錄 ----------

    fun findSession(id: String): Session? = repository.findSession(id)

    fun deleteSession(id: String) = repository.deleteSession(id)

    // ---------- 設定 ----------

    fun setSettings(value: FitSettings) = repository.setSettings(value)

    // ---------- 備份 ----------

    fun exportBackup(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val content = BackupCodec.encode(repository.buildBackup())
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                        ?: error("no output stream")
                }.isSuccess
            }
            onDone(ok)
        }
    }

    fun importBackup(uri: Uri, onDone: (ImportOutcome) -> Unit) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.bufferedReader().readText() }
                }.getOrNull()
            }
            if (text == null) {
                onDone(ImportOutcome.Failed(BackupCodec.Reject.UNREADABLE))
                return@launch
            }
            when (val parsed = BackupCodec.parse(text)) {
                is BackupCodec.Parsed.Failed -> onDone(ImportOutcome.Failed(parsed.reason))
                is BackupCodec.Parsed.Ok -> {
                    repository.applyBackup(parsed.backup)
                    onDone(
                        ImportOutcome.Ok(
                            exercises = exercises.value.size,
                            sessions = sessions.value.size
                        )
                    )
                }
            }
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    override fun onCleared() {
        super.onCleared()
        cues.release()
    }
}
