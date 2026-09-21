package com.routina.fit.ui

import com.routina.fit.model.Measure
import com.routina.fit.model.SetLog

/**
 * 計數：沒有課表，就一個動作往上累加。
 *
 * **數字是往上加的，目標只是參考**。本來想做 20 下、做一做做了 40 下，那就是 40 下——
 * 不該還在跟你講「還剩下」。[target] 給 0 就是完全沒有目標，純粹做一下按一下。
 *
 * [ops] 是每一次按下去加了多少，撤銷就把最後一筆拿掉——
 * 比記「上一個數字」好，因為連按幾下 +10 之後也退得回去。
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
    val hasTarget: Boolean get() = target > 0

    /** 還差幾下才到目標。沒有目標或已經超過就是 0 */
    val remaining: Int get() = if (hasTarget) (target - done).coerceAtLeast(0) else 0

    val reachedTarget: Boolean get() = hasTarget && done >= target

    /** 超過目標的部分。做到 40 下、目標 20，就是 20 */
    val over: Int get() = if (hasTarget) (done - target).coerceAtLeast(0) else 0

    /** 進度條用。沒有目標就不畫 */
    val progress: Float
        get() = if (hasTarget) (done.toFloat() / target).coerceIn(0f, 1f) else 0f
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
    // 沒有目標就存 0，紀錄那邊會顯示成「40 次」而不是「40 / 0 次」
    target = target,
    done = done,
    weightKg = weightKg
)
