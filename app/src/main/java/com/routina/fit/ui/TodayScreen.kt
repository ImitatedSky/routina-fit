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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * 首頁回答兩個問題：這週練了幾天、現在要練哪一張。
 *
 * 快速計數放在最下面一行文字按鈕，不做成大卡片——它是附帶功能，
 * 每天真正會按的是課表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: FitViewModel,
    onStartRoutine: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onCounter: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val week = remember { thisWeek() }
    val today = remember { todayDate() }
    val trained = trainedDates(sessions, week)
    val todaySessions = sessions.filter { it.date == today }.sortedByDescending { it.startedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.today_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = formatDayLabel(today),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
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
            // 組內 6dp，段落標題自己帶 16dp 上緣——組外至少是組內的兩倍，分組才讀得出來
            verticalArrangement = Arrangement.spacedBy(6.dp)
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

            item { SectionLabel(stringResource(R.string.today_start_section)) }

            if (routines.isEmpty()) {
                item { HintText(stringResource(R.string.today_no_routine_tab)) }
                item {
                    TextButton(onClick = onOpenRoutines) {
                        Text(stringResource(R.string.routines_title))
                    }
                }
            } else {
                items(routines, key = { it.id }) { routine ->
                    StartRoutineRow(
                        routine = routine,
                        minutes = estimateMinutes(routine, exercises),
                        onClick = { onStartRoutine(routine.id) }
                    )
                }
            }

            item { SectionLabel(stringResource(R.string.today_done_section)) }

            if (todaySessions.isEmpty()) {
                item { HintText(stringResource(R.string.today_nothing_yet)) }
            } else {
                items(todaySessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onClick = { onOpenSession(session.id) }
                    )
                }
            }

            item {
                TextButton(
                    onClick = onCounter,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.today_counter))
                }
            }
        }
    }
}

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
        // 有練的日子用容器色、不是飽和主色。一頁只留一個飽和元素，
        // 這一頁的名額給「開始」，不是給日曆。
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (trained) {
                MaterialTheme.colorScheme.primaryContainer
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    isToday -> Text(
                        text = stringResource(R.string.weekday_today),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** 首頁的課表列。這裡只有一件事可做——開始——所以整列就是那顆按鈕 */
@Composable
private fun StartRoutineRow(
    routine: Routine,
    minutes: Int,
    onClick: () -> Unit
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.today_start),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
