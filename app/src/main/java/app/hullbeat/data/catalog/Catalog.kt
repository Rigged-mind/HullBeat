package app.hullbeat.data.catalog

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel

/**
 * Reads catalog/catalog.json out of assets and turns it into rows for a vessel.
 *
 * The file is authored in /catalog at the repo root and copied into assets by a
 * Gradle task - never edit the copy. Validate any change with
 *   python tools/validate_catalog.py
 */

@Serializable
data class CatalogFile(
    val version: Int,
    val generated: String? = null,
    val note: String? = null,
    val profileVocabulary: Map<String, List<String>> = emptyMap(),
    val sailHulls: List<String> = emptyList(),
    val categories: List<CatalogCategory>,
)

@Serializable
data class CatalogCategory(
    val code: String,
    val sort: Int,
    val icon: String,
    val en: String,
    val perEngine: Boolean = false,
    val alwaysVisible: Boolean = false,
    val appliesTo: AppliesTo? = null,
    val components: List<CatalogComponent> = emptyList(),
)

@Serializable
data class CatalogComponent(
    val code: String,
    val en: String,
    val crit: String,
    val sched: CatalogSchedule? = null,
    val verify: Boolean = false,
    /**
     * Override for a perEngine category. Null follows the category; false
     * marks something shared between engines - a sea chest is one box feeding
     * both, not one per engine, and duplicating it would put a second one on
     * the boat that does not exist.
     */
    val perEngine: Boolean? = null,
    /**
     * This node's life is a date printed on it, not an interval since it was
     * last touched: a policy, a licence, a raft certificate.
     *
     * ⚠️ This flag was DEAD. It was declared here and read nowhere - not by
     * the seeder, not by DueEngine, not by any screen. Ten nodes carried it
     * with no `sched`, so `CatalogSeeder` gave them no ServiceSchedule at
     * all, `DueEngine` answered NO_SCHEDULES, and insurance, registration,
     * the radio licence and the state survey could never appear on the Now
     * screen no matter how close they were to lapsing.
     */
    val expires: Boolean = false,
    /**
     * How long the paperwork takes, in days - the window in which this turns
     * amber. Not one number for all of them: a broker renews a policy in a
     * month, an inspector in season has a two-month queue.
     */
    val leadDays: Int? = null,
    val reference: Boolean = false,
    val parent: String? = null,
    val specHints: List<String> = emptyList(),
    val appliesTo: AppliesTo? = null,
)

@Serializable
data class CatalogSchedule(
    val days: Int? = null,
    val hours: Double? = null,
    val miles: Double? = null,
)

/** OR inside each list, AND between the fields. All null means "everything". */
@Serializable
data class AppliesTo(
    @SerialName("hullAny") val hullAny: List<String>? = null,
    @SerialName("engineAny") val engineAny: List<String>? = null,
    @SerialName("driveAny") val driveAny: List<String>? = null,
    @SerialName("coolingAny") val coolingAny: List<String>? = null,
    @SerialName("rigAny") val rigAny: List<String>? = null,
    @SerialName("keelAny") val keelAny: List<String>? = null,
    @SerialName("extrasAny") val extrasAny: List<String>? = null,
    @SerialName("storageAny") val storageAny: List<String>? = null,
    @SerialName("enginesMin") val enginesMin: Int? = null,
)

object CatalogLoader {

    private const val ASSET_PATH = "catalog/catalog.json"

    private val json = Json {
        ignoreUnknownKeys = true   // a newer catalog must not crash an older build
        explicitNulls = false
    }

    fun load(context: Context): CatalogFile =
        context.assets.open(ASSET_PATH).bufferedReader().use { json.decodeFromString(it.readText()) }

    @Volatile
    private var cached: CatalogFile? = null

    fun get(context: Context): CatalogFile =
        cached ?: synchronized(this) {
            cached ?: load(context).also { cached = it }
        }

    /**
     * Passport labels for a node, in the reader's language.
     *
     * catalog.json holds the ENGLISH label, and this used to return it
     * straight through - so a Ukrainian owner opening the winch card read
     * "Grease type". The 154 translations existed, were reviewed, and were
     * read by nothing but the HTML prototype. `check_translations.py` was
     * blind to it for the usual reason: a string that never becomes a
     * resource does not exist as far as that check is concerned.
     */
    fun specHintsFor(context: Context, catalogCode: String?): List<String> {
        if (catalogCode == null) return emptyList()
        val baseCode = catalogCode.substringBefore('#')
        val file = get(context)
        for (cat in file.categories) {
            for (comp in cat.components) {
                if (comp.code == baseCode) {
                    return comp.specHints.map { hintString(context, it) }
                }
            }
        }
        return emptyList()
    }

    fun rawSpecHintsFor(context: Context, catalogCode: String?): List<String> {
        if (catalogCode == null) return emptyList()
        val baseCode = catalogCode.substringBefore('#')
        val file = get(context)
        for (cat in file.categories) {
            for (comp in cat.components) {
                if (comp.code == baseCode) {
                    return comp.specHints
                }
            }
        }
        return emptyList()
    }

    /**
     * `%` and `°` carry meaning and must survive into the resource name:
     * without them "Size" and "Size %" collapse onto one key. `make_strings`
     * builds the same slug and refuses to generate on a collision.
     */
    private fun hintSlug(label: String): String =
        label.lowercase()
            .replace("%", " pct ")
            .replace("°", " deg ")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    /** ⚠️ Name-resolved, so `res/raw/keep.xml` lists `@string/hint_*`. */
    fun hintString(context: Context, label: String): String {
        val slug = hintSlug(label)
        if (slug.isBlank()) return label
        val id = context.resources.getIdentifier(
            "hint_$slug", "string", context.packageName)
        return if (id != 0) context.getString(id) else label
    }
}

@Serializable
data class ChecklistsFile(
    val version: Int,
    val checklists: List<ChecklistDef> = emptyList(),
)

/**
 * Checklist text comes from `chk_<code>` string resources, NOT from a field in
 * this JSON.
 *
 * The first version carried a `uk` field here and picked it with
 * `configuration.locales[0].language == "uk"` - a second localisation
 * mechanism running beside Android's own. Three things were wrong with it: a
 * third language could never be added, a translator could never reach the
 * text, and `check_translations.py` could not see it at all, because a string
 * that never becomes a resource does not exist as far as that check is
 * concerned. The same blind spot let ten Ukrainian sentences into Composables.
 *
 * The Ukrainian was in catalog/i18n/uk.json under `chk_<code>` all along;
 * `make_strings.py` simply was not emitting it.
 */
@Serializable
data class ChecklistDef(
    val code: String,
    val season: String? = null,
    /** Seed text. Displayed only if a device somehow has no resource for it. */
    val en: String,
    val items: List<ChecklistItemDef> = emptyList(),
) {
    fun title(context: Context): String = checklistString(context, code, en)
}

@Serializable
data class ChecklistItemDef(
    val code: String,
    val order: Int,
    val en: String,
    val node: String? = null,
    val logs: String? = null,
    val appliesTo: AppliesTo? = null,
) {
    fun title(context: Context): String = checklistString(context, code, en)
}

/**
 * ⚠️ Resolved by name, so the shrinker cannot see it. `res/raw/keep.xml` holds
 * `@string/chk_*` for exactly this reason - the same trap that would have
 * blanked all 329 component names in release builds only.
 */
private fun checklistString(context: Context, code: String, fallback: String): String {
    val id = context.resources.getIdentifier("chk_$code", "string", context.packageName)
    return if (id != 0) context.getString(id) else fallback
}

object ChecklistsLoader {

    private const val ASSET_PATH = "catalog/checklists.json"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Volatile
    private var cached: ChecklistsFile? = null

    fun load(context: Context): ChecklistsFile =
        cached ?: synchronized(this) {
            cached ?: context.assets.open(ASSET_PATH).bufferedReader().use {
                json.decodeFromString<ChecklistsFile>(it.readText())
            }.also { cached = it }
        }

    fun predepartureItemsFor(context: Context, vessel: Vessel): List<ChecklistItemDef> {
        val file = try {
            load(context)
        } catch (_: Exception) {
            return emptyList()
        }
        val pre = file.checklists.firstOrNull { it.code == "predeparture" } ?: return emptyList()
        return pre.items.filter { ProfileMatcher.matches(it.appliesTo, vessel) }
    }
}

object ProfileMatcher {

    /**
     * Whether a catalog entry belongs on this boat.
     *
     * Non-matching entries are hidden, never deleted: an owner may fit a genset
     * or a watermaker later, and "Show everything" stays one tap away.
     */
    fun matches(rule: AppliesTo?, vessel: Vessel): Boolean {
        if (rule == null) return true
        val extras = vessel.extras.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        rule.hullAny?.let { if (vessel.hullType !in it) return false }
        rule.engineAny?.let { if (vessel.engine !in it) return false }
        rule.driveAny?.let { if (vessel.drive !in it) return false }
        rule.coolingAny?.let { if (vessel.cooling !in it) return false }
        rule.rigAny?.let { if (vessel.rigType !in it) return false }
        rule.keelAny?.let { if (vessel.keelType !in it) return false }
        rule.storageAny?.let { if (vessel.storage !in it) return false }
        rule.extrasAny?.let { if (it.none(extras::contains)) return false }
        rule.enginesMin?.let { if (vessel.enginesCount < it) return false }
        return true
    }

    fun categoriesFor(catalog: CatalogFile, vessel: Vessel): List<CatalogCategory> =
        catalog.categories
            .filter { it.alwaysVisible || matches(it.appliesTo, vessel) }
            .sortedBy { it.sort }
}

/**
 * Materialises catalog entries into database rows.
 *
 * Runs once at onboarding, and again - additively - whenever the catalog version
 * moves. It must never overwrite anything the owner has touched; see
 * catalog/README.md for the migration rules this upholds.
 */
class CatalogSeeder(private val catalog: CatalogFile) {

    companion object {
        /**
         * Used only if the catalog forgot a number for an expiring node.
         * Thirty days is the shortest of the twelve real ones, so it warns
         * too early rather than too late.
         */
        const val DEFAULT_LEAD_DAYS = 30
    }

    data class Seeded(
        val components: List<Component>,
        /** Keyed by catalogCode, since component ids only exist after insert. */
        val schedulesByCode: Map<String, ServiceSchedule>,
        val specHintsByCode: Map<String, List<String>>,
        val parentByCode: Map<String, String>,
    )

    /**
     * Seed a vessel, or top it up after a catalog update.
     *
     * ⚠️ `existingCodes` MUST include archived components. The catalog grows -
     * four nodes were added in a single afternoon - so this runs again on
     * every update, and a code that is missing from the set is treated as new
     * and re-inserted. An owner who archived "Газова система" because their
     * boat has no gas would find it back after the next release, every
     * release, with no way to make it stop.
     *
     * Feed it from a query with NO `archived = 0` filter. `observeForVessel`
     * and `forVessel` both have that filter and are the wrong source.
     */
    fun seedFor(vessel: Vessel, existingCodes: Set<String> = emptySet()): Seeded {
        val components = mutableListOf<Component>()
        val schedules = mutableMapOf<String, ServiceSchedule>()
        val hints = mutableMapOf<String, List<String>>()
        val parents = mutableMapOf<String, String>()

        for (category in ProfileMatcher.categoriesFor(catalog, vessel)) {
            // A twin-engine boat gets two of everything under a perEngine
            // category, each with its own meter - one shared meter would make
            // every hour-based prediction wrong on both engines.
            val copies = if (category.perEngine) maxOf(1, vessel.enginesCount) else 1

            for (component in category.components) {
                if (!ProfileMatcher.matches(component.appliesTo, vessel)) continue

                // A component may opt out of per-engine duplication.
                val componentCopies = if (component.perEngine == false) 1 else copies

                for (index in 1..componentCopies) {
                    val code =
                        if (componentCopies > 1) "${component.code}#$index" else component.code
                    if (code in existingCodes) continue

                    val specJson = if (component.specHints.isNotEmpty()) {
                        component.specHints.joinToString(
                            prefix = "{",
                            postfix = "}",
                            separator = ","
                        ) { hint -> "\"${hint.replace("\"", "\\\"")}\":\"\"" }
                    } else null

                    components += Component(
                        vesselId = vessel.id,
                        categoryCode = category.code,
                        catalogCode = code,
                        name = component.en,   // localised at display time via cmp_<code>
                        criticality = when (component.crit) {
                            "high" -> Criticality.HIGH
                            "low" -> Criticality.LOW
                            else -> Criticality.MED
                        },
                        engineIndex = if (componentCopies > 1) index else null,
                        specJson = specJson,
                        archived = !CoreComponents.isCore(code),
                        sortOrder = components.size,
                    )

                    val sched = component.sched
                    if (sched != null) {
                        schedules[code] = ServiceSchedule(
                            componentId = 0,           // filled in after insert
                            intervalDays = sched.days,
                            intervalHours = sched.hours,
                            intervalMiles = sched.miles,
                            // Seed intervals for engine-side parts are guidance only.
                            // The UI must say "check your engine manual" until the
                            // owner confirms - inventing authoritative numbers is
                            // exactly what gets an app dismissed as not built by a boater.
                            needsVerification = component.verify,
                            soonWindowDays = component.leadDays,
                        )
                    } else if (component.expires) {
                        // No interval, because there is nothing to count from:
                        // a policy does not come due six months after the last
                        // one, it comes due on the date printed on it. The
                        // schedule exists so the node HAS one - without it
                        // DueEngine says NO_SCHEDULES and the document is
                        // invisible until it has already lapsed.
                        //
                        // `expiresOn` stays null until the owner enters the
                        // date; the app must never invent an expiry.
                        schedules[code] = ServiceSchedule(
                            componentId = 0,
                            needsVerification = false,
                            soonWindowDays = component.leadDays ?: DEFAULT_LEAD_DAYS,
                        )
                    }
                    if (component.specHints.isNotEmpty()) hints[code] = component.specHints
                    component.parent?.let {
                        parents[code] = if (componentCopies > 1) "$it#$index" else it
                    }
                }
            }
        }
        return Seeded(components, schedules, hints, parents)
    }
}
