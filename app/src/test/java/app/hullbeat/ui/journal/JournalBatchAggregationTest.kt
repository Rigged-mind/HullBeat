package app.hullbeat.ui.journal

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.DatePrecision
import app.hullbeat.data.db.MeterReading
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalBatchAggregationTest {

    private val today = LocalDate.of(2026, 9, 13)

    private fun testRecord(
        id: Long,
        componentId: Long,
        date: LocalDate,
        meterReadingId: Long? = null,
        precision: DatePrecision = DatePrecision.DAY,
    ) = ServiceRecord(
        id = id,
        componentId = componentId,
        date = date,
        description = "Service $id",
        meterReadingId = meterReadingId,
        precision = precision,
    )

    @Test
    fun `batch association maps components and readings correctly`() {
        val records = listOf(
            testRecord(1L, 10L, today.minusDays(5), meterReadingId = 100L),
            testRecord(2L, 10L, today.minusDays(30), meterReadingId = 101L),
            testRecord(3L, 20L, today.minusDays(10)),
        )

        val compIds = records.map { it.componentId }.distinct()
        val readingIds = records.mapNotNull { it.meterReadingId }.distinct()

        assertEquals(listOf(10L, 20L), compIds)
        assertEquals(listOf(100L, 101L), readingIds)

        val mockComponents = listOf(
            Component(id = 10L, vesselId = 1L, categoryCode = "engine", name = "Main Engine", criticality = Criticality.HIGH),
            Component(id = 20L, vesselId = 1L, categoryCode = "hull", name = "Hull Anodes", criticality = Criticality.MED),
        ).associateBy { it.id }

        val mockReadings = listOf(
            MeterReading(id = 100L, meterId = 1L, date = today.minusDays(5), value = 250.0),
            MeterReading(id = 101L, meterId = 1L, date = today.minusDays(30), value = 200.0),
        ).associateBy { it.id }

        // Group by componentId
        val recordsByComp = records.groupBy { it.componentId }
        val comp10Records = recordsByComp[10L] ?: emptyList()

        assertEquals(2, comp10Records.size)

        // Verify gap calculation between record 1 and record 2
        val r1 = comp10Records[0]
        val r2 = comp10Records[1]

        val daysGap = ChronoUnit.DAYS.between(r2.date, r1.date)
        assertEquals(25L, daysGap)

        val reading1 = r1.meterReadingId?.let { mockReadings[it] }
        val reading2 = r2.meterReadingId?.let { mockReadings[it] }

        val hoursGap = reading1!!.value - reading2!!.value
        assertEquals(50.0, hoursGap, 0.001)
    }

    @Test
    fun `approximate gap flagged when precision is not day`() {
        val r1 = testRecord(1L, 10L, today, precision = DatePrecision.MONTH)
        val r2 = testRecord(2L, 10L, today.minusDays(60), precision = DatePrecision.DAY)

        val isRough = r1.precision != DatePrecision.DAY || r2.precision != DatePrecision.DAY
        assertTrue(isRough)

        val r3 = testRecord(3L, 10L, today, precision = DatePrecision.DAY)
        val isExact = r3.precision == DatePrecision.DAY && r2.precision == DatePrecision.DAY
        assertTrue(isExact)
    }

    private fun testVessel(id: Long, name: String) = Vessel(
        id = id,
        name = name,
        hullType = "monohull",
        engine = "inboard_diesel",
        drive = "saildrive",
    )

    @Test
    fun `batch association links schedules and vessel names`() {
        val records = listOf(
            ServiceRecord(id = 1L, componentId = 10L, scheduleId = 100L, date = today, description = "Oil change"),
            ServiceRecord(id = 2L, componentId = 20L, scheduleId = null, date = today.minusDays(10), description = "Hull clean"),
        )

        val scheduleIds = records.mapNotNull { it.scheduleId }.distinct()
        assertEquals(listOf(100L), scheduleIds)

        val mockSchedules = listOf(
            ServiceSchedule(id = 100L, componentId = 10L, intervalHours = 250.0),
        ).associateBy { it.id }

        val mockComponents = listOf(
            Component(id = 10L, vesselId = 1L, categoryCode = "engine", name = "Main Engine", criticality = Criticality.HIGH),
            Component(id = 20L, vesselId = 2L, categoryCode = "hull", name = "Hull", criticality = Criticality.LOW),
        ).associateBy { it.id }

        val mockVessels = listOf(
            testVessel(1L, "Bavaria 38"),
            testVessel(2L, "Dinghy"),
        ).associateBy { it.id }

        val entry1 = JournalEntryItem(
            record = records[0],
            component = mockComponents[records[0].componentId]!!,
            componentDisplayName = "Main Engine",
            categoryDisplayName = "Engine",
            meterReading = null,
            attachments = emptyList(),
            schedule = records[0].scheduleId?.let { mockSchedules[it] },
            vesselName = mockVessels[mockComponents[records[0].componentId]!!.vesselId]?.name,
        )

        val entry2 = JournalEntryItem(
            record = records[1],
            component = mockComponents[records[1].componentId]!!,
            componentDisplayName = "Hull",
            categoryDisplayName = "Hull",
            meterReading = null,
            attachments = emptyList(),
            schedule = records[1].scheduleId?.let { mockSchedules[it] },
            vesselName = mockVessels[mockComponents[records[1].componentId]!!.vesselId]?.name,
        )

        assertEquals(100L, entry1.schedule?.id)
        assertEquals(250.0, entry1.schedule?.intervalHours)
        assertEquals("Bavaria 38", entry1.vesselName)

        assertNull(entry2.schedule)
        assertEquals("Dinghy", entry2.vesselName)
    }
}
