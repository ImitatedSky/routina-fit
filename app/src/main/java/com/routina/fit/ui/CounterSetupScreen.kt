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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
 * 計數分頁還沒開始數的樣子。
 *
 * 表單在最上面、進來就能打字，動作庫只是幫忙填——這是 Routina Bite v0.6.0
 * 學到的教訓：使用者的心智模型是「表單是主角」，不是「先選東西再跳對話框」。
 *
 * 清單只列計次的動作。計時的東西（開合跳、棒式）該進課表，
 * 在這裡點它會變成「35 次」，只會讓人困惑。
 *
 * 按下「開始計數」不用換頁：ViewModel 一有計數狀態，這個分頁自己就變成 [CounterScreen]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterSetupScreen(viewModel: FitViewModel) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val prefillId by viewModel.counterPrefill.collectAsStateWithLifecycle()
    val justSaved by viewModel.counterSaved.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var pickedId by remember { mutableStateOf("") }
    var pickedPart by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // 目標可以不填：純計數就是做一下按一下，沒有要達成什麼
    val targetValue = target.toIntOrNull() ?: 0
    val nameOk = name.isNotBlank()

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

    // 從動作列的「計數」切過來時把表單填好。分頁沒辦法帶路由參數，所以走狀態
    LaunchedEffect(prefillId, exercises) {
        if (prefillId.isEmpty()) return@LaunchedEffect
        exercises.firstOrNull { it.id == prefillId }?.let { pick(it) }
        viewModel.clearCounterPrefill()
    }

    // 剛存完會回到這一頁：捲回表單、講一聲才知道真的存進去了。
    // 注意順序——clearCounterSaved() 會換掉這個 LaunchedEffect 的 key，
    // 先清就會把還沒顯示完的 snackbar 一起取消掉。
    val savedTemplate = stringResource(R.string.counter_saved)
    LaunchedEffect(justSaved) {
        val count = justSaved ?: return@LaunchedEffect
        listState.scrollToItem(0)
        snackbar.showSnackbar(String.format(savedTemplate, count))
        viewModel.clearCounterSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.counter_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Button(
                    enabled = nameOk,
                    onClick = {
                        viewModel.startCounter(
                            name = name,
                            target = targetValue,
                            exerciseId = pickedId,
                            part = pickedPart
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.counter_start),
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
                        pickedPart = ""
                    },
                    label = { Text(stringResource(R.string.counter_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                NumberField(
                    value = target,
                    onValueChange = { target = it },
                    label = stringResource(R.string.counter_target),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { HintText(stringResource(R.string.counter_target_optional)) }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            if (recent.isNotEmpty() && query.isBlank()) {
                item { SectionLabel(stringResource(R.string.counter_recent)) }
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
            item { SectionLabel(stringResource(R.string.counter_pick)) }

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
