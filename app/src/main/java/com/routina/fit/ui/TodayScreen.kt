package com.routina.fit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.estimateMinutes
import com.routina.fit.data.thisWeek
import com.routina.fit.data.todayDate
import com.routina.fit.data.trainedDates
import com.routina.fit.model.Routine

/**
 * 首頁只有三件事：本週有沒有練、快速計數、課表清單。
 * 點課表直接進執行畫面，中間不放確認頁——換好衣服的人不會想多按一次。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: FitViewModel,
    onStartRoutine: (String) -> Unit,
    onEditRoutine: (String) -> Unit,
    onCounter: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val week = remember { thisWeek() }
    val today = remember { todayDate() }
    val trained = trainedDates(sessions, week)

    var deleting by remember { mutableStateOf<Routine?>(null) }

    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.today_title), style = MaterialTheme.typography.titleLarge.zh()) },
                actions = {
                    IconButton(onClick = onOpenLibrary) {
                        Icon(Icons.Default.FitnessCenter, stringResource(R.string.library_title))
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, stringResource(R.string.history_title))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionLabel(
                    stringResource(R.string.today_week) + "　" +
                        stringResource(R.string.today_week_count, trained.size)
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        week.forEach { date ->
                            WeekCell(
                                date = date,
                                trained = date in trained,
                                isToday = date == today,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.today_counter_section)) }
            item { CounterCard(onClick = onCounter) }

            item { SectionLabel(stringResource(R.string.today_routines)) }

            if (routines.isEmpty()) {
                item { HintText(stringResource(R.string.today_no_routine)) }
            } else {
                items(routines, key = { it.id }) { routine ->
                    RoutineRow(
                        routine = routine,
                        minutes = estimateMinutes(routine, exercises),
                        onClick = { onStartRoutine(routine.id) },
                        menu = listOf(
                            editLabel to { onEditRoutine(routine.id) },
                            deleteLabel to { deleting = routine }
                        )
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

/** 新課表用這個 id 進編輯頁，編輯頁看到它就開一張空的 */
const val NEW_ROUTINE = "new"

@Composable
private fun WeekCell(
    date: String,
    trained: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = weekdayLetter(date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (trained) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    trained -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    isToday -> Text(
                        text = stringResource(R.string.weekday_today),
                        style = MaterialTheme.typography.labelMedium.zh(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_counter),
                    style = MaterialTheme.typography.titleMedium.zh(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.today_counter_hint),
                    style = MaterialTheme.typography.bodySmall.zh(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

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
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium.zh(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (routine.items.isEmpty()) {
                        stringResource(R.string.routine_empty_meta)
                    } else {
                        stringResource(R.string.routine_meta, routine.items.size, minutes)
                    },
                    style = MaterialTheme.typography.bodySmall.zh(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.today_start),
                style = MaterialTheme.typography.labelLarge.zh(),
                color = MaterialTheme.colorScheme.primary
            )
            OverflowMenu(menu)
        }
    }
}
