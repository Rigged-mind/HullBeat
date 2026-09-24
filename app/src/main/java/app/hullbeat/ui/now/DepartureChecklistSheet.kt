package app.hullbeat.ui.now

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.data.catalog.ChecklistItemDef
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.displayName
import app.hullbeat.ui.theme.LocalExtendedColors

/**
 * Bottom sheet displaying the pre-departure checklist for a vessel.
 *
 * Provides cockpit-sized checkboxes and a primary confirmation button
 * that records a ChecklistRun in Room.
 *
 * Opened from the fleet view, it gets the whole fleet in [vessels] and shows
 * them as chips in place of the name: a boat leaves on its own, never "all",
 * so the owner picks which one without closing the sheet. A tick on a chip
 * means that boat is already done today.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartureChecklistSheet(
    vessel: Vessel,
    items: List<ChecklistItemDef>,
    isDoneToday: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    vessels: List<Vessel> = emptyList(),
    doneVesselIds: Set<Long> = emptySet(),
    onSelectVessel: (Long) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val extended = LocalExtendedColors.current

    // Keyed on the boat as well: two boats with the same list would otherwise
    // carry one boat's ticks over to the other, and the second could be
    // confirmed without a single item looked at.
    val checkedStates = remember(vessel.id, items, isDoneToday) {
        mutableStateMapOf<String, Boolean>().apply {
            if (isDoneToday) {
                items.forEach { put(it.code, true) }
            }
        }
    }

    val allChecked = items.isNotEmpty() && items.all { checkedStates[it.code] == true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.departure_checklist_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (vessels.size < 2) {
                        Text(
                            text = vessel.displayName(context),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isDoneToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = extended.status.ok,
                        )
                        Text(
                            text = stringResource(R.string.departure_check_done_today),
                            color = extended.status.ok,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (vessels.size >= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    vessels.forEach { v ->
                        FilterChip(
                            selected = v.id == vessel.id,
                            onClick = { onSelectVessel(v.id) },
                            label = { Text(v.displayName(context)) },
                            leadingIcon = if (v.id in doneVesselIds) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = stringResource(R.string.departure_check_done_today),
                                        modifier = Modifier.size(18.dp),
                                        tint = extended.status.ok,
                                    )
                                }
                            } else null,
                            modifier = Modifier.heightIn(min = 56.dp),
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.departure_check_empty_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.code }) { item ->
                        val isChecked = checkedStates[item.code] ?: false
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkedStates[item.code] = !isChecked },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checkedStates[item.code] = it },
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = item.title(context),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (isDoneToday) {
                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_reset_departure_check),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = onConfirm,
                        enabled = allChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_confirm_departure_check),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (!allChecked && items.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.departure_check_all_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
