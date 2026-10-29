package app.hullbeat.ui.journal

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import app.hullbeat.ui.theme.appFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.hullbeat.ui.theme.LocalScheme
import app.hullbeat.ui.theme.Scheme
import app.hullbeat.ui.theme.categorySectionColor
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hullbeat.R
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.DatePrecision
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.categoryIconRes
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.formatRecordDescription
import app.hullbeat.data.db.formatScheduleInterval
import app.hullbeat.data.db.formatWorkTypes
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.formatCostTotals
import app.hullbeat.ui.components.bleedHorizontally
import app.hullbeat.ui.components.rememberSampledBitmap
import app.hullbeat.ui.detail.EditServiceRecordSheet
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Ship's Log (Журнал) screen.
 *
 * Chronological timeline of all vessel maintenance, repairs, inspections, and service records.
 */
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onOpenComponent: (Long) -> Unit,
    /**
     * Picking a vessel here changes the app's selection, not the Journal's
     * private copy of it - otherwise this screen and the header hold two
     * different answers to one question.
     */
    onSelectVessel: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingEntry by remember { mutableStateOf<JournalEntryItem?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntryItem?>(null) }
    var viewingPhotos by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var viewingPhotoIndex by remember { mutableIntStateOf(0) }

    // Filters and totals. Inside the list once there are records, so they
    // scroll away with it: on a phone turned sideways they alone took a third
    // of the height and the records ran in a slot beneath them.
    val header: @Composable () -> Unit = {
        // Vessel selector if fleet > 1
        if (uiState.vessels.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.selectedVesselId == null,
                    onClick = { onSelectVessel(null) },
                    label = { Text(stringResource(R.string.fleet_all)) },
                )
                uiState.vessels.forEach { v ->
                    FilterChip(
                        selected = uiState.selectedVesselId == v.id,
                        onClick = { onSelectVessel(v.id) },
                        label = { Text(v.name) },
                    )
                }
            }
        }

        // Category filter chips
        if (uiState.categoriesWithRecords.isNotEmpty()) {
            val sortedCategories = remember(uiState.categoriesWithRecords, context) {
                uiState.categoriesWithRecords
                    .map { it to categoryDisplayName(context, it) }
                    .sortedBy { it.second }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.selectedCategoryCode == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text(stringResource(R.string.journal_all_categories)) },
                )
                sortedCategories.forEach { (catCode, catName) ->
                    val secColor = categorySectionColor(catCode)
                    val isColorful = LocalScheme.current == Scheme.COLORFUL
                    val isSelected = uiState.selectedCategoryCode == catCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(catCode) },
                        label = { Text(catName) },
                        leadingIcon = if (isColorful) {
                            {
                                Icon(
                                    painter = painterResource(categoryIconRes(catCode)),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) secColor.onContainer else secColor.accent,
                                )
                            }
                        } else null,
                        border = if (isColorful) {
                            BorderStroke(
                                1.dp,
                                if (isSelected) secColor.accent else secColor.accent.copy(alpha = 0.5f)
                            )
                        } else FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected),
                        colors = if (isColorful) {
                            FilterChipDefaults.filterChipColors(
                                containerColor = secColor.container.copy(alpha = 0.35f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                iconColor = secColor.accent,
                                selectedContainerColor = secColor.container,
                                selectedLabelColor = secColor.onContainer,
                                selectedLeadingIconColor = secColor.onContainer,
                            )
                        } else FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }

        // Summary Header Card
        if (uiState.totalCount > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val countText = pluralStringResource(
                        R.plurals.log_records,
                        uiState.totalCount,
                        uiState.totalCount,
                    )
                    val spanText = if (uiState.dateSpan.isNotBlank()) " · ${uiState.dateSpan}" else ""
                    Text(
                        text = "$countText$spanText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )

                    val formattedCost = formatCostTotals(uiState.costTotals) {
                        NumberFormat.getInstance().format(it.toLong())
                    }
                    Text(
                        text = stringResource(R.string.journal_spent_total, formattedCost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (uiState.isLoading) {
            header()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.entries.isEmpty()) {
            header()
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_log),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.journal_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.journal_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // Timeline Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                flingBehavior = appFlingBehavior(),
            ) {
                item(key = "journal_header") {
                    Column(modifier = Modifier.bleedHorizontally(16.dp)) { header() }
                }

                var prevYear: Int? = null
                var prevMonth: Int? = null

                items(uiState.entries, key = { it.record.id }) { entry ->
                    val entryDate = entry.record.date
                    val entryYear = entryDate.year
                    val entryMonth = if (entry.record.precision == DatePrecision.YEAR) -1 else entryDate.monthValue

                    val showYearHeader = entryYear != prevYear
                    val showMonthHeader = entryMonth != -1 && entryMonth != prevMonth

                    if (showYearHeader) {
                        prevYear = entryYear
                        prevMonth = null
                        Text(
                            text = entryYear.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }

                    if (showMonthHeader) {
                        prevMonth = entryMonth
                        val appLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
                        val monthName = formatMonthHeader(entryMonth, appLocale)
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }

                    JournalRecordRow(
                        entry = entry,
                        context = context,
                        onOpenComponent = onOpenComponent,
                        onEdit = { editingEntry = entry },
                        onDelete = { entryToDelete = entry },
                        onPhotoClick = { photos, index ->
                            viewingPhotos = photos
                            viewingPhotoIndex = index
                        },
                        showVesselName = uiState.selectedVesselId == null && uiState.vessels.size > 1,
                    )

                    if (entry.daysSincePrev != null) {
                        JournalGapRow(
                            days = entry.daysSincePrev,
                            hours = entry.hoursSincePrev,
                            isRough = entry.isApproximateGap,
                        )
                    }
                }
            }
        }
    }

    // Edit sheet
    editingEntry?.let { entry ->
        EditServiceRecordSheet(
            record = entry.record,
            componentName = entry.componentDisplayName,
            hasMeter = entry.meterReading != null,
            initialMeterHours = entry.meterReading?.value,
            currentPhotos = entry.attachments,
            onDismiss = { editingEntry = null },
            onSave = { updatedRecord, meterHours, newPhotoUris, removedPhotoIds ->
                editingEntry = null
                viewModel.updateRecord(
                    record = updatedRecord,
                    meterHours = meterHours,
                    newPhotoUris = newPhotoUris,
                    removedPhotoIds = removedPhotoIds,
                )
            },
            onDeleteRequest = {
                editingEntry = null
                entryToDelete = entry
            },
        )
    }

    // Delete confirmation dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_record_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_record_message,
                        entry.record.date.toString(),
                        formatWorkTypes(context, entry.record.workTypes),
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = entry.record.id
                        entryToDelete = null
                        viewModel.deleteRecord(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Photo preview dialog with multi-image navigation
    if (viewingPhotos.isNotEmpty() && viewingPhotoIndex in viewingPhotos.indices) {
        val currentPhoto = viewingPhotos[viewingPhotoIndex]
        val totalPhotos = viewingPhotos.size
        val hasMultiple = totalPhotos > 1
        val canGoBack = viewingPhotoIndex > 0
        val canGoForward = viewingPhotoIndex < viewingPhotos.lastIndex

        val configuration = LocalConfiguration.current
        val dialogImageHeight = (configuration.screenHeightDp * 0.65f).dp.coerceIn(420.dp, 620.dp)

        Dialog(
            onDismissRequest = { viewingPhotos = emptyList() },
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
                            .pointerInput(viewingPhotoIndex, totalPhotos) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > 50f && canGoBack) {
                                            viewingPhotoIndex--
                                        } else if (totalDrag < -50f && canGoForward) {
                                            viewingPhotoIndex++
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
                                    text = "${viewingPhotoIndex + 1} / $totalPhotos",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        // Top-right Close button overlaid on image
                        FilledTonalIconButton(
                            onClick = { viewingPhotos = emptyList() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(56.dp),
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

                        // Left arrow: hidden at the beginning of the list
                        if (canGoBack) {
                            FilledTonalIconButton(
                                onClick = { viewingPhotoIndex-- },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                                    .size(56.dp),
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
                                onClick = { viewingPhotoIndex++ },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .size(56.dp),
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

                    if (!currentPhoto.caption.isNullOrBlank()) {
                        Text(
                            text = currentPhoto.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalRecordRow(
    entry: JournalEntryItem,
    context: Context,
    onOpenComponent: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (List<Attachment>, Int) -> Unit,
    showVesselName: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Date day column
        val dayString = if (entry.record.precision == DatePrecision.DAY) {
            entry.record.date.dayOfMonth.toString()
        } else {
            "\u2014"
        }

        Box(
            modifier = Modifier
                .width(36.dp)
                .padding(top = 10.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = dayString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        val secColor = categorySectionColor(entry.component.categoryCode)
        val isColorful = LocalScheme.current == Scheme.COLORFUL

        // Main card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(
                1.dp,
                if (isColorful) secColor.accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Header: Component name + chevron and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenComponent(entry.component.id) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.component.displayName(context),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = " ›",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clearAndSetSemantics { },
                            )
                        }
                        val categoryName = categoryDisplayName(context, entry.component.categoryCode)
                        val vesselName = entry.vessel?.displayName(context) ?: entry.vesselName
                        val subtitle = if (showVesselName && !vesselName.isNullOrBlank()) {
                            "$vesselName · $categoryName"
                        } else {
                            categoryName
                        }
                        if (isColorful) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = secColor.container,
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(categoryIconRes(entry.component.categoryCode)),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = secColor.accent,
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secColor.onContainer,
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                Icon(
                                    painter = painterResource(categoryIconRes(entry.component.categoryCode)),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onEdit,
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
                            onClick = onDelete,
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

                // Details block (TalkBack reads together)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Work types and schedule tags
                    val workTypeText = formatWorkTypes(context, entry.record.workTypes)
                    val scheduleIntervalText = entry.schedule?.let { formatScheduleInterval(context, it) }

                    if (workTypeText.isNotBlank() || !scheduleIntervalText.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (workTypeText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                ) {
                                    Text(
                                        text = workTypeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            if (!scheduleIntervalText.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                ) {
                                    Text(
                                        text = scheduleIntervalText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Metadata line (hours, cost, photos, by)
                    val metaParts = mutableListOf<String>()
                    entry.meterReading?.value?.let { h ->
                        val hStr = if (h % 1.0 == 0.0) h.toLong().toString() else h.toString()
                        metaParts.add("$hStr ${context.getString(R.string.meter_hours_unit)}")
                    }
                    entry.record.cost?.let { c ->
                        val cStr = if (c % 1.0 == 0.0) c.toLong().toString() else c.toString()
                        val symbol = AppPreferences.currencySymbol(entry.record.currency)
                        metaParts.add(if (symbol.isBlank()) cStr else "$cStr $symbol")
                    }
                    if (entry.attachments.isNotEmpty()) {
                        metaParts.add(
                            context.resources.getQuantityString(
                                R.plurals.log_photos,
                                entry.attachments.size,
                                entry.attachments.size,
                            )
                        )
                    }
                    val performedBy = entry.record.performedBy
                    if (!performedBy.isNullOrBlank()) {
                        metaParts.add(performedBy)
                    }

                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Description and Notes
                    val description = formatRecordDescription(context, entry.record.description)
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    val notes = entry.record.notes
                    if (!notes.isNullOrBlank()) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Provenance or Precision Note
                    val provParts = mutableListOf<String>()
                    if (entry.record.precision == DatePrecision.YEAR) {
                        provParts.add(context.getString(R.string.journal_precision_year))
                    } else if (entry.record.precision == DatePrecision.MONTH) {
                        provParts.add(context.getString(R.string.journal_precision_month))
                    }
                    if (!entry.record.importSheet.isNullOrBlank()) {
                        provParts.add(
                            context.getString(
                                R.string.journal_imported_from,
                                entry.record.importSheet,
                                entry.record.importRowIndex ?: 0,
                            )
                        )
                    }

                    if (provParts.isNotEmpty()) {
                        Text(
                            text = provParts.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                // Photo thumbnails
                if (entry.attachments.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        entry.attachments.forEachIndexed { index, photo ->
                            val thumb = rememberSampledBitmap(context, photo.fileUri, maxDimension = 180)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onPhotoClick(entry.attachments, index) },
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
                                    Icon(
                                        painter = painterResource(R.drawable.ic_camera),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun JournalGapRow(
    days: Long,
    hours: Double?,
    isRough: Boolean,
    modifier: Modifier = Modifier,
) {
    val daysText = pluralStringResource(R.plurals.log_gap_days, days.toInt(), days.toInt())
    val hoursText = hours?.let {
        " " + stringResource(R.string.journal_gap_and) + " " +
            pluralStringResource(R.plurals.log_gap_hours, it.toInt(), it.toInt())
    } ?: ""
    val intervalText = daysText + hoursText
    val roughPrefix = if (isRough) "≈ " else ""
    val fullText = roughPrefix + stringResource(R.string.journal_gap_after_previous, intervalText)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 44.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_timer),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = fullText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

private fun formatMonthHeader(monthValue: Int, locale: Locale): String {
    val month = Month.of(monthValue)
    val name = month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
