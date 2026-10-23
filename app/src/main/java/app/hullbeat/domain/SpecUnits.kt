package app.hullbeat.domain

import app.hullbeat.R
import app.hullbeat.data.preferences.LengthUnit
import app.hullbeat.data.preferences.PressureUnit
import app.hullbeat.data.preferences.TemperatureUnit
import app.hullbeat.data.preferences.VolumeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** What a passport value measures. Each follows one row of the units in Settings. */
enum class SpecQuantity { LENGTH, SMALL_LENGTH, VOLUME, FLOW, PRESSURE, TEMPERATURE }

/**
 * A unit a passport value was typed in, stored by NAME in
 * [app.hullbeat.data.db.Component.specUnitsJson].
 *
 * value in this unit = value in the base unit x [factor] + [offset]; the base
 * is the first unit of each quantity. Only temperature needs the offset.
 *
 * [aliases] are what an owner may already have typed after the number
 * ("40 l", "12 м"), lowercase. A plain "gal" is in neither gallon: which one
 * it means is the owner's Settings, see [SpecUnits.splitExplicitUnit].
 */
enum class SpecUnit(
    val quantity: SpecQuantity,
    val shortNameRes: Int,
    private val factor: Double,
    private val offset: Double = 0.0,
    val aliases: Set<String> = emptySet(),
) {
    METRE(SpecQuantity.LENGTH, R.string.unit_short_m, 1.0,
        aliases = setOf("m", "м", "metre", "metres", "meter", "meters", "метр", "метри", "метрів")),
    FOOT(SpecQuantity.LENGTH, R.string.unit_short_ft, 3.28084,
        aliases = setOf("ft", "'", "foot", "feet", "фут", "фути", "футів")),

    MILLIMETRE(SpecQuantity.SMALL_LENGTH, R.string.unit_short_mm, 1.0,
        aliases = setOf("mm", "мм")),
    INCH(SpecQuantity.SMALL_LENGTH, R.string.unit_short_in, 1.0 / 25.4,
        aliases = setOf("in", "\"", "″", "inch", "inches", "дюйм", "дюйми", "дюймів")),

    LITRE(SpecQuantity.VOLUME, R.string.unit_short_l, 1.0,
        aliases = setOf("l", "л", "lt", "ltr", "litre", "litres", "liter", "liters", "літр", "літри", "літрів")),
    US_GALLON(SpecQuantity.VOLUME, R.string.unit_short_gal_us, 0.264172,
        aliases = setOf("us gal", "usgal")),
    UK_GALLON(SpecQuantity.VOLUME, R.string.unit_short_gal_uk, 0.219969,
        aliases = setOf("uk gal", "ukgal", "imp gal")),

    LITRE_PER_MINUTE(SpecQuantity.FLOW, R.string.unit_short_l_min, 1.0,
        aliases = setOf("l/min", "л/хв", "lpm", "l/m")),
    US_GALLON_PER_HOUR(SpecQuantity.FLOW, R.string.unit_short_gal_us_h, 0.264172 * 60,
        aliases = setOf("us gal/h", "us gph")),
    UK_GALLON_PER_HOUR(SpecQuantity.FLOW, R.string.unit_short_gal_uk_h, 0.219969 * 60,
        aliases = setOf("uk gal/h", "uk gph")),

    BAR(SpecQuantity.PRESSURE, R.string.unit_short_bar, 1.0,
        aliases = setOf("bar", "бар")),
    PSI(SpecQuantity.PRESSURE, R.string.unit_short_psi, 14.5038,
        aliases = setOf("psi")),

    CELSIUS(SpecQuantity.TEMPERATURE, R.string.unit_short_celsius, 1.0,
        aliases = setOf("°c", "c", "℃")),
    FAHRENHEIT(SpecQuantity.TEMPERATURE, R.string.unit_short_fahrenheit, 1.8, offset = 32.0,
        aliases = setOf("°f", "f", "℉"));

    fun convert(value: Double, to: SpecUnit): Double {
        require(to.quantity == quantity) { "$this -> $to" }
        if (to == this) return value
        val base = (value - offset) / factor
        return base * to.factor + to.offset
    }
}

/** The units Settings holds at one moment - what a value typed then is in. */
data class UnitSettings(
    val length: LengthUnit,
    val volume: VolumeUnit,
    val pressure: PressureUnit,
    val temperature: TemperatureUnit,
) {
    /**
     * Small lengths have no row of their own: a boat measured in metres has
     * its shaft in millimetres, one measured in feet has it in inches.
     * Flow follows volume, in the unit pumps are sold by: l/min in Europe,
     * gallons an hour in the US.
     */
    fun unitFor(quantity: SpecQuantity): SpecUnit = when (quantity) {
        SpecQuantity.LENGTH -> if (length == LengthUnit.FEET) SpecUnit.FOOT else SpecUnit.METRE
        SpecQuantity.SMALL_LENGTH -> if (length == LengthUnit.FEET) SpecUnit.INCH else SpecUnit.MILLIMETRE
        SpecQuantity.VOLUME -> when (volume) {
            VolumeUnit.LITRES -> SpecUnit.LITRE
            VolumeUnit.GALLONS_US -> SpecUnit.US_GALLON
            VolumeUnit.GALLONS_UK -> SpecUnit.UK_GALLON
        }
        SpecQuantity.FLOW -> when (volume) {
            VolumeUnit.LITRES -> SpecUnit.LITRE_PER_MINUTE
            VolumeUnit.GALLONS_US -> SpecUnit.US_GALLON_PER_HOUR
            VolumeUnit.GALLONS_UK -> SpecUnit.UK_GALLON_PER_HOUR
        }
        SpecQuantity.PRESSURE -> if (pressure == PressureUnit.PSI) SpecUnit.PSI else SpecUnit.BAR
        SpecQuantity.TEMPERATURE ->
            if (temperature == TemperatureUnit.FAHRENHEIT) SpecUnit.FAHRENHEIT else SpecUnit.CELSIUS
    }
}

/**
 * Passport values keep the unit Settings held when they were typed.
 *
 * The value stays the owner's text in specJson; its unit goes beside it in
 * specUnitsJson, keyed the same way. Switching Settings from litres to
 * gallons must not turn a 40-litre tank into a 40-gallon one - the same
 * defect a cost had before it kept its currency.
 */
object SpecUnits {

    /**
     * The catalog hints that hold a measurement Settings has a unit for.
     * Keyed by the catalog's own English hint, which is also the specJson key.
     *
     * Left out on purpose: sizes that are codes, not measurements ("Belt
     * size", "Thread size", "Propeller size"), the liferaft's "Capacity" (it
     * is people), units the same everywhere (A, W, Ah, CCA, CFM, micron) and
     * those Settings has no row for (weight, torque).
     */
    val QUANTITY_BY_HINT: Map<String, SpecQuantity> = mapOf(
        "Length" to SpecQuantity.LENGTH,
        "Lengths" to SpecQuantity.LENGTH,
        "Height" to SpecQuantity.LENGTH,

        "Diameter" to SpecQuantity.SMALL_LENGTH,
        "Wire dia" to SpecQuantity.SMALL_LENGTH,
        "Gap" to SpecQuantity.SMALL_LENGTH,
        "Intake gap" to SpecQuantity.SMALL_LENGTH,
        "Exhaust gap" to SpecQuantity.SMALL_LENGTH,
        "Pitch" to SpecQuantity.SMALL_LENGTH,

        "Capacity (L)" to SpecQuantity.VOLUME,
        "Coolant capacity (L)" to SpecQuantity.VOLUME,
        "Quantity (L)" to SpecQuantity.VOLUME,
        "Oil quantity" to SpecQuantity.VOLUME,
        "Litres used" to SpecQuantity.VOLUME,

        "Flow (l/min)" to SpecQuantity.FLOW,

        "Pressure" to SpecQuantity.PRESSURE,
        "Working pressure" to SpecQuantity.PRESSURE,
        "Expansion tank pressure" to SpecQuantity.PRESSURE,

        "Alarm threshold (°C)" to SpecQuantity.TEMPERATURE,
        "Opening temp" to SpecQuantity.TEMPERATURE,
    )

    /**
     * Hints that name their unit. A value typed under "Capacity (L)" was
     * typed in litres whatever Settings said, because "L" is what the owner
     * read on the screen. The unit is cut from these labels at display time,
     * see [stripLabelUnit]; "Litres used" has no brackets and is given a
     * label of its own instead.
     */
    val LABEL_UNIT: Map<String, SpecUnit> = mapOf(
        "Capacity (L)" to SpecUnit.LITRE,
        "Coolant capacity (L)" to SpecUnit.LITRE,
        "Quantity (L)" to SpecUnit.LITRE,
        "Litres used" to SpecUnit.LITRE,
        "Flow (l/min)" to SpecUnit.LITRE_PER_MINUTE,
        "Alarm threshold (°C)" to SpecUnit.CELSIUS,
    )

    fun quantityOf(hint: String?): SpecQuantity? = hint?.let { QUANTITY_BY_HINT[it] }

    private val NUMBER = Regex("""^\s*([-+]?\d+(?:[.,]\d+)?)\s*$""")
    private val NUMBER_THEN_TEXT = Regex("""^\s*([-+]?\d+(?:[.,]\d+)?)\s*(\S.*?)\s*$""")
    private val TRAILING_BRACKETS = Regex("""\s*\([^()]*\)\s*$""")

    /** "10,5" and "10.5" alike: the owner types with the keyboard their language gives. */
    fun parseNumber(text: String): Double? =
        NUMBER.matchEntire(text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()

    private fun unitForAlias(alias: String, quantity: SpecQuantity, preferred: SpecUnit): SpecUnit? {
        val a = alias.lowercase().replace(Regex("\\s+"), " ").trim()
        if (a == "gal" || a == "gallon" || a == "gallons" || a == "гал") {
            return when (quantity) {
                SpecQuantity.VOLUME -> if (preferred == SpecUnit.UK_GALLON) SpecUnit.UK_GALLON else SpecUnit.US_GALLON
                else -> null
            }
        }
        if (a == "gph" || a == "gal/h") {
            return when (quantity) {
                SpecQuantity.FLOW ->
                    if (preferred == SpecUnit.UK_GALLON_PER_HOUR) SpecUnit.UK_GALLON_PER_HOUR else SpecUnit.US_GALLON_PER_HOUR
                else -> null
            }
        }
        return SpecUnit.entries.firstOrNull { it.quantity == quantity && a in it.aliases }
    }

    /**
     * "40 l" -> ("40", LITRE): a number followed by a unit of the field's own
     * quantity. The number is returned as typed. Anything else - "≈40",
     * "2 x 50 m", "40 kg" in a volume field - is null and stays as text.
     */
    fun splitExplicitUnit(text: String, quantity: SpecQuantity, preferred: SpecUnit): Pair<String, SpecUnit>? {
        val m = NUMBER_THEN_TEXT.matchEntire(text) ?: return null
        val unit = unitForAlias(m.groupValues[2], quantity, preferred) ?: return null
        return m.groupValues[1] to unit
    }

    /**
     * Whether free text already names a unit of [quantity] as a word of its
     * own ("2 x 50 m"), so the screen does not print a second one after it.
     */
    fun mentionsUnit(text: String, quantity: SpecQuantity): Boolean {
        val words = text.lowercase().split(Regex("""[\s\d.,x×*+\-()]+""")).filter { it.isNotEmpty() }
        return words.any { w -> unitForAlias(w, quantity, SpecUnit.entries.first { it.quantity == quantity }) != null }
    }

    /**
     * At most two decimals, none when they are zero, and the decimal mark of
     * [locale]: 40 l shown in US gallons reads "10.57", in Ukrainian "10,57".
     */
    fun formatNumber(value: Double, locale: Locale): String {
        val rounded = round(value * 100) / 100
        if (abs(rounded - round(rounded)) < 1e-9) return round(rounded).toLong().toString()
        val text = String.format(locale, "%.2f", rounded).trimEnd('0')
        return text.trimEnd('.', ',')
    }

    /**
     * The value to put in the form when it was stored in [from] and Settings
     * now says [to]. Null when it is not a number and cannot be converted:
     * the form then shows the text as it is, with the old unit beside it.
     */
    fun convertForEdit(value: String, from: SpecUnit, to: SpecUnit, locale: Locale): String? {
        if (from == to) return value
        val n = parseNumber(value) ?: return null
        return formatNumber(from.convert(n, to), locale)
    }

    /** "Обʼєм (л)" -> "Обʼєм" for a hint in [LABEL_UNIT]; any other label is returned as it is. */
    fun stripLabelUnit(hint: String?, label: String): String {
        if (hint == null || hint !in LABEL_UNIT) return label
        return label.replace(TRAILING_BRACKETS, "").ifBlank { label }
    }

    /**
     * What one save writes: the text, and the unit it is in. [shown] is the
     * unit the form displayed, read from Settings when it opened. A unit the
     * owner typed after the number wins over it; a blank value has no unit.
     */
    fun normalizeInput(raw: String, quantity: SpecQuantity?, shown: SpecUnit?): Pair<String, SpecUnit?> {
        val text = raw.trim()
        if (quantity == null || shown == null || text.isEmpty()) return text to null
        val explicit = splitExplicitUnit(text, quantity, shown)
        return if (explicit != null) explicit.first to explicit.second else text to shown
    }

    // ------------------------------------------------------------ specUnitsJson

    fun readUnits(json: String?): Map<String, SpecUnit> {
        if (json.isNullOrBlank()) return emptyMap()
        val obj = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return emptyMap()
        return obj.mapNotNull { (k, v) ->
            val name = (v as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            SpecUnit.entries.firstOrNull { it.name == name }?.let { k to it }
        }.toMap()
    }

    fun writeUnits(units: Map<String, SpecUnit>): String? =
        if (units.isEmpty()) null
        else JsonObject(units.mapValues { JsonPrimitive(it.value.name) }).toString()

    /**
     * Gives every measured value that has no unit yet the one it was typed
     * in, in this order:
     *   1. a unit written after the number ("40 gal") - then cut from the
     *      text, since the screen prints the unit itself;
     *   2. the unit the field's label named ("Capacity (L)");
     *   3. what Settings holds now - the unit the screen implied.
     *
     * [hintOf] maps a specJson key to its catalog hint: the key itself, or
     * for values saved under a translated label, the hint behind it.
     *
     * Returns the new (specJson, specUnitsJson), or null when nothing changes
     * - so it can run on every open without rewriting a row.
     */
    fun backfill(
        specJson: String?,
        specUnitsJson: String?,
        settings: UnitSettings,
        hintOf: (String) -> String?,
    ): Pair<String, String?>? {
        if (specJson.isNullOrBlank()) return null
        val spec = runCatching { Json.parseToJsonElement(specJson).jsonObject }.getOrNull() ?: return null
        val units = readUnits(specUnitsJson).toMutableMap()
        val values = spec.toMutableMap()
        var changed = false
        for ((key, element) in spec) {
            if (key in units) continue
            val value = (element as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            if (value.isEmpty()) continue
            val hint = hintOf(key) ?: continue
            val quantity = quantityOf(hint) ?: continue
            val current = settings.unitFor(quantity)
            val explicit = splitExplicitUnit(value, quantity, current)
            if (explicit != null) {
                values[key] = JsonPrimitive(explicit.first)
                units[key] = explicit.second
            } else {
                units[key] = LABEL_UNIT[hint] ?: current
            }
            changed = true
        }
        if (!changed) return null
        return JsonObject(values).toString() to writeUnits(units)
    }
}
