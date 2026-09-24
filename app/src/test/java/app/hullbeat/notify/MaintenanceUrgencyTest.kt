package app.hullbeat.notify

import app.hullbeat.notify.MaintenanceCheckWorker.Companion.Due
import app.hullbeat.notify.MaintenanceCheckWorker.Companion.urgencyComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for notification urgency tie-breaking and ordering.
 */
class MaintenanceUrgencyTest {

    private fun due(
        id: Long,
        name: String,
        overdue: Boolean,
        days: Long?,
        units: Double? = null,
    ) = Due(
        componentId = id,
        name = name,
        urgency = "",
        needsNewExpiry = false,
        // Irrelevant to ordering, which is all this test measures - but a
        // real id, because 0 is not one.
        scheduleId = 1L,
        overdue = overdue,
        daysRemaining = days,
        unitsRemaining = units,
    )

    @Test
    fun overdueTakesPrecedenceOverDueSoon() {
        val overdueItem = due(1, "Anodes", overdue = true, days = -1)
        val soonItem = due(2, "Oil filter", overdue = false, days = 1)

        val result = urgencyComparator.compare(overdueItem, soonItem)
        assertTrue("Overdue must rank before due soon", result < 0)

        val reversed = urgencyComparator.compare(soonItem, overdueItem)
        assertTrue("Due soon must rank after overdue", reversed > 0)
    }

    @Test
    fun moreOverdueRanksBeforeLessOverdue() {
        val severelyOverdue = due(1, "Impeller", overdue = true, days = -60)
        val slightlyOverdue = due(2, "Belt", overdue = true, days = -3)

        val result = urgencyComparator.compare(severelyOverdue, slightlyOverdue)
        assertTrue("More days overdue (-60 vs -3) must come first", result < 0)
    }

    @Test
    fun overdueHoursTieBreakWhenDaysEqual() {
        val hoursOverdueMore = due(1, "Engine oil", overdue = true, days = 0, units = -50.0)
        val hoursOverdueLess = due(2, "Generator oil", overdue = true, days = 0, units = -5.0)

        val result = urgencyComparator.compare(hoursOverdueMore, hoursOverdueLess)
        assertTrue("-50 hours must come before -5 hours", result < 0)
    }

    @Test
    fun nearestDueSoonRanksBeforeFartherDueSoon() {
        val dueTomorrow = due(1, "Coolant", overdue = false, days = 1)
        val dueInFortnight = due(2, "Raft", overdue = false, days = 14)

        val result = urgencyComparator.compare(dueTomorrow, dueInFortnight)
        assertTrue("Due in 1 day must come before due in 14 days", result < 0)
    }

    @Test
    fun fullListSortingOrdersByTrueUrgency() {
        val items = listOf(
            due(1, "Due in 10 days", overdue = false, days = 10),
            due(2, "Overdue by 5 days", overdue = true, days = -5),
            due(3, "Due tomorrow", overdue = false, days = 1),
            due(4, "Overdue by 100 days", overdue = true, days = -100),
        )

        val sorted = items.sortedWith(urgencyComparator)

        assertEquals(4L, sorted[0].componentId) // -100 days
        assertEquals(2L, sorted[1].componentId) // -5 days
        assertEquals(3L, sorted[2].componentId) // 1 day
        assertEquals(1L, sorted[3].componentId) // 10 days
    }
}
