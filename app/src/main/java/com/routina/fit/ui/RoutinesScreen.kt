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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.estimateMinutes
import com.routina.fit.model.Routine

/** 新課表用這個 id 進編輯頁，編輯頁看到它就開一張空的 */
const val NEW_ROUTINE = "new"

/**
 * 管理課表的地方。首頁也列課表，但那裡只能「開始」；
 * 建立、改名、加減項目、刪除都在這一頁。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    viewModel: FitViewModel,
    onEditRoutine: (String) -> Unit,
    onStartRoutine: (String) -> Unit
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    var deleting by remember { mutableStateOf<Routine?>(null) }

    val startLabel = stringResource(R.string.today_start)
    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.routines_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.routines_count, routines.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditRoutine(NEW_ROUTINE) }) {
                Icon(Icons.Default.Add, stringResource(R.string.routine_new))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (routines.isEmpty()) {
                item { HintText(stringResource(R.string.routines_empty)) }
            } else {
                items(routines, key = { it.id }) { routine ->
                    RoutineRow(
                        routine = routine,
                        minutes = estimateMinutes(routine, exercises),
                        onClick = { onEditRoutine(routine.id) },
                        menu = buildList {
                            if (routine.items.isNotEmpty()) {
                                add(startLabel to { onStartRoutine(routine.id) })
                            }
                            add(editLabel to { onEditRoutine(routine.id) })
                            add(deleteLabel to { deleting = routine })
                        }
                    )
                }
            }
        }
    }

    deleting?.let { routine ->
        ConfirmDialog(
            title = stringResource(R.string.routine_delete_title),
            message = stringResource(R.string.routine_delete_message),
            onConfirm = { viewModel.deleteRoutine(routine.id) },
            onDismiss = { deleting = null }
        )
    }
}

/** 這一頁點整列是編輯（管理才是這頁的工作），開始練在 ⋯ 裡 */
@Composable
private fun RoutineRow(
    routine: Routine,
    minutes: Int,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>>
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (routine.items.isEmpty()) {
                        stringResource(R.string.routine_empty_meta)
                    } else {
                        stringResource(R.string.routine_meta, routine.items.size, minutes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OverflowMenu(menu)
        }
    }
}
