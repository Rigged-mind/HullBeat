package app.hullbeat.domain

import app.hullbeat.data.db.DatePrecision
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Works out when a component next falls due, and - the part nobody else does -
 * *predicts the date* rather than only reporting that something is overdue.
 *
 * A Google Sheets user named the gap himself: "Not very good for predictive
 * maintenance, but I can very easily find when the last service took place."
 * A spreadsheet answers "when did I", never "when will I". This class is the
 * answer to the second question, so it carries wedge #1 of the whole product.
 *
 * Pure Kotlin on purpose: no Android imports, so it is unit-testable on the JVM.
 */
object DueCalculator {

    /** A component is amber this far ahead of its date. */
    const val SOON_WINDOW_DAYS = 30L

    /** ...or when this share of the hour interval is left. */
    const val SOON_FRACTION_OF_INTERVAL = 0.10

    /** Never call something "soon" on fewer hours than this, however long the interval. */
    const val SOON_MIN_HOURS = 10.0

    /** Readings closer together than this say nothing useful about a wear rate. */
    const val MIN_TREND_SPAN_DAYS = 14L

    /** Prefer recent behaviour: a boat used hard three years ago is not evidence. */
    const val TREND_WINDOW_DAYS = 365L

    enum class State {
        /** Past the threshold on at least one dimension. */
        OVERDUE,

        /** Inside the warning window. */
        DUE_SOON,

        /** Known date, comfortably ahead. */
        UPCOMING,

        /**
         * Overdue or due, but the owner has consciously put it off to a date.
         * Shown calmly, never red, and never as "done" - see
         * docs/spreadsheet-references.md p.1.4.
         */
        DEFERRED,

        /** Hour-driven, but the boat is not being used - no date can be honest. */
        DORMANT,

        /** Not enough information to say anything. Never guess here. */
        UNKNOWN,
    }

    /** Which dimension runs out first. Drives the "why" line on the card. */
    enum class Driver { CALENDAR, HOURS, MILES, EXPIRY, NONE }

    /**
     * How much the predicted date can be trusted. Surfaced in the UI as wording,
     * not as a number: "around mid-August" reads honestly, "14 Aug" does not when
     * it rests on two readings a fortnight apart.
     */
    enum class Confidence { NONE, LOW, MEDIUM, HIGH }

    data class Reading(
        val date: LocalDate,
        val value: Double,
        val precision: DatePrecision = DatePrecision.DAY,
    )

    data class Input(
        val today: LocalDate,
        val intervalDays: Int? = null,
        val intervalHours: Double? = null,
        val intervalMiles: Double? = null,
        /** Printed expiry from a certificate or stamp; overrides the interval. */
        val expiresOn: LocalDate? = null,
        val lastServiceDate: LocalDate? = null,
        /** Meter value at the moment of that service. */
        val lastServiceMeter: Double? = null,
        /** Whole reading history for this meter, any order. */
        val readings: List<Reading> = emptyList(),
        /**
         * Owner has deliberately put this off until the given date. A deferral
         * in the past has lapsed and is ignored.
         */
        val deferredUntil: LocalDate? = null,
        /** Per-schedule warning window; null falls back to the constants above. */
        val soonWindowDays: Long? = null,
        val soonWindowHours: Double? = null,
        /**
         * Months the boat is afloat, inclusive, wrapping for the southern
         * hemisphere. Both null means year-round, which is the behaviour this
         * class had before seasons existed.
         */
        val seasonStartMonth: Int? = null,
        val seasonEndMonth: Int? = null,
    )

    data class Result(
        val state: State,
        /** Actual date when known, predicted date when derived from the trend. */
        val dueDate: LocalDate?,
        val isPredicted: Boolean,
        val daysRemaining: Long?,
        val unitsRemaining: Double?,
        val driver: Driver,
        /** Units per day, from the recent trend. Null when unknown. */
        val wearRatePerDay: Double?,
        val confidence: Confidence,
    )

    fun evaluate(input: Input): Result {
        val trend = wearRate(input, input.readings, input.today)
        val current = currentMeter(input.readings)

        val candidates = buildList {
            calendarDue(input)?.let { add(it) }
            expiryDue(input)?.let { add(it) }
            usageDue(input, current, trend, Driver.HOURS, input.intervalHours)?.let { add(it) }
            usageDue(input, current, trend, Driver.MILES, input.intervalMiles)?.let { add(it) }
        }

        if (candidates.isEmpty()) {
            return Result(
                state = State.UNKNOWN,
                dueDate = null,
                isPredicted = false,
                daysRemaining = null,
                unitsRemaining = null,
                driver = Driver.NONE,
                wearRatePerDay = trend?.ratePerDay,
                confidence = Confidence.NONE,
            )
        }

        // Whichever comes first wins. This is the "hours or months, whichever
        // comes first" rule, generalised to three dimensions.
        val winner = candidates.minWith(
            compareBy(
                { urgency(it, input) },
                { it.daysRemaining ?: Long.MAX_VALUE },
                { it.driver.ordinal },
            )
        )

        val days = winner.daysRemaining
        val idle = trend != null && trend.ratePerDay == 0.0
        val usageDriven = winner.driver == Driver.HOURS || winner.driver == Driver.MILES

        // A live deferral outranks everything: the owner already knows, and has
        // decided. Keep reporting the real date underneath so the card can say
        // "37 months overdue, put off until October" rather than hiding it.
        val deferred = input.deferredUntil?.let { !it.isBefore(input.today) } == true

        val state = when {
            deferred -> State.DEFERRED
            // A usage-driven item on a boat that is not moving is not "unknown":
            // it genuinely is not coming due, and saying so beats an empty dash.
            days == null && idle && usageDriven -> State.DORMANT
            // No date, but a number we do know: hours left. Amber with no date
            // beats UNKNOWN, which is what this used to report three hours out.
            days == null && winner.soonInUnits -> State.DUE_SOON
            days == null -> State.UNKNOWN
            days < 0 -> State.OVERDUE
            isSoon(winner, input) -> State.DUE_SOON
            else -> State.UPCOMING
        }

        val confidence = when {
            winner.predicted -> trend?.confidence ?: Confidence.NONE
            // A real date is certain. No date at all is not "high confidence".
            winner.date != null -> Confidence.HIGH
            else -> Confidence.NONE
        }

        return Result(
            state = state,
            dueDate = winner.date,
            isPredicted = winner.predicted,
            daysRemaining = days,
            unitsRemaining = winner.unitsRemaining,
            driver = winner.driver,
            wearRatePerDay = trend?.ratePerDay,
            confidence = confidence,
        )
    }

    // ------------------------------------------------------------ internals

    private data class Candidate(
        val driver: Driver,
        val date: LocalDate?,
        val daysRemaining: Long?,
        val unitsRemaining: Double?,
        val predicted: Boolean,
        /**
         * Close in its own unit, decided without reference to days. This is
         * what lets three remaining engine hours be urgent on a boat whose
         * wear trend is not usable yet, and which therefore has no date.
         */
        val soonInUnits: Boolean = false,
    )

    internal data class Trend(val ratePerDay: Double, val confidence: Confidence)

    private fun calendarDue(input: Input): Candidate? {
        val interval = input.intervalDays ?: return null
        val last = input.lastServiceDate ?: return null
        val due = last.plusDays(interval.toLong())
        return Candidate(
            driver = Driver.CALENDAR,
            date = due,
            daysRemaining = ChronoUnit.DAYS.between(input.today, due),
            unitsRemaining = null,
            predicted = false,
        )
    }

    private fun expiryDue(input: Input): Candidate? {
        val expires = input.expiresOn ?: return null
        return Candidate(
            driver = Driver.EXPIRY,
            date = expires,
            daysRemaining = ChronoUnit.DAYS.between(input.today, expires),
            unitsRemaining = null,
            predicted = false,
        )
    }

    private fun usageDue(
        input: Input,
        current: Double?,
        trend: Trend?,
        driver: Driver,
        interval: Double?,
    ): Candidate? {
        if (interval == null) return null
        val base = input.lastServiceMeter ?: return null
        if (current == null) return null

        // A meter only counts up, so a last-service reading ABOVE the current
        // one means the two are not on the same scale: the gauge was replaced
        // (old one stopped at 1850, the new one starts from zero) or the figure
        // was mistyped. Carrying on regardless reported "2060 hours remaining,
        // 4414 days" and hid the oil change for twelve years.
        //
        // Detecting a reset in the reading series (usableSegment) is a separate
        // thing and already worked: the trend stayed clean. What was missing is
        // that lastServiceMeter belongs to the *old* gauge. Refuse to guess -
        // with a calendar interval the item falls back to it, and without one
        // it reports UNKNOWN, which at least asks the owner a question.
        if (base > current) return null

        val remaining = (base + interval) - current

        // Already past the threshold: report it without needing a trend at all.
        if (remaining <= 0) {
            return Candidate(driver, input.today, -1L, remaining, predicted = false)
        }
        // Ashore, nothing is consuming these hours, so "ten left" is not a
        // warning - it is a fact about next spring. The predicted date turns
        // amber on its own as launch approaches, which is when the owner can
        // actually act on it.
        val soon = unitsSoon(driver, remaining, input) && inSeason(input, input.today)
        // No usable trend means no honest date. Report the gap, not a guess -
        // but carry the fact that the gap is small, or it gets lost.
        val rate = trend?.ratePerDay?.takeIf { it > 0 }
            ?: return Candidate(driver, null, null, remaining, false, soon)

        val sailingDays = (remaining / rate).roundToLong()
        val due = addSeasonDays(input, input.today, sailingDays)
            ?: return Candidate(driver, null, null, remaining, false, soon)
        return Candidate(
            driver = driver,
            date = due,
            daysRemaining = ChronoUnit.DAYS.between(input.today, due),
            unitsRemaining = remaining,
            predicted = true,
            soonInUnits = soon,
        )
    }

    /** Inside the warning window measured in hours or miles, not in days. */
    private fun unitsSoon(driver: Driver, remaining: Double, input: Input): Boolean {
        input.soonWindowHours?.let { return remaining <= it }
        val interval = when (driver) {
            Driver.HOURS -> input.intervalHours
            Driver.MILES -> input.intervalMiles
            else -> null
        } ?: return false
        return remaining <= max(SOON_MIN_HOURS, interval * SOON_FRACTION_OF_INTERVAL)
    }

    /**
     * The warning window is per schedule, not one constant for the whole app.
     * Dirona allows 50 hours of notice on the main engine and 15 on the wing,
     * because they wear at completely different rates.
     */
    private fun isSoon(c: Candidate, input: Input): Boolean {
        if (c.soonInUnits) return true
        val days = c.daysRemaining ?: return false
        return days <= (input.soonWindowDays ?: SOON_WINDOW_DAYS)
    }

    /**
     * Ranks candidates before their days are compared, so that a schedule
     * which is close but dateless outranks one that is dated but distant.
     * Without this, ordering on days alone made a dateless candidate sort as
     * +infinity: three engine hours from due always lost to a calendar item
     * nine months out, and the hours vanished from the result entirely.
     */
    private fun urgency(c: Candidate, input: Input): Int = when {
        (c.daysRemaining ?: 0L) < 0L -> 0
        isSoon(c, input) -> 1
        else -> 2
    }

    private fun currentMeter(readings: List<Reading>): Double? =
        usableSegment(readings)?.lastOrNull()?.value

    // ------------------------------------------------------- seasons
    //
    // A boat ashore consumes no engine hours. Extrapolating straight through a
    // lay-up put the oil change in December, with the engine drained and full
    // of antifreeze - wedge #1 silently wrong for half of every year.
    //
    // One rule covers both cases the naive version got wrong, and needs no
    // branch for either: PROJECT BY CONSUMING SAILING DAYS ONLY. Ashore in
    // January, two sailing days of oil left lands in early April on its own;
    // afloat with a resource that outlasts the season, the lay-up gap appears
    // on its own too.

    /** Never chase a prediction further than this; beyond it, say nothing. */
    private const val MAX_PROJECTION_DAYS = 365L * 20

    private fun inSeason(input: Input, day: LocalDate): Boolean {
        val a = input.seasonStartMonth ?: return true
        val b = input.seasonEndMonth ?: return true
        val m = day.monthValue
        return if (a <= b) m in a..b else m >= a || m <= b
    }

    /** Sailing days in [start, end). This is what a wear rate divides by. */
    private fun seasonDaysBetween(input: Input, start: LocalDate, end: LocalDate): Long {
        if (input.seasonStartMonth == null || input.seasonEndMonth == null) {
            return ChronoUnit.DAYS.between(start, end)
        }
        var day = start
        var count = 0L
        while (day.isBefore(end)) {
            if (inSeason(input, day)) count++
            day = day.plusDays(1)
        }
        return count
    }

    /** The date [n] sailing days from [start], skipping every lay-up day. */
    private fun addSeasonDays(input: Input, start: LocalDate, n: Long): LocalDate? {
        if (input.seasonStartMonth == null || input.seasonEndMonth == null) {
            return start.plusDays(n)
        }
        var day = start
        var left = n
        var guard = 0L
        while (left > 0 && guard < MAX_PROJECTION_DAYS) {
            day = day.plusDays(1)
            guard++
            if (inSeason(input, day)) left--
        }
        return if (left == 0L) day else null
    }

    /**
     * Wear rate from the recent trend.
     *
     * Fuzzy dates are dropped: an owner backfilling "spring 2019" from memory
     * must not be allowed to bend the slope. Meter resets (a replaced engine or
     * a rolled-over gauge) split the series and only the newest run is used.
     */
    internal fun wearRate(input: Input, readings: List<Reading>, today: LocalDate): Trend? {
        val segment = usableSegment(readings) ?: return null
        if (segment.size < 2) return null

        val windowStart = today.minusDays(TREND_WINDOW_DAYS)
        val recent = segment.filter { !it.date.isBefore(windowStart) }
        // Fall back to the whole segment for a boat used a handful of times a year.
        val series = if (recent.size >= 2) recent else segment

        val first = series.first()
        val last = series.last()
        val span = ChronoUnit.DAYS.between(first.date, last.date)
        if (span < MIN_TREND_SPAN_DAYS) return null

        // Sailing days, not calendar days. A series spanning one lay-up divides
        // by 365 when only ~210 days were sailing, halving the apparent rate
        // and pushing every prediction roughly twice as far out.
        val sailing = maxOf(seasonDaysBetween(input, first.date, last.date), 1L)
        val rate = (last.value - first.value) / sailing
        if (rate < 0) return null

        val confidence = when {
            series.size >= 4 && span >= 90 -> Confidence.HIGH
            series.size >= 3 && span >= 30 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        return Trend(rate, confidence)
    }

    /**
     * Sorted, precise-enough readings, cut at the last meter reset.
     * A drop larger than rounding noise means a new meter, not usage.
     */
    private fun usableSegment(readings: List<Reading>): List<Reading>? {
        val precise = readings
            .filter { it.precision == DatePrecision.DAY || it.precision == DatePrecision.MONTH }
            .sortedBy { it.date }
        if (precise.isEmpty()) return null

        var start = 0
        for (i in 1 until precise.size) {
            if (precise[i].value < precise[i - 1].value - EPSILON) start = i
        }
        return precise.subList(start, precise.size)
    }

    private const val EPSILON = 0.001

    /** True when two rates differ enough to be worth re-notifying about. */
    internal fun materiallyDifferent(a: Double?, b: Double?): Boolean {
        if (a == null || b == null) return a != b
        return abs(a - b) > EPSILON
    }
}
