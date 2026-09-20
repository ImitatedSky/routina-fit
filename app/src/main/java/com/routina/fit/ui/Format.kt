package com.routina.fit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.routina.fit.R
import com.routina.fit.data.parseDate
import com.routina.fit.model.Measure
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** 倒數與時長一律 m:ss，超過一小時也只是分鐘變大，不另外做小時 */
fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.TAIWAN, "%d:%02d", safe / 60, safe % 60)
}

/** 重量：整數就不顯示小數點，12.5 仍然顯示 12.5 */
fun formatWeight(value: Double): String =
    if (abs(value - value.roundToInt()) < 0.001) value.roundToInt().toString()
    else String.format(Locale.TAIWAN, "%.1f", value)

@Composable
fun unitLabel(measure: Measure): String = when (measure) {
    Measure.REPS -> stringResource(R.string.unit_reps)
    Measure.SECONDS -> stringResource(R.string.unit_seconds)
}

@Composable
fun measureLabel(measure: Measure): String = when (measure) {
    Measure.REPS -> stringResource(R.string.measure_reps)
    Measure.SECONDS -> stringResource(R.string.measure_seconds)
}

private val dayLabel = DateTimeFormatter.ofPattern("M月d日（E）", Locale.TAIWAN)
private val dayLabelWithYear = DateTimeFormatter.ofPattern("yyyy年M月d日（E）", Locale.TAIWAN)
private val timeLabel = DateTimeFormatter.ofPattern("HH:mm", Locale.TAIWAN)

/** 不是今年的日期才帶年份，歷史列表裡跨年的日子才分得出來 */
fun formatDayLabel(date: String): String {
    val parsed = parseDate(date)
    val pattern = if (parsed.year == LocalDate.now().year) dayLabel else dayLabelWithYear
    return parsed.format(pattern)
}

fun formatTime(epochMillis: Long): String =
    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        .format(timeLabel)

/** 這一天是星期幾的單字，今日頁那排格子用 */
fun weekdayLetter(date: String): String =
    parseDate(date).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.TAIWAN)

/**
 * 中文不需要 Material 預設的字距，靠得太開會散。
 * 只在中文為主的文字上套，數字與英文維持原樣。
 */
fun TextStyle.zh(): TextStyle = copy(letterSpacing = 0.sp)
