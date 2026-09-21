package com.routina.fit.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.routina.fit.FitApp
import com.routina.fit.data.BackupCodec
import com.routina.fit.data.dateOf
import com.routina.fit.model.Exercise
import com.routina.fit.model.FitSettings
import com.routina.fit.model.Routine
import com.routina.fit.model.RoutineItem
import com.routina.fit.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** 匯入的結果：成功時回目前的總筆數，失敗時回拒收原因 */
sealed interface ImportOutcome {
    data class Ok(val exercises: Int, val routines: Int, val sessions: Int) : ImportOutcome
    data class Failed(val reason: BackupCodec.Reject) : ImportOutcome
}

class FitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FitApp).repository
    private val cues = Cues(application)

    val exercises: StateFlow<List<Exercise>> = repository.exercises
    val routines: StateFlow<List<Routine>> = repository.routines
    val sessions: StateFlow<List<Session>> = repository.sessions
    val settings: StateFlow<FitSettings> = repository.settings
    val expandedParts: StateFlow<Set<String>> = repository.expandedParts

    private val _run = MutableStateFlow<RunState?>(null)
    val run: StateFlow<RunState?> = _run.asStateFlow()

    private val _counter = MutableStateFlow<CounterState?>(null)
    val counter: StateFlow<CounterState?> = _counter.asStateFlow()

    private var ticker: Job? = null

    // ---------- 執行課表 ----------

    /** 開始練一張課表。課表沒有項目就不開始，回 false */
    fun startRoutine(routineId: String): Boolean {
        val routine = repository.findRoutine(routineId) ?: return false
        val items = buildRunItems(routine, exercises.value)
        if (items.isEmpty()) return false
        _run.value = startRun(routine, items, System.currentTimeMillis())
        ensureTicker()
        return true
    }

    fun adjustCount(delta: Int) = updateRun { it.withCount(delta) }

    fun adjustWeight(delta: Double) = updateRun { it.adjustWeight(delta) }

    fun setWeight(value: Double?) = updateRun { it.setWeight(value) }

    /** 完成這一組。[done] 給 0 就是「這個跳過」 */
    fun finishSet(done: Int? = null) = updateRun { it.completeSet(System.currentTimeMillis(), done) }

    fun skipRest() = updateRun { it.endRest(System.currentTimeMillis()) }

    fun extendTimer(seconds: Int) = updateRun { it.extend(seconds, System.currentTimeMillis()) }

    fun togglePause() = updateRun { it.togglePause(System.currentTimeMillis()) }

    /** 中途結束：目前這一項也算進去，直接跳到摘要 */
    fun stopRun() = updateRun { it.stopHere(System.currentTimeMillis()) }

    fun saveRun() {
        val state = _run.value ?: return
        if (state.logs.isNotEmpty()) {
            repository.addSession(
                Session(
                    id = newId(),
                    date = dateOf(state.startedAt),
                    startedAt = state.startedAt,
                    endedAt = System.currentTimeMillis(),
                    routineId = state.routineId,
                    routineName = state.routineName,
                    logs = state.logs
                )
            )
        }
        discardRun()
    }

    fun discardRun() {
        ticker?.cancel()
        ticker = null
        _run.value = null
    }

    private fun updateRun(transform: (RunState) -> RunState) {
        val current = _run.value ?: return
        _run.value = transform(current)
        ensureTicker()
    }

    /**
     * 倒數的心跳。只有真的在倒數時才跑，狀態一離開倒數就自己收工；
     * 下一次進入倒數的異動會再把它叫起來。
     */
    private fun ensureTicker() {
        if (_run.value?.counting != true) return
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val state = _run.value ?: return@launch
                if (!state.counting) return@launch

                val now = System.currentTimeMillis()
                val ticked = state.tick(now)
                if (ticked === state) continue

                val prefs = settings.value
                when {
                    ticked.remaining == 0 -> cues.done(prefs.sound, prefs.vibrate)
                    ticked.remaining <= LAST_SECONDS -> cues.tick(prefs.sound)
                }

                _run.value = if (ticked.remaining > 0) {
                    ticked
                } else if (ticked.phase == RunPhase.REST) {
                    ticked.endRest(now)
                } else {
                    // 計時型做完，已完成的秒數就是目標
                    ticked.completeSet(now)
                }
            }
        }
    }

    // ---------- 快速計數 ----------

    fun startCounter(name: String, target: Int, exerciseId: String = "", part: String = "") {
        _counter.value = CounterState(
            exerciseId = exerciseId,
            name = name.trim(),
            part = part,
            target = target,
            startedAt = System.currentTimeMillis()
        )
    }

    fun count(n: Int) {
        val current = _counter.value ?: return
        val next = current.add(n)
        // 跨過目標的那一下響一聲，之後繼續加不再響。沒有目標就一直安靜
        if (next.reachedTarget && !current.reachedTarget) {
            val prefs = settings.value
            cues.done(prefs.sound, prefs.vibrate)
        }
        _counter.value = next
    }

    fun undoCount() {
        _counter.value = _counter.value?.undo()
    }

    fun saveCounter() {
        val state = _counter.value ?: return
        if (state.done > 0) {
            repository.addSession(
                Session(
                    id = newId(),
                    date = dateOf(state.startedAt),
                    startedAt = state.startedAt,
                    endedAt = System.currentTimeMillis(),
                    routineId = null,
                    routineName = state.name,
                    logs = listOf(state.toLog())
                )
            )
        }
        _counter.value = null
    }

    fun discardCounter() {
        _counter.value = null
    }

    // ---------- 動作庫 ----------

    fun findExercise(id: String): Exercise? = repository.findExercise(id)

    fun saveExercise(exercise: Exercise) = repository.upsertExercise(exercise)

    fun deleteExercise(id: String) = repository.deleteExercise(id)

    fun setPartExpanded(part: String, expanded: Boolean) = repository.setPartExpanded(part, expanded)

    // ---------- 課表 ----------

    fun findRoutine(id: String): Routine? = repository.findRoutine(id)

    fun saveRoutine(routine: Routine) = repository.upsertRoutine(routine)

    fun deleteRoutine(id: String) = repository.deleteRoutine(id)

    /** 加一個動作到課表尾巴，目標與重量帶動作的預設值，休息帶設定裡的預設 */
    fun routineItemFor(exercise: Exercise): RoutineItem = RoutineItem(
        exerciseId = exercise.id,
        target = exercise.defaultTarget,
        weightKg = exercise.defaultWeightKg,
        restSec = settings.value.defaultRestSec
    )

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
                            routines = routines.value.size,
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
        ticker?.cancel()
        cues.release()
    }

    private companion object {
        /** 比一秒短，畫面上的秒數才會準時翻 */
        const val TICK_MS = 200L

        /** 倒數剩幾秒開始嘟 */
        const val LAST_SECONDS = 3
    }
}
