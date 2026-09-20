package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.model.Session

/**
 * 訓練紀錄。課表與快速計數混在同一份清單，因為它們本來就是同一個資料結構
 * （[Session.routineId] 是 null 就是計數）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FitViewModel,
    onOpenSession: (String) -> Unit,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val ordered = remember(sessions) { sessions.sortedByDescending { it.startedAt } }

    var deleting by remember { mutableStateOf<Session?>(null) }
    val deleteLabel = stringResource(R.string.action_delete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge.zh()
                        )
                        Text(
                            text = stringResource(R.string.history_count, sessions.size),
                            style = MaterialTheme.typography.bodySmall.zh(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
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
            if (ordered.isEmpty()) {
                item { HintText(stringResource(R.string.history_empty)) }
            } else {
                items(ordered, key = { it.id }) { session ->
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

@Composable
private fun SessionRow(
    session: Session,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>>
) {
    val counterTag = stringResource(R.string.history_counter_tag)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.routineName,
                    style = MaterialTheme.typography.bodyLarge.zh()
                )
                Text(
                    text = formatDayLabel(session.date) + " " + formatTime(session.startedAt) +
                        if (session.isCounter) "　·　$counterTag" else "",
                    style = MaterialTheme.typography.bodySmall.zh(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.history_minutes, session.durationSeconds / 60),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OverflowMenu(menu)
        }
    }
}
