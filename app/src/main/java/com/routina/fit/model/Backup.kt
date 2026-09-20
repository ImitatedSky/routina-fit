package com.routina.fit.model

import kotlinx.serialization.Serializable

/**
 * 整包備份，一個 JSON 檔就能把 App 還原回來。
 *
 * [schemaVersion] 只在格式真的不相容時才往上加。新增有預設值的欄位不算不相容，
 * 舊備份照樣讀得進來。
 */
@Serializable
data class FitBackup(
    val schemaVersion: Int = 1,
    val app: String = "fit",
    val exportedAt: Long,
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val settings: FitSettings? = null
)
