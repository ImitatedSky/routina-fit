package com.routina.fit.model

import kotlinx.serialization.Serializable

/**
 * 今天清單上的一項：做什麼、要做幾下、已經做了幾下。
 *
 * 同一個型別也拿來存「每天的預設清單」（設定裡那一份），那時候 [done] 一律是 0。
 * 兩邊欄位完全一樣，沒有理由分成兩個型別，編輯介面也才能共用。
 *
 * [exerciseId] 空字串代表名字是當場打的，不在動作庫裡——像「核心」這種不是單一動作的項目。
 */
@Serializable
data class DayItem(
    val id: String,
    val name: String,
    val target: Int,
    val done: Int = 0,
    val exerciseId: String = ""
) {
    val hasTarget: Boolean get() = target > 0

    val reached: Boolean get() = hasTarget && done >= target

    /** 列上那條填色的長度。沒有目標就不填——不知道要做幾下，填多少都是瞎猜 */
    val progress: Float
        get() = if (hasTarget) (done.toFloat() / target).coerceIn(0f, 1f) else 0f
}

/**
 * 一天的清單。換日時由預設清單長出新的一份。
 *
 * [startedAt] 是這一天第一下按下去的時刻，0 代表今天還沒動過。
 * [settledAt] 有值就是結算過了——結算會把這一天寫成一筆紀錄，再按就是更新同一筆。
 */
@Serializable
data class DayPlan(
    val date: String,
    val items: List<DayItem> = emptyList(),
    val startedAt: Long = 0L,
    val settledAt: Long? = null
) {
    val total: Int get() = items.sumOf { it.done }

    /** 今天到底動過沒有。沒動過就不該出現在紀錄裡，也不該佔掉連續天數 */
    val touched: Boolean get() = total > 0

    val settled: Boolean get() = settledAt != null
}
