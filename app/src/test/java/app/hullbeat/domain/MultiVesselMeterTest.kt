package app.hullbeat.domain

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.domain.DueCalculator.Reading
import app.hullbeat.domain.DueEngine.UnknownReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MultiVesselMeterTest {

    private val today = LocalDate.of(2026, 9, 22)

    private val vessel1 = Vessel(id = 1L, name = "Bug", hullType = "monohull", engine = "none", drive = "none")
    private val vessel2 = Vessel(id = 2L, name = "Maria", hullType = "monohull", engine = "diesel", drive = "shaft")

    @Test
    fun `two vessels with hourly schedules evaluate meters independently`() {
        val mariaOilComponent = Component(
            id = 201L,
            vesselId = vessel2.id,
            categoryCode = "engine_main",
            catalogCode = "engine_oil",
            name = "Engine Oil",
            criticality = Criticality.HIGH,
        )

        val mariaOilSchedule = ServiceSchedule(
            id = 301L,
            componentId = mariaOilComponent.id,
            intervalHours = 200.0,
            intervalDays = 365,
        )

        // Bug has 4.0 hours logged
        val bugReadings = listOf(Reading(date = today, value = 4.0))

        // Maria currently has NO readings logged
        val mariaReadingsBefore = emptyList<Reading>()

        // 1. Maria must report NEEDS_INITIAL_METER even though Bug has readings
        val evalMariaBefore = DueEngine.evaluate(
            component = mariaOilComponent,
            schedules = listOf(mariaOilSchedule),
            records = emptyList(),
            readings = mariaReadingsBefore,
            today = today,
            vessel = vessel2,
        )

        assertTrue("Maria's oil must be unknown before meter reading", evalMariaBefore.isUnknown)
        assertEquals(UnknownReason.NEEDS_INITIAL_METER, evalMariaBefore.unknownReason)

        // 2. Once Maria receives her own reading (e.g. 5.0 hours), she transitions out of NEEDS_INITIAL_METER
        val mariaReadingsAfter = listOf(Reading(date = today, value = 5.0))
        val evalMariaAfter = DueEngine.evaluate(
            component = mariaOilComponent,
            schedules = listOf(mariaOilSchedule),
            records = emptyList(),
            readings = mariaReadingsAfter,
            today = today,
            vessel = vessel2,
        )

        assertTrue(evalMariaAfter.isUnknown)
        // With readings present but no service records, reason becomes INSUFFICIENT_DATA
        assertEquals(UnknownReason.INSUFFICIENT_DATA, evalMariaAfter.unknownReason)
    }

    @Test
    fun `custom engine component with legacy engine category code evaluates correctly`() {
        val customEngine = Component(
            id = 171L,
            vesselId = vessel1.id,
            categoryCode = "engine",
            catalogCode = null,
            name = "Двигун",
            criticality = Criticality.HIGH,
            isCustom = true,
        )

        val schedule = ServiceSchedule(
            id = 401L,
            componentId = customEngine.id,
            intervalHours = 100.0,
        )

        val evalWithReadings = DueEngine.evaluate(
            component = customEngine,
            schedules = listOf(schedule),
            records = emptyList(),
            readings = listOf(Reading(date = today, value = 10.0)),
            today = today,
            vessel = vessel1,
        )

        assertEquals(UnknownReason.INSUFFICIENT_DATA, evalWithReadings.unknownReason)
    }
}
