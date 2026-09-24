package app.hullbeat.ui.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import app.hullbeat.R
import app.hullbeat.data.db.StorageLocation

@Composable
fun AddLocationDialog(
    location: StorageLocation? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, qrCode: String?, note: String?) -> Unit,
) {
    var name by remember { mutableStateOf(location?.name.orEmpty()) }
    var qrCode by remember { mutableStateOf(location?.qrCode.orEmpty()) }
    var note by remember { mutableStateOf(location?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (location == null) R.string.store_add_location else R.string.store_edit_location
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.store_location_name)) },
                    placeholder = { Text(stringResource(R.string.store_location_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = qrCode,
                    onValueChange = { qrCode = it },
                    label = { Text(stringResource(R.string.store_location_qr)) },
                    placeholder = { Text(stringResource(R.string.store_location_qr_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.store_part_notes)) },
                    placeholder = { Text(stringResource(R.string.store_part_notes_hint)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), qrCode.trim().ifBlank { null }, note.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
