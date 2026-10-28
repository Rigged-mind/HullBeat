package app.hullbeat.ui.now

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.data.db.WorkType
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.labelRes
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.DecimalInput
import app.hullbeat.domain.DueCalculator
import app.hullbeat.domain.LogRequirements
import app.hullbeat.ui.components.AppDatePickerDialog
import app.hullbeat.ui.components.DateSelectionRow
import app.hullbeat.ui.components.MeterDecreaseWarning
import app.hullbeat.ui.components.rememberSampledBitmap
import app.hullbeat.ui.theme.SheetSystemBars
import java.time.LocalDate

/**
 * Bottom sheet for two-tap service logging (ui-spec.md §4).
 *
 * Appears when tapping "Зроблено" or "Продовжити…" on a card or when launched
 * from a notification. For expiry items (liferaft, flares, extinguisher),
 * new expiry is mandatory and Save is disabled until provided.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(
    item: NowItem,
    onDismiss: () -> Unit,
    onSave: (
        date: LocalDate,
        workType: WorkType,
        meterHours: Double?,
        cost: Double?,
        notes: String?,
        newExpiry: LocalDate?,
        photoUris: List<Uri>,
    ) -> Unit,
    currency: String = AppPreferences.getCurrency(LocalContext.current),
    /**
     * The caller is writing what [onSave] handed it. The sheet stays up and
     * keeps every field until the write is done: it used to close on the tap,
     * so a write that failed took the owner's typing with it.
     */
    isSaving: Boolean = false,
    /** The last write failed. The fields are still here to try again. */
    saveFailed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var dateString by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = runCatching { LocalDate.parse(dateString) }.getOrDefault(LocalDate.now())
    var workTypeName by rememberSaveable { mutableStateOf(WorkType.SCHEDULED.name) }
    val workType = runCatching { WorkType.valueOf(workTypeName) }.getOrDefault(WorkType.SCHEDULED)
    var workTypeExpanded by rememberSaveable { mutableStateOf(false) }
    // ui-spec.md §4: last known + the trend, for the owner to correct. Unlike
    // the expiry below, hours are something the app can honestly estimate.
    var meterText by rememberSaveable { mutableStateOf(item.meterEstimate?.let(::formatMeterInput) ?: "") }
    var costText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var photoUriStrings by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val photoUris = remember(photoUriStrings) { photoUriStrings.map { Uri.parse(it) } }
    var showExpiryDatePicker by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val existing = photoUriStrings.toSet()
            val newUris = uris.map { it.toString() }.filter { it !in existing }
            photoUriStrings = photoUriStrings + newUris
        }
    }


    // EMPTY, deliberately. This field was prefilled with today + 1 year, which
    // made `canSave` true the instant the sheet opened - so one tap on
    // "Продовжено" wrote a GUESSED certificate date into the database and the
    // app then stopped reminding for a year, confidently.
    //
    // ui-spec.md §4 makes this field mandatory precisely because it is
    // unknowable: a liferaft expiry is printed on the service sticker and
    // nothing in the app can derive it. Prefilling the METER is the opposite
    // case and the spec asks for it - hours are a measurement the app really
    // can estimate. A date on a safety certificate is not a measurement.
    var newExpiryText by rememberSaveable { mutableStateOf("") }

    val parsedNewExpiry: LocalDate? = try {
        if (newExpiryText.isNotBlank()) LocalDate.parse(newExpiryText.trim()) else null
    } catch (_: Exception) {
        null
    }

    // A field that holds something that is not a number blocks Save. It used
    // to be read as blank, and the record went in without the meter reading.
    // Blank blocks it too when the job runs on hours: without the reading
    // the next interval has nothing to count from.
    val meterRequired = item.hasMeter && LogRequirements.needsMeterReading(item.schedule)
    val meterOk = !item.hasMeter || LogRequirements.meterInputOk(meterRequired, meterText)
    val costOk = DecimalInput.isAcceptable(costText)

    // A certificate renewed on the day of the service cannot run out before
    // it. The date used to be checked for its format only, so a slip of the
    // year saved an expiry already in the past and the item went red at once
    // - or, worse, looked renewed in the journal while still expired.
    // A past expiry is still allowed: logging last spring's renewal after the
    // fact is a real case.
    val expiryOk = parsedNewExpiry == null || parsedNewExpiry.isAfter(date)

    // Below the previous reading reads as a replaced meter: the trend starts
    // over from it. Allowed - meters are replaced - but never silently.
    val previousHours = remember(item.meterReadings, date) {
        DueCalculator.previousMeter(item.meterReadings, date)
    }
    val enteredHours = if (item.hasMeter) DecimalInput.parse(meterText) else null
    val isDecrease = enteredHours != null && previousHours != null && enteredHours < previousHours
    var decreaseAcknowledged by rememberSaveable { mutableStateOf(false) }

    val canSave = (!item.needsNewExpiry || parsedNewExpiry != null) && expiryOk &&
        meterOk && costOk && (!isDecrease || decreaseAcknowledged) && !isSaving

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        SheetSystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = item.component.displayName(context),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            DateSelectionRow(
                selectedDate = date,
                onDateChange = { dateString = it.toString() },
            )

            ExposedDropdownMenuBox(
                expanded = workTypeExpanded,
                onExpandedChange = { workTypeExpanded = !workTypeExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = stringResource(workType.labelRes()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_work_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workTypeExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = workTypeExpanded,
                    onDismissRequest = { workTypeExpanded = false },
                ) {
                    WorkType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.labelRes())) },
                            onClick = {
                                workTypeName = type.name
                                workTypeExpanded = false
                            },
                        )
                    }
                }
            }

            if (item.needsNewExpiry) {
                OutlinedTextField(
                    value = newExpiryText,
                    onValueChange = { newExpiryText = it },
                    label = { Text(stringResource(R.string.field_new_expiry)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    trailingIcon = {
                        IconButton(onClick = { showExpiryDatePicker = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = stringResource(R.string.field_new_expiry),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    singleLine = true,
                    isError = item.needsNewExpiry && (parsedNewExpiry == null || !expiryOk),
                    supportingText = if (!expiryOk) {
                        { Text(stringResource(R.string.error_expiry_before_service)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (item.hasMeter) {
                OutlinedTextField(
                    value = meterText,
                    onValueChange = { meterText = it },
                    label = { Text(stringResource(R.string.field_meter_hours)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    // Red is for what is wrong, and a field nobody has typed
                    // in yet is not: the empty required field says so in the
                    // neutral hint below, and Save stays off until it is filled.
                    isError = !meterOk && meterText.isNotBlank(),
                    // No trend, so no estimate: the last reading is stated under
                    // the field, always visible, never in it. As a value, saved
                    // unedited on a later day, it would claim no hours were run.
                    supportingText = when {
                        meterRequired && meterText.isBlank() -> {
                            { Text(stringResource(R.string.error_meter_required)) }
                        }
                        !meterOk -> {
                            { Text(stringResource(R.string.error_number_invalid)) }
                        }
                        item.meterEstimate == null && item.lastMeterReading != null -> {
                            {
                                Text(
                                    "${stringResource(R.string.meter_current_reading)}: " +
                                        "${formatMeterInput(item.lastMeterReading)} ${stringResource(R.string.meter_hours_unit)}"
                                )
                            }
                        }
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isDecrease && previousHours != null) {
                    MeterDecreaseWarning(
                        previousHours = previousHours,
                        acknowledged = decreaseAcknowledged,
                        onAcknowledgedChange = { decreaseAcknowledged = it },
                    )
                }
            }

            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text("${stringResource(R.string.field_cost)} ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = !costOk,
                supportingText = if (!costOk) {
                    { Text(stringResource(R.string.error_number_invalid)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(stringResource(R.string.field_notes)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            if (photoUris.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.detail_add_more_photos),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Heading only. Adding a photo is the tile at the end of the
                    // strip below - a second button up here fired the same picker
                    // under the same label, 24dp away from the first.
                    Text(
                        text = stringResource(R.string.detail_photos) + " (${photoUris.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // First, not last: the strip scrolls, and the tile that
                        // adds a photo is the one thing that must never be behind
                        // the right edge. The photos are what you scroll to.
                        item(key = "add_photo") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
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
                                        modifier = Modifier.size(20.dp),
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

                        items(photoUris, key = { it.toString() }) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                val thumb = rememberSampledBitmap(context, uri.toString(), maxDimension = 160)
                                if (thumb != null) {
                                    Image(
                                        bitmap = thumb,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }

                                // The target is 56 dp; the circle drawn inside it stays
                                // small. Raising the badge itself would cover the
                                // thumbnail it sits on - and this one DELETES a photo,
                                // so it is the last control that should be hard to hit.
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(56.dp)
                                        .clickable {
                                            photoUriStrings = photoUriStrings.filter { it != uri.toString() }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = stringResource(R.string.action_delete_photo),
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (saveFailed) {
                Text(
                    text = stringResource(R.string.error_record_save_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        val hours = if (item.hasMeter) DecimalInput.parse(meterText) else null
                        val cost = DecimalInput.parse(costText)
                        val notes = notesText.ifBlank { null }
                        onSave(date, workType, hours, cost, notes, parsedNewExpiry, photoUris)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showExpiryDatePicker) {
        AppDatePickerDialog(
            initialDate = parsedNewExpiry ?: LocalDate.now().plusYears(1),
            onDateSelected = { picked ->
                newExpiryText = picked.toString()
            },
            onDismiss = { showExpiryDatePicker = false },
        )
    }
}

/** 1250.0 as "1250", 1245.5 as typed: what the owner would have written. */
private fun formatMeterInput(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toLong().toString() else hours.toString()
