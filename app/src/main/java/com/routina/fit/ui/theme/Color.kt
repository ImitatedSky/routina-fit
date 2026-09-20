package com.routina.fit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 訓練狀態色。全 App 只有三個顏色帶意思，各自只講一件事：
 *
 * - **正在做** = 主色。執行畫面那顆按鈕本來就是整頁唯一的主要動作，用主色是它的本分。
 * - **休息** = 綠。與主色同一個明度階，兩個狀態讀起來才是對等的。
 * - **沒做滿** = 錯誤色。紅色不兼差做別的事。
 *
 * v0.2.0 的「正在做」是磚紅，和錯誤紅太近，同一個畫面用兩種紅講不同的事——改掉了。
 *
 * 休息時整頁換底色，是為了手機放在一公尺外也能瞄一眼就知道現在該動還是該喘。
 * 但「整片」指的是很淡的底色：透明度 8%（亮）／14%（暗），這個強度在視覺研究裡
 * 與 2dp 線條等效，卻安靜四倍。飽和填色一頁只留一個。
 */
@Composable
fun workColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun restColor(): Color = if (isSystemInDarkTheme()) Rest.t80 else Rest.t40

/** 整頁底色用的透明度。亮色要更淡，深色底下太淡就看不出來 */
@Composable
fun tintAlpha(): Float = if (isSystemInDarkTheme()) 0.14f else 0.08f
