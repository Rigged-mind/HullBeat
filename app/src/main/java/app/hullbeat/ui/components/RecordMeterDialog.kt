package app.hullbeat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.ui.theme.LocalExtendedColors
import java.time.LocalDate

@Composable
fun RecordMeterDialog(
    currentHours: Double?,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, hours: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalExtendedColors.current
    var date by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    val initialHours: String = currentHours?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
    var hoursText by remember { mutableStateOf<String>(initialHours) }
    var decreaseAcknowledged by remember { mutableStateOf(false) }

    val parsedHours = hoursText.toDoubleOrNull()
    val isDecrease = currentHours != null && parsedHours != null && parsedHours < currentHours
    val canSave = parsedHours != null && parsedHours > 0.0 && (!isDecrease || decreaseAcknowledged)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.record_meter_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DateSelectionRow(
                    selectedDate = date,
                    onDateChange = { date = it },
                )

                if (currentHours != null) {
                    Text(
                        text = "${stringResource(R.string.meter_current_reading)}: $currentHours ${stringResource(R.string.meter_hours_unit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            hoursText = input
                        }
                    },
                    label = { Text(stringResource(R.string.field_meter_hours)) },
                    placeholder = { Text("1245.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isDecrease) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = extended.tint.dueSoon,
                        border = BorderStroke(1.dp, extended.status.dueSoon),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.meter_decrease_warning, currentHours ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { decreaseAcknowledged = !decreaseAcknowledged },
                            ) {
                                Checkbox(
                                    checked = decreaseAcknowledged,
                                    onCheckedChange = { decreaseAcknowledged = it },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.meter_decrease_confirm),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedHours != null) {
                        onSave(date, parsedHours)
                    }
                },
                enabled = canSave,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier,
    )
}
