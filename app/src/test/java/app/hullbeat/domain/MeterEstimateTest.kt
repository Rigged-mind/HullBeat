package app.hullbeat.domain

import app.hullbeat.domain.DueCalculator.Reading
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeterEstimateTest {

    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    // 2 hours a day over 30 days.
    private val trend = listOf(Reading(d(5, 1), 1000.0), Reading(d(5, 31), 1060.0))

    @Test
    fun `last reading plus the trend`() {
        assertEquals(1080.0, DueCalculator.estimateMeter(trend, d(6, 10))!!, 0.0)
    }

    @Test
    fun `no trend gives no estimate, but the last reading stays available as a hint`() {
        // One reading. Prefilling 1000 on a later date and saving it would
        // record zero hours run - the regression this guards against.
        val single = listOf(Reading(d(5, 1), 1000.0))
        assertNull(DueCalculator.estimateMeter(single, d(6, 10)))
        assertEquals(1000.0, DueCalculator.lastMeter(single)!!, 0.0)
    }

    @Test
    fun `an idle engine is not given hours`() {
        val idle = listOf(Reading(d(5, 1), 1000.0), Reading(d(5, 31), 1000.0))
        assertNull(DueCalculator.estimateMeter(idle, d(6, 10)))
    }

    @Test
    fun `lay-up days add nothing`() {
        // Afloat May-October, 2 h a day. Of the 130 days to 10 March only
        // 31 October itself is a sailing day - spans are [start, end), as in
        // wearRate - so 1062, where a calendar projection would say 1320.
        val est = DueCalculator.estimateMeter(
            listOf(Reading(d(10, 1), 1000.0), Reading(d(10, 31), 1060.0)),
            d(3, 10, 2027),
            seasonStartMonth = 5,
            seasonEndMonth = 10,
        )
        assertEquals(1062.0, est!!, 0.0)
    }

    @Test
    fun `a reading taken today is returned as typed`() {
        assertEquals(1060.0, DueCalculator.estimateMeter(trend, d(5, 31))!!, 0.0)
    }

    @Test
    fun `rounding never goes below the last reading`() {
        val slow = listOf(Reading(d(5, 1), 1245.0), Reading(d(5, 31), 1245.4))
        // 0.4 h over 30 days; one more day adds ~0.013, which rounds down.
        val est = DueCalculator.estimateMeter(slow, d(6, 1))!!
        assertEquals(1245.4, est, 0.0)
    }

    @Test
    fun `previous reading is the last one on or before the day`() {
        assertEquals(1060.0, DueCalculator.previousMeter(trend, d(6, 10))!!, 0.0)
        assertEquals(1060.0, DueCalculator.previousMeter(trend, d(5, 31))!!, 0.0)
    }

    @Test
    fun `a service logged after the fact is compared with what came before it`() {
        // Dated 15 May: 1030 is between the two readings and is no decrease,
        // although it is below the last reading of all.
        assertEquals(1000.0, DueCalculator.previousMeter(trend, d(5, 15))!!, 0.0)
        assertNull(DueCalculator.previousMeter(trend, d(4, 1)))
    }

    @Test
    fun `previous reading belongs to the current gauge`() {
        // Replaced on 1 June: the old gauge's 1060 is not a floor for the new one.
        val replaced = trend + Reading(d(6, 1), 5.0)
        assertEquals(5.0, DueCalculator.previousMeter(replaced, d(6, 10))!!, 0.0)
    }

    @Test
    fun `no readings, nothing to say`() {
        assertNull(DueCalculator.estimateMeter(emptyList(), d(6, 10)))
        assertNull(DueCalculator.lastMeter(emptyList()))
    }
}
