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
 * 改一項：名字、要做幾下，還有已經做了幾下。新增與編輯共用，今天的清單與預設清單也共用。
 *
 * 目標可以留空——「核心」這種想到就做幾下的項目，硬要給一個數字只是亂填。
 *
 * [countEditable] 只有今天的清單給 true。**數字一定要能直接改**：忘記按、按太多下、
 * 或是在別的地方做完才想到要記，都是靠點一下慢慢加回去補不完的。
 * 預設清單那邊沒有「已經做了幾下」這回事，所以不顯示。
 */
@Composable
fun DayItemDialog(
    title: String,
    item: DayItem?,
    countEditable: Boolean = false,
    onConfirm: (name: String, target: Int, done: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var target by remember {
        mutableStateOf(item?.target?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var done by remember { mutableStateOf((item?.done ?: 0).toString()) }
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
                if (countEditable) {
                    NumberField(
                        value = done,
                        onValueChange = { done = it },
                        label = stringResource(R.string.item_done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameOk,
                onClick = {
                    // 先取值再關：關掉會把呼叫端的狀態清掉，順序反了就讀到空的
                    val wanted = target.toIntOrNull() ?: 0
                    val counted = done.toIntOrNull() ?: 0
                    val typed = name.trim()
                    onDismiss()
                    onConfirm(typed, wanted, counted)
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
