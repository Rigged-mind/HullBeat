package app.hullbeat.data.preferences

import android.content.Context
import android.content.SharedPreferences
import app.hullbeat.R
import app.hullbeat.domain.UnitSettings
import app.hullbeat.ui.theme.Scheme
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameRes: Int,
)

enum class LengthUnit(val symbol: String, val nameRes: Int, val shortNameRes: Int) {
    METRES("m", R.string.unit_metres, R.string.unit_short_m),
    FEET("ft", R.string.unit_feet, R.string.unit_short_ft);

    fun fromMetres(metres: Double): Double = when (this) {
        METRES -> metres
        FEET -> metres * 3.28084
    }

    fun toMetres(value: Double): Double = when (this) {
        METRES -> value
        FEET -> value / 3.28084
    }
}

enum class DistanceUnit(val symbol: String, val nameRes: Int, val shortNameRes: Int) {
    NAUTICAL_MILES("NM", R.string.unit_nautical_miles, R.string.unit_short_nm),
    KILOMETRES("km", R.string.unit_kilometres, R.string.unit_short_km),
    STATUTE_MILES("mi", R.string.unit_statute_miles, R.string.unit_short_mi);

    fun fromNauticalMiles(nm: Double): Double = when (this) {
        NAUTICAL_MILES -> nm
        KILOMETRES -> nm * 1.852
        STATUTE_MILES -> nm * 1.15078
    }
}

enum class VolumeUnit(val symbol: String, val nameRes: Int, val shortNameRes: Int) {
    LITRES("L", R.string.unit_litres, R.string.unit_short_l),
    GALLONS_US("US gal", R.string.unit_gallons_us, R.string.unit_short_gal_us),
    GALLONS_UK("UK gal", R.string.unit_gallons_uk, R.string.unit_short_gal_uk);

    fun fromLitres(litres: Double): Double = when (this) {
        LITRES -> litres
        GALLONS_US -> litres * 0.264172
        GALLONS_UK -> litres * 0.219969
    }
}

enum class PressureUnit(val symbol: String, val nameRes: Int, val shortNameRes: Int) {
    BAR("bar", R.string.unit_bar, R.string.unit_short_bar),
    PSI("psi", R.string.unit_psi, R.string.unit_short_psi);

    fun fromBar(bar: Double): Double = when (this) {
        BAR -> bar
        PSI -> bar * 14.5038
    }
}

enum class TemperatureUnit(val symbol: String, val nameRes: Int, val shortNameRes: Int) {
    CELSIUS("°C", R.string.unit_celsius, R.string.unit_short_celsius),
    FAHRENHEIT("°F", R.string.unit_fahrenheit, R.string.unit_short_fahrenheit);

    fun fromCelsius(celsius: Double): Double = when (this) {
        CELSIUS -> celsius
        FAHRENHEIT -> (celsius * 9.0 / 5.0) + 32.0
    }
}

object AppPreferences {
    private const val PREFS_NAME = "hullbeat_prefs"
    private const val KEY_CURRENCY = "currency_symbol"
    private const val KEY_CURRENCY_CODE = "currency_code"
    private const val KEY_LENGTH_UNIT = "unit_length"
    private const val KEY_DISTANCE_UNIT = "unit_distance"
    private const val KEY_VOLUME_UNIT = "unit_volume"
    private const val KEY_PRESSURE_UNIT = "unit_pressure"
    private const val KEY_TEMPERATURE_UNIT = "unit_temperature"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_ADVANCE_REMINDER_DAYS = "advance_reminder_days"
    private const val KEY_BOAT_COLLAPSED_CATEGORIES = "boat_collapsed_categories"

    const val DEFAULT_CURRENCY = "₴"
    val DEFAULT_LENGTH_UNIT = LengthUnit.METRES
    val DEFAULT_DISTANCE_UNIT = DistanceUnit.NAUTICAL_MILES
    val DEFAULT_VOLUME_UNIT = VolumeUnit.LITRES
    val DEFAULT_PRESSURE_UNIT = PressureUnit.BAR
    val DEFAULT_TEMPERATURE_UNIT = TemperatureUnit.CELSIUS
    const val DEFAULT_NOTIFICATIONS_ENABLED = true
    const val DEFAULT_ADVANCE_REMINDER_DAYS = 14

    val AVAILABLE_CURRENCIES = listOf(
        CurrencyOption("UAH", "₴", R.string.currency_uah),
        CurrencyOption("USD", "$", R.string.currency_usd),
        CurrencyOption("EUR", "€", R.string.currency_eur),
        CurrencyOption("GBP", "£", R.string.currency_gbp),
        CurrencyOption("PLN", "zł", R.string.currency_pln),
        CurrencyOption("SEK", "kr", R.string.currency_sek),
        CurrencyOption("NOK", "kr", R.string.currency_nok),
        CurrencyOption("TRY", "₺", R.string.currency_try),
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Currency
    fun getCurrency(context: Context): String =
        prefs(context).getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    /**
     * [code] is the ISO code of a listed currency, or null for a custom one.
     * It is kept next to the symbol because the symbol alone cannot tell
     * SEK from NOK: both are "kr".
     */
    fun setCurrency(context: Context, symbol: String, code: String? = null) {
        val clean = symbol.trim().ifEmpty { DEFAULT_CURRENCY }
        prefs(context).edit()
            .putString(KEY_CURRENCY, clean)
            .apply { if (code != null) putString(KEY_CURRENCY_CODE, code) else remove(KEY_CURRENCY_CODE) }
            .apply()
    }

    /**
     * What a new cost is stored with in [app.hullbeat.data.db.ServiceRecord.currency]:
     * the ISO code of a listed currency, or the owner's own text for a custom one.
     */
    fun getCurrencyCode(context: Context): String {
        val p = prefs(context)
        return p.getString(KEY_CURRENCY_CODE, null)
            ?: currencyCodeFor(p.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY)
    }

    /**
     * The code for a symbol saved before the code was. A symbol two listed
     * currencies share ("kr") stays as text: guessing SEK would put the
     * wrong country on a Norwegian owner's history.
     */
    fun currencyCodeFor(symbol: String): String {
        val matches = AVAILABLE_CURRENCIES.filter { it.symbol == symbol }
        return matches.singleOrNull()?.code ?: symbol
    }

    /** The symbol to print after an amount stored with [currency]. */
    fun currencySymbol(currency: String?): String =
        AVAILABLE_CURRENCIES.firstOrNull { it.code == currency }?.symbol ?: currency.orEmpty()

    // Units
    fun getLengthUnit(context: Context): LengthUnit =
        runCatching {
            LengthUnit.valueOf(prefs(context).getString(KEY_LENGTH_UNIT, DEFAULT_LENGTH_UNIT.name) ?: DEFAULT_LENGTH_UNIT.name)
        }.getOrDefault(DEFAULT_LENGTH_UNIT)

    fun setLengthUnit(context: Context, unit: LengthUnit) {
        prefs(context).edit().putString(KEY_LENGTH_UNIT, unit.name).apply()
    }

    fun getDistanceUnit(context: Context): DistanceUnit =
        runCatching {
            DistanceUnit.valueOf(prefs(context).getString(KEY_DISTANCE_UNIT, DEFAULT_DISTANCE_UNIT.name) ?: DEFAULT_DISTANCE_UNIT.name)
        }.getOrDefault(DEFAULT_DISTANCE_UNIT)

    fun setDistanceUnit(context: Context, unit: DistanceUnit) {
        prefs(context).edit().putString(KEY_DISTANCE_UNIT, unit.name).apply()
    }

    fun getVolumeUnit(context: Context): VolumeUnit =
        runCatching {
            VolumeUnit.valueOf(prefs(context).getString(KEY_VOLUME_UNIT, DEFAULT_VOLUME_UNIT.name) ?: DEFAULT_VOLUME_UNIT.name)
        }.getOrDefault(DEFAULT_VOLUME_UNIT)

    fun setVolumeUnit(context: Context, unit: VolumeUnit) {
        prefs(context).edit().putString(KEY_VOLUME_UNIT, unit.name).apply()
    }

    fun getPressureUnit(context: Context): PressureUnit =
        runCatching {
            PressureUnit.valueOf(prefs(context).getString(KEY_PRESSURE_UNIT, DEFAULT_PRESSURE_UNIT.name) ?: DEFAULT_PRESSURE_UNIT.name)
        }.getOrDefault(DEFAULT_PRESSURE_UNIT)

    fun setPressureUnit(context: Context, unit: PressureUnit) {
        prefs(context).edit().putString(KEY_PRESSURE_UNIT, unit.name).apply()
    }

    fun getTemperatureUnit(context: Context): TemperatureUnit =
        runCatching {
            TemperatureUnit.valueOf(prefs(context).getString(KEY_TEMPERATURE_UNIT, DEFAULT_TEMPERATURE_UNIT.name) ?: DEFAULT_TEMPERATURE_UNIT.name)
        }.getOrDefault(DEFAULT_TEMPERATURE_UNIT)

    fun setTemperatureUnit(context: Context, unit: TemperatureUnit) {
        prefs(context).edit().putString(KEY_TEMPERATURE_UNIT, unit.name).apply()
    }

    /** The units Settings holds right now, as one value a passport save keeps. */
    fun getUnitSettings(context: Context): UnitSettings = UnitSettings(
        length = getLengthUnit(context),
        volume = getVolumeUnit(context),
        pressure = getPressureUnit(context),
        temperature = getTemperatureUnit(context),
    )

    // Presets
    fun applyMetricPreset(context: Context) {
        prefs(context).edit()
            .putString(KEY_LENGTH_UNIT, LengthUnit.METRES.name)
            .putString(KEY_DISTANCE_UNIT, DistanceUnit.NAUTICAL_MILES.name)
            .putString(KEY_VOLUME_UNIT, VolumeUnit.LITRES.name)
            .putString(KEY_PRESSURE_UNIT, PressureUnit.BAR.name)
            .putString(KEY_TEMPERATURE_UNIT, TemperatureUnit.CELSIUS.name)
            .apply()
    }

    fun applyNauticalImperialPreset(context: Context) {
        prefs(context).edit()
            .putString(KEY_LENGTH_UNIT, LengthUnit.FEET.name)
            .putString(KEY_DISTANCE_UNIT, DistanceUnit.NAUTICAL_MILES.name)
            .putString(KEY_VOLUME_UNIT, VolumeUnit.GALLONS_US.name)
            .putString(KEY_PRESSURE_UNIT, PressureUnit.PSI.name)
            .putString(KEY_TEMPERATURE_UNIT, TemperatureUnit.CELSIUS.name)
            .apply()
    }

    // Notifications
    fun isNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getAdvanceReminderDays(context: Context): Int =
        prefs(context).getInt(KEY_ADVANCE_REMINDER_DAYS, DEFAULT_ADVANCE_REMINDER_DAYS)

    fun setAdvanceReminderDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_ADVANCE_REMINDER_DAYS, days).apply()
    }

    // Theme scheme
    private const val KEY_THEME_SCHEME = "theme_scheme"
    val DEFAULT_THEME_SCHEME = Scheme.SYSTEM

    fun getThemeScheme(context: Context): Scheme {
        val name = prefs(context).getString(KEY_THEME_SCHEME, DEFAULT_THEME_SCHEME.name)
        return try {
            Scheme.valueOf(name ?: DEFAULT_THEME_SCHEME.name)
        } catch (_: Exception) {
            DEFAULT_THEME_SCHEME
        }
    }

    fun setThemeScheme(context: Context, scheme: Scheme) {
        prefs(context).edit().putString(KEY_THEME_SCHEME, scheme.name).apply()
    }

    // Selected vessel
    private const val KEY_SELECTED_VESSEL_ID = "selected_vessel_id"

    fun getSelectedVesselId(context: Context): Long? {
        val id = prefs(context).getLong(KEY_SELECTED_VESSEL_ID, -1L)
        return if (id > 0L) id else null
    }

    fun setSelectedVesselId(context: Context, vesselId: Long?) {
        if (vesselId != null && vesselId > 0L) {
            prefs(context).edit().putLong(KEY_SELECTED_VESSEL_ID, vesselId).apply()
        } else {
            prefs(context).edit().remove(KEY_SELECTED_VESSEL_ID).apply()
        }
    }

    // Collapsed categories on Boat screen
    fun getCollapsedCategories(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_BOAT_COLLAPSED_CATEGORIES, emptySet())?.toSet() ?: emptySet()
    }

    fun setCollapsedCategories(context: Context, categories: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BOAT_COLLAPSED_CATEGORIES, categories).apply()
    }

    fun expandCategory(context: Context, categoryCode: String?) {
        if (categoryCode.isNullOrBlank()) return
        val current = getCollapsedCategories(context)
        if (categoryCode in current) {
            setCollapsedCategories(context, current - categoryCode)
        }
    }

    // Formatters
    fun formatCost(amount: Double?, currency: String): String {
        if (amount == null) return ""
        val formatted = if (amount % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%,d", amount.toLong()).replace(',', ' ')
        } else {
            String.format(Locale.getDefault(), "%,.2f", amount).replace(',', ' ')
        }
        return "$formatted $currency"
    }

    fun formatLength(metres: Double?, unit: LengthUnit): String {
        if (metres == null) return ""
        val converted = unit.fromMetres(metres)
        val formatted = if (converted % 1.0 == 0.0) {
            converted.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", converted)
        }
        return "$formatted ${unit.symbol}"
    }

    fun formatVolume(litres: Double?, unit: VolumeUnit): String {
        if (litres == null) return ""
        val converted = unit.fromLitres(litres)
        val formatted = if (converted % 1.0 == 0.0) {
            converted.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", converted)
        }
        return "$formatted ${unit.symbol}"
    }
}
