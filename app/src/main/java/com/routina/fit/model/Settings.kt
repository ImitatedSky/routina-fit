package com.routina.fit.model

import kotlinx.serialization.Serializable

/**
 * 全 App 的偏好設定，存在 SharedPreferences（不是 JSON 檔），
 * 但會一起放進備份，換手機才不用重設。
 */
@Serializable
data class FitSettings(
    val defaultRestSec: Int = 45,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true
)
