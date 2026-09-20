package com.routina.fit.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 日期一律用本地日期的 yyyy-MM-dd 字串。這個格式可以直接比大小與排序，
 * 也是備份檔裡的格式，省掉一層轉換。
 */
private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayDate(): String = LocalDate.now().format(ISO)

fun parseDate(date: String): LocalDate = try {
    LocalDate.parse(date, ISO)
} catch (t: Throwable) {
    LocalDate.now()
}

/** 某個時刻落在本地的哪一天。跨午夜的訓練要算成「開始的那天」 */
fun dateOf(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(ISO)

/** 匯出檔名用的 yyyyMMdd */
fun compactToday(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

/** 本週的七天，週一到週日。今日頁那排格子用 */
fun thisWeek(): List<String> {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L..6L).map { monday.plusDays(it).format(ISO) }
}
