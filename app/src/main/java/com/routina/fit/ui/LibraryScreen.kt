package com.routina.fit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.PartGroup
import com.routina.fit.data.groupByPart
import com.routina.fit.data.searchExercises
import com.routina.fit.model.Exercise

/**
 * 整理動作的地方：依部位分組、可摺疊，每一列點進去編輯。
 * 展開狀態存在 prefs，下次進來還在——整頁照 Routina Bite 的食物庫作法。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: FitViewModel,
    onEditExercise: (String) -> Unit,
    onNewExercise: () -> Unit,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val expanded by viewModel.expandedParts.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }

    val groups = remember(exercises) { groupByPart(exercises) }
    val results = searchExercises(exercises, query)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.titleLarge.zh()
                        )
                        Text(
                            text = stringResource(R.string.library_count, exercises.size),
                            style = MaterialTheme.typography.bodySmall.zh(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
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

            if (exercises.isEmpty()) {
                item { HintText(stringResource(R.string.library_empty)) }
            } else if (query.isBlank()) {
                partGroups(
                    groups = groups,
                    expanded = expanded,
                    onToggle = { part -> viewModel.setPartExpanded(part, part !in expanded) },
                    onClick = { onEditExercise(it.id) }
                )
            } else if (results.isEmpty()) {
                item { HintText(stringResource(R.string.library_no_results)) }
            } else {
                items(results, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onEditExercise(exercise.id) })
                }
            }
        }
    }
}

private fun LazyListScope.partGroups(
    groups: List<PartGroup>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onClick: (Exercise) -> Unit
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
                ExerciseRow(exercise = exercise, onClick = { onClick(exercise) })
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
            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = part.ifEmpty { stringResource(R.string.library_part_none) },
            style = MaterialTheme.typography.titleSmall.zh(),
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
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
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
                Text(
                    text = measureLabel(exercise.measure) + "　·　" +
                        (exercise.defaultWeightKg
                            ?.let { stringResource(R.string.exercise_meta_weight, formatWeight(it)) }
                            ?: stringResource(R.string.exercise_meta_bodyweight)),
                    style = MaterialTheme.typography.bodySmall.zh(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = exercise.defaultTarget.toString() + " " + unitLabel(exercise.measure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 給課表編輯頁挑動作用的對話框清單，跟動作庫的列一樣長相 */
@Composable
fun ExercisePickerRow(exercise: Exercise, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(vertical = 2.dp)) {
        ExerciseRow(exercise = exercise, onClick = onClick)
    }
}
