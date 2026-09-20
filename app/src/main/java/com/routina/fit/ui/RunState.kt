package com.routina.fit.ui

import com.routina.fit.model.Exercise
import com.routina.fit.model.Measure
import com.routina.fit.model.Routine
import com.routina.fit.model.SetLog
import kotlin.math.ceil

/**
 * 執行課表的狀態機。
 *
 * WORK（做這一項）→ REST（休息）→ WORK（下一項）→ … → DONE（看摘要）
 *
 * 轉移都寫成這裡的純函式，ViewModel 只負責拿著目前狀態、餵它時間、把結果放回 StateFlow。
 */
enum class RunPhase { WORK, REST, DONE }

/** 課表項目攤平成執行時要的樣子：名稱與計量方式在開始時就查好，跑的時候不再回頭找動作庫 */
data class RunItem(
    val exerciseId: String,
    val name: String,
    val part: String,
    val measure: Measure,
    val target: Int,
    val weightKg: Double?,
    val restSec: Int
)

data class RunState(
    val routineId: String,
    val routineName: String,
    val items: List<RunItem>,
    val startedAt: Long,
    val index: Int = 0,
    val phase: RunPhase = RunPhase.WORK,
    /** 計次型：目前要記下去的次數，預設帶目標，做不完就自己調 */
    val count: Int = 0,
    /** 計時型與休息：還剩幾秒 */
    val remaining: Int = 0,
    /** 倒數的目標時刻。用牆上時間算，App 被切走再回來數字才會是對的 */
    val deadlineAt: Long = 0L,
    val paused: Boolean = false,
    val weightKg: Double? = null,
    val logs: List<SetLog> = emptyList()
) {
    val current: RunItem get() = items[index]
    val next: RunItem? get() = items.getOrNull(index + 1)
    val isLast: Boolean get() = index >= items.lastIndex

    /** 這個當下需不需要每秒更新 */
    val counting: Boolean
        get() = !paused && when (phase) {
            RunPhase.REST -> true
            RunPhase.WORK -> current.measure == Measure.SECONDS
            RunPhase.DONE -> false
        }
}

/** 把課表攤平成可執行的項目。查不到的動作直接略過（動作被刪掉的舊課表） */
fun buildRunItems(routine: Routine, exercises: List<Exercise>): List<RunItem> =
    routine.items.mapNotNull { item ->
        val exercise = exercises.firstOrNull { it.id == item.exerciseId } ?: return@mapNotNull null
        RunItem(
            exerciseId = exercise.id,
            name = exercise.name,
            part = exercise.part,
            measure = exercise.measure,
            target = item.target,
            weightKg = item.weightKg ?: exercise.defaultWeightKg,
            restSec = item.restSec
        )
    }

fun startRun(routine: Routine, items: List<RunItem>, now: Long): RunState =
    RunState(
        routineId = routine.id,
        routineName = routine.name,
        items = items,
        startedAt = now
    ).enterWork(0, now)

/** 進到第 [target] 項的 WORK。計時型同時開始倒數 */
private fun RunState.enterWork(target: Int, now: Long): RunState {
    val item = items[target]
    val timed = item.measure == Measure.SECONDS
    return copy(
        index = target,
        phase = RunPhase.WORK,
        count = if (timed) 0 else item.target,
        remaining = if (timed) item.target else 0,
        deadlineAt = if (timed) now + item.target * 1000L else 0L,
        paused = false,
        weightKg = item.weightKg
    )
}

/**
 * 記下這一組、往下走。
 *
 * [done] 給 null 就用畫面上的值：計次型是 [RunState.count]，計時型是已經過去的秒數。
 * 「這個跳過」就是傳 0。
 */
fun RunState.completeSet(now: Long, done: Int? = null): RunState {
    val item = current
    val actual = done ?: if (item.measure == Measure.SECONDS) item.target - remaining else count
    val log = SetLog(
        exerciseId = item.exerciseId,
        name = item.name,
        measure = item.measure,
        target = item.target,
        done = actual.coerceAtLeast(0),
        weightKg = weightKg
    )
    val logged = copy(logs = logs + log)
    return when {
        isLast -> logged.copy(phase = RunPhase.DONE, paused = false)
        item.restSec > 0 -> logged.copy(
            phase = RunPhase.REST,
            remaining = item.restSec,
            deadlineAt = now + item.restSec * 1000L,
            paused = false
        )
        else -> logged.enterWork(index + 1, now)
    }
}

/** 休息結束（倒數歸零或按跳過） */
fun RunState.endRest(now: Long): RunState = enterWork(index + 1, now)

/** 提早結束整場訓練：把目前這一項也記進去，其餘不補 */
fun RunState.stopHere(now: Long): RunState {
    if (phase == RunPhase.DONE) return this
    // 休息中代表這一項已經記過了，不要重複記一次
    if (phase == RunPhase.REST) return copy(phase = RunPhase.DONE)
    return completeSet(now).copy(phase = RunPhase.DONE)
}

fun RunState.withCount(delta: Int): RunState = copy(count = (count + delta).coerceAtLeast(0))

/** 加減重量；減到 0 就當作沒記，回到「＋ 記重量」 */
fun RunState.adjustWeight(delta: Double): RunState {
    val next = (weightKg ?: 0.0) + delta
    return copy(weightKg = if (next <= 0.0) null else next)
}

fun RunState.setWeight(value: Double?): RunState = copy(weightKg = value)

/** 加時。暫停中也能加，回復時會從新的秒數繼續 */
fun RunState.extend(seconds: Int, now: Long): RunState {
    val next = remaining + seconds
    return copy(
        remaining = next,
        deadlineAt = if (paused) deadlineAt else now + next * 1000L
    )
}

fun RunState.togglePause(now: Long): RunState =
    if (paused) copy(paused = false, deadlineAt = now + remaining * 1000L)
    else copy(paused = true)

/**
 * 每一次心跳：用牆上時間重算剩餘秒數。
 * 沒有變化就回同一個物件，Compose 才不會被沒必要的新狀態叫醒。
 */
fun RunState.tick(now: Long): RunState {
    if (!counting) return this
    val left = ceil((deadlineAt - now) / 1000.0).toInt().coerceAtLeast(0)
    return if (left == remaining) this else copy(remaining = left)
}
