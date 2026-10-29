package com.routina.fit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.BODYWEIGHT_FILTER
import com.routina.fit.data.PartGroup
import com.routina.fit.data.filterByEquipment
import com.routina.fit.data.groupByPart
import com.routina.fit.data.knownEquipment
import com.routina.fit.data.searchExercises
import com.routina.fit.model.Exercise

/**
 * 動作庫。兩層篩選：上面一排器材晶片回答「我現在手邊有什麼」，
 * 下面依部位分組回答「今天要練哪裡」。
 *
 * 搜尋或套了器材篩選時就攤平成一張清單——這時候分組只是擋路。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: FitViewModel,
    onEditExercise: (String) -> Unit,
    onNewExercise: () -> Unit,
    onAddToToday: (String) -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val expanded by viewModel.expandedParts.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }

    val equipmentOptions = remember(exercises) { knownEquipment(exercises) }
    val byEquipment = filterByEquipment(exercises, equipment)
    val results = searchExercises(byEquipment, query)
    val groups = remember(byEquipment) { groupByPart(byEquipment) }
    val flat = query.isNotBlank() || equipment.isNotEmpty()

    val editLabel = stringResource(R.string.action_edit)
    val addLabel = stringResource(R.string.action_add_to_today)

    fun rowMenu(exercise: Exercise) = listOf(
        editLabel to { onEditExercise(exercise.id) },
        addLabel to { onAddToToday(exercise.id) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (flat) {
                                stringResource(R.string.library_count_filtered, results.size, exercises.size)
                            } else {
                                stringResource(R.string.library_count, exercises.size)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewExercise) {
                Icon(Icons.Default.Add, stringResource(R.string.exercise_new))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            // 底部留白讓最後一筆不會被 FAB 蓋住
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.library_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EquipmentFilterChip(
                        label = stringResource(R.string.library_filter_all),
                        selected = equipment.isEmpty(),
                        onClick = { equipment = "" }
                    )
                    EquipmentFilterChip(
                        label = stringResource(R.string.library_filter_bodyweight),
                        selected = equipment == BODYWEIGHT_FILTER,
                        onClick = {
                            equipment = if (equipment == BODYWEIGHT_FILTER) "" else BODYWEIGHT_FILTER
                        }
                    )
                    equipmentOptions.forEach { option ->
                        EquipmentFilterChip(
                            label = option,
                            selected = equipment == option,
                            onClick = { equipment = if (equipment == option) "" else option }
                        )
                    }
                }
            }

            when {
                exercises.isEmpty() -> item { HintText(stringResource(R.string.library_empty)) }

                results.isEmpty() -> item { HintText(stringResource(R.string.library_no_results)) }

                flat -> items(results, key = { it.id }) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onClick = { onEditExercise(exercise.id) },
                        menu = rowMenu(exercise)
                    )
                }

                else -> partGroups(
                    groups = groups,
                    expanded = expanded,
                    onToggle = { part -> viewModel.setPartExpanded(part, part !in expanded) },
                    onClick = { onEditExercise(it.id) },
                    menu = { rowMenu(it) }
                )
            }
        }
    }
}

@Composable
private fun EquipmentFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

private fun LazyListScope.partGroups(
    groups: List<PartGroup>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onClick: (Exercise) -> Unit,
    menu: (Exercise) -> List<Pair<String, () -> Unit>>
) {
    groups.forEach { group ->
        val open = group.part in expanded
        item(key = "header-" + group.part) {
            GroupHeader(
                part = group.part,
                count = group.exercises.size,
                expanded = open,
                onClick = { onToggle(group.part) }
            )
        }
        if (open) {
            items(group.exercises, key = { it.id }) { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    onClick = { onClick(exercise) },
                    menu = menu(exercise)
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(part: String, count: Int, expanded: Boolean, onClick: () -> Unit) {
    // 整列都可以點，不要只有右邊那顆箭頭
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = part.ifEmpty { stringResource(R.string.library_part_none) },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>> = emptyList()
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
                .padding(
                    start = 14.dp,
                    end = if (menu.isEmpty()) 14.dp else 0.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        text = exercise.defaultTarget.toString() + " " + unitLabel(exercise.measure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EquipmentTags(exercise.equipment)
                }
            }
            if (menu.isNotEmpty()) OverflowMenu(menu)
        }
    }
}

/** 「加一項」挑動作時用，與動作庫的列同一個長相，只是沒有 ⋯ */
@Composable
fun ExercisePickerRow(exercise: Exercise, onClick: () -> Unit) {
    ExerciseRow(exercise = exercise, onClick = onClick)
}

/** 器材標籤。空的就標「徒手」——沒有器材本身也是一個有用的答案 */
@Composable
fun EquipmentTags(equipment: List<String>) {
    if (equipment.isEmpty()) {
        Tag(stringResource(R.string.equipment_none))
        return
    }
    equipment.forEach { Tag(it) }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
