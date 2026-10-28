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
    fun `a policy with no date yet asks for its first one`() {
        // The catalog's insurance: no interval, and no date until the owner
        // enters it. The sheet must ask, or it never gets one.
        val insurance = ServiceSchedule(componentId = 4)
        assertTrue(LogRequirements.needsNewExpiry(insurance))
        assertTrue(LogRequirements.awaitsFirstExpiry(insurance))
        assertTrue(LogRequirements.needsForm(insurance))
    }

    @Test
    fun `an interval without an expiry never asks for a date`() {
        assertFalse(LogRequirements.needsNewExpiry(antifouling))
        assertFalse(LogRequirements.needsNewExpiry(oilChange))
        assertFalse(LogRequirements.needsNewExpiry(ServiceSchedule(componentId = 5, intervalMiles = 1000.0)))
        assertFalse(LogRequirements.awaitsFirstExpiry(antifouling))
    }

    @Test
    fun `a dated certificate is renewed, not awaited`() {
        assertTrue(LogRequirements.needsNewExpiry(liferaft))
        assertFalse(LogRequirements.awaitsFirstExpiry(liferaft))
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
