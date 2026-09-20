package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.knownParts
import com.routina.fit.model.Exercise
import com.routina.fit.model.Measure

/**
 * 新增或編輯一個動作。[exerciseId] 為 null 就是新增。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditScreen(
    viewModel: FitViewModel,
    exerciseId: String?,
    onDone: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val existing = remember(exerciseId) { exerciseId?.let { viewModel.findExercise(it) } }
    val parts = remember(exercises) { knownParts(exercises) }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var part by remember { mutableStateOf(existing?.part.orEmpty()) }
    var measure by remember { mutableStateOf(existing?.measure ?: Measure.REPS) }
    var target by remember { mutableStateOf((existing?.defaultTarget ?: DEFAULT_TARGET).toString()) }
    var weight by remember {
        mutableStateOf(existing?.defaultWeightKg?.let { formatWeight(it) }.orEmpty())
    }
    var deleting by remember { mutableStateOf(false) }

    val targetValue = target.toIntOrNull() ?: 0
    val nameOk = name.isNotBlank()
    val targetOk = targetValue > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (existing == null) R.string.exercise_new else R.string.exercise_edit_title
                        ),
                        style = MaterialTheme.typography.titleLarge.zh()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (existing != null) {
                        OverflowMenu(
                            listOf(stringResource(R.string.action_delete) to { deleting = true })
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
                    enabled = nameOk && targetOk,
                    onClick = {
                        viewModel.saveExercise(
                            Exercise(
                                id = existing?.id ?: viewModel.newId(),
                                name = name.trim(),
                                measure = measure,
                                part = part.trim(),
                                defaultTarget = targetValue,
                                defaultWeightKg = weight.toDoubleOrNull()?.takeIf { it > 0.0 }
                            )
                        )
                        onDone()
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.titleMedium.zh()
                    )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.exercise_name)) },
                singleLine = true,
                isError = name.isNotEmpty() && !nameOk,
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel(stringResource(R.string.exercise_measure))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Measure.entries.forEach { option ->
                    FilterChip(
                        selected = option == measure,
                        onClick = { measure = option },
                        label = { Text(measureLabel(option)) }
                    )
                }
            }

            NumberField(
                value = target,
                onValueChange = { target = it },
                label = stringResource(R.string.exercise_default_target) +
                    "（" + unitLabel(measure) + "）",
                isError = !targetOk,
                errorText = stringResource(R.string.exercise_target_required),
                modifier = Modifier.fillMaxWidth()
            )

            NumberField(
                value = weight,
                onValueChange = { weight = it },
                label = stringResource(R.string.exercise_default_weight),
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel(stringResource(R.string.exercise_part))
            OutlinedTextField(
                value = part,
                onValueChange = { part = it },
                label = { Text(stringResource(R.string.exercise_part_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // 已經用過的部位做成晶片，打字與挑現成的都行
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parts.forEach { option ->
                    FilterChip(
                        selected = option == part,
                        onClick = { part = if (option == part) "" else option },
                        label = { Text(option) }
                    )
                }
            }
        }
    }

    if (deleting && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.exercise_delete_title),
            message = stringResource(R.string.exercise_delete_message),
            onConfirm = {
                viewModel.deleteExercise(existing.id)
                onDone()
            },
            onDismiss = { deleting = false }
        )
    }
}

private const val DEFAULT_TARGET = 12
