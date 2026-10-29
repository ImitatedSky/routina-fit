package com.routina.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routina.fit.R
import com.routina.fit.model.DayItem

/**
 * 改一項：名字與要做幾下。新增與編輯共用，今天的清單與預設清單也共用。
 *
 * 目標可以留空——「核心」這種想到就做幾下的項目，硬要給一個數字只是亂填。
 */
@Composable
fun DayItemDialog(
    title: String,
    item: DayItem?,
    onConfirm: (name: String, target: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var target by remember {
        mutableStateOf(item?.target?.takeIf { it > 0 }?.toString().orEmpty())
    }
    val nameOk = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField(
                    value = target,
                    onValueChange = { target = it },
                    label = stringResource(R.string.item_target),
                    modifier = Modifier.fillMaxWidth()
                )
                HintText(stringResource(R.string.item_target_hint))
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameOk,
                onClick = {
                    // 先取值再關：關掉會把呼叫端的狀態清掉，順序反了就讀到空的
                    val value = target.toIntOrNull() ?: 0
                    val typed = name.trim()
                    onDismiss()
                    onConfirm(typed, value)
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
