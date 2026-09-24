package app.hullbeat.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.hullbeat.HullBeatApp
import app.hullbeat.MainActivity
import app.hullbeat.R

/**
 * Builds the reminders, groups them per vessel, and decides here - not in the
 * receiver - which action the owner is offered.
 *
 * THE SPLIT. docs/ui-spec.md §4 lists what each kind of record requires. An
 * interval item (oil, filter, impeller, anodes) needs only a date, so it can
 * be logged from the shade in one tap. An item whose due date comes from a
 * printed expiry - liferaft, flares, extinguisher - needs the NEW expiry, and
 * the verb changes with it: you do not *do* a liferaft, you send it for
 * service and get a date back. That item gets "Продовжити…", which opens the
 * sheet.
 *
 * The decision belongs here because a BroadcastReceiver on Android 10+ cannot
 * reliably start an Activity from the background: by the time the receiver
 * knows it needs the sheet, it can no longer offer one. [MarkDoneReceiver]
 * still refuses such an item, but that is the net, not the plan.
 *
 * GROUPING. A boat coming out of winter storage has eighteen things due.
 * Eighteen separate cards is how an owner learns to hit "Clear all", and then to turn
 * the channel off. So children are grouped per VESSEL - "Оріон" and "Тузик"
 * must not congeal into one pile - under a summary that says how many and of
 * what kind.
 */
object MaintenanceNotification {

    /**
     * PendingIntent identity ignores extras. It compares action, data, type,
     * class and categories - so two intents differing only in a component id
     * are the SAME PendingIntent, and FLAG_UPDATE_CURRENT makes the second
     * silently overwrite the first: every reminder would act on whichever
     * component was scheduled last.
     *
     * Offsetting request codes (id + 10000, id + 20000) does not fix it
     * either, it only moves the collision to component 10000. A unique `data`
     * Uri per component and action is what actually makes them distinct.
     */
    private fun uri(componentId: Long, verb: String): Uri =
        Uri.parse("hullbeat://component/$componentId/$verb")

    /** One group per boat, so a two-vessel fleet gets two tidy stacks. */
    fun groupKey(vesselId: Long): String = "app.hullbeat.vessel.$vesselId"

    /** One reminder per component: a second pass replaces rather than stacks. */
    fun notificationId(componentId: Long): Int = componentId.toInt()

    /**
     * Negative, so it can never collide with a component's id. `1_000_000 + id`
     * would have collided with component 1000001 - the same class of bug as
     * offsetting a PendingIntent request code.
     */
    fun summaryId(vesselId: Long): Int = -vesselId.toInt() - 1

    fun show(
        context: Context,
        vesselId: Long,
        componentId: Long,
        componentName: String,
        urgencyText: String,
        needsNewExpiry: Boolean,
        /**
         * The schedule this notification is about. Carried through so the
         * "Done" action closes THAT schedule and not, by omission, all of
         * them.
         */
        scheduleId: Long? = null,
        vesselName: String? = null,
    ) {
        val notificationId = notificationId(componentId)

        // Tapping the body always opens the node, whatever the action is.
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_COMPONENT
                data = uri(componentId, "open")
                putExtra(MainActivity.EXTRA_COMPONENT_ID, componentId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, HullBeatApp.CHANNEL_DUE)
            .setSmallIcon(R.drawable.ic_notification)
            // From the generated colours, so the accent cannot drift from the
            // screen. brand_60 is the mid ramp on purpose: it has to stay
            // legible on both the light and the dark system shade.
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(componentName)
            .setContentText(urgencyText)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setGroup(groupKey(vesselId))
        // No setPriority: minSdk is 26, where importance lives on the channel
        // and the call is ignored. Same dead-code shape as guarding a
        // NotificationChannel behind a check for Oreo.

        // Which boat, when there is more than one. An owner with a single
        // vessel never sees the line and never learns the app has fleets.
        if (vesselName != null) builder.setSubText(vesselName)

        if (needsNewExpiry) {
            val renew = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_RENEW_COMPONENT
                    data = uri(componentId, "renew")
                    putExtra(MainActivity.EXTRA_COMPONENT_ID, componentId)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                R.drawable.ic_renew,
                context.getString(R.string.action_renew),
                renew,
            )
        } else {
            val done = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, MarkDoneReceiver::class.java).apply {
                    action = MarkDoneReceiver.ACTION_MARK_DONE
                    data = uri(componentId, "done")
                    putExtra(MarkDoneReceiver.EXTRA_COMPONENT_ID, componentId)
                    putExtra(MarkDoneReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    scheduleId?.let { putExtra(MarkDoneReceiver.EXTRA_SCHEDULE_ID, it) }
                    // So the receiver can drop the summary when it has just
                    // dismissed the last child of the group.
                    putExtra(MarkDoneReceiver.EXTRA_VESSEL_ID, vesselId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                R.drawable.ic_check,
                context.getString(R.string.action_mark_done),
                done,
            )
        }

        // Posts nothing and throws nothing when POST_NOTIFICATIONS was
        // declined: that permission is asked for on screen, not from a worker.
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    /**
     * The stack header.
     *
     * Only posted for TWO OR MORE children. A group summary over a single
     * child makes Android show both the summary and the child, so one due job
     * would appear twice.
     *
     * "3 прострочено · 2 скоро" needs no plural resource: `прострочено` is the
     * impersonal -но form, which never agrees with number, and `скоро` is an
     * adverb. That is the same rule that keeps them out of `<plurals>`
     * elsewhere (docs/i18n-uk.md §8.3) - it just happens to make this line
     * easy for once.
     */
    fun showSummary(
        context: Context,
        vesselId: Long,
        vesselName: String?,
        overdue: Int,
        soon: Int,
        lines: List<String>,
    ) {
        if (lines.size < 2) {
            // CANCEL, not just skip. A group that shrinks from three items to
            // one would otherwise keep yesterday's header hanging over today's
            // single card, still reading "3 прострочено" - a number that is
            // now false, which is the one thing a summary must never be.
            NotificationManagerCompat.from(context).cancel(summaryId(vesselId))
            return
        }

        val counts = listOfNotNull(
            if (overdue > 0) context.getString(R.string.summary_overdue, overdue) else null,
            if (soon > 0) context.getString(R.string.summary_soon, soon) else null,
        ).joinToString(" · ")

        val style = NotificationCompat.InboxStyle().setSummaryText(counts)
        // Five is what the shade shows expanded; the rest are counted, not
        // listed, and saying so beats silently truncating.
        lines.take(5).forEach(style::addLine)
        if (lines.size > 5) {
            style.addLine(context.getString(R.string.summary_more, lines.size - 5))
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("hullbeat://vessel/$vesselId/now")
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val summary = NotificationCompat.Builder(context, HullBeatApp.CHANNEL_DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(vesselName ?: context.getString(R.string.app_name))
            .setContentText(counts)
            .setStyle(style)
            .setContentIntent(open)
            .setGroup(groupKey(vesselId))
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(summaryId(vesselId), summary)
    }

    /**
     * Drop the summary once its last child is gone.
     *
     * Modern Android usually removes an empty summary on its own, but "usually"
     * has been version-dependent for years, and the failure is loud: a header
     * reading "3 прострочено" hanging over nothing, which is worse than no
     * grouping at all because it states a number that is now false.
     */
    fun dismissSummaryIfEmpty(context: Context, vesselId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val key = groupKey(vesselId)
        val summary = summaryId(vesselId)
        val childrenLeft = manager.activeNotifications.count { sbn ->
            sbn.id != summary && sbn.notification.group == key
        }
        if (childrenLeft == 0) manager.cancel(summary)
    }

    /**
     * Take down reminders for anything no longer due.
     *
     * Without this the shade only ever grows: yesterday's card for a job the
     * owner finished at the pontoon - logged on the Зараз screen, not from the
     * notification - would sit there indefinitely, and the summary would keep
     * counting it.
     */
    fun pruneStale(context: Context, vesselId: Long, keep: Set<Int>) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val key = groupKey(vesselId)
        val summary = summaryId(vesselId)
        manager.activeNotifications
            .filter { it.notification.group == key && it.id != summary }
            .filter { it.id !in keep }
            .forEach { manager.cancel(it.id) }
        if (keep.isEmpty()) manager.cancel(summary)
    }
}
