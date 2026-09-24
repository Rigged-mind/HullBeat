package app.hullbeat.ui.store

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hullbeat.R
import app.hullbeat.data.db.StorageLocation
import app.hullbeat.data.db.storageLocationDisplayName

@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
    onOpenComponent: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddPartSheet by remember { mutableStateOf(false) }
    var editingPart by remember { mutableStateOf<StoreItemUiModel?>(null) }
    var partToDelete by remember { mutableStateOf<StoreItemUiModel?>(null) }

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<StorageLocation?>(null) }
    var locationToDelete by remember { mutableStateOf<StorageLocation?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Summary & Stats header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Top stats bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.store_stat_items,
                            uiState.totalCount,
                            uiState.totalCount,
                        ) + " " + pluralStringResource(
                            R.plurals.store_stat_lockers,
                            uiState.lockerCount,
                            uiState.lockerCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (uiState.outOfStockCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.store_stat_gone, uiState.outOfStockCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        if (uiState.lowStockCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.store_stat_low, uiState.lowStockCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.store_search_hint)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_hint),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.action_cancel),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Action buttons: "+ Part" and "+ Locker"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showAddPartSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.store_add_part))
                    }

                    OutlinedButton(
                        onClick = { showAddLocationDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.store_add_location))
                    }
                }

                // Tabs: All / To Buy / Lockers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.selectedTab == StoreTab.ALL,
                        onClick = { viewModel.selectTab(StoreTab.ALL) },
                        label = {
                            Text(
                                stringResource(R.string.store_tab_all) +
                                    if (uiState.totalCount > 0) " (${uiState.totalCount})" else ""
                            )
                        },
                    )

                    val toBuyBadge = uiState.outOfStockCount + uiState.lowStockCount
                    FilterChip(
                        selected = uiState.selectedTab == StoreTab.TO_BUY,
                        onClick = { viewModel.selectTab(StoreTab.TO_BUY) },
                        label = {
                            Text(
                                stringResource(R.string.store_tab_tobuy) +
                                    if (toBuyBadge > 0) " ($toBuyBadge)" else ""
                            )
                        },
                    )

                    FilterChip(
                        selected = uiState.selectedTab == StoreTab.LOCATIONS,
                        onClick = { viewModel.selectTab(StoreTab.LOCATIONS) },
                        label = {
                            Text(
                                stringResource(R.string.store_tab_locations) +
                                    if (uiState.lockerCount > 0) " (${uiState.lockerCount})" else ""
                            )
                        },
                    )
                }
            }
        }

        // Main content area
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.totalCount == 0 && uiState.lockerCount == 0) {
            // Global empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_store),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = stringResource(R.string.store_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )

                        Text(
                            text = stringResource(R.string.store_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Button(
                            onClick = { showAddPartSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.store_add_part))
                        }

                        OutlinedButton(
                            onClick = { viewModel.seedDefaultLocations(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.store_seed_locations_btn))
                        }
                    }
                }
            }
        } else {
            when (uiState.selectedTab) {
                StoreTab.ALL, StoreTab.TO_BUY -> {
                    if (uiState.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (uiState.selectedTab == StoreTab.TO_BUY) {
                                    stringResource(R.string.store_tobuy_empty)
                                } else {
                                    stringResource(R.string.store_empty_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.items, key = { it.detail.id }) { item ->
                                StorePartCard(
                                    item = item,
                                    onQuickAdjust = { delta ->
                                        viewModel.quickAdjustQuantity(
                                            itemId = item.detail.id,
                                            delta = delta,
                                        )
                                    },
                                    onEdit = { editingPart = item },
                                    onDelete = { partToDelete = item },
                                    onOpenComponent = onOpenComponent,
                                )
                            }
                        }
                    }
                }

                StoreTab.LOCATIONS -> {
                    if (uiState.locations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.store_locations_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                OutlinedButton(
                                    onClick = { viewModel.seedDefaultLocations(context) },
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(stringResource(R.string.store_seed_locations_btn))
                                }
                            }
                        }
                    } else {
                        val itemsByLocation = remember(uiState.items) {
                            uiState.items.groupBy { it.detail.locationId }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(uiState.locations, key = { it.id }) { location ->
                                val partsInLocation = itemsByLocation[location.id].orEmpty()
                                LockerGroupCard(
                                    location = location,
                                    parts = partsInLocation,
                                    onEditLocation = { editingLocation = location },
                                    onDeleteLocation = { locationToDelete = location },
                                    onQuickAdjust = { itemId, _, delta ->
                                        viewModel.quickAdjustQuantity(itemId, delta)
                                    },
                                    onEditPart = { editingPart = it },
                                    onDeletePart = { partToDelete = it },
                                    onOpenComponent = onOpenComponent,
                                )
                            }

                            // Unassigned parts (no locker)
                            val unassignedParts = itemsByLocation[null].orEmpty()
                            if (unassignedParts.isNotEmpty()) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.store_no_location),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            partsInLocationList(
                                                parts = unassignedParts,
                                                onQuickAdjust = { itemId, _, delta ->
                                                    viewModel.quickAdjustQuantity(itemId, delta)
                                                },
                                                onEditPart = { editingPart = it },
                                                onDeletePart = { partToDelete = it },
                                                onOpenComponent = onOpenComponent,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Part Sheet
    if (showAddPartSheet || editingPart != null) {
        AddEditPartSheet(
            partToEdit = editingPart,
            locations = uiState.locations,
            components = uiState.components,
            onDismiss = {
                showAddPartSheet = false
                editingPart = null
            },
            onSave = { name, partNumber, manufacturer, note, locationId, quantity, minQuantity, linkedComponentIds ->
                viewModel.savePart(
                    itemId = editingPart?.detail?.id,
                    partId = editingPart?.detail?.partId,
                    name = name,
                    partNumber = partNumber,
                    manufacturer = manufacturer,
                    note = note,
                    locationId = locationId,
                    quantity = quantity,
                    minQuantity = minQuantity,
                    linkedComponentIds = linkedComponentIds,
                    onComplete = {
                        showAddPartSheet = false
                        editingPart = null
                    },
                )
            },
            onRequestCreateLocation = {
                showAddLocationDialog = true
            },
        )
    }

    // Add / Edit Location Dialog
    if (showAddLocationDialog || editingLocation != null) {
        AddLocationDialog(
            location = editingLocation,
            onDismiss = {
                showAddLocationDialog = false
                editingLocation = null
            },
            onSave = { name, qrCode, note ->
                viewModel.saveLocation(
                    locationId = editingLocation?.id,
                    name = name,
                    qrCode = qrCode,
                    note = note,
                    onComplete = {
                        showAddLocationDialog = false
                        editingLocation = null
                    },
                )
            },
        )
    }

    // Confirm Delete Part Dialog
    partToDelete?.let { part ->
        AlertDialog(
            onDismissRequest = { partToDelete = null },
            title = { Text(stringResource(R.string.store_delete_part)) },
            text = {
                Text(stringResource(R.string.store_delete_part_confirm, part.detail.partName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePart(
                            itemId = part.detail.id,
                            partId = part.detail.partId,
                            onComplete = { partToDelete = null },
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.dialog_delete_record_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { partToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Confirm Delete Location Dialog
    locationToDelete?.let { loc ->
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text(stringResource(R.string.store_delete_location)) },
            text = {
                Text(stringResource(R.string.store_delete_location_confirm))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLocation(
                            locationId = loc.id,
                            onComplete = { locationToDelete = null },
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.dialog_delete_record_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { locationToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
fun StorePartCard(
    item: StoreItemUiModel,
    onQuickAdjust: (delta: Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenComponent: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = item.detail
    val context = LocalContext.current

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header row: part name + status badge + edit/delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = detail.partName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (item.isOutOfStock) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.store_out_of_stock),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        } else if (item.isLowStock) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.store_low_stock),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    val mfrAndNumber = listOfNotNull(
                        detail.manufacturer?.ifBlank { null },
                        detail.partNumber?.ifBlank { null },
                    ).joinToString(" · ")

                    if (mfrAndNumber.isNotBlank()) {
                        Text(
                            text = mfrAndNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (!detail.partNote.isNullOrBlank()) {
                        Text(
                            text = detail.partNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.store_edit_part),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.store_delete_part),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Locker badge & linked components chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val locLabel = detail.locationName?.let { storageLocationDisplayName(context, it) }
                    ?: stringResource(R.string.store_no_location)
                AssistChip(
                    onClick = {},
                    label = { Text(locLabel, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp),
                )

                item.linkedComponents.forEach { comp ->
                    val compLabel = comp.componentCustomName ?: comp.componentName
                    AssistChip(
                        onClick = { onOpenComponent(comp.componentId) },
                        label = { Text(compLabel, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            // Bottom row: Quick counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val currentFormatted = if (detail.quantity % 1.0 == 0.0) {
                    detail.quantity.toInt().toString()
                } else {
                    detail.quantity.toString()
                }
                val minFormatted = detail.minQuantity?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                }
                val displayStock = if (minFormatted != null) {
                    stringResource(R.string.store_qty_badge, currentFormatted, minFormatted)
                } else {
                    currentFormatted
                }

                // The label yields, the stepper never does. Without a weight the
                // label was measured first at whatever width it wanted and the
                // three buttons took the remainder - which went negative as soon
                // as the text grew, pushing "+" off the right edge of the screen.
                Text(
                    text = "${stringResource(R.string.store_part_qty)}: $displayStock",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        onClick = { onQuickAdjust(-1.0) },
                        enabled = detail.quantity > 0.0,
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = currentFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )

                    Button(
                        onClick = { onQuickAdjust(1.0) },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LockerGroupCard(
    location: StorageLocation,
    parts: List<StoreItemUiModel>,
    onEditLocation: () -> Unit,
    onDeleteLocation: () -> Unit,
    onQuickAdjust: (Long, Double, Double) -> Unit,
    onEditPart: (StoreItemUiModel) -> Unit,
    onDeletePart: (StoreItemUiModel) -> Unit,
    onOpenComponent: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = storageLocationDisplayName(context, location.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val labelCode = location.qrCode
                    if (!labelCode.isNullOrBlank()) {
                        Text(
                            text = labelCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.store_items_count, parts.size),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    IconButton(
                        onClick = onEditLocation,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.store_edit_location),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = onDeleteLocation,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.store_delete_location),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (parts.isEmpty()) {
                Text(
                    text = stringResource(R.string.store_loc_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                partsInLocationList(
                    parts = parts,
                    onQuickAdjust = onQuickAdjust,
                    onEditPart = onEditPart,
                    onDeletePart = onDeletePart,
                    onOpenComponent = onOpenComponent,
                )
            }
        }
    }
}

@Composable
private fun partsInLocationList(
    parts: List<StoreItemUiModel>,
    onQuickAdjust: (Long, Double, Double) -> Unit,
    onEditPart: (StoreItemUiModel) -> Unit,
    onDeletePart: (StoreItemUiModel) -> Unit,
    onOpenComponent: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            StorePartCard(
                item = part,
                onQuickAdjust = { delta ->
                    onQuickAdjust(part.detail.id, part.detail.quantity, delta)
                },
                onEdit = { onEditPart(part) },
                onDelete = { onDeletePart(part) },
                onOpenComponent = onOpenComponent,
            )
        }
    }
}
