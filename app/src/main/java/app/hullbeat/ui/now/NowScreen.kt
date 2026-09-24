package app.hullbeat.ui.now

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.formatRecordDescription
import app.hullbeat.domain.DueCalculator
import app.hullbeat.ui.components.RecordMeterDialog
import app.hullbeat.ui.theme.LocalExtendedColors
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * The Зараз (Now) dashboard - the primary cockpit landing screen.
 */
@Composable
fun NowScreen(
    viewModel: NowViewModel,
    uiState: NowUiState,
    pendingComponentId: Long? = null,
    pendingRenew: Boolean = false,
    onOpenComponent: (componentId: Long) -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onNavigateToLog: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val ctx = context
    val extended = LocalExtendedColors.current

    var quickLogItem by remember { mutableStateOf<NowItem?>(null) }
    var deferItem by remember { mutableStateOf<NowItem?>(null) }
    var showMeterDialog by remember { mutableStateOf(false) }
    var meterDialogTargetVesselId by remember { mutableStateOf<Long?>(null) }
    var meterDialogCurrentHours by remember { mutableStateOf<Double?>(null) }
    var showUnrecordedSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pendingComponentId, uiState.items) {
        if (pendingComponentId != null && pendingComponentId > 0) {
            val match = uiState.items.find { it.component.id == pendingComponentId }
                ?: uiState.upcomingItems.find { it.component.id == pendingComponentId }
            if (match != null) {
                quickLogItem = match
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // This Scaffold exists only to host the snackbar; it sits inside the
        // main scaffold's content, which has already padded for the status and
        // navigation bars. Material3 does not consume those insets, so the
        // default here applied them a second time: a 50dp empty band under the
        // top bar and 24dp of it above the tab bar, on this screen alone.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "departure_chip") {
                val isDone = uiState.departureCheckDoneToday
                Surface(
                    onClick = onOpenChecklist,
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isDone) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isDone) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = extended.status.ok,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.departure_check_done_today),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_nav_boat),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                // In the fleet view no single boat is meant, so
                                // no item count either - each boat's list is its
                                // own. Say which boats are done instead.
                                val doneNames = uiState.vessels
                                    .filter { it.id in uiState.departureDoneVesselIds }
                                    .joinToString(", ") { it.displayName(context) }
                                Text(
                                    text = when {
                                        !uiState.isFleetView -> pluralStringResource(
                                            R.plurals.departure_check_chip,
                                            uiState.departureCheckCount,
                                            uiState.departureCheckCount,
                                        )
                                        doneNames.isEmpty() -> stringResource(R.string.departure_checklist_title)
                                        else -> stringResource(R.string.departure_check_fleet_partial, doneNames)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }


            item(key = "status_banner") {
                if (uiState.overdueCount > 0 || uiState.soonCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Row {
                                Text(
                                    text = "${uiState.overdueCount}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.status.overdue,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.status_overdue),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alignByBaseline(),
                                )
                            }

                            Row {
                                Text(
                                    text = "${uiState.soonCount}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.status.dueSoon,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.status_soon),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alignByBaseline(),
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = extended.status.ok,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                // A deferral is not urgent work, so it stays out
                                // of the two counters - but its card is in the
                                // list below, and "no urgent work today" printed
                                // over a visible "deferred until October" reads
                                // as a contradiction. Said here instead.
                                Text(
                                    text = if (uiState.deferredCount > 0) {
                                        stringResource(
                                            R.string.all_clear_with_deferred,
                                            uiState.deferredCount,
                                        )
                                    } else {
                                        stringResource(R.string.all_clear_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.all_clear_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionButton(
                        icon = R.drawable.ic_edit,
                        label = stringResource(R.string.now_action_quick_log),
                        onClick = onOpenSearch,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionButton(
                        icon = R.drawable.ic_timer,
                        label = stringResource(R.string.now_action_enter_hours),
                        onClick = {
                            meterDialogTargetVesselId = uiState.selectedVesselId ?: uiState.activeVessel?.id
                            meterDialogCurrentHours = uiState.primaryMeterHours
                            showMeterDialog = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionButton(
                        icon = R.drawable.ic_check,
                        label = stringResource(R.string.now_action_checklists),
                        onClick = onOpenChecklist,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val isFleetView = uiState.isFleetView

            if (uiState.items.isNotEmpty()) {
                items(
                    items = uiState.items,
                    key = { "${it.component.id}_${it.schedule.id}" },
                ) { item ->
                    NowCard(
                        item = item,
                        showVesselBadge = isFleetView,
                        onDone = { quickLogItem = item },
                        onDefer = { deferItem = item },
                        onUndefer = {
                            viewModel.undeferSchedule(item.schedule.id) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = ctx.getString(R.string.snack_deferral_cancelled),
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            }
                        },
                        onOpenDetail = { onOpenComponent(item.component.id) },
                    )
                }
            }

            if (uiState.upcomingItems.isNotEmpty()) {
                item(key = "upcoming_header") {
                    Text(
                        text = stringResource(R.string.now_section_upcoming),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(
                    items = uiState.upcomingItems,
                    key = { "upcoming_${it.component.id}_${it.schedule.id}" },
                ) { upcomingItem ->
                    UpcomingMaintenanceCard(
                        item = upcomingItem,
                        showVesselBadge = isFleetView,
                        onLog = { quickLogItem = upcomingItem },
                        onOpenDetail = { onOpenComponent(upcomingItem.component.id) },
                    )
                }
            }

            uiState.recentActivity?.let { activity ->
                item(key = "recent_activity") {
                    RecentActivityCard(
                        activity = activity,
                        showVesselBadge = isFleetView,
                        onNavigateToLog = onNavigateToLog,
                    )
                }
            }

            if (uiState.waitingMetersByVessel.isNotEmpty()) {
                items(
                    items = uiState.waitingMetersByVessel,
                    key = { "needs_meter_${it.vesselId}" },
                ) { entry ->
                    val vName = entry.vessel?.displayName(context) ?: entry.vesselName
                    val rowText = if (isFleetView) {
                        "$vName: " + pluralStringResource(
                            R.plurals.now_needs_meter,
                            entry.count,
                            entry.count,
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.now_needs_meter,
                            entry.count,
                            entry.count,
                        )
                    }
                    WaitingRow(
                        text = rowText,
                        actionText = stringResource(R.string.now_enter_hours),
                        onClick = {
                            meterDialogTargetVesselId = entry.vesselId
                            meterDialogCurrentHours = entry.currentMeterHours
                            showMeterDialog = true
                        },
                    )
                }
            } else if (uiState.needsInitialMeterCount > 0) {
                item(key = "needs_meter") {
                    WaitingRow(
                        text = pluralStringResource(
                            R.plurals.now_needs_meter,
                            uiState.needsInitialMeterCount,
                            uiState.needsInitialMeterCount,
                        ),
                        actionText = stringResource(R.string.now_enter_hours),
                        onClick = {
                            meterDialogTargetVesselId = uiState.selectedVesselId ?: uiState.activeVessel?.id
                            meterDialogCurrentHours = uiState.primaryMeterHours
                            showMeterDialog = true
                        },
                    )
                }
            }

            if (uiState.needsFirstRecordCount > 0) {
                item(key = "needs_first_record") {
                    val firstRecordText = if (isFleetView && uiState.unrecordedComponents.map { it.vesselId }.distinct().size == 1) {
                        val targetVessel = uiState.vessels.firstOrNull { it.id == uiState.unrecordedComponents.first().vesselId }
                        if (targetVessel != null) {
                            "${targetVessel.name}: " + pluralStringResource(
                                R.plurals.now_needs_first_record,
                                uiState.needsFirstRecordCount,
                                uiState.needsFirstRecordCount,
                            )
                        } else {
                            pluralStringResource(
                                R.plurals.now_needs_first_record,
                                uiState.needsFirstRecordCount,
                                uiState.needsFirstRecordCount,
                            )
                        }
                    } else {
                        pluralStringResource(
                            R.plurals.now_needs_first_record,
                            uiState.needsFirstRecordCount,
                            uiState.needsFirstRecordCount,
                        )
                    }
                    WaitingRow(
                        text = firstRecordText,
                        actionText = stringResource(R.string.action_view_nodes),
                        onClick = { showUnrecordedSheet = true },
                    )
                }
            }

            if (isFleetView && uiState.vesselMeterInfos.isNotEmpty()) {
                items(
                    items = uiState.vesselMeterInfos,
                    key = { "meter_banner_${it.vesselId}" },
                ) { meterInfo ->
                    Surface(
                        onClick = {
                            meterDialogTargetVesselId = meterInfo.vesselId
                            meterDialogCurrentHours = meterInfo.hours
                            showMeterDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .semantics(mergeDescendants = true) {},
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_timer),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                val readingText = meterInfo.hours?.let {
                                    "$it ${stringResource(R.string.meter_hours_unit)}"
                                } ?: "—"
                                val vName = meterInfo.vessel?.displayName(context) ?: meterInfo.vesselName
                                Text(
                                    text = "$vName • ${stringResource(R.string.field_meter_hours)}: $readingText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "+ ${stringResource(R.string.action_record_meter)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else if (uiState.primaryMeterId != null || uiState.primaryMeterHours != null) {
                item(key = "meter_banner") {
                    Surface(
                        onClick = {
                            meterDialogTargetVesselId = uiState.selectedVesselId ?: uiState.activeVessel?.id
                            meterDialogCurrentHours = uiState.primaryMeterHours
                            showMeterDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .semantics(mergeDescendants = true) {},
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_timer),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                val readingText = uiState.primaryMeterHours?.let {
                                    "$it ${stringResource(R.string.meter_hours_unit)}"
                                } ?: "—"
                                Text(
                                    text = "${stringResource(R.string.field_meter_hours)}: $readingText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "+ ${stringResource(R.string.action_record_meter)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        quickLogItem?.let { item ->
            QuickLogSheet(
                item = item,
                onDismiss = { quickLogItem = null },
                onSave = { date, workType, hours, cost, notes, newExpiry, photoUris ->
                    quickLogItem = null
                    viewModel.saveQuickLog(
                        componentId = item.component.id,
                        vesselId = item.vessel.id,
                        date = date,
                        workType = workType,
                        meterHours = hours,
                        cost = cost,
                        notes = notes,
                        newExpiry = newExpiry,
                        photoUris = photoUris,
                        scheduleId = item.schedule.id,
                        // "Скасувати" now deletes the record, the reading and
                        // any meter this save created, and puts back the expiry
                        // it overwrote. It used to call refresh(), which changes
                        // nothing - so a mistaken tap was irreversible and the
                        // journal kept a service that never happened. ui-spec.md
                        // §4 requires this button to work, and §5.4 records the
                        // last time it did not.
                        onSaved = { undo ->
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    // A photo the owner chose and the app
                                    // could not copy is worth interrupting
                                    // the confirmation for.
                                    message = if (undo.failedPhotoCount > 0) {
                                        ctx.resources.getQuantityString(
                                            R.plurals.snack_logged_photos_failed,
                                            undo.failedPhotoCount,
                                            undo.failedPhotoCount,
                                        )
                                    } else {
                                        ctx.getString(R.string.snack_logged)
                                    },
                                    actionLabel = ctx.getString(R.string.action_undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    viewModel.undoQuickLog(undo)
                                }
                            }
                        },
                    )
                },
            )
        }

        deferItem?.let { item ->
            DeferSheet(
                item = item,
                onDismiss = { deferItem = null },
                onDefer = { until, reason ->
                    deferItem = null
                    viewModel.deferSchedule(item.schedule.id, until, reason) {
                        scope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = ctx.getString(R.string.snack_deferred_until, until.toString()),
                                actionLabel = ctx.getString(R.string.action_undo),
                                duration = SnackbarDuration.Short,
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                viewModel.undeferSchedule(item.schedule.id) {}
                            }
                        }
                    }
                },
            )
        }

        if (showMeterDialog) {
            RecordMeterDialog(
                currentHours = meterDialogCurrentHours ?: uiState.primaryMeterHours,
                onDismiss = { showMeterDialog = false },
                onSave = { date, hours ->
                    viewModel.recordPrimaryMeterReading(
                        vesselId = meterDialogTargetVesselId,
                        date = date,
                        hours = hours,
                    )
                    showMeterDialog = false
                },
            )
        }

        if (showUnrecordedSheet) {
            UnrecordedNodesSheet(
                components = uiState.unrecordedComponents,
                vessels = uiState.vessels,
                onSelectComponent = { compId ->
                    showUnrecordedSheet = false
                    onOpenComponent(compId)
                },
                onDismiss = { showUnrecordedSheet = false },
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UpcomingMaintenanceCard(
    item: NowItem,
    showVesselBadge: Boolean = false,
    onLog: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.component.displayName(context),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val categoryText = categoryDisplayName(context, item.component.categoryCode)
                        val subtitleText = if (showVesselBadge) {
                            "${item.vessel.displayName(context)} · $categoryText"
                        } else {
                            categoryText
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                val urgencyText = remember(item, context) {
                    app.hullbeat.data.db.formatUrgency(context, item.result, item.schedule)
                }
                Text(
                    text = urgencyText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val verb = if (item.needsNewExpiry) {
                stringResource(R.string.action_renew)
            } else {
                stringResource(R.string.action_mark_done)
            }
            Button(
                onClick = onLog,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = verb,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RecentActivityCard(
    activity: RecentActivityItem,
    showVesselBadge: Boolean = false,
    onNavigateToLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToLog),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_log),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.now_recent_activity),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.now_all_journal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val compName = activity.component?.displayName(context) ?: activity.componentName
                    Text(
                        text = compName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val rawDesc = activity.record.description.ifBlank { activity.record.workTypes }
                    val desc = formatRecordDescription(context, rawDesc)
                    val vName = activity.vessel?.displayName(context) ?: activity.vesselName
                    val subtitle = if (showVesselBadge && !vName.isNullOrBlank()) {
                        "$vName · $desc"
                    } else {
                        desc
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = activity.record.date.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A quiet line that states a number and, when there is one, offers the
 * single action that clears it. Never red: nothing here is overdue, it is
 * unmeasured - and an amber wall on first run teaches an owner to ignore
 * the colour that matters.
 */
@Composable
private fun WaitingRow(
    text: String,
    actionText: String?,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionText != null) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    } else {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    }
}
