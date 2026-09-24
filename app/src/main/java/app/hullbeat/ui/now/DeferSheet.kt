package app.hullbeat.ui.now

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import app.hullbeat.ui.components.AppDatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.db.displayName
import java.time.LocalDate

/**
 * Bottom sheet to defer an overdue or due-soon item (ui-spec.md §240-260).
 *
 * Provides quick presets (one month, haul-out, spring) and an optional
 * explanation note. The item leaves "Now" and returns automatically when the date arrives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeferSheet(
    item: NowItem,
    onDismiss: () -> Unit,
    onDefer: (until: LocalDate, reason: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val today = LocalDate.now()
    val oneMonth = today.plusMonths(1)
    val haulOut = if (today.monthValue < 10) today.withMonth(10).withDayOfMonth(15) else today.plusYears(1).withMonth(10).withDayOfMonth(15)
    val spring = if (today.monthValue < 4) today.withMonth(4).withDayOfMonth(1) else today.plusYears(1).withMonth(4).withDayOfMonth(1)

    var selectedDate by remember { mutableStateOf(oneMonth) }
    var customDateText by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var reasonText by remember { mutableStateOf("") }

    val effectiveDate: LocalDate? = if (isCustom) {
        try {
            LocalDate.parse(customDateText.trim())
        } catch (_: Exception) {
            null
        }
    } else {
        selectedDate
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.defer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = item.component.displayName(context),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.defer_until_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !isCustom && selectedDate == oneMonth,
                    onClick = {
                        isCustom = false
                        selectedDate = oneMonth
                    },
                    label = { Text(stringResource(R.string.defer_option_one_month)) },
                )

                FilterChip(
                    selected = !isCustom && selectedDate == haulOut,
                    onClick = {
                        isCustom = false
                        selectedDate = haulOut
                    },
                    label = { Text(stringResource(R.string.defer_option_haul_out)) },
                )

                FilterChip(
                    selected = !isCustom && selectedDate == spring,
                    onClick = {
                        isCustom = false
                        selectedDate = spring
                    },
                    label = { Text(stringResource(R.string.defer_option_spring)) },
                )
            }

            OutlinedTextField(
                value = if (isCustom) customDateText else selectedDate.toString(),
                onValueChange = {
                    isCustom = true
                    customDateText = it
                },
                label = { Text(stringResource(R.string.defer_option_pick_date)) },
                placeholder = { Text("YYYY-MM-DD") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = stringResource(R.string.defer_option_pick_date),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                singleLine = true,
                isError = isCustom && effectiveDate == null,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = reasonText,
                onValueChange = { reasonText = it },
                label = { Text(stringResource(R.string.defer_reason_label)) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.defer_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        effectiveDate?.let { date ->
                            onDefer(date, reasonText.ifBlank { null })
                        }
                    },
                    enabled = effectiveDate != null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = effectiveDate ?: selectedDate,
            onDateSelected = { picked ->
                isCustom = true
                customDateText = picked.toString()
                selectedDate = picked
            },
            onDismiss = { showDatePicker = false },
        )
    }
}
