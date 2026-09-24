package app.hullbeat.domain

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.domain.DueCalculator.State
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Which record a schedule measures from.
 *
 * Four places used to decide this four different ways, and three of them
 * chose the OLDEST record: `associateBy` keeps the LAST entry for a
 * duplicate key while the queries return newest first. The fourth, the
 * detail screen, did it correctly in SQL - so the same component could show
 * one due date in Now and another one tap deeper.
 *
 * Every test here feeds records in ASCENDING date order on purpose. The DAOs
 * return them descending, so a set fed descending would pass with a `first()`
 * bug still in place; ascending catches both directions and stops the SQL
 * `ORDER BY` from becoming an unwritten part of the contract.
 */
class RecordIndexTest {

    private val today = LocalDate.of(2026, 9, 16)

    private fun component(id: Long = 1L) = Component(
        id = id,
        vesselId = 1L,
        categoryCode = "engine_main",
        catalogCode = "engine_oil",
        name = "Engine oil",
        criticality = Criticality.HIGH,
    )

    private fun schedule(
        id: Long,
        componentId: Long = 1L,
        intervalDays: Int? = 365,
        deferredUntil: LocalDate? = null,
    ) = ServiceSchedule(
        id = id,
        componentId = componentId,
        intervalDays = intervalDays,
        deferredUntil = deferredUntil,
    )

    private fun record(
        id: Long,
        date: LocalDate,
        scheduleId: Long? = null,
        componentId: Long = 1L,
    ) = ServiceRecord(
        id = id,
        componentId = componentId,
        scheduleId = scheduleId,
        date = date,
        description = "r$id",
    )

    // ------------------------------------------------------------------
    // the derivation itself
    // ------------------------------------------------------------------

    @Test
    fun `newest record wins for a schedule, whatever order they arrive in`() {
        val ascending = listOf(
            record(id = 1, date = today.minusMonths(3), scheduleId = 10L),
            record(id = 2, date = today.minusMonths(1), scheduleId = 10L),
            record(id = 3, date = today, scheduleId = 10L),
        )
        assertEquals(3L, DueEngine.indexRecords(ascending).bySchedule[10L]?.id)
        assertEquals(3L, DueEngine.indexRecords(ascending.reversed()).bySchedule[10L]?.id)
        assertEquals(3L, DueEngine.indexRecords(ascending.shuffled()).bySchedule[10L]?.id)
    }

    @Test
    fun `two records on one day break the tie on id`() {
        // An oil change and the filter that goes with it, logged one after
        // the other. Comparing dates alone leaves this to query order.
        val sameDay = listOf(
            record(id = 7, date = today, scheduleId = 10L),
            record(id = 8, date = today, scheduleId = 10L),
        )
        assertEquals(8L, DueEngine.indexRecords(sameDay).bySchedule[10L]?.id)
        assertEquals(8L, DueEngine.indexRecords(sameDay.reversed()).bySchedule[10L]?.id)
    }

    @Test
    fun `a record without a schedule still counts for the component`() {
        // Imported history and everything logged before records carried a
        // scheduleId. It must not appear in bySchedule and must not be lost.
        val records = listOf(
            record(id = 1, date = today.minusYears(2), scheduleId = null),
            record(id = 2, date = today.minusYears(1), scheduleId = null),
        )
        val index = DueEngine.indexRecords(records)
        assertEquals(0, index.bySchedule.size)
        assertEquals(2L, index.latestByComponent[1L]?.id)
    }

    @Test
    fun `a schedule with its own record ignores a newer sibling`() {
        val records = listOf(
            record(id = 1, date = today.minusYears(1), scheduleId = 10L),
            record(id = 2, date = today, scheduleId = 11L),
        )
        val index = DueEngine.indexRecords(records)
        assertEquals(1L, index.lastFor(schedule(10L), componentId = 1L)?.id)
        assertEquals(2L, index.lastFor(schedule(11L), componentId = 1L)?.id)
    }

    @Test
    fun `a schedule with no record of its own falls back to the component`() {
        val records = listOf(record(id = 5, date = today.minusMonths(6), scheduleId = null))
        val index = DueEngine.indexRecords(records)
        assertEquals(5L, index.lastFor(schedule(99L), componentId = 1L)?.id)
    }

    // ------------------------------------------------------------------
    // the derivation as the engine uses it
    // ------------------------------------------------------------------

    @Test
    fun `due date is computed from the newest record, not the oldest`() {
        val records = listOf(
            record(id = 1, date = LocalDate.of(2024, 1, 1), scheduleId = 10L),
            record(id = 2, date = LocalDate.of(2026, 1, 1), scheduleId = 10L),
        )
        val evaluation = DueEngine.evaluate(
            component = component(),
            schedules = listOf(schedule(10L, intervalDays = 365)),
            records = records,
            today = today,
        )
        assertEquals(LocalDate.of(2027, 1, 1), evaluation.dueDate)
        assertNotEquals(LocalDate.of(2025, 1, 1), evaluation.dueDate)
    }

    /**
     * The test that would have been missing.
     *
     * `DueEngine` has two entry points: a suspending one the app calls and
     * an in-memory one the tests call. They used to build the record map
     * separately, with the same bug in both - so a test proving the
     * in-memory path correct proved nothing about the three real callers.
     * This pins them together.
     */
    @Test
    fun `the suspending path and the in-memory path agree`() = runBlocking {
        val comp = component()
        val schedules = listOf(schedule(10L, intervalDays = 365), schedule(11L, intervalDays = 90))
        val records = listOf(
            record(id = 1, date = LocalDate.of(2024, 1, 1), scheduleId = 10L),
            record(id = 2, date = LocalDate.of(2026, 1, 1), scheduleId = 10L),
            record(id = 3, date = LocalDate.of(2026, 8, 1), scheduleId = 11L),
        )

        val inMemory = DueEngine.evaluate(
            component = comp,
            schedules = schedules,
            records = records,
            today = today,
        )
        val suspending = DueEngine.evaluate(
            componentId = comp.id,
            schedules = schedules,
            readings = emptyList(),
            records = records,
            readingById = { null },
            today = today,
        )

        assertEquals(inMemory.state, suspending.state)
        assertEquals(inMemory.dueDate, suspending.dueDate)
        assertEquals(inMemory.primarySchedule?.id, suspending.primarySchedule?.id)
        assertEquals(
            inMemory.allSchedules.map { it.lastRecord?.id },
            suspending.allSchedules.map { it.lastRecord?.id },
        )
    }

    // ------------------------------------------------------------------
    // which schedule speaks for the component
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // an expiring document: no interval, only a date
    // ------------------------------------------------------------------

    @Test
    fun `a schedule with no interval and no expiry asks for the date`() {
        // What the seeder creates for insurance, a registration, a radio
        // licence: a schedule that exists so the node HAS one, carrying only
        // a lead time. Before this it said INSUFFICIENT_DATA - the same
        // answer as a component that has never been serviced.
        val evaluation = DueEngine.evaluate(
            component = component(),
            schedules = listOf(
                ServiceSchedule(id = 10L, componentId = 1L, soonWindowDays = 30)
            ),
            records = emptyList(),
            today = today,
        )
        assertEquals(State.UNKNOWN, evaluation.state)
        assertEquals(DueEngine.UnknownReason.NEEDS_EXPIRY_DATE, evaluation.unknownReason)
    }

    @Test
    fun `an expiry date alone drives the state, and the lead time moves it`() {
        fun stateFor(daysAway: Long, lead: Int) = DueEngine.evaluate(
            component = component(),
            schedules = listOf(
                ServiceSchedule(
                    id = 10L,
                    componentId = 1L,
                    expiresOn = today.plusDays(daysAway),
                    soonWindowDays = lead,
                )
            ),
            records = emptyList(),
            today = today,
        ).state

        // No interval of any kind, and it still computes.
        assertEquals(State.UPCOMING, stateFor(90, 30))
        assertEquals(State.DUE_SOON, stateFor(25, 30))
        assertEquals(State.OVERDUE, stateFor(-5, 30))
        // The lead time is the whole point of the twelve catalog numbers:
        // fifty days out is calm on a short window and amber on a long one.
        assertEquals(State.UPCOMING, stateFor(50, 14))
        assertEquals(State.DUE_SOON, stateFor(50, 60))
    }

    @Test
    fun `a deferred schedule is not hidden by an upcoming one`() {
        // Now shows a component when it is OVERDUE, DUE_SOON or DEFERRED.
        // With UPCOMING ranked above DEFERRED this component vanished from
        // the screen, taking with it the job the owner had postponed.
        val comp = component()
        val deferred = schedule(10L, intervalDays = 365, deferredUntil = today.plusDays(30))
        val upcoming = schedule(11L, intervalDays = 365)
        val records = listOf(
            record(id = 1, date = today, scheduleId = 10L),
            record(id = 2, date = today, scheduleId = 11L),
        )

        val evaluation = DueEngine.evaluate(
            component = comp,
            schedules = listOf(upcoming, deferred),
            records = records,
            today = today,
        )

        assertEquals(State.DEFERRED, evaluation.state)
        assertEquals(10L, evaluation.primarySchedule?.id)
    }
}
