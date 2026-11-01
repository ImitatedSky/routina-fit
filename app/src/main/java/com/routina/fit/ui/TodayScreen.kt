package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // 列高由螢幕決定，不是寫死的。固定 72dp 在 411×731 的螢幕上剛好，
            // 到 448×997 的手機上，最後一列離 ＋ 還有 149dp 全空（量過實機截圖）。
            // 所以先扣掉上面那一塊，剩下的高度分給今天有幾項。
            //
            // 上面那一塊有多高是**量的，不是算的**。算法和實際排版會漂（字型行高、
            // 小數四捨五入），漂多少就吃掉 ＋ 的間距。初值用算的，第一幀就差不多，
            // 量到真的再換——使用者看不到中間那一幀。
            val density = LocalDensity.current
            var headerHeight by remember {
                mutableStateOf(streakCardHeight(maxWidth - SIDE_PAD * 2) + LABEL_BLOCK)
            }
            val rowHeight = rowHeightFor(maxHeight, headerHeight, plan.items.size)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                // 底下留的是 FAB 的高度加上下各 16dp：最後一列才捲得到 ＋ 的上面去
                contentPadding = PaddingValues(
                    start = SIDE_PAD, end = SIDE_PAD, top = LIST_TOP_PAD, bottom = FAB_RESERVE
                ),
                verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
            ) {
                // 卡片與小標合成一個 item：這樣量到的就是整塊的高度
                item {
                    Column(
                        modifier = Modifier.onSizeChanged {
                            headerHeight = with(density) { it.height.toDp() }
                        }
                    ) {
                        StreakCard(volume = volume, today = plan.date, streak = streak)
                        SectionLabel(
                            text = if (plan.total > 0) {
                                stringResource(R.string.today_total, plan.total)
                            } else {
                                stringResource(R.string.today_list)
                            }
                        )
                    }
                }

                if (plan.items.isEmpty()) {
                    item { HintText(stringResource(R.string.today_list_empty)) }
                } else {
                    items(plan.items, key = { it.id }) { item ->
                        DayItemRow(
                            item = item,
                            height = rowHeight,
                            onCount = { delta -> viewModel.count(item.id, delta) },
                            onEdit = { editing = item },
                            // 還沒按過就沒東西好丟，直接刪；有數字才問一聲
                            onDelete = {
                                if (item.done > 0) {
                                    deleting = item
                                } else {
                                    viewModel.deleteTodayItem(item.id)
                                }
                            }
                        )
                    }
                }
            }

            // ＋ 自己擺，不用 Scaffold 的 FAB 槽：槽位放多高由 Scaffold 內部決定，
            // 實機量出來是離內容底 84dp 而不是 56dp，清單就無從知道要讓出多少高度。
            // 自己擺之後 [FAB_RESERVE] 是真的，不是猜的。
            //
            // 小號的 FAB（40dp）不是標準的 56dp：這一頁的主角是清單，
            // ＋ 一天只按一次，不該長得比每一列都還搶眼。
            // 形狀要自己給圓形——M3 1.4 的 small FAB 預設是圓角方形，不是圓的。
            SmallFloatingActionButton(
                onClick = onAddItem,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FAB_GAP)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.today_add_item))
            }

            // 提示條也要自己擺。Scaffold 本來會把它推到 FAB 上面，
            // ＋ 拿出槽位之後它就會整個蓋住 ＋。
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FAB_RESERVE)
            )
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

/**
 * 這個螢幕一列該多高。
 *
 * 清單是這一頁的主角，應該把 [header]（卡片加小標）以外的高度用掉——
 * 固定 72dp 在 411×731 的螢幕上剛剛好，但在 448×997 的手機上，
 * 三列做完下面還空著三分之一個畫面。
 *
 * 清單是 [header][每一列]，所以間距有 items 道。
 *
 * 夾在 [ROW_MIN] 與 [ROW_MAX] 之間：太矮按不準，太高就變成三張卡片而不是一張清單。
 * 項目多到撐不下時會收到下限，接著就是正常捲動。
 */
private fun rowHeightFor(viewport: Dp, header: Dp, items: Int): Dp {
    if (items <= 0) return ROW_MIN
    val free = viewport - LIST_TOP_PAD - header - FAB_RESERVE - ROW_SPACING * items
    return (free / items).coerceIn(ROW_MIN, ROW_MAX)
}

private val SIDE_PAD = 16.dp
private val ROW_SPACING = 12.dp
private val LIST_TOP_PAD = 4.dp

/** 小標連同它的上下留白。只用來給 headerHeight 一個起始值 */
private val LABEL_BLOCK = 38.dp

/** ＋ 的直徑，跟 SmallFloatingActionButton 一樣 */
private val FAB_SIZE = 40.dp

/** ＋ 離最後一列與離畫面底的距離 */
private val FAB_GAP = 16.dp

/** 清單底下要空出來的：＋ 本身加上下各一個 [FAB_GAP] */
private val FAB_RESERVE = FAB_SIZE + FAB_GAP * 2
