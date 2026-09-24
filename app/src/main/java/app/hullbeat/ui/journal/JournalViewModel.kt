package app.hullbeat.ui.journal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.AttachmentKind
import app.hullbeat.data.storage.AttachmentStorage
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.DatePrecision
import app.hullbeat.data.db.Meter
import app.hullbeat.data.db.MeterReading
import app.hullbeat.data.db.MeterUnit
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.ReadingSource
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.displayName
import app.hullbeat.data.preferences.AppPreferences
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class JournalEntryItem(
    val record: ServiceRecord,
    val component: Component,
    val componentDisplayName: String = "",
    val categoryDisplayName: String = "",
    val meterReading: MeterReading?,
    val attachments: List<Attachment>,
    val daysSincePrev: Long? = null,
    val hoursSincePrev: Double? = null,
    val isApproximateGap: Boolean = false,
    val schedule: ServiceSchedule? = null,
    val vessel: Vessel? = null,
    val vesselName: String? = null,
)

data class JournalUiState(
    val vessels: List<Vessel> = emptyList(),
    val selectedVesselId: Long? = null,
    val selectedCategoryCode: String? = null,
    val categoriesWithRecords: List<String> = emptyList(),
    val entries: List<JournalEntryItem> = emptyList(),
    val totalCount: Int = 0,
    val totalCost: Double = 0.0,
    val dateSpan: String = "",
    val isLoading: Boolean = true,
)

/**
 * Manages the Ship's Log (Журнал) feed and actions.
 */
class JournalViewModel(
    private val db: AppDatabase,
    private val context: Context,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    /**
     * The vessel this screen shows.
     *
     * The app's selection is owned by `NowViewModel`, which is the only
     * writer of the preference; this follows it. `SavedStateHandle` keeps
     * the answer across process death, so a phone that kills the app while
     * the owner is in a marina does not come back on a different boat.
     */
    private val _selectedVesselId = MutableStateFlow(
        savedState.get<Long>(KEY_VESSEL_ID)
            ?: AppPreferences.getSelectedVesselId(context)
    )
    private val _selectedCategoryCode = MutableStateFlow(
        savedState.get<String>(KEY_CATEGORY)
    )

    /**
     * A selected vessel that no longer exists is not a selection.
     *
     * Validated against the LIVE vessel list rather than a snapshot, so
     * archiving the boat you are looking at falls back to the whole fleet
     * instead of showing an empty feed.
     */
    private val validatedVesselId: Flow<Long?> =
        combine(db.vesselDao().observeAll(), _selectedVesselId) { vessels, selected ->
            selected?.takeIf { id -> vessels.any { !it.archived && it.id == id } }
        }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recordsFlow: Flow<List<ServiceRecord>> =
        validatedVesselId.flatMapLatest { vesselId ->
            if (vesselId != null && vesselId > 0L) {
                db.serviceRecordDao().observeForVessel(vesselId)
            } else {
                // null is "the whole fleet", a real state - not "nothing
                // chosen yet". That distinction used to need a second
                // boolean beside the id.
                db.serviceRecordDao().observeAll()
            }
        }

    val uiState: StateFlow<JournalUiState> = combine(
        db.vesselDao().observeAll(),
        recordsFlow,
        _selectedCategoryCode,
        validatedVesselId,
    ) { vessels, records, category, vesselId ->
        buildState(vessels.filter { !it.archived }, records, category, vesselId)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            JournalUiState(isLoading = true),
        )

    /**
     * Follow the app's selection. Driven by
     * `LaunchedEffect(nowState.selectedVesselId)`, never by this screen's
     * own chips: they call up, so the preference has exactly one writer.
     */
    fun selectVessel(vesselId: Long?) {
        savedState[KEY_VESSEL_ID] = vesselId
        _selectedVesselId.value = vesselId
    }

    fun selectCategory(categoryCode: String?) {
        savedState[KEY_CATEGORY] = categoryCode
        _selectedCategoryCode.value = categoryCode
    }

    /**
     * Turn one emission into one state. No cancellation, no nested
     * `collect`, no job to forget: `stateIn` owns the subscription and
     * `flatMapLatest` throws away the previous query when the vessel
     * changes.
     *
     * Still five reads, deliberately - they are BATCHED by id, not one per
     * row, so this is not an N+1. Measured before touching it: six queries
     * per emission, and twenty-seven places in the journal package read the
     * nested `record`/`component` objects that a flat projection would
     * remove.
     */
    private suspend fun buildState(
        activeVessels: List<Vessel>,
        records: List<ServiceRecord>,
        selectedCategoryCode: String?,
        currentVesselId: Long?,
    ): JournalUiState {
        return run {
            run {
                run {
                    val compIds = records.map { it.componentId }.distinct()
                    val readingIds = records.mapNotNull { it.meterReadingId }.distinct()
                    val recordIds = records.map { it.id }
                    val scheduleIds = records.mapNotNull { it.scheduleId }.distinct()

                    val componentMap = if (compIds.isNotEmpty()) {
                        db.componentDao().byIds(compIds).associateBy { it.id }
                    } else emptyMap()

                    val meterReadingMap = if (readingIds.isNotEmpty()) {
                        db.meterDao().readingsByIds(readingIds).associateBy { it.id }
                    } else emptyMap()

                    val attachmentsMap = if (recordIds.isNotEmpty()) {
                        db.attachmentDao().forParents(ParentType.SERVICE_RECORD, recordIds)
                            .groupBy { it.parentId }
                    } else emptyMap()

                    val scheduleMap = if (scheduleIds.isNotEmpty()) {
                        db.scheduleDao().byIds(scheduleIds).associateBy { it.id }
                    } else emptyMap()

                    val vesselsMap = activeVessels.associateBy { it.id }

                    // Group by componentId to compute gaps
                    val recordsByComponent = records.groupBy { it.componentId }

                    val allItems = records.mapNotNull { r ->
                        val comp = componentMap[r.componentId] ?: return@mapNotNull null
                        val reading = r.meterReadingId?.let { meterReadingMap[it] }
                        val atts = attachmentsMap[r.id] ?: emptyList()
                        val sched = r.scheduleId?.let { scheduleMap[it] }
                        val vessel = vesselsMap[comp.vesselId]

                        // Find chronologically previous record for this component
                        val compHistory = recordsByComponent[r.componentId] ?: emptyList()
                        val currentIndex = compHistory.indexOf(r)
                        val prevRecord = if (currentIndex >= 0 && currentIndex < compHistory.size - 1) {
                            compHistory[currentIndex + 1]
                        } else null

                        var daysSince: Long? = null
                        var hoursSince: Double? = null
                        var isRough = false

                        if (prevRecord != null) {
                            val days = ChronoUnit.DAYS.between(prevRecord.date, r.date)
                            if (days > 0) {
                                daysSince = days
                                if (r.precision != DatePrecision.DAY || prevRecord.precision != DatePrecision.DAY) {
                                    isRough = true
                                }
                                val prevReading = prevRecord.meterReadingId?.let { meterReadingMap[it] }
                                val currentHours = reading?.value
                                val prevHours = prevReading?.value
                                if (currentHours != null && prevHours != null && currentHours > prevHours) {
                                    hoursSince = currentHours - prevHours
                                }
                            }
                        }

                        JournalEntryItem(
                            record = r,
                            component = comp,
                            componentDisplayName = comp.name,
                            categoryDisplayName = comp.categoryCode,
                            meterReading = reading,
                            attachments = atts,
                            daysSincePrev = daysSince,
                            hoursSincePrev = hoursSince,
                            isApproximateGap = isRough,
                            schedule = sched,
                            vessel = vessel,
                            vesselName = vessel?.name,
                        )
                    }

                    // Collect distinct category codes that actually have records
                    val categoriesWithRecords = allItems
                        .map { it.component.categoryCode }
                        .distinct()
                        .sorted()

                    val filteredItems = if (selectedCategoryCode.isNullOrBlank()) {
                        allItems
                    } else {
                        allItems.filter { it.component.categoryCode == selectedCategoryCode }
                    }

                    val totalSpent = filteredItems.mapNotNull { it.record.cost }.sum()

                    val span = if (filteredItems.isNotEmpty()) {
                        val oldestYear = filteredItems.last().record.date.year
                        val newestYear = filteredItems.first().record.date.year
                        if (oldestYear == newestYear) "$newestYear" else "$oldestYear–$newestYear"
                    } else ""

                    JournalUiState(
                        vessels = activeVessels,
                        selectedVesselId = currentVesselId,
                        selectedCategoryCode = selectedCategoryCode,
                        categoriesWithRecords = categoriesWithRecords,
                        entries = filteredItems,
                        totalCount = filteredItems.size,
                        totalCost = totalSpent,
                        dateSpan = span,
                        isLoading = false,
                    )
                }
            }
        }
    }

    suspend fun getMeterHours(readingId: Long?): Double? {
        if (readingId == null) return null
        return withContext(Dispatchers.IO) {
            db.meterDao().readingById(readingId)?.value
        }
    }

    fun deleteRecord(recordId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.withTransaction {
                    val record = db.serviceRecordDao().byId(recordId)
                    if (record?.meterReadingId != null) {
                        db.meterDao().deleteReading(record.meterReadingId)
                    }
                    val attachments = db.attachmentDao().forParent(ParentType.SERVICE_RECORD, recordId)
                    attachments.forEach { att ->
                        AttachmentStorage.deleteInternalFile(context, att.fileUri)
                    }
                    db.attachmentDao().deleteForParent(ParentType.SERVICE_RECORD, recordId)
                    db.serviceRecordDao().deleteById(recordId)
                }
            }
            // No reload: the record Flow re-emits on its own when the
            // table changes, which is the point of the pipeline.
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun updateRecord(
        record: ServiceRecord,
        meterHours: Double?,
        newPhotoUris: List<Uri> = emptyList(),
        removedPhotoIds: List<Long> = emptyList(),
        photoUri: Uri? = null,
        removePhoto: Boolean = false,
        existingPhotoId: Long? = null,
        onDone: () -> Unit = {},
    ) {
        val allNewUris = if (newPhotoUris.isNotEmpty()) newPhotoUris else listOfNotNull(photoUri)
        val allRemovedIds = if (removedPhotoIds.isNotEmpty()) removedPhotoIds else listOfNotNull(if (removePhoto) existingPhotoId else null)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val savedUris = allNewUris.mapNotNull { uri ->
                    AttachmentStorage.copyToInternalStorage(context, uri, "attachments")
                }

                db.withTransaction {
                    var readingId = record.meterReadingId
                    if (meterHours != null) {
                        if (readingId != null) {
                            val current = db.meterDao().readingById(readingId)
                            if (current != null) {
                                db.meterDao().updateReading(
                                    current.copy(
                                        value = meterHours,
                                        date = record.date,
                                    )
                                )
                            }
                        } else {
                            val compMeters = db.meterDao().forComponent(record.componentId)
                            val targetMeter = compMeters.firstOrNull() ?: run {
                                val newId = db.meterDao().insertMeter(Meter(componentId = record.componentId, unit = MeterUnit.HOURS))
                                Meter(id = newId, componentId = record.componentId, unit = MeterUnit.HOURS)
                            }
                            readingId = db.meterDao().insertReading(
                                MeterReading(
                                    meterId = targetMeter.id,
                                    date = record.date,
                                    value = meterHours,
                                    source = ReadingSource.MANUAL,
                                )
                            )
                        }
                    } else if (record.meterReadingId != null) {
                        db.meterDao().deleteReading(record.meterReadingId)
                        readingId = null
                    }

                    val toSave = record.copy(meterReadingId = readingId)
                    db.serviceRecordDao().update(toSave)

                    allRemovedIds.forEach { photoId ->
                        val att = db.attachmentDao().byId(photoId)
                        if (att != null) {
                            AttachmentStorage.deleteInternalFile(context, att.fileUri)
                        }
                        db.attachmentDao().deleteById(photoId)
                    }

                    savedUris.forEach { uri ->
                        db.attachmentDao().insert(
                            Attachment(
                                parentType = ParentType.SERVICE_RECORD,
                                parentId = toSave.id,
                                kind = AttachmentKind.PHOTO,
                                fileUri = uri.toString(),
                                takenAt = toSave.date,
                            )
                        )
                    }
                }
            }
            // No reload: the record Flow re-emits on its own when the
            // table changes, which is the point of the pipeline.
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    companion object {
        private const val KEY_VESSEL_ID = "journal.vesselId"
        private const val KEY_CATEGORY = "journal.categoryCode"

        /**
         * `AbstractSavedStateViewModelFactory` is what supplies the handle;
         * a plain Factory cannot, and the filters would silently stop
         * surviving a process death while still compiling.
         */
        fun provideFactory(db: AppDatabase, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    val handle = extras.createSavedStateHandle()
                    return JournalViewModel(db, context.applicationContext, handle) as T
                }
            }
    }
}
