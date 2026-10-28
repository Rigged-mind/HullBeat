package app.hullbeat.domain

import app.hullbeat.data.db.ServiceSchedule
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRequirementsTest {

    private val oilChange = ServiceSchedule(componentId = 1, intervalDays = 365, intervalHours = 200.0)
    private val liferaft = ServiceSchedule(componentId = 2, expiresOn = LocalDate.of(2027, 5, 1))
    private val antifouling = ServiceSchedule(componentId = 3, intervalDays = 365)

    @Test
    fun `an hours job cannot be logged in one tap`() {
        // The catalog's engine oil: 200 h or a year. A one-tap record carries
        // no reading, and the hours side of the forecast is lost.
        assertTrue(LogRequirements.needsMeterReading(oilChange))
        assertTrue(LogRequirements.needsForm(oilChange))
    }

    @Test
    fun `an expiry still needs the sheet`() {
        assertTrue(LogRequirements.needsNewExpiry(liferaft))
        assertTrue(LogRequirements.needsForm(liferaft))
    }

    @Test
    fun `a calendar job is still one tap`() {
        assertFalse(LogRequirements.needsForm(antifouling))
    }

    @Test
    fun `the hours field cannot be left blank for an hours job`() {
        assertFalse(LogRequirements.meterInputOk(required = true, text = ""))
        assertFalse(LogRequirements.meterInputOk(required = true, text = "  "))
        assertFalse(LogRequirements.meterInputOk(required = true, text = "abc"))
        assertTrue(LogRequirements.meterInputOk(required = true, text = "1234,5"))
        // A replaced counter starts at zero, and zero is a reading.
        assertTrue(LogRequirements.meterInputOk(required = true, text = "0"))
    }

    @Test
    fun `a calendar job on an engine may leave hours blank`() {
        assertTrue(LogRequirements.meterInputOk(required = false, text = ""))
        assertTrue(LogRequirements.meterInputOk(required = false, text = "812"))
        assertFalse(LogRequirements.meterInputOk(required = false, text = "abc"))
    }
}
