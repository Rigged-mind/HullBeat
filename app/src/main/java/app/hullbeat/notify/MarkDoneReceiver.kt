package app.hullbeat.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import app.hullbeat.HullBeatApp
import app.hullbeat.R
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.WorkType
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "Зроблено" straight from the notification, without opening the app - feature
 * #1 in docs/ui-spec.md §7, and the one thing no competitor offers.
 *
 * Three things here are not obvious, and the first is a correctness rule, not
 * an optimisation.
 *
 * 1. NOT EVERY ITEM CAN BE LOGGED IN ONE TAP. ui-spec.md §4 lists the fields
 *    each kind of record requires: for a node with `expiresOn` - a liferaft,
 *    flares, an extinguisher - the new expiry date is MANDATORY and the button
 *    reads "Продовжено", not "Зроблено", because you do not *do* a liferaft,
 *    you send it for service and get a new date back. The spec is explicit
 *    that Save stays disabled until that date is entered. Writing a record
 *    here without it would either drop the item off the list or bring it back
 *    on a date nobody chose. So an expiry-driven item is refused, and the
 *    tap does nothing: the body tap opens the sheet where the field is.
 *
 * 2. IDEMPOTENT. Notifications stay in the shade until dismissed, and a second
 *    record already written today for this schedule is not written twice. Two
 *    taps on a slow phone are one broadcast each, and "Зроблено" twice must
 *    not mean two oil changes in the journal.
 *
 * 3. `description` is resolved from resources at write time, not stored as a
 *    literal. It is saved into the row, so a Ukrainian sentence hardcoded here
 *    would turn up in an English-locale owner's export.
 */
class MarkDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val componentId = intent.getLongExtra(EXTRA_COMPONENT_ID, NO_ID)
        // Absent on a notification posted by an older build that is still
        // sitting in the shade: fall back rather than lose the record.
        // `> 0`, not `!= NO_ID`: row ids start at 1, so anything else -
        // the -1 default, a 0 from a caller that said nothing, whatever an
        // older build left in the shade - is not a schedule and must become
        // null rather than a foreign key that does not resolve.
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, NO_ID)
            .takeIf { it > 0L }
        if (componentId == NO_ID) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val vesselId = intent.getLongExtra(EXTRA_VESSEL_ID, NO_ID)

        if (!processingComponents.add(componentId)) {
            // Already processing a concurrent broadcast for this component
            return
        }

        val app = context.applicationContext as HullBeatApp
        val description = context.getString(R.string.record_from_notification)
        val today = LocalDate.now()
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = app.database

                // The schedule the button was offered for, if it still exists.
                // A schedule deleted since the notification was posted falls
                // back to the component, like an older build's intent.
                val schedule = scheduleId?.let { db.scheduleDao().byId(it) }

                // Rule 1: an item whose due date comes from a printed expiry
                // needs a new expiry, and only the sheet can ask for one.
                // Judged on THIS schedule, the same test the worker used to
                // offer the button: a liferaft's expiry must not silently
                // swallow the tap on its annual inspection.
                val needsNewExpiry = if (schedule != null) {
                    schedule.expiresOn != null
                } else {
                    db.scheduleDao().forComponent(componentId).any { it.expiresOn != null }
                }
                if (needsNewExpiry) return@launch

                // Rule 2: atomic transaction check-and-insert ensures two rapid broadcasts
                // or repeated actions never insert duplicate service records.
                // Keyed on the schedule: an oil change and a belt check on the
                // same engine on the same day are two jobs, not one twice.
                db.withTransaction {
                    val existing = if (schedule != null) {
                        db.serviceRecordDao().findRecordForScheduleOn(schedule.id, today)
                    } else {
                        db.serviceRecordDao().findMatchingRecord(componentId, today, description)
                    }
                    if (existing != null) {
                        return@withTransaction
                    }

                    db.serviceRecordDao().insert(
                        ServiceRecord(
                            componentId = componentId,
                            scheduleId = schedule?.id,
                            date = today,
                            workTypes = WorkType.SCHEDULED.name,
                            description = description,
                        )
                    )
                }
                dismiss(context, notificationId, vesselId)
            } finally {
                processingComponents.remove(componentId)
                // Always: a receiver that never finishes its async work is an
                // ANR waiting for the next slow query.
                pending.finish()
            }
        }
    }

    /**
     * Take the card down, and the stack header with it when that was the last
     * card under it. Android usually removes an empty summary itself, but
     * "usually" has been version-dependent for years and the failure is loud:
     * a header reading "3 прострочено" left hanging over nothing states a
     * number that is now false.
     */
    private fun dismiss(context: Context, notificationId: Int, vesselId: Long) {
        if (notificationId == -1) return
        NotificationManagerCompat.from(context).cancel(notificationId)
        if (vesselId != NO_ID) {
            MaintenanceNotification.dismissSummaryIfEmpty(context, vesselId)
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "app.hullbeat.action.MARK_DONE"
        const val EXTRA_COMPONENT_ID = "component_id"
        const val EXTRA_SCHEDULE_ID = "schedule_id"

        /** So dismissing the last child can dismiss its group summary too. */
        const val EXTRA_VESSEL_ID = "vessel_id"

        /** So the tapped reminder can be dismissed once the row is written. */
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        private const val NO_ID = -1L
        private val processingComponents = ConcurrentHashMap.newKeySet<Long>()
    }
}
