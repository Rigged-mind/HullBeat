package app.hullbeat.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.hullbeat.HullBeatApp
import app.hullbeat.R
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.displayName
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.DueCalculator
import app.hullbeat.domain.DueEngine
import java.time.LocalDate
import java.util.Comparator

/**
 * The daily pass: for every component of every active vessel, ask
 * [DueCalculator] where it stands, post a reminder for the ones that are
 * overdue or inside the warning window, and TAKE DOWN the ones that are not
 * any more.
 *
 * A CoroutineWorker rather than a Worker because every DAO call here is
 * `suspend`, and because the pass can be long: it is O(components) database
 * round trips on a boat that may carry 259 nodes.
 */
class MaintenanceCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HullBeatApp
        if (!AppPreferences.isNotificationsEnabled(app)) {
            return Result.success()
        }
        val db = app.database
        val today = LocalDate.now()

        val vessels = db.vesselDao().allActive()
        // More than one boat means the reminder has to say which. With one,
        // the line would be noise.
        val multi = vessels.size > 1

        for (vessel in vessels) {
            val due = collect(db, vessel, today)
            post(vessel, due, if (multi) vessel.name else null)
        }
        return Result.success()
    }

    private suspend fun collect(
        db: AppDatabase,
        vessel: Vessel,
        today: LocalDate,
    ): List<Due> {
        val out = mutableListOf<Due>()
        val defaultAdvanceDays = AppPreferences.getAdvanceReminderDays(applicationContext).toLong()

        val components = db.componentDao().forVessel(vessel.id)
        if (components.isEmpty()) return emptyList()

        val schedulesByComponent = db.scheduleDao().forVessel(vessel.id).groupBy { it.componentId }
        val recordsForVessel = db.serviceRecordDao().allForVessel(vessel.id)
        val recordsByComponent = recordsForVessel.groupBy { it.componentId }

        val componentsById = components.associateBy { it.id }
        val vesselMetersList = db.meterDao().forVessel(vessel.id)
        val metersByComponent = vesselMetersList.groupBy { it.componentId }
        val readingsForVessel = db.meterDao().readingsForVessel(vessel.id)
        val readingsByMeter = readingsForVessel.groupBy { it.meterId }
        val readingsMap = readingsForVessel.associateBy { it.id }

        val defaultEngineMeter = vesselMetersList.firstOrNull { m ->
            val c = componentsById[m.componentId]
            c != null && c.categoryCode in setOf("engine_main", "drivetrain", "engine")
        } ?: vesselMetersList.firstOrNull()

        for (component in components) {
            val schedules = schedulesByComponent[component.id] ?: emptyList()
            if (schedules.isEmpty()) continue

            val ownMeters = metersByComponent[component.id] ?: emptyList()
            val meters = if (ownMeters.isNotEmpty()) {
                ownMeters
            } else if (component.categoryCode in setOf("engine_main", "drivetrain", "engine")) {
                val matchingMeter = if (component.engineIndex != null) {
                    vesselMetersList.firstOrNull { m ->
                        componentsById[m.componentId]?.engineIndex == component.engineIndex
                    } ?: defaultEngineMeter
                } else {
                    defaultEngineMeter
                }
                listOfNotNull(matchingMeter)
            } else {
                emptyList()
            }

            val readings = meters.flatMap { meter ->
                readingsByMeter[meter.id]?.map { r ->
                    DueCalculator.Reading(r.date, r.value, r.precision)
                } ?: emptyList()
            }

            val evaluation = DueEngine.evaluate(
                componentId = component.id,
                schedules = schedules,
                readings = readings,
                // The component's whole record set, including rows with
                // no scheduleId - imported and pre-Sprint-2 history.
                // DueEngine.indexRecords decides which one is last.
                records = recordsByComponent[component.id] ?: emptyList(),
                readingById = { id -> readingsMap[id]?.value },
                today = today,
                vessel = vessel,
                defaultAdvanceDays = defaultAdvanceDays,
            )

            if (evaluation.isOverdue || evaluation.isDueSoon) {
                val primarySchedule = evaluation.primarySchedule ?: continue
                val result = evaluation.primaryResult
                out += Due(
                    componentId = component.id,
                    name = component.displayName(applicationContext),
                    urgency = urgency(result),
                    needsNewExpiry = primarySchedule.expiresOn != null,
                    scheduleId = primarySchedule.id,
                    overdue = evaluation.isOverdue,
                    daysRemaining = result.daysRemaining,
                    unitsRemaining = result.unitsRemaining,
                )
            }
        }
        return out.sortedWith(urgencyComparator)
    }

    private fun post(vessel: Vessel, due: List<Due>, vesselName: String?) {
        val ctx = applicationContext

        // Prune BEFORE posting: a job finished on the Зараз screen yesterday
        // must not keep its card, and the summary must not keep counting it.
        // Without this the shade only ever grows.
        MaintenanceNotification.pruneStale(
            ctx, vessel.id,
            due.map { MaintenanceNotification.notificationId(it.componentId) }.toSet(),
        )
        if (due.isEmpty()) return

        due.forEach {
            MaintenanceNotification.show(
                context = ctx,
                vesselId = vessel.id,
                componentId = it.componentId,
                componentName = it.name,
                urgencyText = it.urgency,
                needsNewExpiry = it.needsNewExpiry,
                scheduleId = it.scheduleId,
                vesselName = vesselName,
            )
        }
        // Below two children Android would show the summary AND the child, so
        // one due job would appear twice. showSummary enforces that itself;
        // this call is unconditional so the rule lives in exactly one place.
        MaintenanceNotification.showSummary(
            context = ctx,
            vesselId = vessel.id,
            vesselName = vesselName,
            overdue = due.count { it.overdue },
            soon = due.count { !it.overdue },
            lines = due.map { "${it.name} — ${it.urgency}" },
        )
    }

    /**
     * The line under the component name. Built from resources with plurals,
     * because Ukrainian agrees the noun with the number and `values-uk` has
     * had `due_overdue_days` and `due_in_hours` waiting since before anything
     * could post a notification.
     */
    private fun urgency(r: DueCalculator.Result): String {
        val res = applicationContext.resources
        val days = r.daysRemaining
        return when {
            days != null && days < 0 ->
                res.getQuantityString(
                    R.plurals.due_overdue_days, (-days).toInt(), (-days).toInt())

            r.driver == DueCalculator.Driver.HOURS && r.unitsRemaining != null ->
                res.getQuantityString(
                    R.plurals.due_in_hours,
                    r.unitsRemaining.toInt(), r.unitsRemaining.toInt())

            days != null ->
                res.getQuantityString(
                    R.plurals.due_in_days, days.toInt(), days.toInt())

            else -> res.getString(R.string.due_unknown)
        }
    }

    companion object {
        const val UNIQUE_NAME = "daily_maintenance_check"
        private const val TRIGGERED_NAME = "meter_triggered_check"

        internal data class Due(
            val componentId: Long,
            val name: String,
            val urgency: String,
            val needsNewExpiry: Boolean,
            /**
             * The schedule that produced this notification.
             *
             * Required on purpose. A default here would let a future
             * construction site say nothing and ship 0 - which is not a
             * schedule id, survives the receiver's -1 sentinel, and fails
             * the foreign key at insert time on the most-used path in the
             * app.
             */
            val scheduleId: Long,
            val overdue: Boolean,
            val daysRemaining: Long?,
            val unitsRemaining: Double?,
        )

        internal val urgencyComparator = Comparator<Due> { a, b ->
            if (a.overdue != b.overdue) {
                return@Comparator if (a.overdue) -1 else 1
            }
            if (a.overdue) {
                val daysA = a.daysRemaining ?: 0L
                val daysB = b.daysRemaining ?: 0L
                if (daysA != daysB) {
                    return@Comparator daysA.compareTo(daysB)
                }
                val unitsA = a.unitsRemaining ?: 0.0
                val unitsB = b.unitsRemaining ?: 0.0
                return@Comparator unitsA.compareTo(unitsB)
            }
            val daysA = a.daysRemaining ?: Long.MAX_VALUE
            val daysB = b.daysRemaining ?: Long.MAX_VALUE
            if (daysA != daysB) {
                return@Comparator daysA.compareTo(daysB)
            }
            val unitsA = a.unitsRemaining ?: Double.MAX_VALUE
            val unitsB = b.unitsRemaining ?: Double.MAX_VALUE
            unitsA.compareTo(unitsB)
        }

        /**
         * Run the pass now, off cycle.
         *
         * Call this after a meter reading is saved: docs/ui-spec.md §7 promises
         * a forecast that moves when the owner enters new hours, and waiting
         * for tomorrow's 09:00 would make it a forecast about yesterday.
         *
         * REPLACE, not KEEP: typing three readings in a row should run the pass
         * once, on the last of them, not queue three passes over the same data.
         */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                TRIGGERED_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<MaintenanceCheckWorker>().build(),
            )
        }
    }
}
