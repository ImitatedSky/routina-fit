package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/** 一筆已經存下的訓練紀錄。版面與摘要頁一致，只是不能存檔或丟棄 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: FitViewModel,
    sessionId: String,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val session = remember(sessions, sessionId) { sessions.firstOrNull { it.id == sessionId } }

    // 在這一頁把紀錄刪掉就沒東西可看了
    LaunchedEffect(session == null) {
        if (session == null) onBack()
    }
    if (session == null) return

    var deleting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session.routineName.ifEmpty {
                                stringResource(R.string.session_daily)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formatDayLabel(session.date) + " " + formatTime(session.startedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    OverflowMenu(listOf(stringResource(R.string.action_delete) to { deleting = true }))
                }
            )
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
                    durationSeconds = session.durationSeconds,
                    totalReps = session.totalReps,
                    totalSeconds = session.totalSeconds
                )
            }
            items(session.logs.size) { index -> SetLogRow(session.logs[index]) }
        }
    }

    if (deleting) {
        ConfirmDialog(
            title = stringResource(R.string.history_delete_title),
            onConfirm = { viewModel.deleteSession(session.id) },
            onDismiss = { deleting = false }
        )
    }
}
