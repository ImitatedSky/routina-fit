package com.routina.fit.data

import com.routina.fit.model.Session
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** 格子圖的一格。[level] 0 = 這天沒練，1–4 是由淺到深 */
data class HeatDay(
    val date: String,
    val volume: Int,
    val level: Int,
    val isToday: Boolean,
    val future: Boolean
)

/**
 * 每一天的訓練量，拿來決定格子的深淺。
 *
 * 值是次數，但**有練過的那一天至少算 1**：棒式、開合跳這些計時動作一次也沒有「次」，
 * 練了四十秒卻留一格空白是錯的。所以這個值讀作「強度」而不是字面上的次數。
 */
fun dailyVolume(sessions: List<Session>): Map<String, Int> =
    sessions.groupBy { it.date }
        .mapValues { (_, list) -> list.sumOf { it.totalReps }.coerceAtLeast(1) }

/**
 * 把今天還沒結算的進度疊上去。按下第一下格子就該亮起來，不必等結算——
 * 首頁同時是計數的地方，自己做過的事卻看不到，會以為沒記到。
 */
fun plusToday(volume: Map<String, Int>, date: String, total: Int): Map<String, Int> =
    if (total <= 0) volume else volume + (date to maxOf(volume[date] ?: 0, total))

/**
 * 從今天往回算 [weeks] 週的格子，一週一欄、週一在最上面，最後一欄是本週。
 * 本週還沒到的日子也給格子（[HeatDay.future]），少了它們最後一欄會參差不齊。
 *
 * 深淺是相對於這段期間自己的最大值，不是絕對次數——每個人的量差很多，
 * 用固定門檻不是全滿就是全空。作法與 GitHub 的貢獻圖一致。
 */
fun heatmapWeeks(
    volume: Map<String, Int>,
    weeks: Int,
    today: LocalDate = LocalDate.now()
): List<List<HeatDay>> {
    if (weeks <= 0) return emptyList()
    val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val firstMonday = thisMonday.minusWeeks((weeks - 1).toLong())
    val iso = DateTimeFormatter.ISO_LOCAL_DATE

    val peak = (0 until weeks * 7)
        .map { firstMonday.plusDays(it.toLong()).format(iso) }
        .mapNotNull { volume[it] }
        .maxOrNull()
        ?: 0

    return (0 until weeks).map { week ->
        (0 until 7).map { day ->
            val date = firstMonday.plusWeeks(week.toLong()).plusDays(day.toLong())
            val key = date.format(iso)
            val value = volume[key] ?: 0
            HeatDay(
                date = key,
                volume = value,
                level = heatLevel(value, peak),
                isToday = date == today,
                future = date.isAfter(today)
            )
        }
    }
}

/**
 * 訓練量換成 0–4 級。有練就至少 1 級，等於這段期間的最大值才是 4 級，
 * 中間照比例分三段。
 */
fun heatLevel(value: Int, peak: Int): Int = when {
    value <= 0 -> 0
    peak <= 0 -> 1
    value >= peak -> 4
    else -> 1 + (3 * value / peak)
}

/**
 * 連續練了幾天。
 *
 * **今天還沒練不算中斷**——早上打開 App 看到連續天數歸零，會以為紀錄掉了。
 * 所以今天沒練就從昨天開始數；真的斷了一天，隔天才會看到歸零。
 */
fun currentStreak(volume: Map<String, Int>, today: LocalDate = LocalDate.now()): Int {
    val iso = DateTimeFormatter.ISO_LOCAL_DATE
    fun trained(date: LocalDate) = (volume[date.format(iso)] ?: 0) > 0

    var cursor = if (trained(today)) today else today.minusDays(1)
    var days = 0
    while (trained(cursor)) {
        days++
        cursor = cursor.minusDays(1)
    }
    return days
}
