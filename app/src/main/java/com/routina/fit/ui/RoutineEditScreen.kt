package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.searchExercises
import com.routina.fit.model.Exercise
import com.routina.fit.model.Routine
import com.routina.fit.model.RoutineItem

/**
 * 編輯一張課表。[routineId] 是 [NEW_ROUTINE] 就開一張空的。
 *
 * 重排順序用 ⋯ 選單的上移／下移，不做拖曳——拖曳在 LazyColumn 上要自己接手勢，
 * 換來的只是同一件事的另一種做法。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditScreen(
    viewModel: FitViewModel,
    routineId: String,
    onStart: (String) -> Unit,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val existing = remember(routineId) {
        if (routineId == NEW_ROUTINE) null else viewModel.findRoutine(routineId)
    }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    val items = remember { mutableStateListOf<RoutineItem>().apply { addAll(existing?.items.orEmpty()) } }

    var picking by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }

    val nameOk = name.isNotBlank()
    val id = existing?.id ?: remember { viewModel.newId() }

    fun save() {
        viewModel.saveRoutine(Routine(id = id, name = name.trim(), items = items.toList()))
    }

    fun move(index: Int, by: Int) {
        val to = index + by
        if (to < 0 || to > items.lastIndex) return
        val item = items.removeAt(index)
        items.add(to, item)
    }

    val editLabel = stringResource(R.string.action_edit)
    val upLabel = stringResource(R.string.action_move_up)
    val downLabel = stringResource(R.string.action_move_down)
    val deleteLabel = stringResource(R.string.action_delete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (existing == null) R.string.routine_new else R.string.routine_edit_title
                        ),
                        style = MaterialTheme.typography.titleLarge
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = nameOk,
                    onClick = { save(); onBack() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (items.isNotEmpty()) {
                    OutlinedButton(
                        enabled = nameOk,
                        onClick = { save(); onStart(id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.routine_start_now))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.routine_name)) },
                singleLine = true,
                isError = name.isNotEmpty() && !nameOk,
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel(stringResource(R.string.routine_items))
            if (items.isEmpty()) {
                HintText(stringResource(R.string.routine_empty))
            } else {
                HintText(stringResource(R.string.routine_items_hint))
                items.forEachIndexed { index, item ->
                    val exercise = exercises.firstOrNull { it.id == item.exerciseId }
                    ItemRow(
                        index = index,
                        item = item,
                        exercise = exercise,
                        onClick = { editingIndex = index },
                        menu = listOf(
                            editLabel to { editingIndex = index },
                            upLabel to { move(index, -1) },
                            downLabel to { move(index, 1) },
                            deleteLabel to { if (index in items.indices) items.removeAt(index) }
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = { picking = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.routine_add_item))
            }
        }
    }

    if (picking) {
        ExercisePickerDialog(
            exercises = exercises,
            onPick = { exercise ->
                items.add(viewModel.routineItemFor(exercise))
            },
            onDismiss = { picking = false }
        )
    }

    // 索引先抓成區域值：對話框會先關閉（把 editingIndex 設回 -1）才把結果送回來
    val editIndex = editingIndex
    val editing = items.getOrNull(editIndex)
    if (editing != null) {
        val exercise = exercises.firstOrNull { it.id == editing.exerciseId }
        ItemEditDialog(
            item = editing,
            exercise = exercise,
            onConfirm = { updated ->
                if (editIndex in items.indices) items[editIndex] = updated
            },
            onDismiss = { editingIndex = -1 }
        )
    }
}

@Composable
private fun ItemRow(
    index: Int,
    item: RoutineItem,
    exercise: Exercise?,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>>
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise?.name.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge
                )
                val restText = if (item.restSec > 0) {
                    stringResource(R.string.routine_item_meta, item.restSec)
                } else {
                    stringResource(R.string.routine_item_meta_no_rest)
                }
                val weightText = item.weightKg
                    ?.let { "　·　" + stringResource(R.string.unit_weight, formatWeight(it)) }
                    .orEmpty()
                Text(
                    text = restText + weightText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.target.toString() + " " +
                    (exercise?.let { unitLabel(it.measure) }.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OverflowMenu(menu)
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = searchExercises(exercises, query)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.routine_add_item),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.library_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    if (results.isEmpty()) {
                        item { HintText(stringResource(R.string.library_no_results)) }
                    } else {
                        items(results, key = { it.id }) { exercise ->
                            // 挑完不關對話框，一次可以連續加好幾個動作
                            ExercisePickerRow(exercise = exercise, onClick = { onPick(exercise) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        }
    )
}

@Composable
private fun ItemEditDialog(
    item: RoutineItem,
    exercise: Exercise?,
    onConfirm: (RoutineItem) -> Unit,
    onDismiss: () -> Unit
) {
    var target by remember { mutableStateOf(item.target.toString()) }
    var rest by remember { mutableStateOf(item.restSec.toString()) }
    var weight by remember { mutableStateOf(item.weightKg?.let { formatWeight(it) }.orEmpty()) }

    val targetValue = target.toIntOrNull() ?: 0
    val restValue = rest.toIntOrNull() ?: 0
    val targetOk = targetValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = exercise?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = target,
                    onValueChange = { target = it },
                    label = stringResource(R.string.routine_item_target) +
                        (exercise?.let { "（" + unitLabel(it.measure) + "）" } ?: ""),
                    isError = !targetOk,
                    errorText = stringResource(R.string.exercise_target_required),
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = stringResource(R.string.routine_item_weight),
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField(
                    value = rest,
                    onValueChange = { rest = it },
                    label = stringResource(R.string.routine_item_rest),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = targetOk,
                onClick = {
                    onDismiss()
                    onConfirm(
                        item.copy(
                            target = targetValue,
                            restSec = restValue.coerceAtLeast(0),
                            weightKg = weight.toDoubleOrNull()?.takeIf { it > 0.0 }
                        )
                    )
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
