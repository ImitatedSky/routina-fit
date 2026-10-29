package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.model.DayItem
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 每天的預設清單。換日時今天那份就是照這一份長出來的。
 *
 * 這裡的列不能點著計數——它不是今天的清單，是樣板。所以改用 ⋯ 選單，
 * 和動作庫、課表那邊一樣；長得像可以點的列卻點不動，比多一顆 ⋯ 更糟。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultsScreen(
    viewModel: FitViewModel,
    onBack: () -> Unit
) {
    val defaults by viewModel.defaults.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<DayItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    val copiedText = stringResource(R.string.defaults_copied)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.defaults_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { HintText(stringResource(R.string.defaults_hint)) }

            if (defaults.isEmpty()) {
                item { HintText(stringResource(R.string.defaults_empty)) }
            } else {
                items(defaults, key = { it.id }) { item ->
                    DefaultRow(
                        item = item,
                        onEdit = { editing = item },
                        onDelete = { viewModel.setDefaults(defaults.filterNot { it.id == item.id }) }
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { adding = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.defaults_add))
                }
            }

            item {
                OutlinedButton(
                    enabled = today.items.isNotEmpty(),
                    onClick = {
                        viewModel.saveTodayAsDefaults()
                        scope.launch { snackbar.showSnackbar(copiedText) }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) { Text(stringResource(R.string.defaults_from_today)) }
            }
        }
    }

    if (adding) {
        DayItemDialog(
            title = stringResource(R.string.defaults_add),
            item = null,
            onConfirm = { name, target ->
                viewModel.setDefaults(
                    defaults + DayItem(id = UUID.randomUUID().toString(), name = name, target = target)
                )
            },
            onDismiss = { adding = false }
        )
    }

    editing?.let { item ->
        DayItemDialog(
            title = stringResource(R.string.today_edit_item),
            item = item,
            onConfirm = { name, target ->
                viewModel.setDefaults(
                    defaults.map {
                        if (it.id == item.id) it.copy(name = name, target = target) else it
                    }
                )
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun DefaultRow(item: DayItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (item.hasTarget) {
                    stringResource(R.string.log_done_only, item.target, stringResource(R.string.unit_reps))
                } else {
                    stringResource(R.string.item_no_target)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OverflowMenu(listOf(editLabel to onEdit, deleteLabel to onDelete))
        }
    }
}
