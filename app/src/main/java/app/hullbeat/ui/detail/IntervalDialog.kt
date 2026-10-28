package app.hullbeat.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.domain.DecimalInput
import app.hullbeat.domain.IntervalInput
import app.hullbeat.domain.IntervalInput.Field

/**
 * "365 days or 200 h, whichever comes first" - the limits the schedule
 * counts from, as DueCalculator reads them. Null when there are none.
 */
fun intervalSummary(context: android.content.Context, schedule: ServiceSchedule): String? {
    val res = context.resources
    val parts = buildList {
        schedule.intervalDays?.let { add(res.getQuantityString(R.plurals.interval_days, it, it)) }
        schedule.intervalHours?.let { add(res.getString(R.string.interval_hours, IntervalInput.format(it))) }
        schedule.intervalMiles?.let { add(res.getString(R.string.interval_miles, IntervalInput.format(it))) }
    }
    if (parts.isEmpty()) return null
    val joined = parts.reduce { a, b -> res.getString(R.string.interval_or, a, b) }
    return if (parts.size > 1) res.getString(R.string.interval_whichever_first, joined) else joined
}

/**
 * Days and engine hours, both optional, at least one. Miles only on a
 * schedule that already counts them (trailer bearings): a road-mile field on
 * every engine part would be noise.
 *
 * Invalid fields explain themselves in neutral text rather than the error
 * colour: this sits over the status line, where red means overdue.
 */
@Composable
fun IntervalDialog(
    schedule: ServiceSchedule,
    onDismiss: () -> Unit,
    onSave: (IntervalInput.Result.Valid) -> Unit,
) {
    var days by rememberSaveable { mutableStateOf(schedule.intervalDays?.toString() ?: "") }
    var hours by rememberSaveable { mutableStateOf(IntervalInput.format(schedule.intervalHours)) }
    var miles by rememberSaveable { mutableStateOf(IntervalInput.format(schedule.intervalMiles)) }
    val showMiles = schedule.intervalMiles != null

    val result = IntervalInput.parse(days, hours, if (showMiles) miles else "")
    val bad = (result as? IntervalInput.Result.Invalid)?.fields.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.interval_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.interval_dialog_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IntervalField(
                    value = days,
                    onValueChange = { days = it },
                    label = stringResource(R.string.field_interval_days),
                    problem = if (Field.DAYS in bad) stringResource(R.string.interval_error_days) else null,
                    keyboardType = KeyboardType.Number,
                )
                IntervalField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = stringResource(R.string.field_meter_hours),
                    problem = if (Field.HOURS in bad) stringResource(R.string.interval_error_number) else null,
                )
                if (showMiles) {
                    IntervalField(
                        value = miles,
                        onValueChange = { miles = it },
                        label = stringResource(R.string.field_interval_miles),
                        problem = if (Field.MILES in bad) stringResource(R.string.interval_error_number) else null,
                    )
                }
                if (result is IntervalInput.Result.Empty) {
                    Text(
                        text = stringResource(R.string.interval_error_none),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { (result as? IntervalInput.Result.Valid)?.let(onSave) },
                enabled = result is IntervalInput.Result.Valid,
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
    )
}

@Composable
private fun IntervalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    problem: String?,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Same filter as the meter field: digits and one separator.
            if (DecimalInput.isPartialInput(input)) onValueChange(input)
        },
        label = { Text(label) },
        supportingText = problem?.let {
            {
                Text(
                    text = it,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
