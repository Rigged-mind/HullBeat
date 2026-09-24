package app.hullbeat.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import app.hullbeat.MainActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import app.hullbeat.data.db.ImportSession
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.profileOptionLabel
import app.hullbeat.data.db.formatWorkTypes
import app.hullbeat.data.db.formatRecordDescription
import app.hullbeat.data.db.storageLocationDisplayName
import app.hullbeat.domain.backup.BackupManager
import app.hullbeat.domain.export.ExcelExporter
import app.hullbeat.domain.export.PassportLabels
import app.hullbeat.domain.export.PdfPassportExporter
import app.hullbeat.domain.export.ServiceRecordExportItem
import app.hullbeat.domain.export.VesselProfileLabels
import app.hullbeat.domain.importer.ImportEngine
import app.hullbeat.notify.MaintenanceCheckWorker
import java.time.LocalDate
import app.hullbeat.ui.importer.ImportSheet
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hullbeat.HullBeatApp
import app.hullbeat.R
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.data.preferences.CurrencyOption
import app.hullbeat.data.preferences.DistanceUnit
import app.hullbeat.data.preferences.LengthUnit
import app.hullbeat.data.preferences.PressureUnit
import app.hullbeat.data.preferences.TemperatureUnit
import app.hullbeat.data.preferences.VolumeUnit
import app.hullbeat.ui.AppLanguage
import app.hullbeat.ui.theme.Scheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentCurrency: String,
    onCurrencyChanged: (String) -> Unit,
    currentScheme: Scheme,
    onSchemeChanged: (Scheme) -> Unit,
    onDismiss: () -> Unit,
    activeVesselId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Currency state
    var selectedCurrency by remember { mutableStateOf(currentCurrency) }
    var customCurrencyText by remember {
        mutableStateOf(
            if (AppPreferences.AVAILABLE_CURRENCIES.none { it.symbol == currentCurrency }) currentCurrency else ""
        )
    }
    var isCustomSelected by remember {
        mutableStateOf(AppPreferences.AVAILABLE_CURRENCIES.none { it.symbol == currentCurrency })
    }
    // Hoisted with the rest: a `remember` inside a LazyColumn item is thrown
    // away when the item scrolls out of view.
    var currencyMenuOpen by remember { mutableStateOf(false) }

    // Units state
    var lengthUnit by remember { mutableStateOf(AppPreferences.getLengthUnit(context)) }
    var distanceUnit by remember { mutableStateOf(AppPreferences.getDistanceUnit(context)) }
    var volumeUnit by remember { mutableStateOf(AppPreferences.getVolumeUnit(context)) }
    var pressureUnit by remember { mutableStateOf(AppPreferences.getPressureUnit(context)) }
    var temperatureUnit by remember { mutableStateOf(AppPreferences.getTemperatureUnit(context)) }

    // Notifications state
    var notificationsEnabled by remember { mutableStateOf(AppPreferences.isNotificationsEnabled(context)) }
    var advanceDays by remember { mutableIntStateOf(AppPreferences.getAdvanceReminderDays(context)) }

    // Import and Backup state
    var showImportSheet by remember { mutableStateOf(false) }
    var importSessions by remember { mutableStateOf<List<ImportSession>>(emptyList()) }
    val db = (context.applicationContext as HullBeatApp).database

    fun refreshSessions() {
        scope.launch(Dispatchers.IO) {
            val list = db.importDao().sessions()
            withContext(Dispatchers.Main) {
                importSessions = list
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshSessions()
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = context.contentResolver.openOutputStream(uri)?.use { stream ->
                    BackupManager.createBackup(context, db, stream)
                } ?: false
                withContext(Dispatchers.Main) {
                    val msg = if (ok) context.getString(R.string.settings_backup_success) else context.getString(R.string.settings_backup_failed)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BackupManager.restoreBackup(context, stream)
                } ?: false
                withContext(Dispatchers.Main) {
                    if (ok) {
                        Toast.makeText(
                            context.applicationContext,
                            R.string.settings_restore_success,
                            Toast.LENGTH_LONG
                        ).show()
                        val activity = context.findActivity()
                        val restartIntent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        if (activity != null) {
                            activity.startActivity(restartIntent)
                            activity.finish()
                        } else {
                            context.startActivity(restartIntent)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            R.string.settings_restore_invalid,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val targetVesselId = activeVesselId ?: db.vesselDao().allActive().firstOrNull()?.id
                val components = if (targetVesselId != null) {
                    db.componentDao().allForVessel(targetVesselId).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val rawRecords = if (targetVesselId != null) {
                    db.serviceRecordDao().allForVessel(targetVesselId)
                } else {
                    emptyList()
                }
                val meterIds = rawRecords.mapNotNull { it.meterReadingId }
                val meters = if (meterIds.isNotEmpty()) {
                    db.meterDao().readingsByIds(meterIds).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val records = rawRecords.map { r ->
                    ServiceRecordExportItem(
                        date = r.date.toString(),
                        componentName = components[r.componentId]?.displayName(context) ?: components[r.componentId]?.name ?: "",
                        workType = formatWorkTypes(context, r.workTypes),
                        performedBy = r.performedBy,
                        meterHours = r.meterReadingId?.let { meters[it]?.value },
                        cost = r.cost,
                        description = formatRecordDescription(context, r.description),
                        notes = r.notes,
                    )
                }
                val items = if (targetVesselId != null) {
                    db.inventoryDao().allForVessel(targetVesselId)
                } else {
                    emptyList()
                }
                val locations = if (targetVesselId != null) {
                    db.inventoryDao().allLocationsForVessel(targetVesselId).map { loc ->
                        loc.copy(name = storageLocationDisplayName(context, loc.name))
                    }
                } else {
                    emptyList()
                }

                if (records.isEmpty() && items.isEmpty() && locations.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.settings_export_excel_empty), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val journalTitle = context.getString(R.string.excel_sheet_journal)
                val storeTitle = context.getString(R.string.excel_sheet_store)

                val journalHeaders = listOf(
                    context.getString(R.string.field_map_date),
                    context.getString(R.string.field_map_component),
                    context.getString(R.string.field_map_work_type),
                    context.getString(R.string.field_map_who),
                    context.getString(R.string.field_map_hours),
                    context.getString(R.string.field_map_cost),
                    context.getString(R.string.field_map_desc),
                    context.getString(R.string.field_map_notes),
                )
                val storeHeaders = listOf(
                    context.getString(R.string.store_location_label),
                    context.getString(R.string.store_part_name),
                    context.getString(R.string.store_part_number),
                    context.getString(R.string.store_part_mfr),
                    context.getString(R.string.store_part_qty),
                    context.getString(R.string.store_part_min_qty),
                    context.getString(R.string.store_part_notes),
                )

                val ok = context.contentResolver.openOutputStream(uri)?.use { stream ->
                    ExcelExporter.exportToXlsx(
                        outputStream = stream,
                        journalRecords = records,
                        inventoryItems = items,
                        locations = locations,
                        journalSheetName = journalTitle,
                        storeSheetName = storeTitle,
                        journalHeaders = journalHeaders,
                        storeHeaders = storeHeaders,
                    )
                } ?: false
                withContext(Dispatchers.Main) {
                    val msg = if (ok) context.getString(R.string.settings_export_excel_success) else context.getString(R.string.settings_export_excel_failed)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                // Everything from here to the render was unprotected: the DB
                // reads, the label lookups, preparePassportData. A throw in
                // any of them is an uncaught exception inside a coroutine,
                // which takes the whole app down - and the owner sees a crash
                // instead of a sentence telling them what went wrong.
                try {
                    val targetVesselId = activeVesselId ?: db.vesselDao().allActive().firstOrNull()?.id
                    val vessel = if (targetVesselId != null) db.vesselDao().byId(targetVesselId) else null
                    if (vessel == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.settings_export_excel_empty), Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val components = db.componentDao().allForVessel(vessel.id)
                    val compCategoryNames = components.associate { it.id to categoryDisplayName(context, it.categoryCode) }
                    val compDisplayNames = components.associate { it.id to it.displayName(context) }

                    val rawRecords = db.serviceRecordDao().allForVessel(vessel.id)
                    val meterIds = rawRecords.mapNotNull { it.meterReadingId }
                    val meters = if (meterIds.isNotEmpty()) {
                        db.meterDao().readingsByIds(meterIds).associate { it.id to it.value }
                    } else {
                        emptyMap<Long, Double>()
                    }

                    val recordIds = rawRecords.map { it.id }
                    val recordAttachments = if (recordIds.isNotEmpty()) {
                        db.attachmentDao().forParents(ParentType.SERVICE_RECORD, recordIds).groupBy { it.parentId }
                    } else {
                        emptyMap()
                    }

                    // Photographs hang off the components, not off the work
                    // done to them: the owner's five were all on equipment
                    // and the passport had nowhere to put a single one.
                    val componentIds = components.map { it.id }
                    val componentAttachments = if (componentIds.isNotEmpty()) {
                        db.attachmentDao().forParents(ParentType.COMPONENT, componentIds).groupBy { it.parentId }
                    } else {
                        emptyMap()
                    }

                    val labels = PassportLabels(
                        docTitle = context.getString(R.string.pdf_passport_title),
                        sectionVessel = context.getString(R.string.pdf_section_vessel),
                        sectionEquipment = context.getString(R.string.pdf_section_equipment),
                        sectionEquipmentPhotos = context.getString(R.string.pdf_section_equipment_photos),
                        sectionService = context.getString(R.string.pdf_section_service),
                        summaryTitle = context.getString(R.string.pdf_field_summary),
                        fieldMakeModel = context.getString(R.string.pdf_field_make_model),
                        fieldYear = context.getString(R.string.pdf_field_year),
                        fieldHin = context.getString(R.string.pdf_field_hin),
                        fieldLength = context.getString(R.string.pdf_field_length),
                        fieldHull = context.getString(R.string.pdf_field_hull),
                        fieldEngine = context.getString(R.string.pdf_field_engine),
                        fieldDrive = context.getString(R.string.pdf_field_drive),
                        fieldFuel = context.getString(R.string.pdf_field_fuel),
                        fieldComponentsCount = context.getString(R.string.pdf_field_components_count),
                        fieldRecordsCount = context.getString(R.string.pdf_field_records_count),
                        fieldTotalCost = context.getString(R.string.pdf_field_total_cost),
                        colComponent = context.getString(R.string.field_map_component),
                        colMakeModel = context.getString(R.string.pdf_field_make_model),
                        colSerial = context.getString(R.string.pdf_field_serial),
                        colInstalled = context.getString(R.string.pdf_field_installed),
                        colDate = context.getString(R.string.field_map_date),
                        colWorkType = context.getString(R.string.field_map_work_type),
                        colPerformedBy = context.getString(R.string.field_map_who),
                        colHours = context.getString(R.string.field_map_hours),
                        colCost = context.getString(R.string.field_map_cost),
                        colNotes = context.getString(R.string.field_map_notes),
                        morePhotos = context.getString(R.string.pdf_more_photos),
                        footerBrand = context.getString(R.string.pdf_footer_brand),
                        footerPage = context.getString(R.string.pdf_footer_page),
                    )

                    // Codes become words here, where a Context exists.
                    val lengthUnit = AppPreferences.getLengthUnit(context)
                    val vesselProfile = VesselProfileLabels(
                        hullType = vessel.hullType.ifBlank { null }
                            ?.let { profileOptionLabel(context, "hull", it) },
                        engineType = vessel.engine.ifBlank { null }
                            ?.let { profileOptionLabel(context, "engine", it) },
                        driveType = vessel.drive.ifBlank { null }
                            ?.let { profileOptionLabel(context, "drive", it) },
                        length = vessel.lengthMetres
                            ?.let { AppPreferences.formatLength(it, lengthUnit) },
                    )
                    val workTypeNames = rawRecords
                        .map { it.workTypes }
                        .distinct()
                        .associateWith { formatWorkTypes(context, it) }

                    val reportData = PdfPassportExporter.preparePassportData(
                        vessel = vessel.copy(name = vessel.displayName(context)),
                        components = components,
                        componentCategoryNames = compCategoryNames,
                        componentDisplayNames = compDisplayNames,
                        records = rawRecords.map { r ->
                            r.copy(description = formatRecordDescription(context, r.description))
                        },
                        meters = meters,
                        vesselProfile = vesselProfile,
                        workTypeNames = workTypeNames,
                        recordAttachments = recordAttachments,
                        componentAttachments = componentAttachments,
                    )

                    // `generatePdf` returns null for SUCCESS and openOutputStream
                    // returns null for NO STREAM. Folded into one elvis they
                    // cancel out and every successful export reports as a
                    // failure - which is what the previous version did.
                    val stream = context.contentResolver.openOutputStream(uri)
                    val error = if (stream == null) {
                        "openOutputStream returned null"
                    } else {
                        stream.use { PdfPassportExporter.generatePdf(context, reportData, it, labels) }
                    }

                    // The picker created the file before we ever ran. Leaving an
                    // empty one behind is what the owner reported as "a PDF that
                    // will not open"; better no file than a broken one.
                    if (error != null) {
                        runCatching {
                            DocumentsContract.deleteDocument(context.contentResolver, uri)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val msg = if (error == null) {
                            context.getString(R.string.settings_export_pdf_success)
                        } else {
                            context.getString(R.string.settings_export_pdf_failed) + "\n" + error
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("PdfPassport", "Passport export failed before rendering", e)
                    runCatching {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_export_pdf_failed) +
                                "\n" + e.javaClass.simpleName + ": " + (e.message ?: ""),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // 1. Units of measurement
            item(key = "units_section") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_units_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.settings_units_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                AppPreferences.applyMetricPreset(context)
                                lengthUnit = LengthUnit.METRES
                                distanceUnit = DistanceUnit.NAUTICAL_MILES
                                volumeUnit = VolumeUnit.LITRES
                                pressureUnit = PressureUnit.BAR
                                temperatureUnit = TemperatureUnit.CELSIUS
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_units_preset_metric),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                AppPreferences.applyNauticalImperialPreset(context)
                                lengthUnit = LengthUnit.FEET
                                distanceUnit = DistanceUnit.NAUTICAL_MILES
                                volumeUnit = VolumeUnit.GALLONS_US
                                pressureUnit = PressureUnit.PSI
                                temperatureUnit = TemperatureUnit.CELSIUS
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_units_preset_nautical),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    // Length unit
                    SettingsUnitRow(
                        title = stringResource(R.string.settings_unit_length),
                        selectedName = stringResource(lengthUnit.nameRes),
                        options = LengthUnit.entries.map { it to stringResource(it.shortNameRes) },
                        selected = lengthUnit,
                        onSelect = {
                            lengthUnit = it
                            AppPreferences.setLengthUnit(context, it)
                        },
                    )

                    // Distance unit
                    SettingsUnitRow(
                        title = stringResource(R.string.settings_unit_distance),
                        selectedName = stringResource(distanceUnit.nameRes),
                        options = DistanceUnit.entries.map { it to stringResource(it.shortNameRes) },
                        selected = distanceUnit,
                        onSelect = {
                            distanceUnit = it
                            AppPreferences.setDistanceUnit(context, it)
                        },
                    )

                    // Volume unit
                    SettingsUnitRow(
                        title = stringResource(R.string.settings_unit_volume),
                        selectedName = stringResource(volumeUnit.nameRes),
                        options = VolumeUnit.entries.map { it to stringResource(it.shortNameRes) },
                        selected = volumeUnit,
                        onSelect = {
                            volumeUnit = it
                            AppPreferences.setVolumeUnit(context, it)
                        },
                    )

                    // Pressure unit
                    SettingsUnitRow(
                        title = stringResource(R.string.settings_unit_pressure),
                        selectedName = stringResource(pressureUnit.nameRes),
                        options = PressureUnit.entries.map { it to stringResource(it.shortNameRes) },
                        selected = pressureUnit,
                        onSelect = {
                            pressureUnit = it
                            AppPreferences.setPressureUnit(context, it)
                        },
                    )

                    // Temperature unit
                    SettingsUnitRow(
                        title = stringResource(R.string.settings_unit_temperature),
                        selectedName = stringResource(temperatureUnit.nameRes),
                        options = TemperatureUnit.entries.map { it to stringResource(it.shortNameRes) },
                        selected = temperatureUnit,
                        onSelect = {
                            temperatureUnit = it
                            AppPreferences.setTemperatureUnit(context, it)
                        },
                    )
                }
            }

            item(key = "divider_1") { HorizontalDivider() }

            // 2. Currency
            item(key = "currency_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.settings_currency_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // ⚠️ Matched by SYMBOL because that is what the preference
                    // stores. SEK and NOK are both "kr", so whichever comes
                    // first wins the label here. Harmless for money - the
                    // symbol printed is right either way - but the row can
                    // read "Swedish krona" to someone who picked Norwegian.
                    val selectedCurrencyOption = AppPreferences.AVAILABLE_CURRENCIES
                        .firstOrNull { !isCustomSelected && it.symbol == selectedCurrency }

                    // Built with a plain if rather than ?.let: stringResource is
                    // composable, and reading it inside a lambda is a rule about
                    // inline functions that a reader should not have to know.
                    val currencyFieldText = if (selectedCurrencyOption != null) {
                        selectedCurrencyOption.symbol + " · " + selectedCurrencyOption.code +
                            " · " + stringResource(selectedCurrencyOption.nameRes)
                    } else {
                        selectedCurrency
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { currencyMenuOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = currencyFieldText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_down),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = currencyMenuOpen,
                            onDismissRequest = { currencyMenuOpen = false },
                        ) {
                            AppPreferences.AVAILABLE_CURRENCIES.forEach { option ->
                                DropdownMenuItem(
                                    modifier = Modifier.heightIn(min = 56.dp),
                                    text = {
                                        Text(
                                            text = option.symbol + " · " + option.code +
                                                " · " + stringResource(option.nameRes),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    },
                                    onClick = {
                                        currencyMenuOpen = false
                                        isCustomSelected = false
                                        selectedCurrency = option.symbol
                                        AppPreferences.setCurrency(context, option.symbol)
                                        onCurrencyChanged(option.symbol)
                                    },
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                modifier = Modifier.heightIn(min = 56.dp),
                                text = {
                                    Text(
                                        text = stringResource(R.string.settings_currency_custom),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                onClick = {
                                    currencyMenuOpen = false
                                    isCustomSelected = true
                                    if (customCurrencyText.isNotBlank()) {
                                        selectedCurrency = customCurrencyText.trim()
                                        AppPreferences.setCurrency(context, selectedCurrency)
                                        onCurrencyChanged(selectedCurrency)
                                    }
                                },
                            )
                        }
                    }

                    // Only while it is the answer, instead of a permanent card
                    // holding a radio button nobody had pressed.
                    if (isCustomSelected) {
                        OutlinedTextField(
                            value = customCurrencyText,
                            onValueChange = { input ->
                                customCurrencyText = input
                                val trimmed = input.trim()
                                if (trimmed.isNotEmpty()) {
                                    selectedCurrency = trimmed
                                    AppPreferences.setCurrency(context, trimmed)
                                    onCurrencyChanged(trimmed)
                                }
                            },
                            label = { Text(stringResource(R.string.settings_currency_custom)) },
                            placeholder = { Text(stringResource(R.string.settings_currency_custom_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item(key = "divider_2") { HorizontalDivider() }

            // 3. Theme & Contrast
            item(key = "theme_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    // Row 1: System, Light, Dark (3 equal columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val generalThemes: List<Pair<Scheme, Int>> = listOf(
                            Scheme.SYSTEM to R.string.theme_system,
                            Scheme.LIGHT to R.string.theme_light,
                            Scheme.DARK to R.string.theme_dark,
                        )
                        generalThemes.forEach { (scheme, labelRes) ->
                            SettingsSelectChip(
                                selected = currentScheme == scheme,
                                onClick = { onSchemeChanged(scheme) },
                                label = stringResource(labelRes),
                                modifier = Modifier.weight(1f),
                                height = 40.dp,
                            )
                        }
                    }

                    // Row 2: Sunlight, Night (2 equal columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val marineThemes: List<Pair<Scheme, Int>> = listOf(
                            Scheme.SUNLIGHT to R.string.theme_sunlight,
                            Scheme.NIGHT to R.string.theme_night,
                        )
                        marineThemes.forEach { (scheme, labelRes) ->
                            SettingsSelectChip(
                                selected = currentScheme == scheme,
                                onClick = { onSchemeChanged(scheme) },
                                label = stringResource(labelRes),
                                modifier = Modifier.weight(1f),
                                height = 40.dp,
                            )
                        }
                    }
                }
            }

            item(key = "divider_3") { HorizontalDivider() }

            // 4. Language
            item(key = "language_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsSelectChip(
                            selected = AppLanguage.current(context) == AppLanguage.UK,
                            onClick = {
                                if (AppLanguage.current(context) != AppLanguage.UK) {
                                    AppLanguage.toggle(context)
                                }
                            },
                            label = stringResource(R.string.lang_uk),
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                        )
                        SettingsSelectChip(
                            selected = AppLanguage.current(context) == AppLanguage.EN,
                            onClick = {
                                if (AppLanguage.current(context) != AppLanguage.EN) {
                                    AppLanguage.toggle(context)
                                }
                            },
                            label = stringResource(R.string.lang_en),
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                        )
                    }
                }
            }

            item(key = "divider_4") { HorizontalDivider() }

            // 5. Maintenance Notifications
            item(key = "notifications_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_notifications_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.settings_notifications_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_notifications_enabled),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { checked ->
                                notificationsEnabled = checked
                                AppPreferences.setNotificationsEnabled(context, checked)
                                if (checked) {
                                    MaintenanceCheckWorker.runNow(context)
                                } else {
                                    context.getSystemService(NotificationManager::class.java)?.cancelAll()
                                }
                            },
                        )
                    }

                    if (notificationsEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.settings_notifications_advance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val daysList: List<Pair<Int, Int>> = listOf(
                                    7 to R.string.settings_notifications_days_7,
                                    14 to R.string.settings_notifications_days_14,
                                    30 to R.string.settings_notifications_days_30,
                                )
                                daysList.forEach { (days, labelRes) ->
                                    SettingsSelectChip(
                                        selected = advanceDays == days,
                                        onClick = {
                                            advanceDays = days
                                            AppPreferences.setAdvanceReminderDays(context, days)
                                            MaintenanceCheckWorker.runNow(context)
                                        },
                                        label = stringResource(labelRes),
                                        modifier = Modifier.weight(1f),
                                        height = 38.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "divider_5") { HorizontalDivider() }

            // 6. Data & Export
            item(key = "data_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_data_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.settings_data_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Import CSV Button
                    Button(
                        onClick = { showImportSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_import_btn),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    val exportSuccessText = stringResource(R.string.settings_export_success)
                    val exportEmptyText = stringResource(R.string.settings_export_empty)
                    val exportStoreSuccessText = stringResource(R.string.settings_export_store_success)
                    val exportStoreEmptyText = stringResource(R.string.settings_export_store_empty)

                    // Export to Excel Button (Primary)
                    OutlinedButton(
                        onClick = {
                            val dateStr = LocalDate.now().toString()
                            createExcelLauncher.launch("hullbeat_export_$dateStr.xlsx")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_export_excel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Export Boat Passport (PDF) Button
                    OutlinedButton(
                        onClick = {
                            val dateStr = LocalDate.now().toString()
                            createPdfLauncher.launch("boat_passport_$dateStr.pdf")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_export_pdf_passport),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // CSV exports row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val targetVesselId = activeVesselId ?: db.vesselDao().allActive().firstOrNull()?.id
                                        val records = if (targetVesselId != null) {
                                            db.serviceRecordDao().allForVessel(targetVesselId)
                                        } else {
                                            emptyList()
                                        }
                                        if (records.isEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, exportEmptyText, Toast.LENGTH_SHORT).show()
                                            }
                                            return@withContext
                                        }
                                        val sb = StringBuilder()
                                        sb.append("Date,WorkType,Cost,PerformedBy,Description,Notes\n")
                                        records.forEach { r ->
                                            val costVal = r.cost?.toString() ?: ""
                                            val perfVal = (r.performedBy ?: "").replace("\"", "\"\"")
                                            val workTypeVal = r.workTypes.replace("\"", "\"\"")
                                            val descVal = r.description.replace("\"", "\"\"")
                                            val noteVal = (r.notes ?: "").replace("\"", "\"\"")
                                            sb.append(r.date)
                                                .append(",\"").append(workTypeVal).append("\",")
                                                .append(costVal).append(",\"")
                                                .append(perfVal).append("\",\"")
                                                .append(descVal).append("\",\"")
                                                .append(noteVal).append("\"\n")
                                        }
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "HullBeat Ship's Log")
                                            putExtra(Intent.EXTRA_TEXT, sb.toString())
                                        }
                                        val chooser = Intent.createChooser(sendIntent, null).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, exportSuccessText, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_export_journal_csv),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val targetVesselId = activeVesselId ?: db.vesselDao().allActive().firstOrNull()?.id
                                        val locations = if (targetVesselId != null) {
                                            db.inventoryDao().allLocationsForVessel(targetVesselId)
                                        } else {
                                            emptyList()
                                        }
                                        val items = if (targetVesselId != null) {
                                            db.inventoryDao().allForVessel(targetVesselId)
                                        } else {
                                            emptyList()
                                        }
                                        if (locations.isEmpty() && items.isEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, exportStoreEmptyText, Toast.LENGTH_SHORT).show()
                                            }
                                            return@withContext
                                        }
                                        val sb = StringBuilder()
                                        sb.append("Locker,PartName,PartNumber,Manufacturer,Quantity,MinQuantity,Notes\n")
                                        items.forEach { item ->
                                            val lockerName = (item.locationName ?: "").replace("\"", "\"\"")
                                            val name = item.partName.replace("\"", "\"\"")
                                            val partNum = (item.partNumber ?: "").replace("\"", "\"\"")
                                            val mfr = (item.manufacturer ?: "").replace("\"", "\"\"")
                                            val notes = (item.partNote ?: "").replace("\"", "\"\"")
                                            val minQty = item.minQuantity?.toString() ?: ""
                                            sb.append("\"").append(lockerName).append("\",\"")
                                                .append(name).append("\",\"")
                                                .append(partNum).append("\",\"")
                                                .append(mfr).append("\",")
                                                .append(item.quantity).append(",")
                                                .append(minQty).append(",\"")
                                                .append(notes).append("\"\n")
                                        }
                                        val usedLocationIds = items.mapNotNull { it.locationId }.toSet()
                                        val emptyLocations = locations.filter { it.id !in usedLocationIds }
                                        emptyLocations.forEach { loc ->
                                            val lockerName = loc.name.replace("\"", "\"\"")
                                            val notes = (loc.note ?: "").replace("\"", "\"\"")
                                            sb.append("\"").append(lockerName).append("\",\"\",\"\",\"\",0,\"\",\"")
                                                .append(notes).append("\"\n")
                                        }
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "HullBeat Store & Lockers")
                                            putExtra(Intent.EXTRA_TEXT, sb.toString())
                                        }
                                        val chooser = Intent.createChooser(sendIntent, null).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, exportStoreSuccessText, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_export_store_csv),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_backup_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.settings_backup_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Backup & Restore buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { createBackupLauncher.launch("hullbeat_backup.zip") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_backup_create),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        OutlinedButton(
                            onClick = { restoreBackupLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_backup_restore),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    // Import sessions list & rollback
                    if (importSessions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.import_history_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        importSessions.forEach { session ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.import_rows_count,
                                                session.rowCount,
                                                session.rowCount,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                ImportEngine.rollbackSession(session.id, db)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, context.getString(R.string.import_rollback_success), Toast.LENGTH_SHORT).show()
                                                    refreshSessions()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                    ) {
                                        Text(stringResource(R.string.import_rollback_btn))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "divider_6") { HorizontalDivider() }

            // 7. About
            item(key = "about_section") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_about_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_version, "1.0.0"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showImportSheet) {
        var importVesselId by remember { mutableStateOf<Long?>(activeVesselId) }
        LaunchedEffect(activeVesselId) {
            if (importVesselId == null) {
                withContext(Dispatchers.IO) {
                    importVesselId = db.vesselDao().allActive().firstOrNull()?.id
                }
            }
        }
        if (importVesselId != null) {
            ImportSheet(
                vesselId = importVesselId!!,
                database = db,
                onDismiss = { showImportSheet = false },
                onImportFinished = {
                    refreshSessions()
                },
            )
        }
    }
}

@Composable
private fun <T> SettingsUnitRow(
    title: String,
    selectedName: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = selectedName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (opt, shortLabel) ->
                SettingsSelectChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = shortLabel,
                    modifier = Modifier.weight(1f),
                    height = 38.dp,
                )
            }
        }
    }
}

@Composable
private fun SettingsSelectChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
