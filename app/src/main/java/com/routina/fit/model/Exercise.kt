package com.routina.fit.model

import kotlinx.serialization.Serializable

/**
 * 一個動作要怎麼算完成。
 *
 * 來源是使用者原本的訓練表：多數動作記次數，開合跳寫 `35(sec)`、
 * 啞鈴棒式旋轉寫 `40(sec)`，所以計量方式必須是動作本身的屬性。
 */
@Serializable
enum class Measure { REPS, SECONDS }

/**
 * 動作庫的一筆。首次啟動從 assets/seed_exercises.json 載入。
 *
 * [defaultWeightKg] 允許為 null：徒手動作沒有重量，而且種子資料裡一律是 null——
 * 原始訓練表沒有重量欄，值要由使用者自己在第一次訓練時填。
 */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val measure: Measure = Measure.REPS,
    val part: String = "",
    val defaultTarget: Int = 12,
    val defaultWeightKg: Double? = null
)
