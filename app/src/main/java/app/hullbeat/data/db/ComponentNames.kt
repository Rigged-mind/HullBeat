package app.hullbeat.data.db

import android.content.Context
import app.hullbeat.R

/**
 * What to call a component on screen.
 *
 * Three sources, in this order, and the order is the whole point:
 *
 *  1. `customName` - the owner's own word. Never translated, never
 *     overwritten by a catalog update, never re-localised. Someone whose yard
 *     calls the impeller "крильчатка" is not wrong and the app must not argue.
 *  2. `cmp_<catalogCode>` - the catalog name in the device's language. This is
 *     why a seeded node re-localises when the app switches language AND picks
 *     up glossary corrections, of which this project keeps making them.
 *  3. `name` - the English seed value, as a last resort. It is what
 *     CatalogSeeder wrote and it is not meant to be read by anyone.
 *
 * ⚠️ The resource lookup is BY NAME, so the shrinker cannot see it. All 350
 * catalog strings are held by `res/raw/keep.xml`; deleting that file makes
 * every name on this screen disappear in release builds only.
 */
fun Component.displayName(context: Context): String {
    customName?.takeIf { it.isNotBlank() }?.let { return it }

    val code = catalogCode
    if (code != null) {
        // A per-engine node is seeded as `engine_oil#2`; the resource is keyed
        // by the bare code, and the copy number is shown separately.
        val base = code.substringBefore('#')
        val resId = context.resources.getIdentifier(
            "cmp_$base", "string", context.packageName
        )
        if (resId != 0) {
            val catalogName = context.getString(resId)
            return engineIndex?.let { "$catalogName #$it" } ?: catalogName
        }
    }
    return name
}

/**
 * Localised name of a catalog category (e.g. "cat_hull_underwater" -> "Корпус і підводна частина").
 */
fun categoryDisplayName(context: Context, categoryCode: String): String {
    val id = context.resources.getIdentifier("cat_$categoryCode", "string", context.packageName)
    return if (id != 0) context.getString(id) else categoryCode
}

/**
 * Whether this row still follows the catalog's wording.
 *
 * The tree marks a renamed node so nobody thinks the catalog shipped a
 * component called "Ліхтар Петровича" - not a warning, just an attribution.
 */
val Component.isRenamed: Boolean
    get() = !customName.isNullOrBlank() && catalogCode != null

/**
 * String resource for a [WorkType].
 */
fun WorkType.labelRes(): Int = when (this) {
    WorkType.SCHEDULED -> R.string.work_type_scheduled
    WorkType.REPAIR -> R.string.work_type_repair
    WorkType.INSPECTION -> R.string.work_type_inspection
    WorkType.WINTERIZATION -> R.string.work_type_winterization
    WorkType.COMMISSIONING -> R.string.work_type_commissioning
    WorkType.HAUL_OUT -> R.string.work_type_haul_out
    WorkType.LAUNCH -> R.string.work_type_launch
    WorkType.UPGRADE -> R.string.work_type_upgrade
    WorkType.WARRANTY -> R.string.work_type_warranty
    WorkType.PRE_SALE -> R.string.work_type_pre_sale
}

/**
 * Localised display string for one or more comma-separated work types.
 */
/**
 * A profile axis value in words: `saildrive` -> "Сейлдрайв".
 *
 * Lived as a private helper inside `VesselSetupScreen`, which is why the PDF
 * exporter printed `monohull_sail` and `none` at the owner instead. Shared
 * from here now, so the next thing that renders a vessel profile has it.
 *
 * Resolved by name, so `res/raw/keep.xml` must keep `@string/opt_*` - it
 * already does, and `check_resource_keep` says so on every run.
 */
fun profileOptionLabel(context: Context, axis: String, value: String): String {
    if (value.isBlank()) return value
    val id = context.resources.getIdentifier(
        "opt_" + axis + "_" + value, "string", context.packageName,
    )
    return if (id != 0) context.getString(id) else value
}

fun formatWorkTypes(context: Context, rawWorkTypes: String): String {
    if (rawWorkTypes.isBlank()) return ""
    return rawWorkTypes.split(",").map { raw ->
        val trimmed = raw.trim()
        val type = runCatching { WorkType.valueOf(trimmed) }.getOrNull()
        if (type != null) context.getString(type.labelRes()) else trimmed
    }.joinToString(", ")
}

/**
 * Localised display string for a service schedule interval.
 * Uses string resources and plurals to avoid any hardcoded interface text.
 */
fun formatScheduleInterval(context: Context, schedule: ServiceSchedule): String {
    val parts = mutableListOf<String>()
    schedule.intervalHours?.let { h ->
        val hStr = if (h % 1.0 == 0.0) h.toLong().toString() else h.toString()
        parts.add("$hStr ${context.getString(R.string.meter_hours_unit)}")
    }
    schedule.intervalDays?.let { d ->
        parts.add(context.resources.getQuantityString(R.plurals.due_in_days, d, d))
    }
    schedule.intervalMiles?.let { m ->
        val mStr = if (m % 1.0 == 0.0) m.toLong().toString() else m.toString()
        parts.add("$mStr ${context.getString(R.string.unit_short_nm)}")
    }
    if (schedule.expiresOn != null) {
        parts.add(context.getString(R.string.action_renew))
    }
    return parts.joinToString(" / ")
}

/**
 * Localised display string for a service record description.
 *
 * Historic service records were created with localized default text like "Зроблено"
 * or "Виконано зі сповіщення" written straight into SQLite. When the user switches
 * to English, this helper translates those known default system descriptions to the
 * active locale's string resource (e.g. "Зроблено" -> "Done"), while leaving custom
 * user-written descriptions untouched.
 */
fun formatRecordDescription(context: Context, rawDescription: String): String =
    formatRecordDescription(rawDescription) { context.getString(it) }

fun formatRecordDescription(rawDescription: String, resolver: (Int) -> String): String {
    if (rawDescription.isBlank()) return ""
    val trimmed = rawDescription.trim()
    return when {
        trimmed.equals("Зроблено", ignoreCase = true) ||
        trimmed.equals("Done", ignoreCase = true) ||
        trimmed.equals("action_mark_done", ignoreCase = true) ||
        trimmed.equals("@action_mark_done", ignoreCase = true) -> {
            resolver(R.string.action_mark_done)
        }
        trimmed.equals("Виконано зі сповіщення", ignoreCase = true) ||
        trimmed.equals("Logged from the notification", ignoreCase = true) ||
        trimmed.equals("record_from_notification", ignoreCase = true) ||
        trimmed.equals("@record_from_notification", ignoreCase = true) -> {
            resolver(R.string.record_from_notification)
        }
        trimmed.equals("Продовжити…", ignoreCase = true) ||
        trimmed.equals("Продовжити...", ignoreCase = true) ||
        trimmed.equals("Renew…", ignoreCase = true) ||
        trimmed.equals("Renew...", ignoreCase = true) ||
        trimmed.equals("Продовжено", ignoreCase = true) ||
        trimmed.equals("Renewed", ignoreCase = true) ||
        trimmed.equals("action_renew", ignoreCase = true) ||
        trimmed.equals("@action_renew", ignoreCase = true) -> {
            resolver(R.string.action_renew)
        }
        else -> rawDescription
    }
}

/**
 * Localised display name for a vessel.
 *
 * When created without a custom name, vessels receive a localized default name
 * ("Моє судно" / "My boat") saved into SQLite. This helper ensures it adapts
 * dynamically to the active locale unless the owner provided their own custom name.
 */
fun Vessel.displayName(context: Context): String =
    displayName { context.getString(it) }

fun Vessel.displayName(resolver: (Int) -> String): String {
    val trimmed = name.trim()
    return if (trimmed.equals("Моє судно", ignoreCase = true) ||
               trimmed.equals("My boat", ignoreCase = true) ||
               trimmed.equals("setup_default_name", ignoreCase = true)) {
        resolver(R.string.setup_default_name)
    } else {
        name
    }
}

/**
 * Localised display name for a starter storage locker.
 *
 * Default starter lockers are seeded into SQLite with localized names (e.g. "Моторний відсік").
 * This helper dynamically re-localises them when the app switches language, while
 * preserving any custom locker names created or renamed by the owner.
 */
fun storageLocationDisplayName(context: Context, rawName: String): String =
    storageLocationDisplayName(rawName) { context.getString(it) }

fun storageLocationDisplayName(rawName: String, resolver: (Int) -> String): String {
    if (rawName.isBlank()) return rawName
    val trimmed = rawName.trim()
    return when {
        trimmed.equals("Моторний відсік", ignoreCase = true) ||
        trimmed.equals("Engine room", ignoreCase = true) ||
        trimmed.equals("store_default_loc_engine", ignoreCase = true) -> {
            resolver(R.string.store_default_loc_engine)
        }
        trimmed.equals("Рундук кокпіта", ignoreCase = true) ||
        trimmed.equals("Cockpit locker", ignoreCase = true) ||
        trimmed.equals("store_default_loc_cockpit", ignoreCase = true) -> {
            resolver(R.string.store_default_loc_cockpit)
        }
        trimmed.equals("Штурманський стіл", ignoreCase = true) ||
        trimmed.equals("Chart table", ignoreCase = true) ||
        trimmed.equals("store_default_loc_chart", ignoreCase = true) -> {
            resolver(R.string.store_default_loc_chart)
        }
        trimmed.equals("Форпік", ignoreCase = true) ||
        trimmed.equals("Forepeak", ignoreCase = true) ||
        trimmed.equals("store_default_loc_forepeak", ignoreCase = true) -> {
            resolver(R.string.store_default_loc_forepeak)
        }
        trimmed.equals("Камбуз", ignoreCase = true) ||
        trimmed.equals("Galley", ignoreCase = true) ||
        trimmed.equals("store_default_loc_galley", ignoreCase = true) -> {
            resolver(R.string.store_default_loc_galley)
        }
        else -> rawName
    }
}

/**
 * Localised display string for service urgency.
 * Evaluated on the fly in Composable scopes using LocalContext.current.
 */
fun formatUrgency(
    context: Context,
    r: app.hullbeat.domain.DueCalculator.Result?,
    schedule: ServiceSchedule?,
    unknownReason: app.hullbeat.domain.DueEngine.UnknownReason = app.hullbeat.domain.DueEngine.UnknownReason.NONE,
): String {
    val res = context.resources
    if (r == null || schedule == null) {
        return when (unknownReason) {
            app.hullbeat.domain.DueEngine.UnknownReason.NEEDS_INITIAL_METER -> res.getString(R.string.due_unknown_needs_meter)
            app.hullbeat.domain.DueEngine.UnknownReason.NO_SCHEDULES -> res.getString(R.string.due_unknown_needs_schedule)
            else -> res.getString(R.string.detail_no_interval)
        }
    }

    if (r.state == app.hullbeat.domain.DueCalculator.State.DEFERRED) {
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

        r.driver == app.hullbeat.domain.DueCalculator.Driver.HOURS && r.unitsRemaining != null ->
            res.getQuantityString(R.plurals.due_in_hours, r.unitsRemaining.toInt(), r.unitsRemaining.toInt())

        days != null ->
            res.getQuantityString(R.plurals.due_in_days, days.toInt(), days.toInt())

        unknownReason == app.hullbeat.domain.DueEngine.UnknownReason.NEEDS_INITIAL_METER ->
            res.getString(R.string.due_unknown_needs_meter)

        unknownReason == app.hullbeat.domain.DueEngine.UnknownReason.NO_SCHEDULES ->
            res.getString(R.string.due_unknown_needs_schedule)

        else -> res.getString(R.string.due_unknown)
    }
}

/**
 * Localised anode water suitability advice for a vessel.
 */
fun formatWaterAdvice(context: Context, water: String): String {
    val res = context.resources
    return when (water) {
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

/**
 * Localised passport field label.
 */
fun displayPassportLabel(context: Context, key: String): String {
    val res = context.resources
    return when (key) {
        "make" -> res.getString(R.string.passport_make)
        "model" -> res.getString(R.string.passport_model)
        "serial" -> res.getString(R.string.passport_serial)
        "year", "yearInstalled" -> res.getString(R.string.passport_year)
        else -> app.hullbeat.data.catalog.CatalogLoader.hintString(context, key)
    }
}


