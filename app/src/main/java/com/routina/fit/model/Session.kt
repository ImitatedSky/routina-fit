package com.routina.fit.model

import kotlinx.serialization.Serializable

/**
 * 實際做完的一組。
 *
 * [name] 是當下的動作名稱快照：之後改名或把動作從庫裡刪掉，歷史紀錄都不該跟著變
 * （和 Routina Bite 的 DiaryEntry 存營養快照同一個理由）。
 */
@Serializable
data class SetLog(
    val exerciseId: String,
    val name: String,
    val measure: Measure,
    val target: Int,
    val done: Int,
    val weightKg: Double? = null
) {
    val completed: Boolean get() = done >= target
}

/**
 * 一次訓練。課表模式與快速計數共用這個結構，差別只在 [routineId]：
 * null 就是沒有課表的計數，清單裡只有一筆 [SetLog]。
 *
 * [date] 是本地日期的 yyyy-MM-dd，用來分組與排序；[startedAt] / [endedAt] 才是精確時刻。
 *
 * 下面幾個是算出來的，沒有 backing field，所以不會進 JSON。
 */
@Serializable
data class Session(
    val id: String,
    val date: String,
    val startedAt: Long,
    val endedAt: Long,
    val routineId: String? = null,
    val routineName: String = "",
    val logs: List<SetLog> = emptyList(),
    val note: String = ""
) {
    val isCounter: Boolean get() = routineId == null

    val totalReps: Int get() = logs.filter { it.measure == Measure.REPS }.sumOf { it.done }

    val totalSeconds: Int get() = logs.filter { it.measure == Measure.SECONDS }.sumOf { it.done }

    /** 訓練花掉的秒數。時鐘被往回調過就當 0，不顯示負數 */
    val durationSeconds: Int
        get() = ((endedAt - startedAt) / 1000L).coerceAtLeast(0L).toInt()
}
