package app.hullbeat.domain

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.domain.DueCalculator.Reading
import app.hullbeat.domain.DueCalculator.State
import app.hullbeat.domain.DueEngine.UnknownReason
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DueEngineTest {

    private val today = LocalDate.of(2026, 9, 13)

    private fun testComponent(id: Long = 1L): Component = Component(
        id = id,
        vesselId = 1L,
        categoryCode = "propulsion",
        catalogCode = "engine_oil",
        name = "Engine Oil",
        customName = "Main Engine Oil",
        criticality = Criticality.HIGH,
    )

    private fun testSchedule(
        id: Long,
        componentId: Long = 1L,
        intervalDays: Int? = null,
        intervalHours: Double? = null,
        deferredUntil: LocalDate? = null,
        deferReason: String? = null,
        soonWindowDays: Int? = null,
    ): ServiceSchedule = ServiceSchedule(
        id = id,
        componentId = componentId,
        intervalDays = intervalDays,
        intervalHours = intervalHours,
        deferredUntil = deferredUntil,
        deferReason = deferReason,
        soonWindowDays = soonWindowDays,
    )

    private fun testRecord(
        id: Long = 1L,
        componentId: Long = 1L,
        scheduleId: Long? = null,
        date: LocalDate = today,
        meterReadingId: Long? = null,
        description: String = "Test record",
    ): ServiceRecord = ServiceRecord(
        id = id,
        componentId = componentId,
        scheduleId = scheduleId,
        date = date,
        meterReadingId = meterReadingId,
        description = description,
    )

    @Test
    fun `no schedules returns UNKNOWN with NO_SCHEDULES reason`() {
        val comp = testComponent()
        val eval = DueEngine.evaluate(
            component = comp,
            schedules = emptyList(),
            records = emptyList(),
            readings = emptyList(),
            today = today,
        )

        assertNull(eval.primarySchedule)
        assertEquals(State.UNKNOWN, eval.primaryResult.state)
        assertEquals(UnknownReason.NO_SCHEDULES, eval.unknownReason)
        assertTrue(eval.allSchedules.isEmpty())
    }

    @Test
    fun `schedule with hours but no meter readings returns NEEDS_INITIAL_METER`() {
        val comp = testComponent()
        val sched = testSchedule(id = 10L, intervalHours = 100.0)

        val eval = DueEngine.evaluate(
            component = comp,
            schedules = listOf(sched),
            records = emptyList(),
            readings = emptyList(),
            today = today,
        )

        assertEquals(sched.id, eval.primarySchedule?.id)
        assertEquals(State.UNKNOWN, eval.primaryResult.state)
        assertEquals(UnknownReason.NEEDS_INITIAL_METER, eval.unknownReason)
    }

    @Test
    fun `schedule with hours and meter reading transitions from NEEDS_INITIAL_METER to INSUFFICIENT_DATA when unserviced`() {
        val comp = testComponent()
        val sched = testSchedule(id = 10L, intervalHours = 100.0)

        val eval = DueEngine.evaluate(
            component = comp,
            schedules = listOf(sched),
            records = emptyList(),
            readings = listOf(DueCalculator.Reading(today, 250.0)),
            today = today,
        )

        assertEquals(State.UNKNOWN, eval.primaryResult.state)
        assertEquals(UnknownReason.INSUFFICIENT_DATA, eval.unknownReason)
    }

    @Test
    fun `multi-schedule selects the most urgent overdue schedule`() {
        val comp = testComponent()
        val schedCalendar = testSchedule(id = 10L, intervalDays = 365)
        val schedHours = testSchedule(id = 20L, intervalHours = 100.0)

        val calRecord = testRecord(
            id = 1L,
            componentId = comp.id,
            scheduleId = schedCalendar.id,
            date = today.minusDays(30),
        )
        val hourRecord = testRecord(
            id = 2L,
            componentId = comp.id,
            scheduleId = schedHours.id,
            date = today.minusDays(60),
            meterReadingId = 100L,
        )
        val meterReadings = listOf(
            Reading(today.minusDays(60), 50.0),
            Reading(today, 160.0),
        )

        val eval = DueEngine.evaluate(
            component = comp,
            schedules = listOf(schedCalendar, schedHours),
            records = listOf(calRecord, hourRecord),
            readings = meterReadings,
            today = today,
            readingById = { id -> if (id == 100L) 50.0 else null },
        )

        assertNotNull(eval.primarySchedule)
        assertEquals(schedHours.id, eval.primarySchedule?.id)
        assertEquals(State.OVERDUE, eval.primaryResult.state)
        assertEquals(2, eval.allSchedules.size)
        assertEquals(schedHours.id, eval.allSchedules[0].schedule.id)
        assertEquals(schedCalendar.id, eval.allSchedules[1].schedule.id)
    }

    @Test
    fun `overdue schedule takes precedence over deferred schedule`() {
        val comp = testComponent()
        val schedDeferred = testSchedule(id = 10L, intervalDays = 30, deferredUntil = today.plusDays(10), deferReason = "Yard")
        val schedOverdue = testSchedule(id = 20L, intervalDays = 60)

        val defRecord = testRecord(
            id = 1L,
            componentId = comp.id,
            scheduleId = schedDeferred.id,
            date = today.minusDays(40),
        )
        val ovdRecord = testRecord(
            id = 2L,
            componentId = comp.id,
            scheduleId = schedOverdue.id,
            date = today.minusDays(80),
        )

        val eval = DueEngine.evaluate(
            component = comp,
            schedules = listOf(schedDeferred, schedOverdue),
            records = listOf(defRecord, ovdRecord),
            readings = emptyList(),
            today = today,
        )

        assertNotNull(eval.primarySchedule)
        assertEquals(schedOverdue.id, eval.primarySchedule?.id)
        assertEquals(State.OVERDUE, eval.primaryResult.state)
        assertEquals(State.DEFERRED, eval.allSchedules[1].result.state)
    }

    @Test
    fun `schedule records matching by scheduleId or fallback to component`() {
        val comp = testComponent()
        val schedA = testSchedule(id = 10L, intervalDays = 30, soonWindowDays = 7)

        val recordA = testRecord(
            id = 1L,
            componentId = comp.id,
            scheduleId = 10L,
            date = today.minusDays(10),
        )

        val eval = DueEngine.evaluate(
            component = comp,
            schedules = listOf(schedA),
            records = listOf(recordA),
            readings = emptyList(),
            today = today,
        )

        assertEquals(State.UPCOMING, eval.primaryResult.state)
        assertEquals(20L, eval.primaryResult.daysRemaining)
    }

    @Test
    fun `suspend evaluate handles async meter lookups`() = runBlocking {
        val comp = testComponent()
        val sched = testSchedule(id = 10L, intervalHours = 100.0)
        val record = testRecord(id = 1L, scheduleId = sched.id, meterReadingId = 42L, date = today.minusDays(50))
        val readings = listOf(Reading(today.minusDays(50), 10.0), Reading(today, 120.0))

        val eval = DueEngine.evaluate(
            componentId = comp.id,
            schedules = listOf(sched),
            readings = readings,
            records = listOf(record),
            readingById = { id -> if (id == 42L) 10.0 else null },
            today = today,
        )

        assertEquals(sched.id, eval.primarySchedule?.id)
        assertEquals(State.OVERDUE, eval.primaryResult.state)
    }
}