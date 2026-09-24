package app.hullbeat.ui.store

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.StorageLocation
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.storageLocationDisplayName

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPartSheet(
    partToEdit: StoreItemUiModel? = null,
    locations: List<StorageLocation>,
    components: List<Component>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        partNumber: String?,
        manufacturer: String?,
        note: String?,
        locationId: Long?,
        quantity: Double,
        minQuantity: Double?,
        linkedComponentIds: List<Long>,
    ) -> Unit,
    onRequestCreateLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val initialDetail = partToEdit?.detail
    var name by remember { mutableStateOf(initialDetail?.partName.orEmpty()) }
    var partNumber by remember { mutableStateOf(initialDetail?.partNumber.orEmpty()) }
    var manufacturer by remember { mutableStateOf(initialDetail?.manufacturer.orEmpty()) }
    var note by remember { mutableStateOf(initialDetail?.partNote.orEmpty()) }
    var locationId by remember { mutableStateOf(initialDetail?.locationId) }

    var qtyText by remember {
        mutableStateOf(formatNumber(initialDetail?.quantity ?: 1.0))
    }
    var minQtyText by remember {
        mutableStateOf(initialDetail?.minQuantity?.let { formatNumber(it) }.orEmpty())
    }

    val selectedComponentIds = remember {
        mutableStateListOf<Long>().apply {
            partToEdit?.linkedComponents?.forEach { add(it.componentId) }
        }
    }
    val initiallyLinkedIds = remember {
        partToEdit?.linkedComponents?.map { it.componentId }?.toSet().orEmpty()
    }
    var compFilterText by remember { mutableStateOf("") }

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
                text = stringResource(if (partToEdit == null) R.string.store_add_part else R.string.store_edit_part),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.store_part_name)) },
                placeholder = { Text(stringResource(R.string.store_part_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = partNumber,
                onValueChange = { partNumber = it },
                label = { Text(stringResource(R.string.store_part_number)) },
                placeholder = { Text(stringResource(R.string.store_part_number_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = manufacturer,
                onValueChange = { manufacturer = it },
                label = { Text(stringResource(R.string.store_part_mfr)) },
                placeholder = { Text(stringResource(R.string.store_part_mfr_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Quantity row with +/-
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text(stringResource(R.string.store_part_qty)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            val current = qtyText.toDoubleOrNull() ?: 0.0
                            val next = maxOf(0.0, current - 1.0)
                            qtyText = formatNumber(next)
                        },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val current = qtyText.toDoubleOrNull() ?: 0.0
                            val next = current + 1.0
                            qtyText = formatNumber(next)
                        },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Min quantity row with +/-
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = minQtyText,
                    onValueChange = { minQtyText = it },
                    label = { Text(stringResource(R.string.store_part_min_qty)) },
                    placeholder = { Text(stringResource(R.string.store_part_min_qty_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            val current = minQtyText.toDoubleOrNull() ?: 0.0
                            val next = maxOf(0.0, current - 1.0)
                            minQtyText = if (next == 0.0) "" else formatNumber(next)
                        },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val current = minQtyText.toDoubleOrNull() ?: 0.0
                            val next = current + 1.0
                            minQtyText = formatNumber(next)
                        },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Locker / Location selection
            Text(
                text = stringResource(R.string.store_location_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Wrapping instead of one long scrolling line: on a phone a row of
            // chips shows two or three of a dozen lockers, and the rest are found
            // only by swiping sideways inside a sheet that already scrolls down.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = locationId == null,
                    onClick = { locationId = null },
                    label = { Text(stringResource(R.string.store_no_location)) },
                )
                locations.forEach { loc ->
                    FilterChip(
                        selected = locationId == loc.id,
                        onClick = { locationId = loc.id },
                        label = { Text(storageLocationDisplayName(context, loc.name)) },
                    )
                }
                AssistChip(
                    onClick = onRequestCreateLocation,
                    label = { Text(stringResource(R.string.store_new_location_prompt)) },
                )
            }

            // Linked Components
            Text(
                text = stringResource(R.string.store_linked_components),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.store_linked_components_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (components.isNotEmpty()) {
                if (components.size > 8) {
                    OutlinedTextField(
                        value = compFilterText,
                        onValueChange = { compFilterText = it },
                        placeholder = { Text(stringResource(R.string.store_filter_components_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val cleanCompFilter = compFilterText.trim().lowercase()
                val filteredComponents = if (cleanCompFilter.isEmpty()) {
                    components
                } else {
                    components.filter { comp ->
                        comp.displayName(context).lowercase().contains(cleanCompFilter)
                    }
                }
                // Already-linked components come first, but the order is frozen at
                // the moment the sheet opens: re-sorting on every tap would move
                // the chips out from under the finger between two selections.
                val visibleComponents = filteredComponents
                    .sortedByDescending { initiallyLinkedIds.contains(it.id) }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    visibleComponents.forEach { comp ->
                        val isSelected = selectedComponentIds.contains(comp.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedComponentIds.remove(comp.id)
                                } else {
                                    selectedComponentIds.add(comp.id)
                                }
                            },
                            label = { Text(comp.displayName(context)) },
                            // A tick rather than an icon: this project draws its
                            // glyphs as text everywhere (see the +/- buttons above)
                            // and carries no icon dependency of its own.
                            leadingIcon = if (isSelected) {
                                {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.store_part_notes)) },
                placeholder = { Text(stringResource(R.string.store_part_notes_hint)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Action buttons
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
                        if (name.isNotBlank()) {
                            val finalQty = qtyText.toDoubleOrNull() ?: 1.0
                            val finalMin = minQtyText.toDoubleOrNull()
                            onSave(
                                name.trim(),
                                partNumber.trim().ifBlank { null },
                                manufacturer.trim().ifBlank { null },
                                note.trim().ifBlank { null },
                                locationId,
                                finalQty,
                                finalMin,
                                selectedComponentIds.toList(),
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
