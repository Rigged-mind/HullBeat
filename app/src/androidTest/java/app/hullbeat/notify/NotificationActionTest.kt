package app.hullbeat.notify

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.hullbeat.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Which button a reminder carries, read back from the notification Android
 * actually posted. One tap may only write a record that needs nothing more:
 * an expiry needs the new date and a job on hours needs the meter reading,
 * so both must open the sheet instead.
 *
 * The test posts under a component id far above any real one, so it never
 * replaces one of the owner's reminders, and removes it afterwards. With
 * notifications off it is skipped, not switched on.
 */
@RunWith(AndroidJUnit4::class)
class NotificationActionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val componentId = 900_000_001L
    private val notificationId = MaintenanceNotification.notificationId(componentId)

    @Before
    fun notificationsAllowed() {
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        // PendingIntent.isActivity / isBroadcast arrived in API 31.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }

    @After
    fun removeIt() {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun post(needsNewExpiry: Boolean, needsMeterReading: Boolean): Notification.Action {
        MaintenanceNotification.show(
            context = context,
            vesselId = componentId,
            componentId = componentId,
            componentName = "test",
            urgencyText = "",
            needsNewExpiry = needsNewExpiry,
            needsMeterReading = needsMeterReading,
            scheduleId = 1L,
        )
        // notify() returns before the system lists the card; wait for it.
        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + 3_000
        while (true) {
            val posted = manager.activeNotifications.firstOrNull { it.id == notificationId }
            if (posted != null) return posted.notification.actions.single()
            check(System.currentTimeMillis() < deadline) { "the reminder was never posted" }
            Thread.sleep(50)
        }
    }

    @Test
    fun aJobOnHoursOpensTheSheet() {
        val action = post(needsNewExpiry = false, needsMeterReading = true)
        assertEquals(context.getString(R.string.action_mark_done_with_hours), action.title.toString())
        assertTrue("must open the app, not write from the shade", action.actionIntent.isActivity)
    }

    @Test
    fun anExpiryOpensTheSheet() {
        val action = post(needsNewExpiry = true, needsMeterReading = false)
        assertEquals(context.getString(R.string.action_renew), action.title.toString())
        assertTrue(action.actionIntent.isActivity)
    }

    @Test
    fun aCalendarJobIsStillOneTap() {
        val action = post(needsNewExpiry = false, needsMeterReading = false)
        assertEquals(context.getString(R.string.action_mark_done), action.title.toString())
        assertTrue("one tap goes to MarkDoneReceiver", action.actionIntent.isBroadcast)
    }
}
