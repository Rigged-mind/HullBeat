package app.hullbeat.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.formatRecordDescription
import app.hullbeat.data.db.formatScheduleInterval
import app.hullbeat.data.db.formatUrgency
import app.hullbeat.data.db.formatWaterAdvice
import app.hullbeat.data.db.formatWorkTypes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hullbeat.R
import app.hullbeat.data.db.ComponentSpare
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.WorkType
import app.hullbeat.domain.DueCalculator
import app.hullbeat.ui.components.AppDatePickerDialog
import app.hullbeat.ui.components.RecordMeterDialog
import app.hullbeat.ui.components.rememberSampledBitmap
import app.hullbeat.ui.now.NowItem
import app.hullbeat.ui.now.QuickLogSheet
import app.hullbeat.ui.store.LOW_STOCK_FRACTION
import app.hullbeat.ui.theme.LocalExtendedColors
import java.time.LocalDate

/**
 * Screen displaying passport, maintenance status, on-board spares, and service history
 * for a single component (ui-spec.md §3, prototype #s-detail).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentDetailScreen(
    viewModel: ComponentDetailViewModel,
    onBack: () -> Unit,
    onLogSaved: () -> Unit,
    initialOpenLog: Boolean = false,
    backDestinationTitle: String = stringResource(R.string.detail_back),
    onSaveQuickLog: (
        componentId: Long,
        vesselId: Long,
        /**
         * Which schedule this closes. Null only when the component has no
         * schedule at all - never because the caller could not be bothered
         * to look it up.
         */
        scheduleId: Long?,
        date: LocalDate,
        workType: WorkType,
        meterHours: Double?,
        cost: Double?,
        notes: String?,
        newExpiry: LocalDate?,
        photoUris: List<Uri>,
        onSaved: () -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = LocalExtendedColors.current
    val context = LocalContext.current
    // Which passport field is open for editing, if any. A dialog rather
    // than an inline field: the ui-spec forbids an action that exists only
    // as a gesture, and a row that silently saves on focus loss is one.
    var editingField by remember { mutableStateOf<PassportField?>(null) }
    // Non-null when the owner is naming a NEW field rather than filling a
    // catalog one. Kept separate so the editor never has to guess.
    var addingField by remember { mutableStateOf(false) }

    var showLogSheet by remember { mutableStateOf(initialOpenLog) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showHideFromBoatDialog by remember { mutableStateOf(false) }
    var showMeterDialog by remember { mutableStateOf(false) }
    var viewingPhotos by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var viewingPhotoIndex by remember { mutableIntStateOf(0) }
    var showDeleteConfirmPhoto by remember { mutableStateOf<Attachment?>(null) }
    var editingRecord by remember { mutableStateOf<ServiceRecord?>(null) }
    var editingRecordMeterHours by remember { mutableStateOf<Double?>(null) }
    var recordToDelete by remember { mutableStateOf<ServiceRecord?>(null) }

    val galleryPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.addPhoto(uri)
        }
    }


    LaunchedEffect(editingRecord?.id) {
        val readingId = editingRecord?.meterReadingId
        editingRecordMeterHours = if (readingId != null) viewModel.getMeterHours(readingId) else null
    }

    val component = state.component
    val vessel = state.vessel
    val schedule = state.schedules.firstOrNull()

    var renameText by remember(component?.customName) {
        mutableStateOf(component?.customName ?: "")
    }

    if (state.isLoading || component == null || vessel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isOverdue = state.dueResult?.state == DueCalculator.State.OVERDUE
    val isSoon = state.dueResult?.state == DueCalculator.State.DUE_SOON
    val isDeferred = state.dueResult?.state == DueCalculator.State.DEFERRED

    val glyph = when {
        isOverdue -> "●"
        isSoon -> "◐"
        isDeferred -> "◌"
        else -> "○"
    }

    val glyphColor = when {
        isOverdue -> extended.status.overdue
        isSoon -> extended.status.dueSoon
        isDeferred -> extended.status.deferred
        else -> extended.status.ok
    }

    val urgencyText = remember(state.dueResult, state.primarySchedule, state.unknownReason, context) {
        formatUrgency(context, state.dueResult, state.primarySchedule, state.unknownReason)
    }

    val dynamicWaterAdvice = remember(state.isAnodeAdvice, vessel, state.waterAdvice, context) {
        if (state.isAnodeAdvice) {
            formatWaterAdvice(context, vessel.water)
        } else {
            state.waterAdvice
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(component.displayName(context), maxLines = 1) },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "←",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(backDestinationTitle, style = MaterialTheme.typography.titleSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = component.displayName(context),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.edit_record_title),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // `cat_<code>`, not the code: this line read
                            // "standing_rig" to an owner.
                            text = categoryLabel(context, component.categoryCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val (critRes, critColor) = when (component.criticality) {
                            Criticality.HIGH -> R.string.crit_high to extended.status.overdue
                            Criticality.MED -> R.string.crit_med to extended.status.dueSoon
                            Criticality.LOW -> R.string.crit_low to extended.status.ok
                        }
                        Text(
                            text = stringResource(critRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = critColor,
                        )
                    }
                }
            }

            // Next Maintenance Card
            item(key = "next_service") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.detail_next_service),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = glyph,
                                color = glyphColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = urgencyText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isOverdue) extended.status.overdue else MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Button(
                            onClick = { showLogSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            val btnText = if (schedule?.expiresOn != null) {
                                stringResource(R.string.action_renew)
                            } else {
                                stringResource(R.string.action_mark_done)
                            }
                            Text("+ $btnText", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (state.hasMeter) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showMeterDialog = true },
                                modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                val reading = state.latestMeterReading
                                val meterText = if (reading != null) {
                                    "${stringResource(R.string.action_record_meter)} (${reading.value} ${stringResource(R.string.meter_hours_unit)})"
                                } else {
                                    stringResource(R.string.action_record_meter)
                                }
                                Text(meterText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Passport / Specs Section
            item(key = "passport_header") {
                Text(
                    text = stringResource(R.string.detail_passport),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            dynamicWaterAdvice?.let { advice ->
                item(key = "water_advice") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = extended.tint.dueSoon,
                        border = BorderStroke(1.dp, extended.status.dueSoon),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = advice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            if (state.passportFields.isEmpty()) {
                item(key = "passport_empty_hint") {
                    Text(
                        text = stringResource(R.string.detail_passport_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.passportFields, key = { it.key }) { field ->
                    PassportRow(field = field, onEdit = { editingField = field })
                }
            }
            item(key = "passport_add") {
                TextButton(
                    onClick = { addingField = true },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.passport_add_field))
                }
            }

            // Condition Photos Section (timeline of photos from service records and condition inspections)
            item(key = "photos_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.gallery.isNotEmpty()) {
                            stringResource(R.string.detail_photos) + " (${state.gallery.size})"
                        } else {
                            stringResource(R.string.detail_photos)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    // Only while there is no strip to put a tile in. Once the
                    // gallery has a photo, the tile at the end of it is the one
                    // way to add another: this button fired the same picker under
                    // the same label a few dp away, which read as two doors.
                    if (state.gallery.isEmpty()) {
                        OutlinedButton(
                            onClick = {
                                galleryPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.detail_add_photo),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            if (state.gallery.isNotEmpty()) {
                item(key = "photos_gallery") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        // First, not last: the strip scrolls, and the tile that
                        // adds a photo is the one thing that must never be behind
                        // the right edge. The photos are what you scroll to.
                        item(key = "gallery_add_photo") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        galleryPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_camera),
                                        contentDescription = stringResource(R.string.detail_add_photo),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.detail_add_photo),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        itemsIndexed(state.gallery, key = { _, photo -> photo.id }) { index, photo ->
                            PhotoThumbnailCard(
                                photo = photo,
                                onClick = {
                                    viewingPhotos = state.gallery
                                    viewingPhotoIndex = index
                                },
                            )
                        }
                    }
                }
            } else {
                item(key = "no_photos") {
                    Text(
                        text = stringResource(R.string.detail_no_photos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Service History Section
            item(key = "history_header") {
                Text(
                    text = stringResource(R.string.detail_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (state.history.isEmpty()) {
                item(key = "no_history") {
                    Text(
                        text = stringResource(R.string.detail_no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.history, key = { it.id }) { record ->
                    val recordPhotos = state.gallery.filter {
                        it.parentType == ParentType.SERVICE_RECORD && it.parentId == record.id
                    }
                    val recordSchedule = record.scheduleId?.let { sId ->
                        state.schedules.firstOrNull { it.id == sId }
                    }
                    HistoryRow(
                        record = record,
                        photos = recordPhotos,
                        schedule = recordSchedule,
                        onPhotoClick = { photos, index ->
                            viewingPhotos = photos
                            viewingPhotoIndex = index
                        },
                        onEditClick = { editingRecord = record },
                        onDeleteClick = { recordToDelete = record },
                    )
                }
            }

            // On-board Spares Section
            item(key = "spares_header") {
                Text(
                    text = stringResource(R.string.detail_stock),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (state.spares.isEmpty()) {
                item(key = "no_spares") {
                    Text(
                        text = stringResource(R.string.detail_no_spares),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.spares, key = { it.itemId ?: it.partId }) { spare ->
                    SpareRow(spare = spare)
                }
            }

            item(key = "hide_from_boat") {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showHideFromBoatDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.action_hide_from_boat))
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        if (showLogSheet) {
            val nowItem = NowItem(
                component = component,
                schedule = schedule ?: return@Scaffold,
                vessel = vessel,
                state = state.dueResult?.state ?: DueCalculator.State.UNKNOWN,
                result = state.dueResult ?: DueCalculator.Result(
                    state = DueCalculator.State.UNKNOWN,
                    dueDate = null,
                    isPredicted = false,
                    daysRemaining = null,
                    unitsRemaining = null,
                    driver = DueCalculator.Driver.NONE,
                    wearRatePerDay = null,
                    confidence = DueCalculator.Confidence.NONE,
                ),
                urgencyText = urgencyText,
                actionVerb = if (schedule.expiresOn != null) stringResource(R.string.action_renew) else stringResource(R.string.action_mark_done),
                needsNewExpiry = schedule.expiresOn != null,
                isDeferred = isDeferred,
                deferredUntil = schedule.deferredUntil,
                deferReason = schedule.deferReason,
                criticality = component.criticality,
                hasMeter = schedule.intervalHours != null,
            )

            QuickLogSheet(
                item = nowItem,
                onDismiss = { showLogSheet = false },
                onSave = { date, workType, hours, cost, notes, newExpiry, photoUris ->
                    showLogSheet = false
                    onSaveQuickLog(
                        component.id,
                        vessel.id,
                        // The schedule whose due date this screen is
                        // showing - NOT schedules.firstOrNull(), which is
                        // the raw query order and picks an arbitrary one on
                        // a component that has several.
                        state.primaryScheduleId,
                        date,
                        workType,
                        hours,
                        cost,
                        notes,
                        newExpiry,
                        photoUris,
                    ) {
                        onLogSaved()
                    }
                },
            )
        }

        if (editingField != null || addingField) {
            PassportEditor(
                field = editingField,
                onDismiss = {
                    editingField = null
                    addingField = false
                },
                onSave = { key, value ->
                    if (key.isNotEmpty()) viewModel.setPassportField(key, value)
                    editingField = null
                    addingField = false
                },
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text(stringResource(R.string.component_rename_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            placeholder = { Text(component.displayName(context)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.component_rename_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRenameDialog = false
                            viewModel.renameComponent(renameText)
                        }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRenameDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        if (showMeterDialog) {
            RecordMeterDialog(
                currentHours = state.latestMeterReading?.value,
                onDismiss = { showMeterDialog = false },
                onSave = { date, hours ->
                    viewModel.recordMeterReading(date, hours) {
                        showMeterDialog = false
                        onLogSaved()
                    }
                },
            )
        }

        if (viewingPhotos.isNotEmpty() && viewingPhotoIndex in viewingPhotos.indices) {
            ViewPhotoDialog(
                photos = viewingPhotos,
                currentIndex = viewingPhotoIndex,
                onIndexChange = { viewingPhotoIndex = it },
                onDismiss = { viewingPhotos = emptyList() },
                onDelete = { photo ->
                    showDeleteConfirmPhoto = photo
                },
            )
        }

        showDeleteConfirmPhoto?.let { photo ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmPhoto = null },
                title = { Text(stringResource(R.string.action_delete_photo)) },
                text = { Text(stringResource(R.string.dialog_delete_photo_confirm)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePhoto(photo.id)
                            showDeleteConfirmPhoto = null
                            val updated = viewingPhotos.filter { it.id != photo.id }
                            if (updated.isEmpty()) {
                                viewingPhotos = emptyList()
                                viewingPhotoIndex = 0
                            } else {
                                viewingPhotos = updated
                                if (viewingPhotoIndex >= updated.size) {
                                    viewingPhotoIndex = updated.lastIndex
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.action_delete_photo))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirmPhoto = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        editingRecord?.let { record ->
            val recordPhotos = state.gallery.filter {
                it.parentType == ParentType.SERVICE_RECORD && it.parentId == record.id
            }
            EditServiceRecordSheet(
                record = record,
                componentName = component.displayName(context),
                hasMeter = state.hasMeter,
                initialMeterHours = editingRecordMeterHours,
                currentPhotos = recordPhotos,
                onDismiss = { editingRecord = null },
                onSave = { updatedRecord, meterHours, newPhotoUris, removedPhotoIds ->
                    editingRecord = null
                    viewModel.updateServiceRecord(
                        record = updatedRecord,
                        meterHours = meterHours,
                        newPhotoUris = newPhotoUris,
                        removedPhotoIds = removedPhotoIds,
                        onDone = onLogSaved,
                    )
                },
                onDeleteRequest = {
                    val rec = editingRecord
                    editingRecord = null
                    recordToDelete = rec
                },
            )
        }

        recordToDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_record_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.dialog_delete_record_message,
                            record.date.toString(),
                            formatWorkTypes(context, record.workTypes),
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = record.id
                            recordToDelete = null
                            viewModel.deleteServiceRecord(id, onDone = onLogSaved)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { recordToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        if (showHideFromBoatDialog) {
            AlertDialog(
                onDismissRequest = { showHideFromBoatDialog = false },
                title = { Text(stringResource(R.string.dialog_hide_component_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.dialog_hide_component_message,
                            component.displayName(context),
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showHideFromBoatDialog = false
                            viewModel.hideFromBoat {
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.action_hide_from_boat))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showHideFromBoatDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SpareRow(spare: ComponentSpare, modifier: Modifier = Modifier) {
    val extended = LocalExtendedColors.current
    // Same rule as the store list: minQuantity holds a norm, so half of it
    // gone is low. Compared as a floor, a full locker read as low.
    val isLow = spare.quantity != null && spare.minQuantity != null &&
        spare.quantity <= spare.minQuantity * LOW_STOCK_FRACTION
    val isGone = spare.quantity == null || spare.quantity == 0.0

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isGone || isLow) extended.status.dueSoon else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spare.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!spare.partNumber.isNullOrBlank()) {
                    Text(spare.partNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val loc = spare.locationName ?: stringResource(R.string.spare_no_location)
                Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Column(horizontalAlignment = Alignment.End) {
                val qty = spare.quantity?.toInt() ?: 0
                val qtyStr = pluralStringResource(R.plurals.store_qty, qty, qty)
                Text(
                    text = qtyStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGone) extended.status.overdue else if (isLow) extended.status.dueSoon else MaterialTheme.colorScheme.onSurface,
                )
                val perServiceQty = spare.quantityPerService.toInt()
                val perServicePcs = pluralStringResource(R.plurals.store_qty, perServiceQty, perServiceQty)
                Text(
                    text = stringResource(R.string.spare_per_service, perServicePcs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    record: ServiceRecord,
    photos: List<Attachment> = emptyList(),
    schedule: ServiceSchedule? = null,
    onPhotoClick: (List<Attachment>, Int) -> Unit = { _, _ -> },
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(record.date.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(formatWorkTypes(context, record.workTypes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (schedule != null) {
                        val intervalText = formatScheduleInterval(context, schedule)
                        if (intervalText.isNotBlank()) {
                            Text(
                                text = "($intervalText)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.edit_record_title),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.dialog_delete_record_title),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val description = formatRecordDescription(context, record.description)
            if (description.isNotBlank()) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            if (!record.notes.isNullOrBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (photos.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    for ((index, photo) in photos.withIndex()) {
                        val thumb = rememberSampledBitmap(context, photo.fileUri, maxDimension = 180)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onPhotoClick(photos, index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb,
                                    contentDescription = photo.caption,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One passport value, typed in.
 *
 * `specJson` had no write path at all before this, so every field on every
 * node read "—" for good. It also feeds the search index at rank 20, the
 * tier that finds a node by a part number - empty passports made that tier
 * unreachable.
 */
@Composable
private fun PassportEditor(
    field: PassportField?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val known = field != null
    val fieldLabel = remember(field?.key, context) { field?.displayLabel(context) ?: "" }
    var name by remember(field?.key) { mutableStateOf(fieldLabel) }
    var text by remember(field?.key) { mutableStateOf(field?.value ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateWord = stringResource(R.string.field_date)
    val isDate = remember(field?.key, name, dateWord) {
        val k = (field?.key ?: name).lowercase()
        val lbl = fieldLabel.lowercase()
        k.contains("date") || k.contains("install") || k.contains("fitted") ||
            k.contains("expiry") || k.contains("applied") ||
            lbl.contains(dateWord, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (known) fieldLabel
                else stringResource(R.string.passport_add_field)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!known) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.passport_field_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // No calendar in the trailing slot. It was drawn on every field,
                // ignoring isDate, so 143 of the catalog's 155 passport fields
                // offered a date picker: tapping it on an anchor's "Type" wrote
                // 2026-09-20 into it. On the twelve fields that ARE dates it was
                // a second door anyway - the "Today / Pick a date" row below does
                // the same thing, with a bigger target and a word on it.
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { if (isDate) Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isDate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { text = LocalDate.now().toString() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(stringResource(R.string.date_today), maxLines = 1)
                        }
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.defer_option_pick_date), maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (known) field.key else name.trim(), text) },
                enabled = if (known) true else name.trim().isNotEmpty(),
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

    if (showDatePicker) {
        val initial = runCatching { LocalDate.parse(text.trim()) }.getOrElse { LocalDate.now() }
        AppDatePickerDialog(
            initialDate = initial,
            onDateSelected = { selectedDate ->
                text = selectedDate.toString()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** `cat_<code>` - the approved category name, not the code itself. */
private fun categoryLabel(context: android.content.Context, code: String): String {
    val id = context.resources.getIdentifier("cat_$code", "string", context.packageName)
    return if (id != 0) context.getString(id) else code
}

@Composable
private fun PassportRow(
    field: PassportField,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val label = remember(field.key, context) { field.displayLabel(context) }
    val dateWord = stringResource(R.string.field_date)
    val isDate = remember(field.key, label, dateWord) {
        val k = field.key.lowercase()
        val lbl = label.lowercase()
        k.contains("date") || k.contains("install") || k.contains("fitted") ||
            k.contains("expiry") || k.contains("applied") ||
            lbl.contains(dateWord, ignoreCase = true)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onEdit() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isDate && field.value.isNotBlank()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (field.value.isNotBlank()) field.value else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (field.value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun PhotoThumbnailCard(
    photo: Attachment,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = rememberSampledBitmap(context, photo.fileUri, maxDimension = 320)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(width = 130.dp, height = 150.dp)
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = photo.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val dateText = photo.takenAt?.toString() ?: ""
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (!photo.caption.isNullOrBlank()) {
                    Text(
                        text = photo.caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}


@Composable
private fun ViewPhotoDialog(
    photos: List<Attachment>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (Attachment) -> Unit,
) {
    if (photos.isEmpty() || currentIndex !in photos.indices) return

    val currentPhoto = photos[currentIndex]
    val totalPhotos = photos.size
    val hasMultiple = totalPhotos > 1
    val canGoBack = currentIndex > 0
    val canGoForward = currentIndex < photos.lastIndex

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val dialogImageHeight = (configuration.screenHeightDp * 0.65f).dp.coerceIn(420.dp, 620.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Main image container with swipe gestures & overlay controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dialogImageHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(currentIndex, totalPhotos) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > 50f && canGoBack) {
                                        onIndexChange(currentIndex - 1)
                                    } else if (totalDrag < -50f && canGoForward) {
                                        onIndexChange(currentIndex + 1)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = rememberSampledBitmap(context, currentPhoto.fileUri, maxDimension = 1200)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = currentPhoto.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }

                    // Top-left Counter badge (only when multiple photos)
                    if (hasMultiple) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                        ) {
                            Text(
                                text = "${currentIndex + 1} / $totalPhotos",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Top-right Action buttons (Delete & Close)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(
                            onClick = { onDelete(currentPhoto) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.action_delete_photo),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.action_cancel),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Left arrow: hidden at the beginning of the list
                    if (canGoBack) {
                        FilledTonalIconButton(
                            onClick = { onIndexChange(currentIndex - 1) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                                .size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_left),
                                contentDescription = stringResource(R.string.action_previous),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    // Right arrow: hidden at the end of the list
                    if (canGoForward) {
                        FilledTonalIconButton(
                            onClick = { onIndexChange(currentIndex + 1) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = stringResource(R.string.action_next),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                // Date & Caption below image if present
                val hasDate = currentPhoto.takenAt != null
                val hasCaption = !currentPhoto.caption.isNullOrBlank()
                if (hasDate || hasCaption) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (hasDate) {
                            Text(
                                text = currentPhoto.takenAt.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (hasCaption) {
                            Text(
                                text = currentPhoto.caption!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
