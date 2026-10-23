package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.ui.theme.bigNumber

/**
 * 計數分頁正在數的樣子。做一下按一下，數字往上加。
 *
 * 這個狀態下分頁列會收起來（見 MainActivity 的 Tab 註解）：`＋1` 要連按幾十下，
 * 下面緊貼分頁列的話手指低一點就切走了。離開只能按 X 或「結束並存檔」。
 *
 * 同樣的理由，**`−1` 不放在 `＋1` 旁邊**——主鈕旁邊擺反向操作就是等著誤觸。
 *
 * **目標只是參考，不是框架**：本來想做 20 下、做一做做了 40 下，畫面上就該寫 40，
 * 不該還在講「還剩下」。目標也**隨時可以改**，做到一半想加量是常有的事。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(viewModel: FitViewModel) {
    val state by viewModel.counter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    // 存檔或丟棄之後計數狀態就沒了，這一頁會被分頁換回設定頁，不必自己導覽
    val current = state ?: return

    KeepScreenOn(settings.keepScreenOn)

    var confirmQuit by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 還沒按過就沒東西可丟，直接走
                            if (current.done == 0) viewModel.discardCounter() else confirmQuit = true
                        }
                    ) {
                        Icon(Icons.Default.Close, stringResource(R.string.action_cancel))
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.count(1) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Text(
                        text = stringResource(R.string.counter_plus_one),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepButton(
                        label = stringResource(R.string.counter_minus_one),
                        enabled = current.done > 0
                    ) { viewModel.count(-1) }
                    StepButton(label = stringResource(R.string.counter_plus, BATCH_SMALL)) {
                        viewModel.count(BATCH_SMALL)
                    }
                    StepButton(label = stringResource(R.string.counter_plus, BATCH_LARGE)) {
                        viewModel.count(BATCH_LARGE)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = current.ops.isNotEmpty(),
                        onClick = { viewModel.undoCount() },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.counter_undo)) }
                    OutlinedButton(
                        onClick = { viewModel.saveCounter() },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.counter_finish)) }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (current.part.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = current.part,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // 這個數字就是做了幾下，沒有別的意思
                Text(
                    text = current.done.toString(),
                    style = bigNumber(BIG_SP),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.unit_reps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (current.hasTarget) {
                    LinearProgressIndicator(
                        progress = { current.progress },
                        modifier = Modifier
                            .padding(top = 28.dp)
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }

                // 目標是狀態也是入口：文字維持中性，可編輯的訊號交給旁邊的鉛筆
                TextButton(
                    onClick = { editingTarget = true },
                    modifier = Modifier.padding(top = if (current.hasTarget) 4.dp else 24.dp)
                ) {
                    Text(
                        text = when {
                            !current.hasTarget -> stringResource(R.string.counter_set_target)
                            current.over > 0 ->
                                stringResource(R.string.counter_goal_over, current.target, current.over)
                            current.reachedTarget ->
                                stringResource(R.string.counter_goal_met, current.target)
                            else ->
                                stringResource(R.string.counter_goal_short, current.target, current.remaining)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (current.hasTarget) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }

    if (confirmQuit) {
        ConfirmDialog(
            title = stringResource(R.string.summary_discard_title),
            message = stringResource(R.string.summary_discard_message),
            confirmLabel = stringResource(R.string.summary_discard),
            onConfirm = { viewModel.discardCounter() },
            onDismiss = { confirmQuit = false }
        )
    }

    if (editingTarget) {
        TargetDialog(
            current = current.target,
            onConfirm = { viewModel.setCounterTarget(it) },
            onDismiss = { editingTarget = false }
        )
    }
}

@Composable
private fun RowScope.StepButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}

/** 計數中改目標。留空就是取消目標，變回純計數 */
@Composable
private fun TargetDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.counter_target_dialog),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.counter_target_edit),
                    modifier = Modifier.fillMaxWidth()
                )
                HintText(stringResource(R.string.counter_target_edit_hint))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm(text.toIntOrNull() ?: 0)
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private const val BATCH_SMALL = 5
private const val BATCH_LARGE = 10
private const val BIG_SP = 112
