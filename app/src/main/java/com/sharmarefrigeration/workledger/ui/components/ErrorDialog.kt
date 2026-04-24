package com.sharmarefrigeration.workledger.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("An Error Occurred") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm?.invoke()
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        modifier = modifier
    )
}