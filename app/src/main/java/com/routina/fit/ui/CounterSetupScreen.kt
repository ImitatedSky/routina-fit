package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.recentExerciseIds
import com.routina.fit.data.searchExercises
import com.routina.fit.model.Exercise
import com.routina.fit.model.Measure
import kotlinx.coroutines.launch

/**
 * 快速計數的入口。
 *
 * 表單在最上面、進來就能打字，動作庫只是幫忙填——這是 Routina Bite v0.6.0
 * 學到的教訓：使用者的心智模型是「表單是主角」，不是「先選東西再跳對話框」。
 *
 * 清單只列計次的動作。計時的東西（開合跳、棒式）該進課表，
 * 在這裡點它會變成「35 次」，只會讓人困惑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterSetupScreen(
    viewModel: FitViewModel,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(DEFAULT_TARGET) }
    var pickedId by remember { mutableStateOf("") }
    var pickedPart by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val targetValue = target.toIntOrNull() ?: 0
    val nameOk = name.isNotBlank()
    val targetOk = targetValue > 0

    val countable = exercises.filter { it.measure == Measure.REPS }
    val recentIds = remember(sessions) { recentExerciseIds(sessions, RECENT_LIMIT) }
    val recent = recentIds.mapNotNull { id -> countable.firstOrNull { it.id == id } }
    val results = searchExercises(countable, query)

    fun pick(exercise: Exercise) {
        name = exercise.name
        target = exercise.defaultTarget.toString()
        pickedId = exercise.id
        pickedPart = exercise.part
        // 在清單底下點到的動作，要把畫面帶回表單才看得到填進去了
        scope.launch { listState.animateScrollToItem(0) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.counter_title),
                        style = MaterialTheme.typography.titleLarge.zh()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Button(
                    enabled = nameOk && targetOk,
                    onClick = {
                        viewModel.startCounter(
                            name = name,
                            target = targetValue,
                            exerciseId = pickedId,
                            part = pickedPart
                        )
                        onStart()
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.counter_start),
                        style = MaterialTheme.typography.titleMedium.zh()
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        // 自己改過名字就不再算是「從庫裡挑的那一個」
                        pickedId = ""
                        pickedPart = ""
                    },
                    label = { Text(stringResource(R.string.counter_name)) },
                    singleLine = true,
                    isError = name.isNotEmpty() && !nameOk,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                NumberField(
                    value = target,
                    onValueChange = { target = it },
                    label = stringResource(R.string.counter_target),
                    isError = !targetOk,
                    errorText = stringResource(R.string.counter_target_required),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            if (recent.isNotEmpty() && query.isBlank()) {
                item { SectionLabel(stringResource(R.string.counter_recent)) }
                items(recent, key = { "recent-" + it.id }) { exercise ->
                    PickRow(exercise = exercise, onClick = { pick(exercise) })
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.library_search)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            item { SectionLabel(stringResource(R.string.counter_pick)) }

            if (results.isEmpty()) {
                item { HintText(stringResource(R.string.library_no_results)) }
            } else {
                items(results, key = { it.id }) { exercise ->
                    PickRow(exercise = exercise, onClick = { pick(exercise) })
                }
            }
        }
    }
}

@Composable
private fun PickRow(exercise: Exercise, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
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
                Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge.zh())
                if (exercise.part.isNotEmpty()) {
                    Text(
                        text = exercise.part,
                        style = MaterialTheme.typography.bodySmall.zh(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = exercise.defaultTarget.toString() + " " + stringResource(R.string.unit_reps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val DEFAULT_TARGET = "100"
private const val RECENT_LIMIT = 5
