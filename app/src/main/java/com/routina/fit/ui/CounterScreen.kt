package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.routina.fit.ui.theme.restColor
import com.routina.fit.ui.theme.workColor

/**
 * 「我要做 100 個伏地挺身」就是這一頁：一個大數字往下扣。
 *
 * 大 −1 加旁邊的 −5 / −10：100 下沒人一次做完，是 20+20+15 這樣堆的，
 * 所以要同時吃「一下一下點」與「做完一組才記」兩種節奏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    viewModel: FitViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.counter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = state

    LaunchedEffect(current == null) {
        if (current == null) onClose()
    }
    if (current == null) return

    KeepScreenOn(settings.keepScreenOn)

    var confirmQuit by remember { mutableStateOf(false) }

    val accent = if (current.finished) restColor() else workColor()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(current.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.counter_target_hint, current.target),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 還沒扣過就沒東西可丟，直接走
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
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                ) {
                    Text(
                        text = if (current.finished) {
                            stringResource(R.string.counter_continue)
                        } else {
                            stringResource(R.string.counter_minus_one)
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.count(BATCH_SMALL) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.counter_minus, BATCH_SMALL),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.count(BATCH_LARGE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.counter_minus, BATCH_LARGE),
                            style = MaterialTheme.typography.titleMedium
                        )
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

                Text(
                    text = if (current.finished) current.done.toString() else current.left.toString(),
                    style = bigNumber(if (current.finished) FINISHED_SP else BIG_SP),
                    color = if (current.finished) accent else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = when {
                        current.over > 0 -> stringResource(R.string.counter_over, current.over)
                        current.finished -> stringResource(R.string.counter_done)
                        else -> stringResource(R.string.counter_left)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (current.finished) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )

                LinearProgressIndicator(
                    progress = { (current.done.toFloat() / current.target).coerceIn(0f, 1f) },
                    color = accent,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Text(
                    text = stringResource(R.string.counter_progress, current.done, current.target),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
}

private const val BATCH_SMALL = 5
private const val BATCH_LARGE = 10
private const val BIG_SP = 112
private const val FINISHED_SP = 88
