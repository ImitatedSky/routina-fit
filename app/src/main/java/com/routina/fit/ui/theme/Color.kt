package com.routina.fit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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

/**
 * 訓練量的色階：格子圖的五階深淺，加上那團火。
 *
 * 色相是琥珀，不是 GitHub 的綠——這個 App 的綠已經是「休息」了，同一個顏色不兼差。
 * 琥珀離靛藍（正在做）與紅（沒做滿）也都夠遠，而且差異落在明度與藍黃軸上，
 * 紅綠色盲照樣分得出來。
 *
 * **第 0 級（那天沒練）用中性灰，不是最淡的琥珀**：有練和沒練是兩種東西，
 * 不是同一條梯子的最底層。GitHub 也是這樣分的。
 *
 * 亮色由淺到深、深色由深到亮——兩邊都是「越明顯＝做越多」，但方向相反，
 * 因為底色一個是白的一個是黑的。
 */
@Composable
fun heatColors(): HeatColors = if (isSystemInDarkTheme()) DarkHeat else LightHeat

@Immutable
class HeatColors(
    private val levels: List<Color>,
    private val flameLow: Color,
    private val flameHigh: Color,
    /** 今天那一格的外框，整張圖裡找得到自己 */
    val marker: Color
) {
    fun level(index: Int): Color = levels[index.coerceIn(0, levels.lastIndex)]

    /** 火的顏色。[heat] 0 = 剛開始連，1 = 連滿 */
    fun flame(heat: Float): Color = lerp(flameLow, flameHigh, heat.coerceIn(0f, 1f))
}

private val LightHeat = HeatColors(
    levels = listOf(N.t94, Heat.t88, Heat.t76, Heat.t64, Heat.t52),
    flameLow = Heat.t64,
    flameHigh = Heat.t52,
    marker = Heat.t52
)

private val DarkHeat = HeatColors(
    levels = listOf(N.t22, Heat.t28, Heat.t40, Heat.t52, Heat.t64),
    flameLow = Heat.t64,
    flameHigh = Heat.t88,
    marker = Heat.t88
)
