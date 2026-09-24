package app.hullbeat.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Schema notes that are expensive to change later - see docs/product-spec.md p.3.
 *
 *  1. A [ServiceSchedule] carries days / hours / miles at once, all nullable.
 *     Due = whichever threshold arrives first. Competitors support a single unit
 *     and therefore nag at the wrong time.
 *
 *  2. [MeterReading] is a first-class entity, separate from [ServiceRecord].
 *     Owners photograph the hour meter far more often than they service anything,
 *     and a series of readings is what makes a *predicted date* possible. Without
 *     it we would only ever be able to say "overdue", like everyone else.
 *
 *  3. Dates are ISO-8601 text. SQLite sorts them correctly, CSV export is a
 *     straight copy, and a human can read the database file - which matters for
 *     a product whose whole promise is that the data stays legible without us.
 *
 *  4. [DatePrecision] exists because owners backfill years of history from memory
 *     and from spreadsheets that only recorded "spring 2019". A fuzzy point must
 *     not be allowed to skew the wear-rate trend.
 */

enum class DatePrecision { DAY, MONTH, YEAR, SEASON }

enum class MeterUnit { HOURS, MILES, NAUTICAL_MILES, KILOMETRES }

enum class ReadingSource { MANUAL, OCR, IMPORT, ESTIMATE }

enum class WorkType {
    SCHEDULED, REPAIR, INSPECTION, WINTERIZATION, COMMISSIONING,
    HAUL_OUT, LAUNCH, UPGRADE, WARRANTY, PRE_SALE
}

enum class Criticality { HIGH, MED, LOW }

enum class AttachmentKind { PHOTO, RECEIPT, MANUAL_PDF, DIAGRAM, METER_PHOTO, VIDEO, AUDIO_NOTE }

/** Polymorphic owner of an attachment. Stored as text so new hosts do not migrate. */
enum class ParentType { VESSEL, COMPONENT, SERVICE_RECORD, CHECKLIST_ITEM, DOCUMENT, INVENTORY_ITEM, METER_READING }

// ---------------------------------------------------------------- vessel

@Entity(tableName = "vessel")
data class Vessel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hullType: String,
    /**
     * Two axes, not one. `propulsion` used to answer both questions with a
     * single value and the seams showed twice: a DIESEL sterndrive was offered
     * a petrol-vapour blower, and a diesel saildrive - the commonest modern
     * cruising yacht there is - never saw its glow plugs. Both were papered
     * over with rules listing two values side by side, which is precisely the
     * hedge that produced the `shaft` duplicate.
     *
     * engine: diesel | petrol | electric | none   - what makes the power
     * drive:  shaft | saildrive | sterndrive | outboard | none - what transmits it
     *
     * Now `blower` is engineAny:[petrol] and `glow_plugs` is engineAny:[diesel],
     * with no exceptions on either.
     */
    val engine: String,
    val drive: String,
    /**
     * raw_water or keel_cooled. Not cosmetic: keel cooling almost always comes
     * with a dry exhaust, which means no impeller, no heat exchanger, no mixing
     * elbow and no waterlock - and instead expansion bellows and exhaust gas
     * temperature monitoring. Getting this wrong shows an owner seven parts
     * their boat does not physically have.
     */
    val cooling: String = "raw_water",
    val enginesCount: Int = 1,
    val rigType: String = "none",
    val keelType: String = "none",
    /** Comma-separated profile extras, e.g. "genset,liferaft,epirb". */
    val extras: String = "",
    val storage: String = "afloat_year_round",
    /**
     * salt, brackish or fresh. The one dimension that decides a correct part
     * rather than a preference: a zinc anode films over and stops protecting
     * in fresh water, where magnesium is required, and magnesium is eaten
     * alive in salt. See profileVocabulary.water in catalog.json.
     *
     * This was added to the catalog vocabulary and then missed here, so the
     * anode specHints had a dimension the vessel could not answer.
     */
    val water: String = "salt",
    /**
     * Months she is afloat, inclusive; null means year-round. DueCalculator
     * consumes sailing days only, so a hauled-out boat stops accruing engine
     * hours instead of having its oil change predicted into January.
     */
    val seasonStartMonth: Int? = null,
    val seasonEndMonth: Int? = null,
    val countryCode: String? = null,
    val lengthMetres: Double? = null,
    val year: Int? = null,
    /**
     * Hull identification number - HIN in the US, CIN in the EU. Stamped on
     * the starboard side of the transom and unreadable half the time, which is
     * exactly why it belongs in a searchable field: insurers, surveyors and
     * parts desks all ask for it, and it is what a buyer checks first.
     */
    val hin: String? = null,
    val make: String? = null,
    val model: String? = null,
    val photoUri: String? = null,
    val archived: Boolean = false,
)

// ------------------------------------------------------------- component

@Entity(
    tableName = "component",
    foreignKeys = [
        ForeignKey(
            entity = Vessel::class, parentColumns = ["id"], childColumns = ["vesselId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Component::class, parentColumns = ["id"], childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index("vesselId"), Index("parentId"), Index("categoryCode"), Index("catalogCode")]
)
data class Component(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vesselId: Long,
    val categoryCode: String,
    /** Null for a component the owner invented; otherwise the seed catalog code. */
    val catalogCode: String? = null,
    val parentId: Long? = null,
    /**
     * The SEED name, in English, written once by CatalogSeeder. It is not what
     * the owner reads: a seeded node is displayed from `cmp_<catalogCode>` in
     * strings_catalog.xml, so the tree follows the device locale.
     *
     * Storing the localised name here instead would freeze it: an owner who
     * switched the app to English would still see Ukrainian, the same defect
     * as a Ukrainian sentence hardcoded in a Composable.
     */
    val name: String,
    /**
     * What the owner calls it, when that is not what the catalog calls it.
     *
     * NULL means "follow the catalog", so the node re-localises with the app
     * and picks up any glossary correction - which matters, because this
     * project keeps making them ("джекштоки" -> "Страхувальні лінійні леєри").
     *
     * NON-NULL is the owner's word and is never translated, never overwritten
     * by a catalog update, and never re-localised. Someone whose yard calls
     * the impeller "крильчатка" is not wrong, and the app must not argue.
     *
     * The catalogCode SURVIVES a rename on purpose: synonyms and spec hints
     * are keyed by it, so a renamed node is still found by searching the
     * catalog's own words. Renaming is not the same as inventing.
     */
    val customName: String? = null,
    val criticality: Criticality = Criticality.MED,
    /** Which engine this belongs to when the category is perEngine. 1-based, null otherwise. */
    val engineIndex: Int? = null,
    val make: String? = null,
    val model: String? = null,
    val serial: String? = null,
    val yearInstalled: Int? = null,
    /**
     * The spec sheet: free key/value JSON such as oil grade, capacity, part
     * numbers, torque. This is what an owner actually needs while standing in a
     * chandlery, so it sits third on the component screen - see ui-spec.md p.5.
     */
    val specJson: String? = null,
    val notes: String? = null,
    /**
     * Who owns this row, which is a different question from what it is.
     *
     *   catalogCode  - WHAT it is. Null when the owner invented the node;
     *                  set when it came from the catalog, even after a rename.
     *   isCustom     - WHO manages it. True means a catalog upgrade must not
     *                  touch it: not re-seed it, not rename it, not re-gate it
     *                  when the profile changes.
     *
     * They are not redundant. An owner who adds a SECOND fuel filter the
     * catalog does not model keeps `catalogCode` so spec hints and synonyms
     * still work, and sets `isCustom` so the next seed leaves it alone.
     */
    val isCustom: Boolean = false,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

// ----------------------------------------------------------------- meters

@Entity(
    tableName = "meter",
    foreignKeys = [ForeignKey(
        entity = Component::class, parentColumns = ["id"], childColumns = ["componentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("componentId")]
)
data class Meter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Attached to a component, not a vessel: twin engines mean two meters. */
    val componentId: Long,
    val unit: MeterUnit = MeterUnit.HOURS,
    val label: String? = null,
)

@Entity(
    tableName = "meter_reading",
    foreignKeys = [ForeignKey(
        entity = Meter::class, parentColumns = ["id"], childColumns = ["meterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["meterId", "date"])]
)
data class MeterReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterId: Long,
    val date: LocalDate,
    val precision: DatePrecision = DatePrecision.DAY,
    val value: Double,
    val source: ReadingSource = ReadingSource.MANUAL,
    val note: String? = null,
)

// -------------------------------------------------------------- schedules

@Entity(
    tableName = "service_schedule",
    foreignKeys = [ForeignKey(
        entity = Component::class, parentColumns = ["id"], childColumns = ["componentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("componentId")]
)
data class ServiceSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentId: Long,
    /** All three nullable; due = whichever threshold is reached first. */
    val intervalDays: Int? = null,
    val intervalHours: Double? = null,
    val intervalMiles: Double? = null,
    /**
     * An explicit expiry printed on the item (liferaft certificate, flare stamp).
     * When present it overrides the computed interval - see catalog/README.md.
     */
    val expiresOn: LocalDate? = null,
    /** Seed default that the owner has not confirmed against the maker manual. */
    val needsVerification: Boolean = false,
    val enabled: Boolean = true,

    /**
     * Deferral, borrowed from the MV Dirona workbook, which is the most mature
     * owner-built log we found (docs/spreadsheet-references.md p.1.4).
     *
     * An owner who knows the anodes are overdue but is deliberately waiting for
     * the October haul-out needs a way to take the item out of the red WITHOUT
     * claiming it is done. Dirona writes "ACK:Yard" and the row goes green while
     * sitting 37 months overdue - that is normal operation, not neglect.
     *
     * Without this the "Due now" screen goes permanently red, the owner stops
     * trusting it, and the app dies exactly the way the others did.
     */
    val deferredUntil: LocalDate? = null,
    val deferReason: String? = null,

    /**
     * Warning window, per schedule rather than one constant for the app.
     * Dirona sets it per equipment section: 50 hours on the main engine, 15 on
     * the wing engine, none at all on items that are not hour-driven. Null falls
     * back to the DueCalculator defaults.
     */
    val soonWindowDays: Int? = null,
    val soonWindowHours: Double? = null,
)

// ---------------------------------------------------------------- records

@Entity(
    tableName = "service_record",
    foreignKeys = [
        ForeignKey(
            entity = Component::class, parentColumns = ["id"], childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MeterReading::class, parentColumns = ["id"], childColumns = ["meterReadingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ServiceSchedule::class, parentColumns = ["id"], childColumns = ["scheduleId"],
            onDelete = ForeignKey.SET_NULL
        ),
    ],
    indices = [
        Index(value = ["componentId", "date"]),
        Index("meterReadingId"),
        Index("scheduleId"),
        Index("importSessionId"),
    ]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentId: Long,
    val scheduleId: Long? = null,
    val date: LocalDate,
    val precision: DatePrecision = DatePrecision.DAY,
    /** Comma-separated [WorkType] names. Cross-cutting labels, not a hierarchy. */
    val workTypes: String = WorkType.SCHEDULED.name,
    val description: String,
    val cost: Double? = null,
    val currency: String? = null,
    val meterReadingId: Long? = null,
    val performedBy: String? = null,
    val notes: String? = null,
    /** Provenance, so a whole import can be rolled back. */
    val importSessionId: Long? = null,
    val importSheet: String? = null,
    val importRowIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

// -------------------------------------------------------------- inventory

@Entity(
    tableName = "location",
    foreignKeys = [ForeignKey(
        entity = Vessel::class, parentColumns = ["id"], childColumns = ["vesselId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vesselId")]
)
data class StorageLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vesselId: Long,
    val name: String,
    /** Printed on a sticker; scanning it opens this locker. */
    val qrCode: String? = null,
    val note: String? = null,
)

@Entity(tableName = "part", indices = [Index("partNumber")])
data class Part(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val partNumber: String? = null,
    val manufacturer: String? = null,
    val note: String? = null,
)

@Entity(
    tableName = "inventory_item",
    foreignKeys = [
        ForeignKey(entity = Part::class, parentColumns = ["id"], childColumns = ["partId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StorageLocation::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Vessel::class, parentColumns = ["id"], childColumns = ["vesselId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("partId"), Index("locationId"), Index("vesselId")]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vesselId: Long,
    val partId: Long,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    /** Below this the part joins the shopping list - see ui-spec.md p.7 item 4. */
    val minQuantity: Double? = null,
)

/**
 * Which nodes a part fits. THE MISSING EDGE: `Part` and `InventoryItem` were
 * created with no link to the component tree at all, while `MeterReading`,
 * `ServiceSchedule` and `ServiceRecord` all carry a `componentId`. So the
 * spares list knew a Jabsco 17937-0001 existed and no screen could say what
 * it was for - and the node page could not answer the one question an owner
 * asks while the engine is open: "do I have one aboard?"
 *
 * Many-to-many, not a column on `Part`, because both directions are real:
 * one zinc part number fits `anodes_hull` AND `shaft_anode`, and a raw-water
 * pump service takes an impeller, a cover gasket and screws. A generic
 * consumable - sealant, hose clamps, grease - fits dozens of nodes.
 */
@Entity(
    tableName = "component_part",
    primaryKeys = ["componentId", "partId"],
    foreignKeys = [
        ForeignKey(entity = Component::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Part::class, parentColumns = ["id"], childColumns = ["partId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("partId")]
)
data class ComponentPart(
    val componentId: Long,
    val partId: Long,
    /**
     * How many of this part one service of that node consumes: 1 impeller, but
     * 4 anodes and 4.5 litres of oil. Lets the shopping list ask for the right
     * quantity instead of one of everything.
     */
    val quantityPerService: Double = 1.0,
)

// -------------------------------------------------------------- documents

@Entity(
    tableName = "document",
    foreignKeys = [ForeignKey(
        entity = Vessel::class, parentColumns = ["id"], childColumns = ["vesselId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vesselId"), Index("expiresOn")]
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vesselId: Long,
    val title: String,
    val catalogCode: String? = null,
    val issuedOn: LocalDate? = null,
    val expiresOn: LocalDate? = null,
    /**
     * How long the renewal actually takes, so the warning arrives in time to act.
     *
     * Dirona groups expiries by turnaround rather than by category: prepaid SIM
     * gets one month of notice, a passport eight. A single 30-day rule warns far
     * too late for anything that needs an appointment or a survey.
     */
    val leadTimeDays: Int? = null,
    val reference: String? = null,
    val notes: String? = null,
)

// ------------------------------------------------------------- checklists

@Entity(
    tableName = "checklist",
    foreignKeys = [ForeignKey(
        entity = Vessel::class, parentColumns = ["id"], childColumns = ["vesselId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vesselId")]
)
data class Checklist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vesselId: Long,
    val name: String,
    val isBuiltIn: Boolean = false,
    /** Completing an item with a component creates a ServiceRecord with this tag. */
    val workType: WorkType = WorkType.SCHEDULED,
)

@Entity(
    tableName = "checklist_item",
    foreignKeys = [
        ForeignKey(entity = Checklist::class, parentColumns = ["id"], childColumns = ["checklistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Component::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("checklistId"), Index("componentId")]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checklistId: Long,
    /** Null is fine: a checklist line can be plain text with no component. */
    val componentId: Long? = null,
    val text: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "checklist_run",
    foreignKeys = [
        ForeignKey(entity = Vessel::class, parentColumns = ["id"], childColumns = ["vesselId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vesselId"), Index("date")]
)
data class ChecklistRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vesselId: Long,
    val checklistCode: String = "predeparture",
    val date: LocalDate,
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
)

// ------------------------------------------------------------ attachments

@Entity(
    tableName = "attachment",
    indices = [Index(value = ["parentType", "parentId"])]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentType: ParentType,
    val parentId: Long,
    val kind: AttachmentKind,
    val fileUri: String,
    /** Caption so a photo is findable by words, not only by date. */
    val caption: String? = null,
    /** Recognised text from a receipt or meter face; feeds the search index. */
    val ocrText: String? = null,
    /** From EXIF where available, not the import time. */
    val takenAt: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

// ----------------------------------------------------------------- import

@Entity(tableName = "import_session")
data class ImportSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileHash: String,
    val format: String,
    val layout: String,
    val mappingJson: String,
    val sheetCount: Int,
    val rowCount: Int,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "import_row",
    foreignKeys = [ForeignKey(
        entity = ImportSession::class, parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ImportRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sheet: String,
    val rowIndex: Int,
    /**
     * The untouched source row, kept forever. When the parser improves we can
     * re-parse old imports without the original file and without asking the
     * owner for anything - see docs/import-spec.md p.4.6.
     */
    val rawJson: String,
    val parseStatus: String,
    val targetType: String? = null,
    val targetId: Long? = null,
)

// ----------------------------------------------------------------- search

/**
 * Full-text index over everything an owner might search by word.
 *
 * NOTE: Room ships @Fts3 and @Fts4 only - there is no @Fts5 annotation, so the
 * docs' earlier mention of FTS5 was wrong. FTS4 with unicode61 gives us
 * diacritic-insensitive matching and prefix queries, which covers the actual
 * complaint about competitors ("fails on fundamental search functionality").
 * What we lose is bm25() ranking; ranking is done in Kotlin by field weight and
 * recency instead, which for a few thousand rows is indistinguishable.
 *
 * This is a standalone table, not an external-content one: Room does not
 * generate sync triggers, so we write it inside the same transaction as the
 * source row. See [app.hullbeat.data.db.dao.SearchDao].
 */
/*
 * ⚠️ UNVERIFIED, and the obvious fix is a trap. Read before touching the line
 * below.
 *
 * Room requires an FTS entity's primary key to be named `rowid` with INTEGER
 * affinity - that part is satisfied. What is NOT established is whether Room
 * also rejects `autoGenerate = true` on it, because SQLite manages an FTS
 * rowid itself. Room's own documentation example omits autoGenerate but does
 * not say it is forbidden, and this project has never run KSP.
 *
 * The trap: simply deleting `autoGenerate = true` compiles and then loses
 * data. Without it Room binds the field verbatim, so every insert carries
 * rowId = 0 from the default. Combined with SearchDao.index's
 * onConflict = REPLACE, the second row does not raise UNIQUE - it REPLACES
 * the first. The index would hold exactly one row and search would return
 * almost nothing, quietly. Trading a build error for silent data loss is the
 * worst trade available here.
 *
 * If `./gradlew :app:kspDebugKotlin` rejects this line, the fix is BOTH
 * halves: drop autoGenerate AND make the field nullable, so Room binds NULL
 * and SQLite assigns the rowid itself.
 *
 *     @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long? = null,
 *
 * Should Room also reject a nullable primary key, the third form is to declare
 * no primary key at all and let the implicit rowid stand. Nothing in this
 * table needs a stable id: SearchDao.reindex deletes by (ownerType, ownerId)
 * and inserts afresh inside one transaction.
 */
@Fts4(tokenizer = "unicode61")
@Entity(tableName = "search_index")
data class SearchIndex(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
    /** [ParentType] name of the indexed row, so results can be grouped. */
    val ownerType: String,
    val ownerId: Long,
    val vesselId: Long,
    /** Title-ish text: component name, part name, document title. Weighted highest. */
    val title: String,
    /** Description, notes, spec values, captions, OCR text. */
    val body: String,
)
