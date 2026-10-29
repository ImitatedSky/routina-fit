package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.currentStreak
import com.routina.fit.data.dailyVolume
import com.routina.fit.data.plusToday
import com.routina.fit.model.DayItem

/**
 * 首頁就是今天要做的清單，點一下加一下。
 *
 * 這一頁回答兩件事：**最近有沒有在練**（上面那塊火與格子圖）、**今天還剩什麼**（清單）。
 * 動作庫與歷史紀錄是別的地方，各自有分頁——首頁不是目錄。
 *
 * 「結算」把今天寫成一筆紀錄。沒按也不會掉：跨到隔天時有做卻沒結算的會自動收進去
 * （見 FitRepository.ensureToday），所以這顆的意思是「現在就記下來」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: FitViewModel,
    onAddItem: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val plan by viewModel.today.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val justSettled by viewModel.settled.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<DayItem?>(null) }
    var deleting by remember { mutableStateOf<DayItem?>(null) }

    // App 開著過午夜是常態，回到前景就確認一次手上這份還是不是今天的
    LifecycleResumeEffect(Unit) {
        viewModel.ensureToday()
        onPauseOrDispose { }
    }

    val volume = remember(sessions, plan) {
        plusToday(dailyVolume(sessions), plan.date, plan.total)
    }
    val streak = remember(volume) { currentStreak(volume) }

    val settledTemplate = stringResource(R.string.today_settled)
    LaunchedEffect(justSettled) {
        val count = justSettled ?: return@LaunchedEffect
        snackbar.showSnackbar(String.format(settledTemplate, count))
        viewModel.clearSettled()
    }

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
                            text = formatDayLabel(plan.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = plan.touched,
                        onClick = { viewModel.settleToday() }
                    ) {
                        Text(
                            text = if (plan.settled) {
                                stringResource(R.string.today_settle_again)
                            } else {
                                stringResource(R.string.today_settle)
                            }
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        // 小號的 FAB（40dp）不是標準的 56dp：這一頁的主角是清單，
        // ＋ 一天只按一次，不該長得比每一列都還搶眼。
        // 形狀要自己給圓形——M3 1.4 的 small FAB 預設是圓角方形，不是圓的。
        floatingActionButton = {
            SmallFloatingActionButton(onClick = onAddItem, shape = CircleShape) {
                Icon(Icons.Default.Add, stringResource(R.string.today_add_item))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            // 底下留的是 FAB 的高度加上下各 16dp：最後一列才捲得到 ＋ 的上面去
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StreakCard(volume = volume, today = plan.date, streak = streak)
            }

            item {
                SectionLabel(
                    text = if (plan.total > 0) {
                        stringResource(R.string.today_total, plan.total)
                    } else {
                        stringResource(R.string.today_list)
                    }
                )
            }

            if (plan.items.isEmpty()) {
                item { HintText(stringResource(R.string.today_list_empty)) }
            } else {
                items(plan.items, key = { it.id }) { item ->
                    DayItemRow(
                        item = item,
                        onCount = { delta -> viewModel.count(item.id, delta) },
                        onEdit = { editing = item },
                        // 還沒按過就沒東西好丟，直接刪；有數字才問一聲
                        onDelete = {
                            if (item.done > 0) deleting = item else viewModel.deleteTodayItem(item.id)
                        }
                    )
                }
            }
        }
    }

    editing?.let { item ->
        DayItemDialog(
            title = stringResource(R.string.today_edit_item),
            item = item,
            countEditable = true,
            onConfirm = { name, target, done ->
                viewModel.updateTodayItem(item.copy(name = name, target = target, done = done))
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.today_delete_item),
            message = stringResource(R.string.today_delete_item_message, item.done),
            onConfirm = { viewModel.deleteTodayItem(item.id) },
            onDismiss = { deleting = null }
        )
    }
}

