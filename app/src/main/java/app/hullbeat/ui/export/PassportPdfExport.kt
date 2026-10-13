package app.hullbeat.ui.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.hullbeat.HullBeatApp
import app.hullbeat.R
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.displayName
import app.hullbeat.data.db.formatRecordDescription
import app.hullbeat.data.db.formatWorkTypes
import app.hullbeat.data.db.profileOptionLabel
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.export.PassportLabels
import app.hullbeat.domain.export.PdfPassportExporter
import app.hullbeat.domain.export.VesselProfileLabels
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The export outlives the screen that started it. It used to run in the
 * settings drawer's own scope; from the Journal, switching tabs while the
 * PDF was being built would have cancelled it halfway through a file the
 * picker had already created.
 */
private val exportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Starts the vessel passport export: the system "save as" picker, then the
 * render into the file chosen there. One entry point for every screen that
 * offers it, so the Settings button and the Journal button cannot drift
 * into two documents.
 *
 * The returned function takes the vessel to export; null means the first
 * active one, which is what Settings did with no vessel selected.
 */
@Composable
fun rememberPassportPdfExport(): (vesselId: Long?) -> Unit {
    val context = LocalContext.current
    // Saveable: the picker is another activity, and the process may be
    // gone by the time it returns. A plain `remember` would come back null
    // and export a different boat than the one the owner asked for.
    var pendingVesselId by rememberSaveable { mutableStateOf<Long?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            val vesselId = pendingVesselId
            exportScope.launch { exportPassport(context, vesselId, uri) }
        }
    }

    return { vesselId ->
        pendingVesselId = vesselId
        launcher.launch("boat_passport_${LocalDate.now()}.pdf")
    }
}

private suspend fun exportPassport(context: Context, requestedVesselId: Long?, uri: Uri) {
    val db = (context.applicationContext as HullBeatApp).database
    // Everything from here to the render was unprotected: the DB
    // reads, the label lookups, preparePassportData. A throw in
    // any of them is an uncaught exception inside a coroutine,
    // which takes the whole app down - and the owner sees a crash
    // instead of a sentence telling them what went wrong.
    try {
        val targetVesselId = requestedVesselId ?: db.vesselDao().allActive().firstOrNull()?.id
        val vessel = if (targetVesselId != null) db.vesselDao().byId(targetVesselId) else null
        if (vessel == null) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.settings_export_excel_empty), Toast.LENGTH_SHORT).show()
            }
            return
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
            compiledBy = context.getString(R.string.pdf_compiled_by),
            currency = AppPreferences.getCurrency(context),
            hoursUnit = context.getString(R.string.meter_hours_unit),
            // The locale these strings were resolved in, so the numbers
            // match the language of the words around them.
            locale = context.resources.configuration.locales[0],
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
