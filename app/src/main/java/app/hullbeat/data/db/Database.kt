package app.hullbeat.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ------------------------------------------------------------- converters

class Converters {
    /** ISO-8601 text: sorts correctly in SQLite and stays readable to a human. */
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun precisionToString(v: DatePrecision): String = v.name
    @TypeConverter fun stringToPrecision(v: String): DatePrecision = DatePrecision.valueOf(v)

    @TypeConverter fun meterUnitToString(v: MeterUnit): String = v.name
    @TypeConverter fun stringToMeterUnit(v: String): MeterUnit = MeterUnit.valueOf(v)

    @TypeConverter fun sourceToString(v: ReadingSource): String = v.name
    @TypeConverter fun stringToSource(v: String): ReadingSource = ReadingSource.valueOf(v)

    @TypeConverter fun critToString(v: Criticality): String = v.name
    @TypeConverter fun stringToCrit(v: String): Criticality = Criticality.valueOf(v)

    @TypeConverter fun workTypeToString(v: WorkType): String = v.name
    @TypeConverter fun stringToWorkType(v: String): WorkType = WorkType.valueOf(v)

    @TypeConverter fun kindToString(v: AttachmentKind): String = v.name
    @TypeConverter fun stringToKind(v: String): AttachmentKind = AttachmentKind.valueOf(v)

    @TypeConverter fun parentToString(v: ParentType): String = v.name
    @TypeConverter fun stringToParent(v: String): ParentType = ParentType.valueOf(v)
}

// -------------------------------------------------------------------- dao

@Dao
interface VesselDao {
    @Query("SELECT * FROM vessel WHERE archived = 0 ORDER BY name")
    fun observeAll(): Flow<List<Vessel>>

    @Query("SELECT * FROM vessel WHERE id = :id")
    suspend fun byId(id: Long): Vessel?

    /** One-shot, for the daily worker: a Flow would be a subscription to leak. */
    @Query("SELECT * FROM vessel WHERE archived = 0 ORDER BY name")
    suspend fun allActive(): List<Vessel>

    @Insert suspend fun insert(vessel: Vessel): Long
    @Update suspend fun update(vessel: Vessel)
}

@Dao
interface ComponentDao {
    @Query("SELECT * FROM component WHERE vesselId = :vesselId AND archived = 0 ORDER BY sortOrder, name")
    fun observeForVessel(vesselId: Long): Flow<List<Component>>

    @Query("SELECT * FROM component WHERE vesselId = :vesselId AND categoryCode = :category AND archived = 0 ORDER BY sortOrder, name")
    fun observeInCategory(vesselId: Long, category: String): Flow<List<Component>>

    @Query("SELECT * FROM component WHERE id = :id")
    suspend fun byId(id: Long): Component?

    /** One-shot counterpart of observeForVessel, for the daily worker. */
    @Query(
        "SELECT * FROM component WHERE vesselId = :vesselId AND archived = 0 "
        + "ORDER BY sortOrder, name"
    )
    suspend fun forVessel(vesselId: Long): List<Component>

    @Insert suspend fun insert(component: Component): Long
    @Insert suspend fun insertAll(components: List<Component>): List<Long>
    @Update suspend fun update(component: Component)

    /**
     * Every catalog code this vessel has ever been seeded with, ARCHIVED ONES
     * INCLUDED. The only correct source for `CatalogSeeder.seedFor`.
     *
     * Deliberately without `archived = 0`: the catalog grows, so seeding runs
     * again on every update, and a code missing from this set is treated as
     * new and inserted. Feeding it from `forVessel` - which does filter
     * archived - would resurrect the gas system an owner removed, after every
     * single release, with no way to make it stop.
     *
     * Custom nodes have no catalogCode and are excluded by the NULL test:
     * seeding neither knows nor needs to know about them.
     */
    @Query(
        "SELECT catalogCode FROM component "
        + "WHERE vesselId = :vesselId AND catalogCode IS NOT NULL"
    )
    suspend fun allCatalogCodesForVessel(vesselId: Long): List<String>

    /** Components are archived, never deleted: history must survive. */
    @Query("UPDATE component SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    /** Unarchive a component to show it on the boat. */
    @Query("UPDATE component SET archived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    @Query("UPDATE component SET archived = 1 WHERE id IN (:ids)")
    suspend fun archiveMany(ids: List<Long>)

    @Query("SELECT * FROM component WHERE vesselId = :vesselId ORDER BY categoryCode, sortOrder, name")
    suspend fun allForVessel(vesselId: Long): List<Component>

    @Query("SELECT * FROM component WHERE vesselId = :vesselId ORDER BY categoryCode, sortOrder, name")
    fun observeAllForVessel(vesselId: Long): Flow<List<Component>>

    @Query("SELECT DISTINCT componentId FROM service_record")
    suspend fun componentIdsWithService(): List<Long>

    @Query("SELECT * FROM component WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Component>
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM service_schedule WHERE componentId = :componentId AND enabled = 1")
    suspend fun forComponent(componentId: Long): List<ServiceSchedule>

    @Query("SELECT * FROM service_schedule WHERE componentId = :componentId AND enabled = 1")
    fun observeForComponent(componentId: Long): Flow<List<ServiceSchedule>>

    @Query(
        """
        SELECT s.* FROM service_schedule s
        JOIN component c ON c.id = s.componentId
        WHERE c.vesselId = :vesselId AND s.enabled = 1
        """
    )
    fun observeForVessel(vesselId: Long): Flow<List<ServiceSchedule>>

    @Query(
        """
        SELECT s.* FROM service_schedule s
        JOIN component c ON c.id = s.componentId
        WHERE c.vesselId = :vesselId AND s.enabled = 1
        """
    )
    suspend fun forVessel(vesselId: Long): List<ServiceSchedule>

    @Query("SELECT * FROM service_schedule WHERE id = :id")
    suspend fun byId(id: Long): ServiceSchedule?

    @Query("SELECT * FROM service_schedule WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<ServiceSchedule>

    @Query("SELECT * FROM service_schedule WHERE enabled = 1")
    suspend fun allEnabled(): List<ServiceSchedule>

    @Query("UPDATE service_schedule SET deferredUntil = :until, deferReason = :reason WHERE id = :scheduleId")
    suspend fun updateDeferral(scheduleId: Long, until: LocalDate?, reason: String?)

    @Insert suspend fun insert(schedule: ServiceSchedule): Long
    @Insert suspend fun insertAll(schedules: List<ServiceSchedule>)
    @Update suspend fun update(schedule: ServiceSchedule)
}

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_record WHERE componentId = :componentId ORDER BY date DESC")
    fun observeForComponent(componentId: Long): Flow<List<ServiceRecord>>

    @Query(
        """
        SELECT r.* FROM service_record r
        JOIN component c ON c.id = r.componentId
        WHERE c.vesselId = :vesselId
        ORDER BY r.date DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeJournal(vesselId: Long, limit: Int, offset: Int): Flow<List<ServiceRecord>>

    @Query(
        """
        SELECT r.* FROM service_record r
        JOIN component c ON c.id = r.componentId
        WHERE c.vesselId = :vesselId
        ORDER BY r.date DESC
        """
    )
    fun observeForVessel(vesselId: Long): Flow<List<ServiceRecord>>

    @Query(
        """
        SELECT r.* FROM service_record r
        JOIN component c ON c.id = r.componentId
        WHERE c.vesselId = :vesselId
        ORDER BY r.date DESC
        """
    )
    suspend fun allForVessel(vesselId: Long): List<ServiceRecord>

    @Query("SELECT * FROM service_record ORDER BY date DESC")
    fun observeAll(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_record WHERE componentId = :componentId ORDER BY date DESC, id DESC")
    suspend fun forComponent(componentId: Long): List<ServiceRecord>

    @Query("SELECT * FROM service_record WHERE componentId = :componentId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun lastForComponent(componentId: Long): ServiceRecord?

    @Query("SELECT * FROM service_record WHERE scheduleId = :scheduleId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun lastForSchedule(scheduleId: Long): ServiceRecord?

    @Query("SELECT * FROM service_record WHERE scheduleId IN (:scheduleIds) ORDER BY date DESC, id DESC")
    suspend fun forSchedules(scheduleIds: List<Long>): List<ServiceRecord>

    @Query("SELECT * FROM service_record WHERE componentId = :componentId AND date = :date AND description = :description LIMIT 1")
    suspend fun findMatchingRecord(componentId: Long, date: LocalDate, description: String): ServiceRecord?

    @Query("SELECT * FROM service_record WHERE scheduleId = :scheduleId AND date = :date LIMIT 1")
    suspend fun findRecordForScheduleOn(scheduleId: Long, date: LocalDate): ServiceRecord?

    @Query("SELECT * FROM service_record WHERE id = :id")
    suspend fun byId(id: Long): ServiceRecord?

    @Insert suspend fun insert(record: ServiceRecord): Long
    @Update suspend fun update(record: ServiceRecord)

    /** Undo for a whole import, using the provenance stamped on every row. */
    @Query("DELETE FROM service_record WHERE importSessionId = :sessionId")
    suspend fun deleteByImportSession(sessionId: Long)

    /**
     * Undo for one record, five seconds after it was written.
     *
     * ui-spec.md §4 requires the snackbar's "Скасувати" to actually undo, and
     * the spec already records the day it did not - it just hid the snackbar,
     * which made a mistaken tap irreversible. On a maintenance log a phantom
     * record is worse than a missing one: it says the impeller was changed
     * when it was not.
     */
    @Query("DELETE FROM service_record WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MeterDao {
    @Query("SELECT * FROM meter WHERE componentId = :componentId")
    suspend fun forComponent(componentId: Long): List<Meter>

    @Query(
        """
        SELECT m.* FROM meter m
        JOIN component c ON c.id = m.componentId
        WHERE c.vesselId = :vesselId
        ORDER BY m.id
        """
    )
    suspend fun forVessel(vesselId: Long): List<Meter>

    @Query("SELECT * FROM meter_reading WHERE meterId = :meterId ORDER BY date")
    suspend fun readings(meterId: Long): List<MeterReading>

    @Query("SELECT * FROM meter_reading WHERE meterId = :meterId ORDER BY date DESC LIMIT 1")
    suspend fun latestReading(meterId: Long): MeterReading?

    /**
     * The reading a service record points at, which is the BASE an
     * hours-driven interval counts from. Without it DueCalculator.usageDue
     * returns null and the item silently falls back to its calendar interval -
     * or to UNKNOWN if it has none. There was no way to ask for it.
     */
    @Query("SELECT * FROM meter_reading WHERE id = :id")
    suspend fun readingById(id: Long): MeterReading?

    @Query("SELECT * FROM meter_reading WHERE id IN (:ids)")
    suspend fun readingsByIds(ids: List<Long>): List<MeterReading>

    @Query(
        """
        SELECT r.* FROM meter_reading r
        JOIN meter m ON m.id = r.meterId
        JOIN component c ON c.id = m.componentId
        WHERE c.vesselId = :vesselId
        ORDER BY r.date
        """
    )
    suspend fun readingsForVessel(vesselId: Long): List<MeterReading>

    @Insert suspend fun insertMeter(meter: Meter): Long
    /**
     * Whoever calls this must also call
     * `MaintenanceCheckWorker.runNow(context)`: ui-spec.md §7 promises a
     * forecast that moves the moment new hours are entered, and the daily pass
     * would not notice until 09:00 tomorrow - a forecast about yesterday.
     */
    @Insert suspend fun insertReading(reading: MeterReading): Long
    @Update suspend fun updateReading(reading: MeterReading)

    /** Undo, for a reading the quick-log sheet wrote by mistake. */
    @Query("DELETE FROM meter_reading WHERE id = :id")
    suspend fun deleteReading(id: Long)

    /**
     * Only ever used to undo a meter the quick-log sheet CREATED in the same
     * save. An existing meter with history behind it is never removed by an
     * undo - that would take its readings with it.
     */
    @Query("DELETE FROM meter WHERE id = :id")
    suspend fun deleteMeter(id: Long)
}

@Dao
interface InventoryDao {
    @Query(
        """
        SELECT i.* FROM inventory_item i
        WHERE i.vesselId = :vesselId AND i.locationId = :locationId
        """
    )
    fun observeInLocation(vesselId: Long, locationId: Long): Flow<List<InventoryItem>>

    @Query("SELECT * FROM location WHERE qrCode = :code LIMIT 1")
    suspend fun locationByQr(code: String): StorageLocation?

    // A `belowMinimum()` query used to sit here, claiming to feed the shopping
    // list with everything "at or below its minimum". Nothing called it - the
    // list is filtered in StoreViewModel - and it encoded the rule that made a
    // full locker read as low. Removed rather than fixed: two definitions of
    // "low", one of them dead, is how the two drift apart. See
    // StoreItemUiModel.isLowStock for the live one.

    @Insert suspend fun insertPart(part: Part): Long
    @Insert suspend fun insertItem(item: InventoryItem): Long
    @Insert suspend fun insertLocation(location: StorageLocation): Long

    /**
     * What is aboard for one node, in one query - the question an owner asks
     * with the engine already open.
     *
     * The first cut of this returned `List<InventoryItem>`, which carries only
     * partId, quantity and locationId, so the node page would have issued a
     * query per row just to learn the names. This projection joins them.
     *
     * The joins are LEFT for a reason: the row that matters most is a part
     * this node NEEDS and the owner does not have. An inner join would hide
     * exactly that - the Jabsco impeller linked to the pump with no stock
     * behind it - and the screen would say nothing where it should say zero.
     */
    @Query(
        """
        SELECT
            cp.partId          AS partId,
            i.id               AS itemId,
            p.name             AS name,
            p.partNumber       AS partNumber,
            p.manufacturer     AS manufacturer,
            i.quantity         AS quantity,
            i.minQuantity      AS minQuantity,
            l.name             AS locationName,
            cp.quantityPerService AS quantityPerService
        FROM component_part cp
        JOIN part p ON p.id = cp.partId
        LEFT JOIN inventory_item i ON i.partId = p.id AND i.vesselId = :vesselId
        LEFT JOIN location l ON l.id = i.locationId
        WHERE cp.componentId = :componentId
        ORDER BY p.name
        """
    )
    fun observeSparesForComponent(vesselId: Long, componentId: Long): Flow<List<ComponentSpare>>

    /** The other direction: one zinc part number fits hull AND shaft anodes. */
    @Query("SELECT componentId FROM component_part WHERE partId = :partId")
    suspend fun componentsForPart(partId: Long): List<Long>

    /**
     * REPLACE, not the default abort: the primary key is (componentId, partId),
     * so re-linking an existing pair is how `quantityPerService` gets
     * corrected - four anodes, not one - and that must not throw.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkPartToComponent(link: ComponentPart)

    /**
     * How much of each part one service of this node consumes, so the shopping
     * list can ask for four anodes and one impeller instead of one of each.
     */
    @Query("SELECT * FROM component_part WHERE componentId = :componentId")
    suspend fun partsForComponent(componentId: Long): List<ComponentPart>

    @Query(
        """
        SELECT
            i.id               AS id,
            i.vesselId         AS vesselId,
            i.partId           AS partId,
            p.name             AS partName,
            p.partNumber       AS partNumber,
            p.manufacturer     AS manufacturer,
            p.note             AS partNote,
            i.locationId       AS locationId,
            l.name             AS locationName,
            i.quantity         AS quantity,
            i.minQuantity      AS minQuantity
        FROM inventory_item i
        JOIN part p ON p.id = i.partId
        LEFT JOIN location l ON l.id = i.locationId
        WHERE i.vesselId = :vesselId
        ORDER BY p.name ASC
        """
    )
    fun observeAllForVessel(vesselId: Long): Flow<List<InventoryItemDetail>>

    @Query(
        """
        SELECT
            i.id               AS id,
            i.vesselId         AS vesselId,
            i.partId           AS partId,
            p.name             AS partName,
            p.partNumber       AS partNumber,
            p.manufacturer     AS manufacturer,
            p.note             AS partNote,
            i.locationId       AS locationId,
            l.name             AS locationName,
            i.quantity         AS quantity,
            i.minQuantity      AS minQuantity
        FROM inventory_item i
        JOIN part p ON p.id = i.partId
        LEFT JOIN location l ON l.id = i.locationId
        WHERE i.vesselId = :vesselId
        ORDER BY p.name ASC
        """
    )
    suspend fun allForVessel(vesselId: Long): List<InventoryItemDetail>

    @Query("SELECT * FROM location WHERE vesselId = :vesselId ORDER BY name ASC")
    fun observeLocationsForVessel(vesselId: Long): Flow<List<StorageLocation>>

    @Query("SELECT * FROM location WHERE vesselId = :vesselId ORDER BY name ASC")
    suspend fun allLocationsForVessel(vesselId: Long): List<StorageLocation>

    @Query(
        """
        SELECT
            cp.partId            AS partId,
            c.id                 AS componentId,
            c.name               AS componentName,
            c.customName         AS componentCustomName
        FROM component_part cp
        JOIN component c ON c.id = cp.componentId
        WHERE c.vesselId = :vesselId
        ORDER BY c.name ASC
        """
    )
    fun observePartComponentLinks(vesselId: Long): Flow<List<ComponentPartLink>>

    /** Applied to the stored value in one statement, so rapid taps cannot lose one. */
    @Query("UPDATE inventory_item SET quantity = MAX(0, quantity + :delta) WHERE id = :itemId")
    suspend fun adjustQuantity(itemId: Long, delta: Double)

    @Query("DELETE FROM inventory_item WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM location WHERE id = :locationId")
    suspend fun deleteLocation(locationId: Long)

    @Query("DELETE FROM part WHERE id = :partId")
    suspend fun deletePart(partId: Long)

    @Update suspend fun updatePart(part: Part)
    @Update suspend fun updateItem(item: InventoryItem)
    @Update suspend fun updateLocation(location: StorageLocation)

    @Query("DELETE FROM component_part WHERE partId = :partId")
    suspend fun unlinkPartFromAllComponents(partId: Long)

    @Query("DELETE FROM component_part WHERE componentId = :componentId AND partId = :partId")
    suspend fun unlinkPartFromComponent(componentId: Long, partId: Long)

    @Query("SELECT * FROM part WHERE id = :id")
    suspend fun partById(id: Long): Part?

    @Query("SELECT * FROM inventory_item WHERE id = :id")
    suspend fun itemById(id: Long): InventoryItem?

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun locationById(id: Long): StorageLocation?

    @Query("SELECT COUNT(*) FROM inventory_item WHERE partId = :partId")
    suspend fun countItemsForPart(partId: Long): Int
}

data class InventoryItemDetail(
    val id: Long,
    val vesselId: Long,
    val partId: Long,
    val partName: String,
    val partNumber: String?,
    val manufacturer: String?,
    val partNote: String?,
    val locationId: Long?,
    val locationName: String?,
    val quantity: Double,
    val minQuantity: Double?,
)

data class ComponentPartLink(
    val partId: Long,
    val componentId: Long,
    val componentName: String,
    val componentCustomName: String?,
)

/**
 * Query projection, not an entity: it exists to answer the node page in one
 * round trip and has no table of its own.
 *
 * Everything from `inventory_item` and `location` is nullable because the
 * joins are LEFT. A null `quantity` means "this part belongs to this node and
 * there is none aboard", which is a different statement from `quantity = 0`
 * ("there is a slot for it and the slot is empty") - and the screen should be
 * able to tell them apart.
 *
 * `itemId` is here because a part can sit in TWO lockers on the same vessel -
 * one impeller in the cockpit locker, a spare at the nav table - and without
 * it those rows would be indistinguishable.
 */
data class ComponentSpare(
    val partId: Long,
    val itemId: Long?,
    val name: String,
    val partNumber: String?,
    val manufacturer: String?,
    val quantity: Double?,
    val minQuantity: Double?,
    val locationName: String?,
    val quantityPerService: Double,
)

@Dao
interface SearchDao {
    /**
     * One box over everything - the direct answer to the complaint levelled at
     * the best-rated competitor: "fails on fundamental search functionality".
     *
     * Room has no @Fts5, so there is no bm25(). Ranking happens in Kotlin:
     * title hits outrank body hits, ties break on recency.
     *
     * WARNING: `SELECT rowid, *`, not `SELECT *`. On an FTS table the star does
     * NOT include rowid - verified against a real SQLite engine, which
     * returns exactly [vesselId, ownerType, ownerId, title, body]. The entity
     * declares `rowId` non-null, so Room rejected the mapping at build time.
     * The SQL was always valid, which is why check_room_sql.py - it only
     * PREPARES each query - passed it for months.
     */
    @Query(
        """
        SELECT rowid, * FROM search_index
        WHERE vesselId = :vesselId AND search_index MATCH :query
        LIMIT :limit
        """
    )
    suspend fun search(vesselId: Long, query: String, limit: Int = 100): List<SearchIndex>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun index(entry: SearchIndex)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun indexAll(entries: List<SearchIndex>)

    @Query("DELETE FROM search_index WHERE vesselId = :vesselId")
    suspend fun clearForVessel(vesselId: Long)

    @Query("DELETE FROM search_index WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun removeFor(ownerType: String, ownerId: Long)

    /**
     * Room does not generate sync triggers for FTS tables, so the index is
     * rewritten in the same transaction as the row it describes. Doing it any
     * other way is how search silently goes stale.
     */
    @Transaction
    suspend fun reindex(entry: SearchIndex) {
        removeFor(entry.ownerType, entry.ownerId)
        index(entry)
    }
}

@Dao
interface ImportDao {
    @Insert suspend fun insertSession(session: ImportSession): Long
    @Insert suspend fun insertRows(rows: List<ImportRow>)

    @Query("SELECT * FROM import_row WHERE sessionId = :sessionId ORDER BY rowIndex")
    suspend fun rows(sessionId: Long): List<ImportRow>

    @Query("SELECT * FROM import_session ORDER BY createdAt DESC")
    suspend fun sessions(): List<ImportSession>

    @Query("SELECT * FROM import_session ORDER BY createdAt DESC")
    fun observeSessions(): Flow<List<ImportSession>>

    @Query("SELECT * FROM import_session WHERE id = :id")
    suspend fun sessionById(id: Long): ImportSession?

    @Query("DELETE FROM import_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM import_row WHERE sessionId = :sessionId")
    suspend fun deleteRowsForSession(sessionId: Long)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachment WHERE parentType = :type AND parentId = :id ORDER BY takenAt DESC, createdAt DESC")
    fun observeFor(type: ParentType, id: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachment WHERE parentType = :type AND parentId = :id ORDER BY takenAt DESC, createdAt DESC")
    suspend fun forParent(type: ParentType, id: Long): List<Attachment>

    @Query("SELECT * FROM attachment WHERE parentType = :type AND parentId IN (:ids) ORDER BY takenAt DESC, createdAt DESC")
    suspend fun forParents(type: ParentType, ids: List<Long>): List<Attachment>

    /**
     * Every photo ever taken of one component, newest first - the time gallery.
     * Straight from the forum: "comparing photos over time is very helpful in
     * troubleshooting root cause of a problem".
     */
    @Query(
        """
        SELECT a.* FROM attachment a
        WHERE a.kind IN ('PHOTO', 'METER_PHOTO')
          AND (
            (a.parentType = 'COMPONENT' AND a.parentId = :componentId)
            OR (a.parentType = 'SERVICE_RECORD' AND a.parentId IN
                 (SELECT id FROM service_record WHERE componentId = :componentId))
          )
        ORDER BY a.takenAt DESC, a.createdAt DESC
        """
    )
    fun observeComponentGallery(componentId: Long): Flow<List<Attachment>>

    @Insert suspend fun insert(attachment: Attachment): Long

    @Query("SELECT * FROM attachment WHERE id = :id")
    suspend fun byId(id: Long): Attachment?

    @Query("SELECT * FROM attachment WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Attachment>

    @Query("SELECT * FROM attachment")
    suspend fun allAttachments(): List<Attachment>

    @Query("DELETE FROM attachment WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM attachment WHERE parentType = :parentType AND parentId = :parentId")
    suspend fun deleteForParent(parentType: ParentType, parentId: Long)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM document WHERE vesselId = :vesselId ORDER BY expiresOn IS NULL, expiresOn")
    fun observeForVessel(vesselId: Long): Flow<List<Document>>

    @Query("SELECT * FROM document WHERE vesselId = :vesselId AND expiresOn IS NOT NULL AND expiresOn <= :before")
    suspend fun expiringBefore(vesselId: Long, before: LocalDate): List<Document>

    @Insert suspend fun insert(document: Document): Long
    @Update suspend fun update(document: Document)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist WHERE vesselId = :vesselId ORDER BY name")
    fun observeForVessel(vesselId: Long): Flow<List<Checklist>>

    @Query("SELECT * FROM checklist_item WHERE checklistId = :checklistId ORDER BY sortOrder")
    fun observeItems(checklistId: Long): Flow<List<ChecklistItem>>

    @Insert suspend fun insert(checklist: Checklist): Long
    @Insert suspend fun insertItems(items: List<ChecklistItem>)

    @Query(
        """
        SELECT COUNT(*) > 0 FROM checklist_run
        WHERE vesselId = :vesselId AND checklistCode = :code AND date = :date
        """
    )
    suspend fun isRunDoneToday(vesselId: Long, code: String, date: LocalDate): Boolean

    @Query("SELECT DISTINCT vesselId FROM checklist_run WHERE checklistCode = :code AND date = :date")
    suspend fun vesselIdsDoneOn(code: String, date: LocalDate): List<Long>

    /** The boat that went out last - the likeliest one to go out next. */
    @Query(
        """
        SELECT vesselId FROM checklist_run WHERE checklistCode = :code
        ORDER BY date DESC, completedAt DESC LIMIT 1
        """
    )
    suspend fun lastRunVesselId(code: String): Long?

    @Insert suspend fun insertRun(run: ChecklistRun): Long

    @Query(
        """
        DELETE FROM checklist_run
        WHERE vesselId = :vesselId AND checklistCode = :code AND date = :date
        """
    )
    suspend fun deleteRunForDate(vesselId: Long, code: String, date: LocalDate)
}

// --------------------------------------------------------------- database

@Database(
    entities = [
        Vessel::class, Component::class, Meter::class, MeterReading::class,
        ServiceSchedule::class, ServiceRecord::class,
        Part::class, InventoryItem::class, StorageLocation::class,
        ComponentPart::class,
        Document::class, Checklist::class, ChecklistItem::class, ChecklistRun::class,
        Attachment::class, ImportSession::class, ImportRow::class,
        SearchIndex::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vesselDao(): VesselDao
    abstract fun componentDao(): ComponentDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun meterDao(): MeterDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun searchDao(): SearchDao
    abstract fun importDao(): ImportDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun documentDao(): DocumentDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        const val NAME = "hullbeat.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_record ADD COLUMN scheduleId INTEGER REFERENCES service_schedule(id) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_record_scheduleId ON service_record(scheduleId)")
            }
        }
    }
}
