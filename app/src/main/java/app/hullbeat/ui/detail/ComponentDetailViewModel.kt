package app.hullbeat.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.hullbeat.R
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.AttachmentKind
import app.hullbeat.data.storage.AttachmentStorage
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.ComponentSpare
import app.hullbeat.data.db.Meter
import app.hullbeat.data.db.MeterReading
import app.hullbeat.data.db.MeterUnit
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.domain.DueCalculator
import app.hullbeat.domain.DueEngine
import app.hullbeat.notify.MaintenanceCheckWorker
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import app.hullbeat.data.catalog.CatalogLoader
import org.json.JSONObject

data class PassportField(
    val key: String,
    val value: String,
    val displayLabel: String = key,
) {
    fun displayLabel(context: Context): String =
        app.hullbeat.data.db.displayPassportLabel(context, key)
}

data class ComponentDetailUiState(
    val component: Component? = null,
    val vessel: Vessel? = null,
    val schedules: List<ServiceSchedule> = emptyList(),
    val dueResult: DueCalculator.Result? = null,
    /**
     * The schedule [dueResult] belongs to. The screen shows one urgency line
     * for a component that may have several schedules, and a service logged
     * from here closes THAT one - `schedules.firstOrNull()` is the raw query
     * order and would close an arbitrary sibling.
     */
    val primaryScheduleId: Long? = null,
    val primarySchedule: ServiceSchedule? = null,
    val unknownReason: DueEngine.UnknownReason = DueEngine.UnknownReason.NONE,
    val urgencyText: String = "",
    val spares: List<ComponentSpare> = emptyList(),
    val history: List<ServiceRecord> = emptyList(),
    val gallery: List<Attachment> = emptyList(),
    val isAnodeAdvice: Boolean = false,
    val waterAdvice: String? = null,
    val passportFields: List<PassportField> = emptyList(),
    val hasMeter: Boolean = false,
    val primaryMeterId: Long? = null,
    val latestMeterReading: MeterReading? = null,
    val isLoading: Boolean = true,
)

/**
 * Manages state for the Вузол (Component Detail) screen.
 *
 * Joins Component, Vessel, DueCalculator maintenance prediction,
 * ComponentSpare on-board stock projection, and service history.
 */
class ComponentDetailViewModel(
    private val componentId: Long,
    private val db: AppDatabase,
    private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComponentDetailUiState())
    val uiState: StateFlow<ComponentDetailUiState> = _uiState.asStateFlow()

    private var sparesJob: Job? = null
    private var historyJob: Job? = null
    private var galleryJob: Job? = null

    init {
        loadData()
    }

    fun renameComponent(newCustomName: String?) {
        viewModelScope.launch {
            val comp = _uiState.value.component ?: return@launch
            val trimmed = newCustomName?.trim()?.ifEmpty { null }
            val updated = comp.copy(customName = trimmed)
            db.componentDao().update(updated)
            val searchRepo = app.hullbeat.data.search.SearchRepository(db, context)
            searchRepo.indexComponent(updated)
            loadData()
        }
    }

    /**
     * Write one passport value.
     *
     * Rewrites the whole object rather than patching a string: the labels
     * come from the catalog and contain spaces, brackets and degree signs,
     * and hand-built JSON is how a stray quote corrupts a row.
     *
     * Reindexes afterwards, because specJson feeds the search index.
     */
    fun setPassportField(key: String, value: String) {
        viewModelScope.launch {
            val comp = _uiState.value.component ?: return@launch
            val obj = if (comp.specJson.isNullOrBlank()) {
                JSONObject()
            } else {
                runCatching { JSONObject(comp.specJson) }.getOrElse { JSONObject() }
            }
            obj.put(key, value.trim())
            val label = CatalogLoader.hintString(context, key)
            if (label != key && obj.has(label)) {
                obj.remove(label)
            }
            val updated = comp.copy(specJson = obj.toString())
            db.componentDao().update(updated)
            app.hullbeat.data.search.SearchRepository(db, context)
                .indexComponent(updated)
            loadData()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            val component = db.componentDao().byId(componentId) ?: return@launch
            val vessel = db.vesselDao().byId(component.vesselId)

            val schedules = db.scheduleDao().forComponent(componentId)

            val ownMeters = db.meterDao().forComponent(componentId)
            val meters = if (ownMeters.isNotEmpty()) {
                ownMeters
            } else if (component.categoryCode in setOf("engine_main", "drivetrain", "engine")) {
                val vesselMeters = db.meterDao().forVessel(component.vesselId)
                val vComponents = db.componentDao().forVessel(component.vesselId).associateBy { it.id }
                val engineMeter = if (component.engineIndex != null) {
                    vesselMeters.firstOrNull { vComponents[it.componentId]?.engineIndex == component.engineIndex }
                } else {
                    vesselMeters.firstOrNull { vComponents[it.componentId]?.categoryCode in setOf("engine_main", "drivetrain", "engine") }
                        ?: vesselMeters.firstOrNull()
                }
                listOfNotNull(engineMeter)
            } else {
                emptyList()
            }
            val readings = meters.flatMap { meter ->
                db.meterDao().readings(meter.id).map { r ->
                    DueCalculator.Reading(r.date, r.value, r.precision)
                }
            }
            // `forSchedules` has `WHERE scheduleId IN (...)`, so it can never
            // return the imported and pre-Sprint-2 rows whose scheduleId is
            // null - the very history the component fallback exists for. The
            // `?: 0L` that used to key this map was dead against that query
            // and would have collided every legacy row under key 0 the moment
            // the query changed. Read the component's whole history instead.
            val records = db.serviceRecordDao().forComponent(componentId)
            val readingIds = records.mapNotNull { it.meterReadingId }.distinct()
            val readingsMap = if (readingIds.isNotEmpty()) {
                db.meterDao().readingsByIds(readingIds).associateBy { it.id }
            } else emptyMap()

            val evaluation = DueEngine.evaluate(
                componentId = componentId,
                schedules = schedules,
                readings = readings,
                records = records,
                readingById = { id -> readingsMap[id]?.value },
                today = LocalDate.now(),
                vessel = vessel,
            )

            val topSchedule = evaluation.primarySchedule
            val topDueResult = evaluation.primaryResult
            val urgency = formatUrgency(topDueResult, topSchedule, evaluation.unknownReason)

            val isAnode = component.catalogCode?.contains("anode") == true
            val waterAdvice = resolveWaterAdvice(component, vessel)

            val passportFields = resolvePassportFields(component)

            val primaryMeter = meters.firstOrNull()
            val latestReading = primaryMeter?.let { db.meterDao().latestReading(it.id) }
            val hasMeter = meters.isNotEmpty() || schedules.any { it.intervalHours != null }

            _uiState.value = ComponentDetailUiState(
                component = component,
                vessel = vessel,
                schedules = schedules,
                primaryScheduleId = topSchedule?.id,
                primarySchedule = topSchedule,
                unknownReason = evaluation.unknownReason,
                dueResult = topDueResult,
                urgencyText = urgency,
                isAnodeAdvice = isAnode && vessel != null,
                waterAdvice = waterAdvice,
                passportFields = passportFields,
                hasMeter = hasMeter,
                primaryMeterId = primaryMeter?.id,
                latestMeterReading = latestReading,
                isLoading = false,
            )

            // Cancel previous collectors so they do not accumulate on reload
            sparesJob?.cancel()
            if (vessel != null) {
                sparesJob = launch {
                    db.inventoryDao().observeSparesForComponent(vessel.id, componentId).collectLatest { spares ->
                        _uiState.value = _uiState.value.copy(spares = spares)
                    }
                }
            }

            historyJob?.cancel()
            historyJob = launch {
                db.serviceRecordDao().observeForComponent(componentId).collectLatest { history ->
                    _uiState.value = _uiState.value.copy(history = history)
                }
            }

            galleryJob?.cancel()
            galleryJob = launch {
                db.attachmentDao().observeComponentGallery(componentId).collectLatest { gallery ->
                    _uiState.value = _uiState.value.copy(gallery = gallery)
                }
            }
        }
    }

    fun addPhoto(uri: Uri, caption: String? = null, takenAt: LocalDate? = LocalDate.now()) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedUri = AttachmentStorage.copyToInternalStorage(context, uri, "attachments")
            if (savedUri != null) {
                db.attachmentDao().insert(
                    Attachment(
                        parentType = ParentType.COMPONENT,
                        parentId = componentId,
                        kind = AttachmentKind.PHOTO,
                        fileUri = savedUri.toString(),
                        caption = caption?.trim()?.ifEmpty { null },
                        takenAt = takenAt ?: LocalDate.now(),
                    )
                )
            }
        }
    }

    fun deletePhoto(attachmentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val att = db.attachmentDao().byId(attachmentId)
            if (att != null) {
                AttachmentStorage.deleteInternalFile(context, att.fileUri)
            }
            db.attachmentDao().deleteById(attachmentId)
        }
    }

    suspend fun getMeterHours(readingId: Long): Double? {
        return withContext(Dispatchers.IO) {
            db.meterDao().readingById(readingId)?.value
        }
    }

    fun updateServiceRecord(
        record: ServiceRecord,
        meterHours: Double? = null,
        newPhotoUris: List<Uri> = emptyList(),
        removedPhotoIds: List<Long> = emptyList(),
        photoUri: Uri? = null,
        removePhoto: Boolean = false,
        existingPhotoId: Long? = null,
        onDone: () -> Unit = {},
    ) {
        val allNewUris = if (newPhotoUris.isNotEmpty()) newPhotoUris else listOfNotNull(photoUri)
        val allRemovedIds = if (removedPhotoIds.isNotEmpty()) removedPhotoIds else listOfNotNull(if (removePhoto) existingPhotoId else null)

        viewModelScope.launch(Dispatchers.IO) {
            val savedUris = allNewUris.mapNotNull { uri ->
                AttachmentStorage.copyToInternalStorage(context, uri, "attachments")
            }

            db.withTransaction {
                var updatedRecord = record

                if (meterHours != null) {
                    val existingReadingId = record.meterReadingId
                    if (existingReadingId != null) {
                        val existing = db.meterDao().readingById(existingReadingId)
                        if (existing != null) {
                            db.meterDao().updateReading(existing.copy(date = record.date, value = meterHours))
                        } else {
                            val primaryMeter = getOrCreateMeterForComponent(componentId)
                            val newId = db.meterDao().insertReading(MeterReading(meterId = primaryMeter.id, date = record.date, value = meterHours))
                            updatedRecord = updatedRecord.copy(meterReadingId = newId)
                        }
                    } else {
                        val primaryMeter = getOrCreateMeterForComponent(componentId)
                        val newId = db.meterDao().insertReading(MeterReading(meterId = primaryMeter.id, date = record.date, value = meterHours))
                        updatedRecord = updatedRecord.copy(meterReadingId = newId)
                    }
                } else if (record.meterReadingId != null) {
                    db.meterDao().deleteReading(record.meterReadingId)
                    updatedRecord = updatedRecord.copy(meterReadingId = null)
                }

                db.serviceRecordDao().update(updatedRecord)

                allRemovedIds.forEach { photoId ->
                    val att = db.attachmentDao().byId(photoId)
                    if (att != null) {
                        AttachmentStorage.deleteInternalFile(context, att.fileUri)
                    }
                    db.attachmentDao().deleteById(photoId)
                }
                if (removePhoto && existingPhotoId == null && removedPhotoIds.isEmpty()) {
                    val existingAtts = db.attachmentDao().forParent(ParentType.SERVICE_RECORD, record.id)
                    existingAtts.forEach { att ->
                        AttachmentStorage.deleteInternalFile(context, att.fileUri)
                    }
                    db.attachmentDao().deleteForParent(ParentType.SERVICE_RECORD, record.id)
                }

                savedUris.forEach { uri ->
                    db.attachmentDao().insert(
                        Attachment(
                            parentType = ParentType.SERVICE_RECORD,
                            parentId = record.id,
                            kind = AttachmentKind.PHOTO,
                            fileUri = uri.toString(),
                            takenAt = record.date,
                        )
                    )
                }
            }

            MaintenanceCheckWorker.runNow(context)
            loadData()
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    private suspend fun getOrCreateMeterForComponent(componentId: Long): Meter {
        val ownMeters = db.meterDao().forComponent(componentId)
        return ownMeters.firstOrNull() ?: run {
            val comp = db.componentDao().byId(componentId)
            val vesselMeters = if (comp != null) db.meterDao().forVessel(comp.vesselId) else emptyList()
            val vComponents = if (comp != null) db.componentDao().forVessel(comp.vesselId).associateBy { it.id } else emptyMap()
            val engineMeter = if (comp != null && comp.categoryCode in setOf("engine_main", "drivetrain", "engine")) {
                if (comp.engineIndex != null) {
                    vesselMeters.firstOrNull { vComponents[it.componentId]?.engineIndex == comp.engineIndex }
                } else {
                    vesselMeters.firstOrNull { vComponents[it.componentId]?.categoryCode in setOf("engine_main", "drivetrain", "engine") }
                        ?: vesselMeters.firstOrNull()
                }
            } else null
            engineMeter ?: run {
                val newId = db.meterDao().insertMeter(Meter(componentId = componentId, unit = MeterUnit.HOURS))
                Meter(id = newId, componentId = componentId, unit = MeterUnit.HOURS)
            }
        }
    }

    fun deleteServiceRecord(recordId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                val record = db.serviceRecordDao().byId(recordId)
                if (record?.meterReadingId != null) {
                    db.meterDao().deleteReading(record.meterReadingId)
                }
                db.serviceRecordDao().deleteById(recordId)
                val attachments = db.attachmentDao().forParent(ParentType.SERVICE_RECORD, recordId)
                attachments.forEach { att ->
                    AttachmentStorage.deleteInternalFile(context, att.fileUri)
                }
                db.attachmentDao().deleteForParent(ParentType.SERVICE_RECORD, recordId)
            }
            MaintenanceCheckWorker.runNow(context)
            loadData()
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun recordMeterReading(
        date: LocalDate,
        hours: Double,
        onSaved: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val primaryMeter = getOrCreateMeterForComponent(componentId)
            db.meterDao().insertReading(
                MeterReading(meterId = primaryMeter.id, date = date, value = hours)
            )
            MaintenanceCheckWorker.runNow(context)
            loadData()
            withContext(Dispatchers.Main) {
                onSaved?.invoke()
            }
        }
    }

    fun hideFromBoat(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.componentDao().archive(componentId)
            }
            onDone()
        }
    }

    private fun resolveWaterAdvice(component: Component, vessel: Vessel?): String? {
        // There is no "anodes" category. Anodes are spread across five of
        // them - hull, drivetrain, engine_main, elec_ac - because an anode
        // belongs to the thing it protects, not to a drawer of anodes. The
        // first half of this test could therefore never be true; only the
        // code check was doing any work.
        //
        // `anodes_hull`, `shaft_anode`, `saildrive_anode`, `thruster_anode`,
        // `heat_exchanger_anode`, `ob_anodes`, `water_heater_anode` - every
        // one of them carries "anode" in its catalog code, which is what
        // makes this reliable.
        val isAnode = component.catalogCode?.contains("anode") == true
        if (!isAnode || vessel == null) return null

        val res = context.resources
        return when (vessel.water) {
            "fresh" -> res.getString(
                R.string.detail_water_advice,
                res.getString(R.string.water_fresh),
                res.getString(R.string.anode_magnesium),
            )
            "brackish" -> res.getString(
                R.string.detail_water_advice,
                res.getString(R.string.water_brackish),
                res.getString(R.string.anode_aluminum),
            )
            else -> res.getString(
                R.string.detail_water_advice,
                res.getString(R.string.water_salt),
                res.getString(R.string.anode_zinc),
            )
        }
    }

    private fun resolvePassportFields(component: Component): List<PassportField> {
        val fields = mutableListOf<PassportField>()
        val res = context.resources
        if (!component.make.isNullOrBlank()) fields += PassportField(res.getString(R.string.passport_make), component.make)
        if (!component.model.isNullOrBlank()) fields += PassportField(res.getString(R.string.passport_model), component.model)
        if (!component.serial.isNullOrBlank()) fields += PassportField(res.getString(R.string.passport_serial), component.serial)
        if (component.yearInstalled != null) fields += PassportField(res.getString(R.string.passport_year), component.yearInstalled.toString())

        val rawHints = try {
            CatalogLoader.rawSpecHintsFor(context, component.catalogCode)
        } catch (_: Exception) {
            emptyList()
        }

        val jsonObj = if (!component.specJson.isNullOrBlank()) {
            runCatching { JSONObject(component.specJson) }.getOrNull()
        } else null

        val processedKeys = mutableSetOf<String>()

        // 1. Catalog hints first (always visible!)
        for (hint in rawHints) {
            val label = CatalogLoader.hintString(context, hint)
            val value = jsonObj?.let {
                if (it.has(hint)) it.optString(hint, "")
                else if (it.has(label)) it.optString(label, "")
                else ""
            } ?: ""
            fields += PassportField(key = hint, value = value, displayLabel = label)
            processedKeys.add(hint)
            processedKeys.add(label)
        }

        // 2. Any additional custom fields from specJson
        if (jsonObj != null) {
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k !in processedKeys) {
                    val v = jsonObj.optString(k, "")
                    val label = CatalogLoader.hintString(context, k)
                    fields += PassportField(key = k, value = v, displayLabel = label)
                    processedKeys.add(k)
                    processedKeys.add(label)
                }
            }
        }
        return fields
    }

    private fun formatUrgency(
        r: DueCalculator.Result?,
        schedule: ServiceSchedule?,
        unknownReason: DueEngine.UnknownReason = DueEngine.UnknownReason.NONE,
    ): String {
        val res = context.resources
        if (r == null || schedule == null) {
            return res.getString(R.string.detail_no_interval)
        }

        if (r.state == DueCalculator.State.DEFERRED) {
            val dateStr = schedule.deferredUntil?.toString() ?: ""
            return if (!schedule.deferReason.isNullOrBlank()) {
                res.getString(R.string.deferred_until_reason, schedule.deferReason, dateStr)
            } else {
                res.getString(R.string.deferred_until, dateStr)
            }
        }

        val days = r.daysRemaining
        return when {
            days != null && days < 0 ->
                res.getQuantityString(R.plurals.due_overdue_days, (-days).toInt(), (-days).toInt())

            r.driver == DueCalculator.Driver.HOURS && r.unitsRemaining != null ->
                res.getQuantityString(R.plurals.due_in_hours, r.unitsRemaining.toInt(), r.unitsRemaining.toInt())

            days != null ->
                res.getQuantityString(R.plurals.due_in_days, days.toInt(), days.toInt())

            unknownReason == DueEngine.UnknownReason.NEEDS_INITIAL_METER ->
                res.getString(R.string.due_unknown_needs_meter)

            unknownReason == DueEngine.UnknownReason.NO_SCHEDULES ->
                res.getString(R.string.due_unknown_needs_schedule)

            else -> res.getString(R.string.due_unknown)
        }
    }

    companion object {
        fun provideFactory(componentId: Long, db: AppDatabase, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ComponentDetailViewModel(componentId, db, context.applicationContext) as T
                }
            }
    }
}
