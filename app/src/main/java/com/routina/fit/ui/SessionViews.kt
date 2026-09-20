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
                text = stringResource(R.string.log_done_of_target, log.done, log.target, unit),
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

/** 一次訓練一列。首頁的「今天做了什麼」與紀錄分頁共用；[menu] 空的就不顯示 ⋯ */
@Composable
fun SessionRow(
    session: Session,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>> = emptyList()
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
                .padding(
                    start = 14.dp,
                    end = if (menu.isEmpty()) 14.dp else 0.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.routineName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = formatTime(session.startedAt) + "　·　" +
                        stringResource(
                            R.string.today_session_summary,
                            session.durationSeconds / 60,
                            session.totalReps
                        ) +
                        if (session.isCounter) "　·　$counterTag" else "",
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
