package app.hullbeat.ui.now

import app.hullbeat.data.db.ServiceSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulesToRenewTest {

    private val certificate = ServiceSchedule(
        id = 1, componentId = 10, expiresOn = LocalDate.of(2026, 5, 1),
    )
    private val secondCertificate = ServiceSchedule(
        id = 2, componentId = 10, expiresOn = LocalDate.of(2027, 3, 1),
    )
    private val inspection = ServiceSchedule(id = 3, componentId = 10, intervalDays = 365)

    @Test
    fun `renewing one certificate leaves the other alone`() {
        val renewed = schedulesToRenew(listOf(certificate, secondCertificate, inspection), 2)
        assertEquals(listOf(secondCertificate), renewed)
    }

    @Test
    fun `a schedule without an expiry is never given one`() {
        val renewed = schedulesToRenew(listOf(certificate, inspection), 3)
        assertEquals(emptyList<ServiceSchedule>(), renewed)
    }

    @Test
    fun `no schedule named keeps the old behaviour`() {
        val renewed = schedulesToRenew(listOf(certificate, secondCertificate, inspection), null)
        assertEquals(listOf(certificate, secondCertificate), renewed)
    }
}
