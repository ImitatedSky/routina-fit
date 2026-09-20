package com.routina.fit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * 給中文用的字級表。三件事與 Material 預設不同，都是裝置實測過的：
 *
 * 1. **字重封頂在 Medium(500)**。Android 14 以前的 zh-Hant 字體只宣告 weight 400，
 *    SemiBold(600) 與 Bold(700) 會觸發 Minikin 的合成假粗（演算法塗抹筆畫），
 *    看起來就是糊的。層級改用字級與顏色深淺做，不用字重。
 * 2. **letterSpacing = 0**。Material 的預設字距是給拉丁字母的，中文會散開。
 * 3. **includeFontPadding = false ＋ 行高置中**。不關掉的話中文會在行框裡偏下。
 *
 * 數字是拉丁字形，不受假粗問題影響，所以大數字另外用 [bigNumber]。
 */
private fun cjk(
    sizeSp: Int,
    weight: FontWeight = FontWeight.Normal,
    lineHeightRatio: Float = 1.5f
) = TextStyle(
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * lineHeightRatio).sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

/** 大數字（倒數、次數）。拉丁字形，Bold 是真的粗；行高收緊，不然上下會空一大塊 */
fun bigNumber(sizeSp: Int) = TextStyle(
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 1.05f).sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.02f * sizeSp).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

val FitTypography = Typography(
    headlineMedium = cjk(26, FontWeight.Medium, 1.35f),
    headlineSmall = cjk(22, FontWeight.Medium, 1.35f),

    titleLarge = cjk(21, FontWeight.Medium, 1.4f),
    titleMedium = cjk(16, FontWeight.Medium),
    titleSmall = cjk(15, FontWeight.Medium),

    bodyLarge = cjk(16),
    bodyMedium = cjk(14),
    bodySmall = cjk(13),

    labelLarge = cjk(14, FontWeight.Medium),
    labelMedium = cjk(12, FontWeight.Medium),
    labelSmall = cjk(11, FontWeight.Medium)
)
