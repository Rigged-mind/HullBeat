package app.hullbeat.ui.now

import app.hullbeat.data.db.ServiceSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    /** What the catalog seeds for a policy: no interval and, until now, no date. */
    private val insuranceNoDateYet = ServiceSchedule(id = 4, componentId = 11)

    @Test
    fun `the first date of a policy goes in`() {
        // Filtered on `expiresOn != null`, the first date had nothing to be
        // written into, and the policy was never reminded about.
        val renewed = schedulesToRenew(listOf(insuranceNoDateYet, inspection), 4)
        assertEquals(listOf(insuranceNoDateYet), renewed)
    }

    @Test
    fun `undo puts the policy back without a date`() {
        // The save keeps these rows as they were and undo writes them back
        // whole, so the row it holds must be the undated one.
        val keptForUndo = schedulesToRenew(listOf(insuranceNoDateYet), 4)
        assertNull(keptForUndo.single().expiresOn)
    }

    @Test
    fun `once dated, the policy renews like any certificate`() {
        val dated = insuranceNoDateYet.copy(expiresOn = LocalDate.of(2027, 6, 1))
        assertEquals(listOf(dated), schedulesToRenew(listOf(dated, inspection), 4))
    }

    @Test
    fun `an hours job without an expiry is never given one`() {
        val oilChange = ServiceSchedule(id = 5, componentId = 12, intervalHours = 200.0)
        assertEquals(emptyList<ServiceSchedule>(), schedulesToRenew(listOf(oilChange), 5))
        assertEquals(emptyList<ServiceSchedule>(), schedulesToRenew(listOf(oilChange, inspection), null))
    }
}
