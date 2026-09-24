package app.hullbeat.ui.now

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.hullbeat.R
import app.hullbeat.data.catalog.ChecklistsLoader
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.AttachmentKind
import app.hullbeat.data.storage.AttachmentStorage
import app.hullbeat.data.db.ChecklistRun
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.Meter
import app.hullbeat.data.db.MeterReading
import app.hullbeat.data.db.MeterUnit
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.WorkType
import app.hullbeat.data.db.displayName
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.DueCalculator
import app.hullbeat.domain.DueEngine
import app.hullbeat.notify.MaintenanceCheckWorker
import app.hullbeat.notify.MaintenanceNotification
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages the state for the Зараз (Now) screen.
 *
 * Runs components through DueCalculator.evaluate(), maps them to NowItem rows,
 * and maintains the fleet-wide or single-vessel view.
 */
class NowViewModel(
    private val db: AppDatabase,
    private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NowUiState(isLoading = true))
    val uiState: StateFlow<NowUiState> = _uiState.asStateFlow()

    private val _selectedVesselId = MutableStateFlow<Long?>(AppPreferences.getSelectedVesselId(context))
    private var lastComputedDate: LocalDate? = null

    init {
        viewModelScope.launch {
            db.vesselDao().observeAll().collectLatest { vessels ->
                val activeVessels = vessels.filter { !it.archived }
                val savedId = _selectedVesselId.value
                val validatedId = if (savedId != null && activeVessels.any { it.id == savedId }) {
                    savedId
                } else if (savedId != null) {
                    AppPreferences.setSelectedVesselId(context, null)
                    _selectedVesselId.value = null
                    null
                } else {
                    null
                }
                computeDue(activeVessels, validatedId)
            }
        }
    }

    fun selectVessel(vesselId: Long?) {
        _selectedVesselId.value = vesselId
        AppPreferences.setSelectedVesselId(context, vesselId)
        viewModelScope.launch {
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, vesselId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
        }
    }

    fun refreshIfDateChanged() {
        if (lastComputedDate != LocalDate.now()) {
            refresh()
        }
    }

    private suspend fun computeDue(vessels: List<Vessel>, targetVesselId: Long?) = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        lastComputedDate = today
        val activeVessels = if (targetVesselId != null) {
            vessels.filter { it.id == targetVesselId }
        } else {
            vessels
        }

        val items = mutableListOf<NowItem>()
        val upcomingItemsList = mutableListOf<NowItem>()
        val unrecordedList = mutableListOf<Component>()
        val waitingMetersList = mutableListOf<VesselWaitingMeters>()
        val vesselMeterInfosList = mutableListOf<VesselMeterInfo>()

        val defaultAdvanceDays = AppPreferences.getAdvanceReminderDays(context).toLong()

        // Counted across every active vessel, like the items themselves.
        var needsMeter = 0
        var needsFirstRecord = 0

        for (vessel in activeVessels) {
            val components = db.componentDao().forVessel(vessel.id)
            if (components.isEmpty()) continue

            var vesselNeedsMeter = 0
            val schedulesByComponent = db.scheduleDao().forVessel(vessel.id).groupBy { it.componentId }
            val recordsForVessel = db.serviceRecordDao().allForVessel(vessel.id)
            val recordsByComponent = recordsForVessel.groupBy { it.componentId }

            val componentsById = components.associateBy { it.id }
            val vesselMetersList = db.meterDao().forVessel(vessel.id)
            val metersByComponent = vesselMetersList.groupBy { it.componentId }
            val readingsForVessel = db.meterDao().readingsForVessel(vessel.id)
            val readingsByMeter = readingsForVessel.groupBy { it.meterId }
            val readingsMap = readingsForVessel.associateBy { it.id }

            val defaultEngineMeter = vesselMetersList.firstOrNull { m ->
                val c = componentsById[m.componentId]
                c != null && c.categoryCode in setOf("engine_main", "drivetrain", "engine")
            }

            for (component in components) {
                val schedules = schedulesByComponent[component.id] ?: emptyList()
                if (schedules.isEmpty()) continue

                val ownMeters = metersByComponent[component.id] ?: emptyList()
                val meters = if (ownMeters.isNotEmpty()) {
                    ownMeters
                } else if (component.categoryCode in setOf("engine_main", "drivetrain", "engine")) {
                    val matchingMeter = if (component.engineIndex != null) {
                        vesselMetersList.firstOrNull { m ->
                            componentsById[m.componentId]?.engineIndex == component.engineIndex
                        } ?: defaultEngineMeter
                    } else {
                        defaultEngineMeter
                    }
                    listOfNotNull(matchingMeter)
                } else {
                    emptyList()
                }

                val readings = meters.flatMap { meter ->
                    readingsByMeter[meter.id]?.map { r ->
                        DueCalculator.Reading(r.date, r.value, r.precision)
                    } ?: emptyList()
                }

                val evaluation = DueEngine.evaluate(
                    componentId = component.id,
                    schedules = schedules,
                    readings = readings,
                    // The component's whole record set, including rows with
                    // no scheduleId - imported and pre-Sprint-2 history.
                    // DueEngine.indexRecords decides which one is last.
                    records = recordsByComponent[component.id] ?: emptyList(),
                    readingById = { id -> readingsMap[id]?.value },
                    today = today,
                    vessel = vessel,
                    defaultAdvanceDays = defaultAdvanceDays,
                )

                if (evaluation.isUnknown) {
                    // Counted, not listed - see NowUiState. A component with
                    // no history is not silent about nothing; it is waiting
                    // for one of exactly two things.
                    when (evaluation.unknownReason) {
                        DueEngine.UnknownReason.NEEDS_INITIAL_METER -> {
                            needsMeter++
                            vesselNeedsMeter++
                            unrecordedList += component
                        }
                        DueEngine.UnknownReason.INSUFFICIENT_DATA,
                        DueEngine.UnknownReason.NEEDS_EXPIRY_DATE -> {
                            needsFirstRecord++
                            unrecordedList += component
                        }
                        else -> Unit
                    }
                }

                if (evaluation.isOverdue || evaluation.isDueSoon || evaluation.isDeferred) {
                    val schedule = evaluation.primarySchedule ?: continue
                    val result = evaluation.primaryResult
                    val urgency = formatUrgency(result, schedule.deferredUntil, schedule.deferReason)
                    val verb = if (schedule.expiresOn != null) {
                        context.getString(R.string.action_renew)
                    } else {
                        context.getString(R.string.action_mark_done)
                    }

                    items += NowItem(
                        component = component,
                        schedule = schedule,
                        vessel = vessel,
                        state = result.state,
                        result = result,
                        urgencyText = urgency,
                        actionVerb = verb,
                        needsNewExpiry = schedule.expiresOn != null,
                        isDeferred = evaluation.isDeferred,
                        deferredUntil = schedule.deferredUntil,
                        deferReason = schedule.deferReason,
                        criticality = component.criticality,
                        hasMeter = meters.isNotEmpty() || schedule.intervalHours != null,
                    )
                } else if (evaluation.isUpcoming) {
                    val schedule = evaluation.primarySchedule
                    if (schedule != null) {
                        val result = evaluation.primaryResult
                        val urgency = formatUrgency(result, null, null)
                        val verb = if (schedule.expiresOn != null) {
                            context.getString(R.string.action_renew)
                        } else {
                            context.getString(R.string.action_mark_done)
                        }

                        upcomingItemsList += NowItem(
                            component = component,
                            schedule = schedule,
                            vessel = vessel,
                            state = result.state,
                            result = result,
                            urgencyText = urgency,
                            actionVerb = verb,
                            needsNewExpiry = schedule.expiresOn != null,
                            isDeferred = false,
                            deferredUntil = null,
                            deferReason = null,
                            criticality = component.criticality,
                            hasMeter = meters.isNotEmpty() || schedule.intervalHours != null,
                        )
                    }
                }
            }

            val vesselEngineMeter = vesselMetersList.firstOrNull { m ->
                val c = componentsById[m.componentId]
                c != null && c.categoryCode in setOf("engine_main", "drivetrain", "engine")
            }
            val vesselReading = vesselEngineMeter?.let { db.meterDao().latestReading(it.id) }

            if (vesselNeedsMeter > 0) {
                waitingMetersList.add(
                    VesselWaitingMeters(
                        vesselId = vessel.id,
                        vessel = vessel,
                        vesselName = vessel.name,
                        count = vesselNeedsMeter,
                        currentMeterHours = vesselReading?.value,
                    )
                )
            }

            val hasEngine = vessel.engine != "none" || components.any { it.categoryCode in setOf("engine_main", "drivetrain", "engine") }
            if (hasEngine || vesselEngineMeter != null) {
                vesselMeterInfosList.add(
                    VesselMeterInfo(
                        vesselId = vessel.id,
                        vessel = vessel,
                        vesselName = vessel.name,
                        meterId = vesselEngineMeter?.id,
                        hours = vesselReading?.value,
                    )
                )
            }
        }

        val sorted = items.sortedWith(
            compareBy<NowItem> { item ->
                when (item.state) {
                    DueCalculator.State.OVERDUE -> 0
                    DueCalculator.State.DUE_SOON -> 1
                    DueCalculator.State.DEFERRED -> 2
                    else -> 3
                }
            }.thenBy { item ->
                when (item.criticality) {
                    Criticality.HIGH -> 0
                    Criticality.MED -> 1
                    Criticality.LOW -> 2
                }
            }
        )

        val sortedUpcoming = upcomingItemsList.sortedWith(
            compareBy<NowItem> { it.result.daysRemaining ?: Long.MAX_VALUE }
                .thenBy { it.result.unitsRemaining ?: Double.MAX_VALUE }
        ).take(3)

        val primaryVessel = if (targetVesselId != null) {
            vessels.firstOrNull { it.id == targetVesselId }
        } else {
            vessels.firstOrNull()
        }

        val latestRecord = if (primaryVessel != null) {
            db.serviceRecordDao().allForVessel(primaryVessel.id).firstOrNull()
        } else {
            activeVessels.mapNotNull { v -> db.serviceRecordDao().allForVessel(v.id).firstOrNull() }
                .maxByOrNull { it.date }
        }

        val recentActivity = latestRecord?.let { rec ->
            val comp = db.componentDao().byId(rec.componentId)
            val vessel = comp?.let { db.vesselDao().byId(it.vesselId) }
            comp?.let { c ->
                RecentActivityItem(
                    record = rec,
                    component = c,
                    componentName = c.name,
                    categoryCode = c.categoryCode,
                    vessel = vessel,
                    vesselName = vessel?.name,
                )
            }
        }

        // Per boat: a check passed on one boat says nothing about another,
        // so in the fleet view "done today" means every boat has been done.
        val activeIds = vessels.map { it.id }.toSet()
        val departureDoneIds = db.checklistDao().vesselIdsDoneOn("predeparture", today)
            .filter { it in activeIds }.toSet()
        val departureDone = if (targetVesselId == null && vessels.size > 1) {
            departureDoneIds.containsAll(activeIds)
        } else {
            primaryVessel != null && primaryVessel.id in departureDoneIds
        }
        val lastDepartedId = db.checklistDao().lastRunVesselId("predeparture")
            ?.takeIf { it in activeIds }

        val departureCount = if (primaryVessel != null) {
            ChecklistsLoader.predepartureItemsFor(context, primaryVessel).size
        } else 8

        val vesselMeters = if (primaryVessel != null) db.meterDao().forVessel(primaryVessel.id) else emptyList()
        val primaryVesselComponents = if (primaryVessel != null) {
            db.componentDao().forVessel(primaryVessel.id).associateBy { it.id }
        } else emptyMap()
        val primaryMeter = vesselMeters.firstOrNull { m ->
            primaryVesselComponents[m.componentId]?.categoryCode in setOf("engine_main", "drivetrain", "engine")
        }
        val primaryMeterReading = primaryMeter?.let { db.meterDao().latestReading(it.id) }

        _uiState.value = NowUiState(
            vessels = vessels,
            needsInitialMeterCount = needsMeter,
            needsFirstRecordCount = needsFirstRecord,
            waitingMetersByVessel = waitingMetersList,
            vesselMeterInfos = vesselMeterInfosList,
            selectedVesselId = targetVesselId,
            items = sorted,
            upcomingItems = sortedUpcoming,
            recentActivity = recentActivity,
            unrecordedComponents = unrecordedList,
            overdueCount = sorted.count { it.state == DueCalculator.State.OVERDUE },
            soonCount = sorted.count { it.state == DueCalculator.State.DUE_SOON },
            deferredCount = sorted.count { it.state == DueCalculator.State.DEFERRED },
            departureCheckCount = departureCount,
            departureCheckDoneToday = departureDone,
            departureDoneVesselIds = departureDoneIds,
            lastDepartedVesselId = lastDepartedId,
            primaryMeterHours = primaryMeterReading?.value,
            primaryMeterId = primaryMeter?.id,
            isLoading = false,
        )
    }

    private fun formatUrgency(r: DueCalculator.Result, deferredUntil: LocalDate?, deferReason: String?): String {
        val res = context.resources
        if (r.state == DueCalculator.State.DEFERRED) {
            val dateStr = deferredUntil?.toString() ?: ""
            return if (!deferReason.isNullOrBlank()) {
                res.getString(R.string.deferred_until_reason, deferReason, dateStr)
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

            else -> res.getString(R.string.due_unknown)
        }
    }

    /**
     * Everything one quick-log save changed, so five seconds later it can all be
     * put back.
     *
     * A save writes up to four things and the sheet only ever showed one of them:
     * a Meter when the component had none, a MeterReading, the ServiceRecord, and
     * an overwrite of `expiresOn` that also clears any deferral. Undoing the
     * record alone would leave a boat whose liferaft certificate silently changed
     * date - a different lie from the one being undone.
     *
     * `expiryBefore` therefore captures the schedule rows AS THEY WERE, because a
     * value that has been overwritten cannot be recomputed from anything.
     */
    data class QuickLogUndo(
        val recordId: Long,
        val meterReadingId: Long?,
        /** Non-null only when this save created the meter; never an existing one. */
        val createdMeterId: Long?,
        val expiryBefore: List<ServiceSchedule>,
        val componentId: Long,
        val vesselId: Long,
        val attachmentIds: List<Long> = emptyList(),
        val attachmentId: Long? = null,
        /**
         * Photos the owner chose that could not be copied into app storage.
         * Reported, never swallowed: the record saved, the photo did not,
         * and only the screen can say so.
         */
        val failedPhotoCount: Int = 0,
    )

    fun saveQuickLog(
        componentId: Long,
        vesselId: Long,
        date: LocalDate,
        workType: WorkType,
        meterHours: Double?,
        cost: Double?,
        notes: String?,
        newExpiry: LocalDate?,
        photoUris: List<Uri> = emptyList(),
        photoUri: Uri? = null,
        scheduleId: Long? = null,
        onSaved: (QuickLogUndo) -> Unit,
    ) {
        val allUris = if (photoUris.isNotEmpty()) photoUris else listOfNotNull(photoUri)
        viewModelScope.launch(Dispatchers.IO) {
            val savedUris = allUris.mapNotNull { uri ->
                AttachmentStorage.copyToInternalStorage(context, uri, "attachments")
            }
            val failedPhotoCount = allUris.size - savedUris.size

            val undo = db.withTransaction {
                var meterReadingId: Long? = null
                var createdMeterId: Long? = null
                if (meterHours != null) {
                    val meters = db.meterDao().forComponent(componentId)
                    val primaryMeter = meters.firstOrNull() ?: run {
                        val vesselMeters = db.meterDao().forVessel(vesselId)
                        val vComponents = db.componentDao().forVessel(vesselId).associateBy { it.id }
                        val comp = vComponents[componentId]
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
                            createdMeterId = newId
                            Meter(id = newId, componentId = componentId, unit = MeterUnit.HOURS)
                        }
                    }
                    meterReadingId = db.meterDao().insertReading(
                        MeterReading(meterId = primaryMeter.id, date = date, value = meterHours)
                    )
                }

                val record = ServiceRecord(
                    componentId = componentId,
                    scheduleId = scheduleId,
                    date = date,
                    workTypes = workType.name,
                    description = if (newExpiry != null) {
                        context.getString(R.string.action_renew)
                    } else {
                        context.getString(R.string.action_mark_done)
                    },
                    cost = cost,
                    notes = notes,
                    meterReadingId = meterReadingId,
                )
                val recordId = db.serviceRecordDao().insert(record)
                db.componentDao().unarchive(componentId)

                val attachmentIds = savedUris.map { uri ->
                    db.attachmentDao().insert(
                        Attachment(
                            parentType = ParentType.SERVICE_RECORD,
                            parentId = recordId,
                            kind = AttachmentKind.PHOTO,
                            fileUri = uri.toString(),
                            takenAt = date,
                        )
                    )
                }

                val expiryBefore = mutableListOf<ServiceSchedule>()
                if (newExpiry != null) {
                    val schedules = db.scheduleDao().forComponent(componentId)
                    for (s in schedules) {
                        if (s.expiresOn != null) {
                            expiryBefore += s
                            db.scheduleDao().update(s.copy(expiresOn = newExpiry, deferredUntil = null, deferReason = null))
                        }
                    }
                }

                QuickLogUndo(
                    recordId = recordId,
                    meterReadingId = meterReadingId,
                    createdMeterId = createdMeterId,
                    expiryBefore = expiryBefore,
                    componentId = componentId,
                    vesselId = vesselId,
                    attachmentIds = attachmentIds,
                    attachmentId = attachmentIds.firstOrNull(),
                    failedPhotoCount = failedPhotoCount,
                )
            }

            MaintenanceCheckWorker.runNow(context)

            val notifManager = context.getSystemService(NotificationManager::class.java)
            notifManager?.cancel(MaintenanceNotification.notificationId(componentId))
            MaintenanceNotification.dismissSummaryIfEmpty(context, vesselId)

            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)

            withContext(Dispatchers.Main) {
                onSaved(undo)
            }
        }
    }

    /**
     * Put back everything [saveQuickLog] changed, in reverse order.
     *
     * The notification is deliberately NOT re-posted here: `runNow` recomputes
     * the state from the database it has just been handed back, and posting
     * one directly would race with it.
     */
    fun undoQuickLog(undo: QuickLogUndo) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                db.serviceRecordDao().deleteById(undo.recordId)
                val idsToDelete = if (undo.attachmentIds.isNotEmpty()) undo.attachmentIds else listOfNotNull(undo.attachmentId)
                if (idsToDelete.isNotEmpty()) {
                    val attachments = db.attachmentDao().byIds(idsToDelete)
                    attachments.forEach { att ->
                        AttachmentStorage.deleteInternalFile(context, att.fileUri)
                    }
                    idsToDelete.forEach { db.attachmentDao().deleteById(it) }
                }
                undo.meterReadingId?.let { db.meterDao().deleteReading(it) }
                // Only a meter this save created; an existing one keeps its history.
                undo.createdMeterId?.let { db.meterDao().deleteMeter(it) }
                for (before in undo.expiryBefore) {
                    db.scheduleDao().update(before)
                }
            }

            MaintenanceCheckWorker.runNow(context)
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
        }
    }

    fun deferSchedule(scheduleId: Long, until: LocalDate, reason: String?, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            db.scheduleDao().updateDeferral(scheduleId, until, reason)
            MaintenanceCheckWorker.runNow(context)
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun undeferSchedule(scheduleId: Long, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            db.scheduleDao().updateDeferral(scheduleId, null, null)
            MaintenanceCheckWorker.runNow(context)
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun markDepartureCheckDone(vesselId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.checklistDao().insertRun(
                ChecklistRun(
                    vesselId = vesselId,
                    checklistCode = "predeparture",
                    date = LocalDate.now(),
                )
            )
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
        }
    }

    fun resetDepartureCheck(vesselId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.checklistDao().deleteRunForDate(
                vesselId = vesselId,
                code = "predeparture",
                date = LocalDate.now(),
            )
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
        }
    }

    fun recordPrimaryMeterReading(
        vesselId: Long? = null,
        date: LocalDate,
        hours: Double,
        onSaved: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val vessels = db.vesselDao().allActive()
            val targetVesselId = vesselId ?: _selectedVesselId.value
            val primaryVessel = if (targetVesselId != null) {
                vessels.firstOrNull { it.id == targetVesselId }
            } else {
                vessels.firstOrNull()
            } ?: return@launch

            val vesselMeters = db.meterDao().forVessel(primaryVessel.id)
            val components = db.componentDao().forVessel(primaryVessel.id)
            val componentsById = components.associateBy { it.id }

            val targetMeter = vesselMeters.firstOrNull { m ->
                val c = componentsById[m.componentId]
                c != null && c.categoryCode in setOf("engine_main", "drivetrain", "engine")
            }

            val meterId = targetMeter?.id ?: run {
                val primaryComp = components.firstOrNull { it.catalogCode == "engine_oil" }
                    ?: components.firstOrNull { it.catalogCode?.startsWith("engine_oil") == true }
                    ?: components.firstOrNull { it.categoryCode in setOf("engine_main", "drivetrain", "engine") }

                if (primaryComp != null) {
                    db.meterDao().insertMeter(
                        Meter(componentId = primaryComp.id, unit = MeterUnit.HOURS, label = "Engine")
                    )
                } else {
                    null
                }
            }

            if (meterId != null) {
                db.meterDao().insertReading(
                    MeterReading(meterId = meterId, date = date, value = hours)
                )
                MaintenanceCheckWorker.runNow(context)
                computeDue(vessels, _selectedVesselId.value)
            }

            withContext(Dispatchers.Main) {
                onSaved?.invoke()
            }
        }
    }

    fun recordMeterReading(
        meterId: Long,
        date: LocalDate,
        hours: Double,
        onSaved: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            db.meterDao().insertReading(
                MeterReading(meterId = meterId, date = date, value = hours)
            )
            MaintenanceCheckWorker.runNow(context)
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
            withContext(Dispatchers.Main) {
                onSaved?.invoke()
            }
        }
    }

    fun recordComponentMeterReading(
        componentId: Long,
        date: LocalDate,
        hours: Double,
        onSaved: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val meters = db.meterDao().forComponent(componentId)
            val meterId = meters.firstOrNull()?.id ?: db.meterDao().insertMeter(
                Meter(componentId = componentId, unit = MeterUnit.HOURS)
            )
            db.meterDao().insertReading(
                MeterReading(meterId = meterId, date = date, value = hours)
            )
            MaintenanceCheckWorker.runNow(context)
            val vessels = db.vesselDao().allActive()
            computeDue(vessels, _selectedVesselId.value)
            withContext(Dispatchers.Main) {
                onSaved?.invoke()
            }
        }
    }

    companion object {
        fun provideFactory(db: AppDatabase, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NowViewModel(db, context.applicationContext) as T
                }
            }
    }
}
