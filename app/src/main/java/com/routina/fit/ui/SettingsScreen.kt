package com.routina.fit.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.fit.R
import com.routina.fit.data.BackupCodec
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FitViewModel,
    onOpenDefaults: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var rest by remember { mutableStateOf(settings.defaultRestSec.toString()) }

    // 匯入會改掉設定，欄位要跟著換成新值
    LaunchedEffect(settings) {
        rest = settings.defaultRestSec.toString()
    }

    val restValue = rest.toIntOrNull()
    val restOk = restValue != null && restValue >= 0

    val savedText = stringResource(R.string.settings_saved)
    val exportDone = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)
    val importDone = stringResource(R.string.import_done)
    val importUnreadable = stringResource(R.string.import_failed_unreadable)
    val importNotFit = stringResource(R.string.import_failed_not_fit)
    val importNewer = stringResource(R.string.import_failed_newer)

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup(uri) { ok -> toast(if (ok) exportDone else exportFailed) }
    }

    // 檔案管理器對 .json 標的 MIME 不一致，一律放行由內容判斷
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importBackup(uri) { outcome ->
            when (outcome) {
                is ImportOutcome.Ok -> toast(
                    String.format(importDone, outcome.exercises, outcome.routines, outcome.sessions)
                )
                is ImportOutcome.Failed -> toast(
                    when (outcome.reason) {
                        BackupCodec.Reject.UNREADABLE -> importUnreadable
                        BackupCodec.Reject.NOT_FIT -> importNotFit
                        BackupCodec.Reject.NEWER_SCHEMA -> importNewer
                    }
                )
            }
        }
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_today),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(onClick = onOpenDefaults, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.defaults_title))
            }
            HintText(stringResource(R.string.defaults_hint))

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_training),
                style = MaterialTheme.typography.titleMedium
            )
            NumberField(
                value = rest,
                onValueChange = { rest = it },
                label = stringResource(R.string.settings_default_rest),
                isError = !restOk,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = restOk,
                onClick = {
                    viewModel.setSettings(settings.copy(defaultRestSec = restValue ?: settings.defaultRestSec))
                    toast(savedText)
                }
            ) { Text(stringResource(R.string.action_save)) }

            SwitchRow(
                label = stringResource(R.string.settings_sound),
                checked = settings.sound,
                onChange = { viewModel.setSettings(settings.copy(sound = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.settings_vibrate),
                checked = settings.vibrate,
                onChange = { viewModel.setSettings(settings.copy(vibrate = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.settings_keep_screen_on),
                checked = settings.keepScreenOn,
                onChange = { viewModel.setSettings(settings.copy(keepScreenOn = it)) }
            )
            HintText(stringResource(R.string.settings_keep_screen_on_hint))

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_backup),
                style = MaterialTheme.typography.titleMedium
            )
            HintText(stringResource(R.string.settings_backup_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch(BackupCodec.defaultFileName()) }) {
                    Text(stringResource(R.string.action_export))
                }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.action_import))
                }
            }

            HorizontalDivider()

            HintText(stringResource(R.string.settings_permissions))
            HintText(stringResource(R.string.settings_version, versionName))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
