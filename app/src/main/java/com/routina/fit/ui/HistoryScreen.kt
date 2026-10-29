package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.model.Session

/**
 * 訓練紀錄，依日期分段。課表與每日清單混在同一份清單，
 * 因為它們本來就是同一個資料結構（[Session.routineId] 是 null 就是每日清單）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FitViewModel,
    onOpenSession: (String) -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    // 先依開始時刻排好再分組，同一天內也維持新的在上面
    val byDay = remember(sessions) {
        sessions.sortedByDescending { it.startedAt }.groupBy { it.date }.toList()
    }

    var deleting by remember { mutableStateOf<Session?>(null) }
    val deleteLabel = stringResource(R.string.action_delete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.history_count, sessions.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (byDay.isEmpty()) {
                item { HintText(stringResource(R.string.history_empty)) }
            }
            byDay.forEach { (date, daySessions) ->
                // 前綴不能和 Session 的 id 撞——每日清單那筆的 id 就是 "day-" 加日期，
                // 撞到的話同一個 LazyColumn 會有兩個一樣的 key，直接丟例外
                item(key = "header-$date") { SectionLabel(formatDayLabel(date)) }
                items(daySessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onClick = { onOpenSession(session.id) },
                        menu = listOf(deleteLabel to { deleting = session })
                    )
                }
            }
        }
    }

    deleting?.let { session ->
        ConfirmDialog(
            title = stringResource(R.string.history_delete_title),
            onConfirm = { viewModel.deleteSession(session.id) },
            onDismiss = { deleting = null }
        )
    }
}
