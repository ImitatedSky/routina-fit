package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R

/**
 * 練完看到的一頁。
 *
 * 這一頁是整個 App 的重點：紀錄到這裡已經寫好了，使用者只是按存檔——
 * 訓練全程沒有打過一個字。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: FitViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.run.collectAsStateWithLifecycle()
    val current = state

    // 存檔或丟棄之後 run 會變成 null，這一頁就該讓位
    LaunchedEffect(current == null) {
        if (current == null) onClose()
    }
    if (current == null) return

    var confirmDiscard by remember { mutableStateOf(false) }

    val logs = current.logs
    val duration = ((System.currentTimeMillis() - current.startedAt) / 1000L).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(current.routineName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(
                                R.string.summary_started_at,
                                formatTime(current.startedAt)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    onClick = { viewModel.saveRun() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.summary_save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedButton(
                    onClick = { confirmDiscard = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.summary_discard))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SessionStats(
                    durationSeconds = duration,
                    totalReps = repsOf(logs),
                    totalSeconds = secondsOf(logs)
                )
            }
            items(logs.size) { index -> SetLogRow(logs[index]) }
            item { HintText(stringResource(R.string.summary_no_typing)) }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.summary_discard_title),
            message = stringResource(R.string.summary_discard_message),
            confirmLabel = stringResource(R.string.summary_discard),
            onConfirm = { viewModel.discardRun() },
            onDismiss = { confirmDiscard = false }
        )
    }
}
