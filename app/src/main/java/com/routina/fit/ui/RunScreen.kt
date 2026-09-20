package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.model.Measure
import com.routina.fit.ui.theme.restColor
import com.routina.fit.ui.theme.workColor

/**
 * 執行課表。三種狀態共用這一頁：做計次的、做計時的、休息。
 *
 * 休息時整片換成綠色，是為了手機放在一公尺外也能一眼分辨現在該動還是該喘。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    viewModel: FitViewModel,
    onDone: () -> Unit,
    onQuit: () -> Unit
) {
    val state by viewModel.run.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val current = state
    // 沒有進行中的訓練（例如 App 被系統回收後回到這一頁）就退回首頁
    LaunchedEffect(current == null) {
        if (current == null) onQuit()
    }
    if (current == null) return

    LaunchedEffect(current.phase) {
        if (current.phase == RunPhase.DONE) onDone()
    }
    if (current.phase == RunPhase.DONE) return

    KeepScreenOn(settings.keepScreenOn)

    var confirmStop by remember { mutableStateOf(false) }
    var editingWeight by remember { mutableStateOf(false) }

    val resting = current.phase == RunPhase.REST
    val accent = if (resting) restColor() else workColor()

    Scaffold(
        // 休息的底色交給 Scaffold，才會連底部按鈕列一起換；
        // 只染內容區的話中間會出現一條接縫
        containerColor = if (resting) {
            accent.copy(alpha = REST_TINT)
        } else {
            MaterialTheme.colorScheme.background
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = current.routineName,
                            style = MaterialTheme.typography.titleMedium.zh()
                        )
                        Text(
                            text = stringResource(
                                R.string.run_progress,
                                current.index + 1,
                                current.items.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { confirmStop = true }) {
                        Icon(Icons.Default.Close, stringResource(R.string.run_stop))
                    }
                }
            )
        },
        bottomBar = {
            RunControls(
                state = current,
                accent = accent,
                onFinish = { viewModel.finishSet() },
                onSkipItem = { viewModel.finishSet(done = 0) },
                onSkipRest = { viewModel.skipRest() },
                onExtend = { viewModel.extendTimer(EXTEND_SECONDS) },
                onTogglePause = { viewModel.togglePause() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = { (current.index + 1f) / current.items.size },
                color = accent,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (resting) {
                    RestBody(state = current, accent = accent)
                } else {
                    WorkBody(
                        state = current,
                        accent = accent,
                        onCount = { viewModel.adjustCount(it) },
                        onWeight = { viewModel.adjustWeight(it) },
                        onEditWeight = { editingWeight = true }
                    )
                }
            }
        }
    }

    if (confirmStop) {
        ConfirmDialog(
            title = stringResource(R.string.run_stop_title),
            message = stringResource(R.string.run_stop_message),
            confirmLabel = stringResource(R.string.run_stop),
            onConfirm = { viewModel.stopRun() },
            onDismiss = { confirmStop = false }
        )
    }

    if (editingWeight) {
        WeightDialog(
            title = current.current.name,
            current = current.weightKg,
            onConfirm = { viewModel.setWeight(it) },
            onDismiss = { editingWeight = false }
        )
    }
}

@Composable
private fun WorkBody(
    state: RunState,
    accent: Color,
    onCount: (Int) -> Unit,
    onWeight: (Double) -> Unit,
    onEditWeight: () -> Unit
) {
    val item = state.current
    val timed = item.measure == Measure.SECONDS

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (item.part.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = item.part,
                    style = MaterialTheme.typography.labelMedium.zh(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineSmall.zh(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (timed) {
            BigNumber(text = state.remaining.toString(), color = accent)
            Text(
                text = stringResource(R.string.unit_seconds) + "　" +
                    stringResource(R.string.run_remaining),
                style = MaterialTheme.typography.bodyMedium.zh(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // 中間這個數字就是要記下去的次數，做不完就按 − 改成實際做到的數
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedIconButton(
                    onClick = { onCount(-1) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Remove, stringResource(R.string.run_less))
                }
                BigNumber(text = state.count.toString(), color = MaterialTheme.colorScheme.onBackground)
                OutlinedIconButton(
                    onClick = { onCount(1) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.run_more))
                }
            }
            Text(
                text = stringResource(R.string.unit_reps),
                style = MaterialTheme.typography.bodyMedium.zh(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))
        WeightControl(
            weightKg = state.weightKg,
            onAdjust = onWeight,
            onEdit = onEditWeight
        )
    }
}

@Composable
private fun RestBody(state: RunState, accent: Color) {
    val next = state.next
    val unit = next?.let { unitLabel(it.measure) }.orEmpty()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.run_rest),
            style = MaterialTheme.typography.titleMedium.zh(),
            color = accent
        )
        BigNumber(text = formatClock(state.remaining), color = accent)
        if (next != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.run_next),
                style = MaterialTheme.typography.labelMedium.zh(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = next.name,
                style = MaterialTheme.typography.titleLarge.zh(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = buildString {
                    append(next.target)
                    append(' ')
                    append(unit)
                    next.weightKg?.let {
                        append("　·　")
                        append(formatWeight(it))
                        append(" kg")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RunControls(
    state: RunState,
    accent: Color,
    onFinish: () -> Unit,
    onSkipItem: () -> Unit,
    onSkipRest: () -> Unit,
    onExtend: () -> Unit,
    onTogglePause: () -> Unit
) {
    val timed = state.phase == RunPhase.WORK && state.current.measure == Measure.SECONDS
    val resting = state.phase == RunPhase.REST

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            resting -> {
                PrimaryButton(
                    text = stringResource(R.string.run_skip),
                    color = accent,
                    onClick = onSkipRest
                )
                OutlinedButton(
                    onClick = onExtend,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.run_add15)) }
            }

            timed -> {
                PrimaryButton(
                    text = stringResource(
                        if (state.paused) R.string.run_resume else R.string.run_pause
                    ),
                    color = accent,
                    onClick = onTogglePause
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExtend,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.run_add15)) }
                    OutlinedButton(
                        onClick = onFinish,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.run_finish_now)) }
                }
                NextUpLine(state)
            }

            else -> {
                PrimaryButton(
                    text = stringResource(R.string.run_finish_set),
                    color = accent,
                    onClick = onFinish
                )
                TextButton(
                    onClick = onSkipItem,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.run_skip_item)) }
                NextUpLine(state)
            }
        }
    }
}

@Composable
private fun NextUpLine(state: RunState) {
    val next = state.next
    val unit = next?.let { unitLabel(it.measure) }.orEmpty()
    Text(
        text = if (next == null) {
            stringResource(R.string.run_last_item)
        } else {
            stringResource(R.string.run_next) + "　" + next.name + "　" + next.target + " " + unit
        },
        style = MaterialTheme.typography.bodySmall.zh(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrimaryButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge.zh())
    }
}

/**
 * 重量。刻意做得比次數小很多——同一個畫面上有兩個加減，靠大小分主從。
 * 加減是 2.5 kg 一階（一片槓片），要精確的值就點中間那顆開對話框。
 */
@Composable
private fun WeightControl(
    weightKg: Double?,
    onAdjust: (Double) -> Unit,
    onEdit: () -> Unit
) {
    if (weightKg == null) {
        OutlinedButton(onClick = onEdit, shape = CircleShape) {
            Text(stringResource(R.string.run_add_weight), style = MaterialTheme.typography.bodyMedium.zh())
        }
        return
    }
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = { onAdjust(-WEIGHT_STEP) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Remove, stringResource(R.string.run_weight_down))
            }
            TextButton(onClick = onEdit) {
                Text(
                    text = stringResource(R.string.unit_weight, formatWeight(weightKg)),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(onClick = { onAdjust(WEIGHT_STEP) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, stringResource(R.string.run_weight_up))
            }
        }
    }
}

@Composable
private fun WeightDialog(
    title: String,
    current: Double?,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(current?.let { formatWeight(it) }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge.zh()) },
        text = {
            NumberField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.routine_item_weight),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm(text.toDoubleOrNull()?.takeIf { it > 0.0 })
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun BigNumber(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = BIG_NUMBER_SP.sp,
            fontWeight = FontWeight.Bold
        ),
        color = color,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private const val EXTEND_SECONDS = 15
private const val WEIGHT_STEP = 2.5
private const val REST_TINT = 0.12f
private const val BIG_NUMBER_SP = 88
