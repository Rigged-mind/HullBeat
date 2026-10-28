package app.hullbeat.domain

import app.hullbeat.data.db.ServiceSchedule

/**
 * Reading a service interval the owner typed over the catalog's default.
 *
 * catalog/README.md promised every default interval is editable, and the
 * "verify" note told the owner to follow the manual - with nowhere to write
 * what the manual says. This is the rule for what may be written.
 *
 * Each field is optional: blank means that limit does not apply. Zero is not
 * "no limit" - DueCalculator would read it as due on the day of the service -
 * so it is refused, and the owner clears the field instead. Days are whole;
 * hours and miles take a decimal, with either separator ([DecimalInput]).
 * At least one limit must remain: a schedule with none is never due, and an
 * interval that quietly vanished is worse than the guess it replaced.
 */
object IntervalInput {

    enum class Field { DAYS, HOURS, MILES }

    sealed interface Result {
        data class Valid(val days: Int?, val hours: Double?, val miles: Double?) : Result
        /** Fields that hold something that is not a usable interval. */
        data class Invalid(val fields: Set<Field>) : Result
        /** Every field blank. */
        data object Empty : Result
    }

    /** Longest interval accepted, so a stray digit cannot push a date past LocalDate. */
    const val MAX_DAYS = 36_500

    fun parse(days: String, hours: String, miles: String): Result {
        val bad = mutableSetOf<Field>()
        val d = if (days.isBlank()) null else parseDays(days).also { if (it == null) bad += Field.DAYS }
        val h = if (hours.isBlank()) null else parsePositive(hours).also { if (it == null) bad += Field.HOURS }
        val m = if (miles.isBlank()) null else parsePositive(miles).also { if (it == null) bad += Field.MILES }
        return when {
            bad.isNotEmpty() -> Result.Invalid(bad)
            d == null && h == null && m == null -> Result.Empty
            else -> Result.Valid(d, h, m)
        }
    }

    /**
     * The schedule with the owner's interval. Saving is the owner's own
     * figure, so the "check against the manual" flag goes with it.
     */
    fun apply(schedule: ServiceSchedule, valid: Result.Valid): ServiceSchedule =
        schedule.copy(
            intervalDays = valid.days,
            intervalHours = valid.hours,
            intervalMiles = valid.miles,
            needsVerification = false,
        )

    /** A schedule worth an interval editor: one that counts from the last service. */
    fun isEditable(schedule: ServiceSchedule): Boolean =
        schedule.intervalDays != null || schedule.intervalHours != null || schedule.intervalMiles != null

    /** How a stored value goes back into a field: 200.0 as "200", not "200.0". */
    fun format(value: Double?): String =
        when {
            value == null -> ""
            value % 1.0 == 0.0 -> value.toLong().toString()
            else -> value.toString()
        }

    private fun parseDays(raw: String): Int? {
        val v = DecimalInput.parse(raw) ?: return null
        if (v % 1.0 != 0.0 || v < 1 || v > MAX_DAYS) return null
        return v.toInt()
    }

    private fun parsePositive(raw: String): Double? =
        DecimalInput.parse(raw)?.takeIf { it > 0.0 }
}
