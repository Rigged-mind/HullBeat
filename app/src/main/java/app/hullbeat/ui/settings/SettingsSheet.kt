package app.hullbeat.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import app.hullbeat.MainActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import app.hullbeat.data.db.ImportSession
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.formatWorkTypes
import app.hullbeat.data.db.formatRecordDescription
import app.hullbeat.data.db.storageLocationDisplayName
import app.hullbeat.domain.backup.BackupManager
import app.hullbeat.domain.backup.BackupResult
import app.hullbeat.domain.export.ExcelExporter
import app.hullbeat.domain.export.ServiceRecordExportItem
import app.hullbeat.domain.importer.ImportEngine
import app.hullbeat.notify.MaintenanceCheckWorker
import java.time.LocalDate
import app.hullbeat.ui.importer.ImportSheet
import app.hullbeat.ui.export.rememberPassportPdfExport
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import app.hullbeat.ui.theme.appFlingBehavior
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
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
import app.hullbeat.ui.theme.LocalExtendedColors
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MenuDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SettingsDrawerSheet(
    currentCurrency: String,
    onCurrencyChanged: (String) -> Unit,
    currentScheme: Scheme,
    onSchemeChanged: (Scheme) -> Unit,
    onClose: () -> Unit,
    activeVesselId: Long? = null,
    activeVesselName: String? = null,
    onSwitchVessel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

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
    var languageMenuOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }

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
                val result = context.contentResolver.openOutputStream(uri)?.use { stream ->
                    BackupManager.createBackup(context, db, stream)
                } ?: BackupResult(success = false)
                withContext(Dispatchers.Main) {
                    val msg = when {
                        !result.success -> context.getString(R.string.settings_backup_failed)
                        result.skippedFiles > 0 -> context.resources.getQuantityString(
                            R.plurals.settings_backup_skipped_files,
                            result.skippedFiles,
                            result.skippedFiles,
                        )
                        else -> context.getString(R.string.settings_backup_success)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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

    val exportPassportPdf = rememberPassportPdfExport()

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.widthIn(max = 360.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            flingBehavior = appFlingBehavior(),
        ) {
            // Header
            item(key = "drawer_header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_boat),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.action_cancel),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    if (activeVesselName != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeVesselName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (onSwitchVessel != null) {
                                    TextButton(
                                        onClick = onSwitchVessel,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.drawer_switch_vessel),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "divider_top") { HorizontalDivider() }

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

                    SettingsDropdown(
                        fieldText = currencyFieldText,
                        expanded = currencyMenuOpen,
                        onExpandedChange = { currencyMenuOpen = it },
                    ) {
                        AppPreferences.AVAILABLE_CURRENCIES.forEach { option ->
                            SettingsDropdownItem(
                                text = option.symbol + " · " + option.code +
                                    " · " + stringResource(option.nameRes),
                                // By identity, not symbol: only the row the
                                // field shows is ticked, not both "kr".
                                selected = option === selectedCurrencyOption,
                                onClick = {
                                    currencyMenuOpen = false
                                    isCustomSelected = false
                                    selectedCurrency = option.symbol
                                    AppPreferences.setCurrency(context, option.symbol)
                                    onCurrencyChanged(option.symbol)
                                },
                            )
                        }

                        SettingsDropdownDivider()

                        SettingsDropdownItem(
                            text = stringResource(R.string.settings_currency_custom),
                            selected = isCustomSelected,
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

                    // Everyday schemes, then the marine ones (sunlight, night,
                    // colour), split by a divider.
                    val generalThemes: List<Pair<Scheme, Int>> = listOf(
                        Scheme.SYSTEM to R.string.theme_system,
                        Scheme.LIGHT to R.string.theme_light,
                        Scheme.DARK to R.string.theme_dark,
                    )
                    val marineThemes: List<Pair<Scheme, Int>> = listOf(
                        Scheme.SUNLIGHT to R.string.theme_sunlight,
                        Scheme.NIGHT to R.string.theme_night,
                        Scheme.COLORFUL to R.string.theme_colorful,
                    )
                    val currentThemeLabel = (generalThemes + marineThemes)
                        .first { it.first == currentScheme }.second

                    SettingsDropdown(
                        fieldText = stringResource(currentThemeLabel),
                        expanded = themeMenuOpen,
                        onExpandedChange = { themeMenuOpen = it },
                    ) {
                        generalThemes.forEach { (scheme, labelRes) ->
                            SettingsDropdownItem(
                                text = stringResource(labelRes),
                                selected = currentScheme == scheme,
                                onClick = {
                                    themeMenuOpen = false
                                    onSchemeChanged(scheme)
                                },
                            )
                        }
                        SettingsDropdownDivider()
                        marineThemes.forEach { (scheme, labelRes) ->
                            SettingsDropdownItem(
                                text = stringResource(labelRes),
                                selected = currentScheme == scheme,
                                onClick = {
                                    themeMenuOpen = false
                                    onSchemeChanged(scheme)
                                },
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

                    // A dropdown, built like the currency one above: chips took
                    // a row per two languages and stop scaling past four. The
                    // list is AppLanguage.SUPPORTED, so a language released
                    // later appears here without touching this screen.
                    val current = AppLanguage.current(context)
                    SettingsDropdown(
                        fieldText = stringResource(AppLanguage.nameRes(current)),
                        expanded = languageMenuOpen,
                        onExpandedChange = { languageMenuOpen = it },
                    ) {
                        AppLanguage.SUPPORTED.forEach { tag ->
                            SettingsDropdownItem(
                                text = stringResource(AppLanguage.nameRes(tag)),
                                selected = tag == current,
                                onClick = {
                                    languageMenuOpen = false
                                    if (current != tag) AppLanguage.set(context, tag)
                                },
                            )
                        }
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
                            exportPassportPdf(activeVesselId)
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
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = {
                                val url = "https://github.com/Rigged-mind/HullBeat"
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = stringResource(R.string.settings_about_github),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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

/**
 * A field that opens a list, drawn in the app's own colours.
 *
 * The stock DropdownMenu paints itself `surfaceContainer`, which none of our
 * schemes define, so it fell back to Material's baseline lavender in every
 * theme. Here the container, border and selection come from the live scheme,
 * and the menu is as wide as its field.
 */
@Composable
private fun SettingsDropdown(
    fieldText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    items: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderWidth = LocalExtendedColors.current.borderWidth.dp
    val density = LocalDensity.current
    var fieldWidth by remember { mutableIntStateOf(0) }
    val chevronTurn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { onExpandedChange(true) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .onSizeChanged { fieldWidth = it.width },
            shape = RoundedCornerShape(12.dp),
            color = if (expanded) colors.primaryContainer.copy(alpha = 0.35f) else colors.surface,
            border = BorderStroke(
                width = if (expanded) borderWidth + 0.5.dp else borderWidth,
                color = if (expanded) colors.primary else colors.outline,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = fieldText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronTurn),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .width(with(density) { fieldWidth.toDp() })
                .padding(vertical = 2.dp),
            offset = DpOffset(0.dp, 6.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(borderWidth, colors.outlineVariant),
            content = items,
        )
    }
}

/** One row of a [SettingsDropdown]; the chosen one sits on the brand tint with a tick. */
@Composable
private fun SettingsDropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
        trailingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            null
        },
        colors = MenuDefaults.itemColors(
            textColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        ),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .heightIn(min = 48.dp),
    )
}

@Composable
private fun SettingsDropdownDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
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

@Deprecated("Use SettingsDrawerSheet instead")
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
    SettingsDrawerSheet(
        currentCurrency = currentCurrency,
        onCurrencyChanged = onCurrencyChanged,
        currentScheme = currentScheme,
        onSchemeChanged = onSchemeChanged,
        onClose = onDismiss,
        activeVesselId = activeVesselId,
        modifier = modifier,
    )
}

