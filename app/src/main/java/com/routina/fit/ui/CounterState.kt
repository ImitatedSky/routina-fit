package com.routina.fit.ui

import com.routina.fit.model.Measure
import com.routina.fit.model.SetLog

/**
 * 快速計數：沒有課表，就一個動作扣到 0。
 *
 * [ops] 是每一次按下去加了多少，撤銷就把最後一筆拿掉——
 * 比記「上一個數字」好，因為連按幾下 −10 之後也退得回去。
 *
 * [exerciseId] 空字串代表名字是當場打的，不在動作庫裡。
 */
data class CounterState(
    val exerciseId: String,
    val name: String,
    val part: String,
    val target: Int,
    val startedAt: Long,
    val done: Int = 0,
    val ops: List<Int> = emptyList(),
    val weightKg: Double? = null
) {
    val left: Int get() = (target - done).coerceAtLeast(0)
    val finished: Boolean get() = done >= target
    /** 超過目標的部分。做到 105 下就顯示 +5，不擋 */
    val over: Int get() = (done - target).coerceAtLeast(0)
}

fun CounterState.add(n: Int): CounterState =
    copy(done = (done + n).coerceAtLeast(0), ops = ops + n)

fun CounterState.undo(): CounterState {
    val last = ops.lastOrNull() ?: return this
    return copy(done = (done - last).coerceAtLeast(0), ops = ops.dropLast(1))
}

fun CounterState.toLog(): SetLog = SetLog(
    exerciseId = exerciseId,
    name = name,
    measure = Measure.REPS,
    target = target,
    done = done,
    weightKg = weightKg
)
