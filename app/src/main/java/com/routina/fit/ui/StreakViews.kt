package com.routina.fit.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routina.fit.R
import com.routina.fit.data.heatmapWeeks
import com.routina.fit.ui.theme.heatColors
import kotlin.math.ln

/**
 * 首頁最上面那一塊：一團火、連續天數，底下是 GitHub 那種格子圖。
 *
 * 這一塊只回答一個問題——**我最近有沒有在練**。它不可點、不是入口，
 * 所以整塊都用容器色，不跟下面的清單搶。
 *
 * 格子底下不寫「一天一格，做得越多顏色越深」這種說明：那是看圖就知道的事，
 * 寫出來只是多一行字。畫面上只留看不出來的資訊。
 */
@Composable
fun StreakCard(
    volume: Map<String, Int>,
    today: String,
    streak: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StreakFlame(streak)
                Text(
                    text = if (streak > 0) {
                        stringResource(R.string.streak_days, streak)
                    } else {
                        stringResource(R.string.streak_none)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Heatmap(
                volume = volume,
                today = today,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/**
 * 連續天數的火。大小與顏色隨天數變，天數一變就彈一下——**不是一直在燒**。
 *
 * 不做持續動畫是刻意的：首頁上那些列一個 session 會被點幾十下，旁邊有東西
 * 每一格都在重畫，既耗電又一直搶走注意力（NN/g：重複遇到的動畫是內容的路障）。
 * 實際產品也站在這一邊——Duolingo 的常駐 streak 就是圖示加數字，火焰動畫只留給里程碑。
 *
 * 用 [animateFloatAsState] 而不是 InfiniteTransition，還順手拿到系統的「移除動畫」設定：
 * Compose 會從 MotionDurationScale 讀到縮放為 0，直接跳到結束值，不必自己判斷。
 *
 * 天數走對數：連續天數沒有上限，線性映射到 100 天就大得離譜了。
 */
@Composable
fun StreakFlame(streak: Int, modifier: Modifier = Modifier) {
    val heat = (ln(1f + streak) / ln(1f + STREAK_FULL)).coerceIn(0f, 1f)
    val colors = heatColors()

    // 讀 state 的 modifier 一律用 lambda 版：縮放只跑 draw，不要每一格都重組
    val scale = animateFloatAsState(
        targetValue = if (streak == 0) 1f else 1f + heat * 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "streak-scale"
    )

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            // 視覺強度不是唯一的資訊通道：天數也要唸得出來
            contentDescription = if (streak > 0) {
                stringResource(R.string.streak_days, streak)
            } else {
                stringResource(R.string.streak_none)
            },
            tint = if (streak == 0) {
                // M3 的停用強度。沒在連的時候，差別靠「灰掉」比靠大小更看得出來
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            } else {
                colors.flame(heat)
            },
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}

/**
 * GitHub 貢獻圖那種格子。一週一欄、週一在最上面，最後一欄是本週，
 * 所以今天永遠落在右邊。
 *
 * **欄數與格子大小由寬度決定，不是寫死的**：先算這個寬度放得下幾欄，
 * 再把剩下的寬度平分回每一格。任何螢幕寬度都剛好用完，不會留半格也不會被切掉。
 *
 * 整張用一個 Canvas 畫。一百多個格子如果各自是一個 composable，
 * 光是量測就夠讓首頁掉格，而這些格子完全不需要互動。
 */
@Composable
fun Heatmap(
    volume: Map<String, Int>,
    today: String,
    modifier: Modifier = Modifier
) {
    val colors = heatColors()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val weeks = ((maxWidth + GAP) / (CELL_TARGET + GAP)).toInt().coerceAtLeast(1)
        val cell = (maxWidth - GAP * (weeks - 1)) / weeks
        val grid = remember(volume, weeks, today) { heatmapWeeks(volume, weeks) }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(cell * 7 + GAP * 6)
        ) {
            val cellPx = cell.toPx()
            val gapPx = GAP.toPx()
            val radius = CornerRadius(cellPx * 0.28f)
            val size = Size(cellPx, cellPx)

            grid.forEachIndexed { column, days ->
                days.forEachIndexed { row, day ->
                    // 還沒到的日子不畫。空格比「畫了一個不一樣的空格」更好讀
                    if (day.future) return@forEachIndexed
                    val offset = Offset(column * (cellPx + gapPx), row * (cellPx + gapPx))
                    drawRoundRect(
                        color = colors.level(day.level),
                        topLeft = offset,
                        size = size,
                        cornerRadius = radius
                    )
                    // 今天框起來，不然一百多個格子裡找不到自己在哪
                    if (day.isToday) {
                        drawRoundRect(
                            color = colors.marker,
                            topLeft = offset,
                            size = size,
                            cornerRadius = radius,
                            style = Stroke(width = TODAY_RING.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * 一週的格子寬度目標，實際寬度會依螢幕調整。
 *
 * 11dp 在 411dp 寬的螢幕上大約是 27 欄（半年），整張圖高度 96dp——
 * 和一列 72dp 差不多。這一塊是回顧，不該長得比今天要做的事還大。
 */
private val CELL_TARGET = 11.dp
private val GAP = 3.dp
private val TODAY_RING = 1.5.dp

/** 火燒到最大的天數。再多也只是維持 */
private const val STREAK_FULL = 100f
