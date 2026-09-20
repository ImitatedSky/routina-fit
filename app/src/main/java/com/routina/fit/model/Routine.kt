package com.routina.fit.model

import kotlinx.serialization.Serializable

/**
 * 課表裡的一個項目。[target] 與 [weightKg] 會蓋掉動作的預設值，
 * 同一個動作在不同課表可以有不同目標。
 *
 * [restSec] 是「這一項做完休息幾秒」。0 表示不休息，直接進下一項——
 * 課表最後一項一律是 0，練完就該看摘要，不該再倒數一次。
 */
@Serializable
data class RoutineItem(
    val exerciseId: String,
    val target: Int,
    val weightKg: Double? = null,
    val restSec: Int = 45
)

@Serializable
data class Routine(
    val id: String,
    val name: String,
    val items: List<RoutineItem> = emptyList()
)
