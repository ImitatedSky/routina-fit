package com.routina.fit.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.routina.fit.R
import com.routina.fit.model.DayItem
import com.routina.fit.ui.theme.bigNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Reveal { CLOSED, ACTIONS }

/**
 * 今天清單上的一列，也是這個 App 主要的互動：
 *
 * - **點一下加一下**。整列都是按鈕，不是列尾一顆小小的 ＋——做完一下看都不用看就能按到。
 * - **長按連續加**。一次做二十下才想到要記，一下一下點二十次太蠢。
 * - **往左滑**露出編輯與刪除。這兩件事一天用不到一次，不該常駐在列上佔走點擊面積。
 *
 * 進度不是另外畫一條進度條，而是**整列自己填滿**：列本身就是進度，
 * 填到滿就是做完了。一頁十列的話，十條進度條會比十列文字還吵。
 */
@Composable
fun DayItemRow(
    item: DayItem,
    onCount: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val revealPx = with(LocalDensity.current) { REVEAL_WIDTH.toPx() }

    val drag = remember(revealPx) {
        AnchoredDraggableState(
            initialValue = Reveal.CLOSED,
            anchors = DraggableAnchors {
                Reveal.CLOSED at 0f
                Reveal.ACTIONS at -revealPx
            }
        )
    }
    // 已經滑開的時候點整列是「收回去」，不是再加一下——不然想關掉反而多算一次。
    // 這個值**不能當 pointerInput 的 key**：滑到一半它就會翻面，手勢偵測器會在手指還按著的
    // 時候被重啟，onPress 裡的收尾就跑不到了。所以改成按下去的當下才讀。
    fun revealed() = drag.targetValue == Reveal.ACTIONS

    fun close() {
        scope.launch { drag.animateTo(Reveal.CLOSED) }
    }

    val fill = MaterialTheme.colorScheme.primaryContainer
    val progress = animateFloatAsState(
        targetValue = item.progress,
        animationSpec = tween(FILL_MS),
        label = "fill"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(ROW_HEIGHT)
                .fillMaxWidth()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RevealButton(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.action_edit),
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                close()
                onEdit()
            }
            RevealButton(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.action_delete),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                close()
                onDelete()
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(drag.offsetOrZero().roundToInt(), 0) }
                .anchoredDraggable(drag, Orientation.Horizontal)
                .pointerInput(item.id) {
                    detectTapGestures(
                        onPress = {
                            if (revealed()) return@detectTapGestures
                            // 連加跑在 row 自己的 scope 上，不是手勢的 scope——手勢結束它才停，
                            // 所以**一定要 finally**：滑動接手時這裡是被取消的，
                            // 取消會跳過後面的程式碼，少了 finally 就會一直加下去。
                            val repeat = scope.launch {
                                delay(REPEAT_START_MS)
                                while (true) {
                                    onCount(1)
                                    delay(REPEAT_MS)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeat.cancel()
                            }
                        },
                        // 空的，但不能拿掉：有 onLongPress 時長按放開才不會再補一次 onTap
                        onLongPress = { },
                        onTap = {
                            if (revealed()) {
                                close()
                            } else {
                                // 鍵盤按鍵的那種回饋。這一列的作用就是一顆會被連按幾十下的鍵
                                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                onCount(1)
                            }
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .height(ROW_HEIGHT)
                    .fillMaxWidth()
                    .drawBehind {
                        val width = size.width * progress.value
                        if (width > 0f) drawRect(color = fill, size = Size(width, size.height))
                    }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.reached) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp)
                    )
                }
                Row {
                    Text(
                        text = item.done.toString(),
                        style = bigNumber(COUNT_SP),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignByBaseline()
                    )
                    if (item.hasTarget) {
                        Text(
                            text = " / " + item.target,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevealButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, tint = content)
        }
    }
}

/** 錨點是在建構時就給的，所以這裡拿得到值；NaN 只會出現在還沒量到寬度的那一瞬間 */
private fun AnchoredDraggableState<Reveal>.offsetOrZero(): Float =
    offset.takeIf { !it.isNaN() } ?: 0f

private val ROW_HEIGHT = 72.dp

/** 滑開後露出來的寬度：兩顆 44dp 加上間距 */
private val REVEAL_WIDTH = 100.dp

/** 長按多久開始連加。要和系統的長按門檻差不多，不然會和 onTap 打架 */
private const val REPEAT_START_MS = 500L
private const val REPEAT_MS = 110L
private const val FILL_MS = 180
private const val COUNT_SP = 24
