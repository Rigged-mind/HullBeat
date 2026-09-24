package app.hullbeat.domain

import app.hullbeat.data.db.DatePrecision
import app.hullbeat.domain.DueCalculator.Confidence
import app.hullbeat.domain.DueCalculator.Driver
import app.hullbeat.domain.DueCalculator.Input
import app.hullbeat.domain.DueCalculator.Reading
import app.hullbeat.domain.DueCalculator.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * These cases were first run as tools/prototype_due.py, which is where three
 * real bugs turned up: DORMANT could never fire, a missing date reported
 * itself as HIGH confidence, and a schedule three hours from due reported
 * UNKNOWN because it had no date to sort on. Keep the two in step.
 */
class DueCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 5)
    private fun d(offset: Long): LocalDate = today.plusDays(offset)

    // ------------------------------------------- meter replaced mid-life
    //
    // Oil changed at 1850 h on the old tachometer; the gauge was then replaced
    // and the new one now reads 40. Detecting the reset in the series already
    // worked - the trend stayed clean. What did not was that lastServiceMeter
    // belongs to the old gauge, so this reported "2060 hours remaining, 4414
    // days" and hid the service for twelve years.
    private val afterMeterSwap = listOf(
        Reading(d(-400), 1700.0),
        Reading(d(-300), 1850.0),
        Reading(d(-60), 12.0),
        Reading(today, 40.0),
    )

    @Test
    fun `meter replaced means the hour dimension cannot be computed`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0,
                lastServiceMeter = 1850.0, readings = afterMeterSwap,
            )
        )
        // Honest silence beats a confident wrong number.
        assertEquals(State.UNKNOWN, r.state)
        assertEquals(Driver.NONE, r.driver)
        assertNull(r.dueDate)
    }

    @Test
    fun `meter replaced still leaves the calendar carrying the item`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today,
                intervalDays = 365, lastServiceDate = d(-300),
                intervalHours = 250.0, lastServiceMeter = 1850.0,
                readings = afterMeterSwap,
            )
        )
        assertEquals(State.UPCOMING, r.state)
        assertEquals(Driver.CALENDAR, r.driver)
        assertEquals(65L, r.daysRemaining)
    }

    // ------------------------------------------------- seasonal haul-out
    //
    // Season 4..10 means afloat April through October. 145 hours over 141
    // sailing days gives 1.028 h/day; five hours left is five sailing days.
    private val nearlyOut = listOf(
        Reading(LocalDate.of(2026, 6, 1), 100.0),
        Reading(LocalDate.of(2026, 8, 1), 200.0),
        Reading(LocalDate.of(2026, 10, 20), 245.0),
    )

    @Test
    fun `ashore in January, the prediction lands after launch`() {
        val r = DueCalculator.evaluate(
            Input(
                today = LocalDate.of(2027, 1, 15),
                intervalHours = 500.0, lastServiceMeter = 0.0,
                readings = listOf(
                    Reading(LocalDate.of(2026, 6, 1), 100.0),
                    Reading(LocalDate.of(2026, 9, 1), 250.0),
                ),
                seasonStartMonth = 4, seasonEndMonth = 10,
            )
        )
        // Extrapolating through the lay-up put this in the middle of winter,
        // with the engine drained and full of antifreeze.
        assertEquals(State.UPCOMING, r.state)
        assertTrue(r.isPredicted)
        assertTrue("due date must fall inside the season", r.dueDate!!.monthValue in 4..10)
    }

    @Test
    fun `ashore, nearly out of hours is not amber`() {
        val r = DueCalculator.evaluate(
            Input(
                today = LocalDate.of(2027, 1, 15),
                intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = nearlyOut,
                seasonStartMonth = 4, seasonEndMonth = 10,
            )
        )
        // Five hours left, but nothing consumes them until spring. Amber here
        // would be a warning the owner cannot act on.
        assertEquals(State.UPCOMING, r.state)
        assertEquals(80L, r.daysRemaining)
    }

    @Test
    fun `just before launch, the same item turns amber on its own`() {
        val r = DueCalculator.evaluate(
            Input(
                today = LocalDate.of(2027, 3, 20),
                intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = nearlyOut,
                seasonStartMonth = 4, seasonEndMonth = 10,
            )
        )
        // Nothing changed but the calendar: the predicted date is now inside
        // the warning window, which is exactly when it is actionable.
        assertEquals(State.DUE_SOON, r.state)
        assertEquals(16L, r.daysRemaining)
    }

    @Test
    fun `wear rate divides by sailing days, not calendar days`() {
        val r = DueCalculator.evaluate(
            Input(
                today = LocalDate.of(2027, 5, 1),
                intervalHours = 1000.0, lastServiceMeter = 0.0,
                readings = listOf(
                    Reading(LocalDate.of(2026, 5, 1), 0.0),
                    Reading(LocalDate.of(2027, 5, 1), 400.0),
                ),
                seasonStartMonth = 4, seasonEndMonth = 10,
            )
        )
        // 400 hours over a calendar year, but only 214 of those days were
        // sailing days. Dividing by 365 reports 1.10 h/day and predicts
        // almost twice as far out.
        assertEquals(400.0 / 214.0, r.wearRatePerDay!!, 0.001)
    }

    @Test
    fun `hours nearly gone with no trend yet is due soon, not unknown`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(today, 247.0)),
            )
        )
        // One reading gives no wear rate, so there is no honest date - but
        // three hours left is a fact, and it used to be thrown away.
        assertEquals(State.DUE_SOON, r.state)
        assertEquals(Driver.HOURS, r.driver)
        assertNull(r.dueDate)
        assertNull(r.daysRemaining)
        assertEquals(3.0, r.unitsRemaining!!, 0.001)
        assertEquals(Confidence.NONE, r.confidence)
    }

    @Test
    fun `hours nearly gone outrank a calendar item nine months out`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today,
                intervalDays = 365, lastServiceDate = d(-85),
                intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(today, 247.0)),
            )
        )
        // Ordering on days alone sorted the dateless candidate as +infinity,
        // so the calendar won with 280 days and the three hours disappeared
        // from the result altogether.
        assertEquals(State.DUE_SOON, r.state)
        assertEquals(Driver.HOURS, r.driver)
        assertEquals(3.0, r.unitsRemaining!!, 0.001)
    }

    @Test
    fun `hours already past threshold is overdue`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(d(-100), 160.0), Reading(today, 260.0)),
            )
        )
        assertEquals(State.OVERDUE, r.state)
        assertEquals(Driver.HOURS, r.driver)
    }

    @Test
    fun `hours due soon are predicted from the trend`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(d(-100), 147.0), Reading(today, 247.0)),
            )
        )
        assertEquals(State.DUE_SOON, r.state)
        assertEquals(3L, r.daysRemaining)
        assertTrue(r.isPredicted)
        assertEquals(1.0, r.wearRatePerDay!!, 1e-9)
    }

    @Test
    fun `distant hours still get a predicted date`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(d(-200), 0.0), Reading(today, 100.0)),
            )
        )
        assertEquals(State.UPCOMING, r.state)
        assertEquals(300L, r.daysRemaining)
        assertEquals(d(300), r.dueDate)
    }

    /** The "hours or months, whichever comes first" rule, generalised. */
    @Test
    fun `calendar beats hours when it lands sooner`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalDays = 365, lastServiceDate = d(-360),
                intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(d(-200), 0.0), Reading(today, 100.0)),
            )
        )
        assertEquals(Driver.CALENDAR, r.driver)
        assertEquals(5L, r.daysRemaining)
        assertTrue(!r.isPredicted)
    }

    @Test
    fun `printed expiry wins over the computed interval`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, expiresOn = d(10),
                intervalDays = 365, lastServiceDate = d(-100),
            )
        )
        assertEquals(Driver.EXPIRY, r.driver)
        assertEquals(10L, r.daysRemaining)
    }

    /** A replaced engine or a rolled-over gauge must not read as huge usage. */
    @Test
    fun `meter reset splits the series and only the newest run counts`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 100.0, lastServiceMeter = 0.0,
                readings = listOf(
                    Reading(d(-400), 100.0), Reading(d(-300), 200.0), Reading(d(-200), 300.0),
                    Reading(d(-100), 5.0), Reading(d(-50), 15.0), Reading(today, 25.0),
                ),
            )
        )
        assertEquals(0.2, r.wearRatePerDay!!, 1e-9)
        assertEquals(State.UPCOMING, r.state)
    }

    /** "Spring 2019" recalled from memory must not bend the slope. */
    @Test
    fun `fuzzy dates are excluded from the trend`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(
                    Reading(d(-700), 10.0, DatePrecision.YEAR),
                    Reading(d(-500), 20.0, DatePrecision.SEASON),
                    Reading(d(-100), 147.0),
                    Reading(today, 247.0),
                ),
            )
        )
        assertEquals(1.0, r.wearRatePerDay!!, 1e-9)
        assertEquals(State.DUE_SOON, r.state)
    }

    @Test
    fun `an unused boat is dormant, not unknown`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(d(-100), 50.0), Reading(today, 50.0)),
            )
        )
        assertEquals(State.DORMANT, r.state)
        assertNull(r.dueDate)
        assertEquals(Confidence.NONE, r.confidence)
    }

    @Test
    fun `nothing known stays unknown rather than guessing`() {
        val r = DueCalculator.evaluate(Input(today = today))
        assertEquals(State.UNKNOWN, r.state)
        assertEquals(Driver.NONE, r.driver)
        assertNull(r.wearRatePerDay)
    }

    @Test
    fun `readings a week apart are too close together to trend`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 250.0, lastServiceMeter = 0.0,
                readings = listOf(Reading(d(-7), 100.0), Reading(today, 110.0)),
            )
        )
        assertEquals(State.UNKNOWN, r.state)
        assertNull(r.dueDate)
        // Still useful: we know the gap even without a date.
        assertEquals(140.0, r.unitsRemaining!!, 1e-9)
        assertEquals(Confidence.NONE, r.confidence)
    }

    /**
     * From the MV Dirona workbook: an item can sit years overdue, acknowledged,
     * and that is normal operation. Without this the due list goes permanently
     * red and the owner stops believing it.
     */
    @Test
    fun `a live deferral outranks being overdue`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalDays = 365, lastServiceDate = d(-1500),
                deferredUntil = d(60),
            )
        )
        assertEquals(State.DEFERRED, r.state)
        // The real position is still reported underneath, never hidden.
        assertEquals(-1135L, r.daysRemaining)
    }

    @Test
    fun `a lapsed deferral goes back to overdue`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalDays = 365, lastServiceDate = d(-1500),
                deferredUntil = d(-1),
            )
        )
        assertEquals(State.OVERDUE, r.state)
    }

    /** Dirona allows 50 hours of notice on the main engine and 15 on the wing. */
    @Test
    fun `a narrower hour window means it is not soon after all`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 1000.0, lastServiceMeter = 0.0,
                soonWindowHours = 15.0,
                readings = listOf(Reading(d(-200), 840.0), Reading(today, 940.0)),
            )
        )
        // The default 10%-of-interval rule would have called 60 hours "soon".
        assertEquals(State.UPCOMING, r.state)
        assertEquals(60.0, r.unitsRemaining!!, 1e-9)
    }

    @Test
    fun `a wider day window brings it forward into soon`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalDays = 365, lastServiceDate = d(-305),
                soonWindowDays = 90L,
            )
        )
        assertEquals(State.DUE_SOON, r.state)
        assertEquals(60L, r.daysRemaining)
    }

    @Test
    fun `confidence rises with more evidence over a longer span`() {
        val r = DueCalculator.evaluate(
            Input(
                today = today, intervalHours = 500.0, lastServiceMeter = 0.0,
                readings = listOf(
                    Reading(d(-300), 0.0), Reading(d(-200), 50.0),
                    Reading(d(-100), 100.0), Reading(today, 150.0),
                ),
            )
        )
        assertEquals(Confidence.HIGH, r.confidence)
        assertEquals(State.UPCOMING, r.state)
    }
}
