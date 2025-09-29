package com.berlin.snatchy.presentation.permissions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDialog(
    permission: IPermission,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (isPermanentlyDeclined) {
                        onGoToAppSettingsClick()
                    } else {
                        onOkClick()
                    }
                }
            ) {
                Text(
                    text = if (isPermanentlyDeclined) {
                        "Open Settings"
                    } else {
                        "Grant Permission"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = permission.getDescription(
                    isPermanentlyDeclined = isPermanentlyDeclined
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp
    )
}


@Composable
@Preview
fun PermissionDialogPreview(){
    PermissionDialog(
        ReadPermission(),
        false,
        {},{},{}
    )
}