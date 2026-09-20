package com.routina.fit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 每一個角色都給值，一個都不留白。
 *
 * Material 的 `lightColorScheme()` 對沒指定的角色會沿用內建基線，而內建基線是**紫色**——
 * v0.2.0 只寫了十幾個角色，於是分頁列、選取晶片、進度條軌道全是紫的，和家族的靛藍打架。
 * 對照表就是 M3 的標準映射（亮色 40/90/10、深色 80/30/90），值來自 [P] 等色階。
 *
 * 版面上的用法：[surface] 是卡片（亮色白、深色比底稍亮），[background] 是頁面本身。
 * 兩者差一階就夠，卡片不再需要外框或陰影來證明自己是一塊。
 */
private val LightColors = lightColorScheme(
    primary = P.t40,
    onPrimary = P.t100,
    primaryContainer = P.t90,
    onPrimaryContainer = P.t10,
    inversePrimary = P.t80,

    secondary = S.t40,
    onSecondary = S.t100,
    secondaryContainer = S.t90,
    onSecondaryContainer = S.t10,

    tertiary = T.t40,
    onTertiary = T.t100,
    tertiaryContainer = T.t90,
    onTertiaryContainer = T.t10,

    error = E.t40,
    onError = E.t100,
    errorContainer = E.t90,
    onErrorContainer = E.t10,

    background = N.t97,
    onBackground = N.t10,
    surface = N.t100,
    onSurface = N.t10,
    surfaceVariant = NV.t92,
    onSurfaceVariant = NV.t30,
    surfaceTint = P.t40,

    inverseSurface = N.t20,
    inverseOnSurface = N.t95,

    outline = NV.t50,
    outlineVariant = NV.t80,
    scrim = N.t0,

    surfaceBright = N.t98,
    surfaceDim = N.t87,
    surfaceContainerLowest = N.t100,
    surfaceContainerLow = N.t96,
    surfaceContainer = N.t94,
    surfaceContainerHigh = N.t92,
    surfaceContainerHighest = N.t90
)

private val DarkColors = darkColorScheme(
    primary = P.t80,
    onPrimary = P.t20,
    primaryContainer = P.t30,
    onPrimaryContainer = P.t90,
    inversePrimary = P.t40,

    secondary = S.t80,
    onSecondary = S.t20,
    secondaryContainer = S.t30,
    onSecondaryContainer = S.t90,

    tertiary = T.t80,
    onTertiary = T.t20,
    tertiaryContainer = T.t30,
    onTertiaryContainer = T.t90,

    error = E.t80,
    onError = E.t20,
    errorContainer = E.t30,
    onErrorContainer = E.t90,

    background = N.t6,
    onBackground = N.t90,
    // 深色時卡片要比頁面亮，不是暗——亮色是靠白色浮起來，深色是靠亮度浮起來
    surface = N.t12,
    onSurface = N.t90,
    surfaceVariant = NV.t30,
    onSurfaceVariant = NV.t80,
    surfaceTint = P.t80,

    inverseSurface = N.t90,
    inverseOnSurface = N.t20,

    outline = NV.t60,
    outlineVariant = NV.t30,
    scrim = N.t0,

    surfaceBright = N.t24,
    surfaceDim = N.t6,
    surfaceContainerLowest = N.t4,
    surfaceContainerLow = N.t10,
    surfaceContainer = N.t12,
    surfaceContainerHigh = N.t17,
    surfaceContainerHighest = N.t22
)

@Composable
fun FitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FitTypography,
        content = content
    )
}
