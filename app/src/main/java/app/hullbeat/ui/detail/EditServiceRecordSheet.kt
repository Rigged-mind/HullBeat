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
import androidx.compose.material3.ButtonDefaults
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
import app.hullbeat.R
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.WorkType
import app.hullbeat.data.db.labelRes
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.ui.components.DateSelectionRow
import app.hullbeat.ui.components.rememberSampledBitmap
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceRecordSheet(
    record: ServiceRecord,
    componentName: String,
    hasMeter: Boolean,
    initialMeterHours: Double? = null,
    currentPhotos: List<Attachment>,
    onDismiss: () -> Unit,
    onSave: (
        updatedRecord: ServiceRecord,
        meterHours: Double?,
        newPhotoUris: List<Uri>,
        removedPhotoIds: List<Long>,
    ) -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currency = remember { AppPreferences.getCurrency(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val parsedWorkType = record.workTypes.split(",")
        .firstOrNull()?.trim()
        ?.let { raw -> runCatching { WorkType.valueOf(raw) }.getOrNull() }
        ?: WorkType.SCHEDULED

    var dateString by rememberSaveable { mutableStateOf(record.date.toString()) }
    val date = runCatching { LocalDate.parse(dateString) }.getOrDefault(record.date)
    var workTypeName by rememberSaveable { mutableStateOf(parsedWorkType.name) }
    val workType = runCatching { WorkType.valueOf(workTypeName) }.getOrDefault(WorkType.SCHEDULED)
    var workTypeExpanded by rememberSaveable { mutableStateOf(false) }

    val initialMeter: String = initialMeterHours?.let { h ->
        if (h % 1.0 == 0.0) h.toLong().toString() else h.toString()
    } ?: ""
    var meterText by rememberSaveable { mutableStateOf(initialMeter) }

    val initialCost: String = record.cost?.let { c ->
        if (c % 1.0 == 0.0) c.toLong().toString() else c.toString()
    } ?: ""
    var costText by rememberSaveable { mutableStateOf(initialCost) }

    var notesText by rememberSaveable { mutableStateOf(record.notes ?: "") }

    var removedPhotoIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    val remainingPhotos = remember(currentPhotos, removedPhotoIds) {
        currentPhotos.filter { it.id !in removedPhotoIds }
    }
    var newPhotoUriStrings by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val newPhotoUris = remember(newPhotoUriStrings) { newPhotoUriStrings.map { Uri.parse(it) } }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val existing = (remainingPhotos.map { it.fileUri } + newPhotoUriStrings).toSet()
            val fresh = uris.map { it.toString() }.filter { it !in existing }
            newPhotoUriStrings = newPhotoUriStrings + fresh
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.edit_record_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = componentName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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
                    for (type in WorkType.entries) {
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

            if (hasMeter) {
                OutlinedTextField(
                    value = meterText,
                    onValueChange = { meterText = it },
                    label = { Text(stringResource(R.string.field_meter_hours)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text("${stringResource(R.string.field_cost)} ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(stringResource(R.string.field_notes)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            val totalPhotos = remainingPhotos.size + newPhotoUris.size

            if (totalPhotos == 0) {
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
                        text = stringResource(R.string.detail_photos) + " ($totalPhotos)",
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

                        items(remainingPhotos, key = { "exist_${it.id}" }) { photo ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                val thumb = rememberSampledBitmap(context, photo.fileUri, maxDimension = 160)
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
                                            removedPhotoIds = removedPhotoIds + photo.id
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

                        items(newPhotoUris, key = { "new_$it" }) { uri ->
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
                                            newPhotoUriStrings = newPhotoUriStrings.filter { it != uri.toString() }
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
                        val hours = meterText.toDoubleOrNull()
                        val cost = costText.toDoubleOrNull()
                        val notes = notesText.ifBlank { null }
                        val updated = record.copy(
                            date = date,
                            workTypes = workType.name,
                            cost = cost,
                            notes = notes,
                        )
                        val removedPhotoIds = currentPhotos.map { it.id }.filter { id -> remainingPhotos.none { it.id == id } }
                        onSave(updated, hours, newPhotoUris, removedPhotoIds)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }

            OutlinedButton(
                onClick = onDeleteRequest,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_delete_record),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
