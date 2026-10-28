package app.hullbeat.domain

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.domain.IntervalInput.Field
import app.hullbeat.domain.IntervalInput.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IntervalInputTest {

    @Test
    fun `days and hours both set`() {
        assertEquals(Result.Valid(365, 250.0, null), IntervalInput.parse("365", "250", ""))
    }

    @Test
    fun `either field alone is enough`() {
        assertEquals(Result.Valid(180, null, null), IntervalInput.parse("180", "", ""))
        assertEquals(Result.Valid(null, 100.0, null), IntervalInput.parse(" ", "100", ""))
    }

    @Test
    fun `hours take a comma`() {
        assertEquals(Result.Valid(null, 62.5, null), IntervalInput.parse("", "62,5", ""))
    }

    @Test
    fun `all blank is empty, not valid`() {
        assertEquals(Result.Empty, IntervalInput.parse("", "", ""))
    }

    @Test
    fun `zero is refused rather than read as due at once`() {
        assertEquals(Result.Invalid(setOf(Field.DAYS, Field.HOURS)), IntervalInput.parse("0", "0", ""))
    }

    @Test
    fun `days must be whole and in range`() {
        assertEquals(Result.Invalid(setOf(Field.DAYS)), IntervalInput.parse("30,5", "", ""))
        assertEquals(Result.Invalid(setOf(Field.DAYS)), IntervalInput.parse("99999", "", ""))
        assertEquals(Result.Valid(IntervalInput.MAX_DAYS, null, null), IntervalInput.parse("36500", "", ""))
    }

    @Test
    fun `a bad field is reported even beside a good one`() {
        assertEquals(Result.Invalid(setOf(Field.HOURS)), IntervalInput.parse("365", "1.2,3", ""))
        assertEquals(Result.Invalid(setOf(Field.MILES)), IntervalInput.parse("", "", "-5"))
    }

    @Test
    fun `apply writes the interval and clears the verify flag`() {
        val before = ServiceSchedule(
            id = 7,
            componentId = 3,
            intervalDays = 365,
            intervalHours = 200.0,
            needsVerification = true,
            deferredUntil = LocalDate.of(2026, 11, 1),
            soonWindowDays = 30,
        )
        val after = IntervalInput.apply(before, Result.Valid(null, 250.0, null))
        assertNull(after.intervalDays)
        assertEquals(250.0, after.intervalHours!!, 0.0)
        assertFalse(after.needsVerification)
        // Everything else about the schedule stays the owner's.
        assertEquals(before.copy(intervalDays = null, intervalHours = 250.0, needsVerification = false), after)
    }

    @Test
    fun `the new interval drives the due date`() {
        // Through DueEngine, the path the Pulse and component screens take.
        val component = Component(
            id = 1, vesselId = 1, categoryCode = "engine", catalogCode = "engine_oil",
            name = "Engine oil", criticality = Criticality.HIGH,
        )
        val last = LocalDate.of(2026, 1, 1)
        val record = ServiceRecord(id = 1, componentId = 1, scheduleId = 5, date = last, description = "Oil")
        val seeded = ServiceSchedule(id = 5, componentId = 1, intervalDays = 365, needsVerification = true)
        val edited = IntervalInput.apply(seeded, Result.Valid(180, null, null))
        val due = { s: ServiceSchedule ->
            DueEngine.evaluate(component, listOf(s), listOf(record), today = LocalDate.of(2026, 3, 1))
                .primaryResult.dueDate
        }
        assertEquals(last.plusDays(365), due(seeded))
        assertEquals(last.plusDays(180), due(edited))
    }

    @Test
    fun `only interval schedules get an editor`() {
        assertTrue(IntervalInput.isEditable(ServiceSchedule(componentId = 1, intervalHours = 100.0)))
        // A policy: nothing to count from, only the printed date.
        assertFalse(IntervalInput.isEditable(ServiceSchedule(componentId = 1, expiresOn = LocalDate.of(2027, 5, 1))))
    }

    @Test
    fun `stored values go back without a trailing zero`() {
        assertEquals("200", IntervalInput.format(200.0))
        assertEquals("62.5", IntervalInput.format(62.5))
        assertEquals("", IntervalInput.format(null))
    }
}
