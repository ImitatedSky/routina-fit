package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
 * 加一項到今天的清單。
 *
 * 表單在最上面、進來就能打字，下面的動作庫只是幫忙填——這是 Routina Bite 學到的教訓：
 * 使用者的心智模型是「表單是主角」，不是「先選東西再跳對話框」。
 *
 * 清單只列計次的動作。計時的東西（開合跳、棒式）點一下沒有「一下」可言，
 * 放進來只會變成「35 次」，讓人困惑。
 *
 * [exerciseId] 是從動作庫的「加到今天」帶過來的，進來就把表單填好。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: FitViewModel,
    exerciseId: String?,
    onDone: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var pickedId by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val nameOk = name.isNotBlank()
    val countable = exercises.filter { it.measure == Measure.REPS }
    val recentIds = remember(sessions) { recentExerciseIds(sessions, RECENT_LIMIT) }
    val recent = recentIds.mapNotNull { id -> countable.firstOrNull { it.id == id } }
    val results = searchExercises(countable, query)

    fun pick(exercise: Exercise) {
        name = exercise.name
        target = exercise.defaultTarget.toString()
        pickedId = exercise.id
        // 在清單底下點到的動作，要把畫面帶回表單才看得到填進去了
        scope.launch { listState.animateScrollToItem(0) }
    }

    LaunchedEffect(exerciseId, exercises) {
        if (exerciseId.isNullOrEmpty()) return@LaunchedEffect
        exercises.firstOrNull { it.id == exerciseId }?.let { pick(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.today_add_item),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
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
                    enabled = nameOk,
                    onClick = {
                        viewModel.addTodayItem(
                            name = name,
                            target = target.toIntOrNull() ?: 0,
                            exerciseId = pickedId
                        )
                        onDone()
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.today_add_confirm),
                        style = MaterialTheme.typography.titleMedium
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        // 自己改過名字就不再算是「從庫裡挑的那一個」
                        pickedId = ""
                    },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                NumberField(
                    value = target,
                    onValueChange = { target = it },
                    label = stringResource(R.string.item_target),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            if (recent.isNotEmpty() && query.isBlank()) {
                item { SectionLabel(stringResource(R.string.item_recent)) }
                items(recent, key = { "recent-" + it.id }) { exercise ->
                    ExercisePickerRow(exercise = exercise, onClick = { pick(exercise) })
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
            item { SectionLabel(stringResource(R.string.item_pick)) }

            if (results.isEmpty()) {
                item { HintText(stringResource(R.string.library_no_results)) }
            } else {
                items(results, key = { it.id }) { exercise ->
                    ExercisePickerRow(exercise = exercise, onClick = { pick(exercise) })
                }
            }
        }
    }
}

private const val RECENT_LIMIT = 5
