package com.routina.fit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routina.fit.R
import com.routina.fit.model.Measure
import com.routina.fit.model.Session
import com.routina.fit.model.SetLog

/**
 * 摘要頁、歷史清單、紀錄詳情共用的幾塊。三個地方顯示的是同樣的東西，沒有理由寫三次。
 */
@Composable
fun SessionStats(durationSeconds: Int, totalReps: Int, totalSeconds: Int) {
    StatRow(
        stats = listOf(
            stringResource(R.string.summary_duration) to formatClock(durationSeconds),
            stringResource(R.string.summary_reps) to totalReps.toString(),
            stringResource(R.string.summary_seconds) to totalSeconds.toString()
        )
    )
}

/** 一組一列。沒做滿的用錯誤色標出來，一眼看得到哪幾項撐不住 */
@Composable
fun SetLogRow(log: SetLog) {
    val unit = unitLabel(log.measure)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = log.name, style = MaterialTheme.typography.bodyLarge)
                log.weightKg?.let {
                    Text(
                        text = stringResource(R.string.unit_weight, formatWeight(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (log.target > 0) {
                    stringResource(R.string.log_done_of_target, log.done, log.target, unit)
                } else {
                    // 沒有目標的計數，顯示「40 次」而不是「40 / 0 次」
                    stringResource(R.string.log_done_only, log.done, unit)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (log.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/** 一次訓練一列。紀錄分頁與紀錄詳情共用；[menu] 空的就不顯示 ⋯ */
@Composable
fun SessionRow(
    session: Session,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>> = emptyList()
) {
    // 每日清單那筆紀錄沒有課表名字，名字由這一層補——資料層不碰字串資源
    val name = session.routineName.ifEmpty { stringResource(R.string.session_daily) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = if (menu.isEmpty()) 14.dp else 0.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = formatTime(session.startedAt) + "　·　" +
                        stringResource(
                            R.string.today_session_summary,
                            session.durationSeconds / 60,
                            session.totalReps
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (menu.isNotEmpty()) OverflowMenu(menu)
        }
    }
}

fun repsOf(logs: List<SetLog>): Int =
    logs.filter { it.measure == Measure.REPS }.sumOf { it.done }

fun secondsOf(logs: List<SetLog>): Int =
    logs.filter { it.measure == Measure.SECONDS }.sumOf { it.done }
