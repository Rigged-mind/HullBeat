package app.hullbeat.domain

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import java.time.LocalDate
import java.util.Comparator

/**
 * Unified maintenance due and forecast engine.
 *
 * Serves as the single source of truth for schedule evaluation across
 * NowViewModel, ComponentDetailViewModel, and MaintenanceCheckWorker.
 */
object DueEngine {

    enum class UnknownReason {
        NONE,
        NO_SCHEDULES,
        NEEDS_INITIAL_METER,
        /**
         * A schedule with no interval of any kind - the node's life is the
         * date printed on it, and that date has not been entered yet.
         *
         * Separate from INSUFFICIENT_DATA on purpose: a policy is not a
         * component that has never been serviced. One needs a service date,
         * the other needs the expiry off the paper, and the Now screen can
         * only ask for the right one if it knows which is missing.
         */
        NEEDS_EXPIRY_DATE,
        INSUFFICIENT_DATA,
    }

    data class ScheduleEvaluation(
        val schedule: ServiceSchedule,
        val result: DueCalculator.Result,
        val lastRecord: ServiceRecord?,
        val baseMeterValue: Double?,
    )

    data class ComponentEvaluation(
        val componentId: Long,
        val primarySchedule: ServiceSchedule?,
        val primaryResult: DueCalculator.Result,
        val allSchedules: List<ScheduleEvaluation>,
        val unknownReason: UnknownReason,
    ) {
        val state: DueCalculator.State get() = primaryResult.state
        val dueDate: LocalDate? get() = primaryResult.dueDate
        val daysRemaining: Long? get() = primaryResult.daysRemaining
        val unitsRemaining: Double? get() = primaryResult.unitsRemaining
        val isOverdue: Boolean get() = state == DueCalculator.State.OVERDUE
        val isDueSoon: Boolean get() = state == DueCalculator.State.DUE_SOON
        val isDeferred: Boolean get() = state == DueCalculator.State.DEFERRED
        val isUpcoming: Boolean get() = state == DueCalculator.State.UPCOMING
        val isDormant: Boolean get() = state == DueCalculator.State.DORMANT
        val isUnknown: Boolean get() = state == DueCalculator.State.UNKNOWN
    }

    /**
     * The last service record per schedule, and per component.
     *
     * ⚠️ FEED THIS THE COMPONENT'S COMPLETE RECORD SET.
     *
     * `latestByComponent` is the fallback for a schedule that has no record
     * of its own - imported history and everything logged before records
     * carried a scheduleId, which is most of it and always will be: a
     * spreadsheet from a previous owner cannot know about our schedules.
     * Hand this a slice filtered by schedule (`forSchedules`, whose WHERE
     * clause excludes `scheduleId IS NULL`) and the fallback goes blind to
     * exactly the history it exists for.
     */
    data class RecordIndex(
        val bySchedule: Map<Long, ServiceRecord>,
        val latestByComponent: Map<Long, ServiceRecord>,
    ) {
        /** The record a schedule should measure from. */
        fun lastFor(schedule: ServiceSchedule, componentId: Long): ServiceRecord? =
            bySchedule[schedule.id] ?: latestByComponent[componentId]

        companion object {
            val EMPTY = RecordIndex(emptyMap(), emptyMap())
        }
    }

    /**
     * Newest wins, and "newest" is (date, id) - never (date) alone.
     *
     * Two records on one day are ordinary: an oil change and the filter that
     * goes with it, logged one after the other. Comparing dates alone leaves
     * the winner to whatever order the query happened to return, and the
     * queries did not agree - `allForVessel` sorts by date only while
     * `forSchedules` sorts by date and id. So Now and the component screen
     * could disagree about the same component's last service, and therefore
     * about its next due date.
     *
     * This comparator is why `indexRecords` does NOT depend on input order.
     * The test feeds it ascending precisely to prove that.
     */
    private val NEWEST: Comparator<ServiceRecord> =
        compareBy<ServiceRecord> { it.date }.thenBy { it.id }

    /**
     * Build the index. The ONE place "which record is the last one" is
     * decided.
     *
     * Before this, four places decided it four ways: `maxByOrNull { it.date }`
     * in the test overload, `lastForComponent()` in SQL from the detail
     * screen, and `groupBy(...).firstOrNull()` over a date-only ordering in
     * both Now and the worker. Three of the four had no tie-break, and the
     * two that shared code still read different queries.
     */
    fun indexRecords(records: List<ServiceRecord>): RecordIndex {
        if (records.isEmpty()) return RecordIndex.EMPTY
        val bySchedule = mutableMapOf<Long, ServiceRecord>()
        val byComponent = mutableMapOf<Long, ServiceRecord>()
        for (record in records) {
            record.scheduleId?.let { scheduleId ->
                val best = bySchedule[scheduleId]
                if (best == null || NEWEST.compare(record, best) > 0) {
                    bySchedule[scheduleId] = record
                }
            }
            val bestForComponent = byComponent[record.componentId]
            if (bestForComponent == null || NEWEST.compare(record, bestForComponent) > 0) {
                byComponent[record.componentId] = record
            }
        }
        return RecordIndex(bySchedule, byComponent)
    }

    private val scheduleEvaluationComparator = Comparator<ScheduleEvaluation> { a, b ->
        val stateA = a.result.state
        val stateB = b.result.state
        if (stateA != stateB) {
            return@Comparator stateOrder(stateA).compareTo(stateOrder(stateB))
        }
        when (stateA) {
            DueCalculator.State.OVERDUE -> {
                val daysA = a.result.daysRemaining ?: 0L
                val daysB = b.result.daysRemaining ?: 0L
                if (daysA != daysB) return@Comparator daysA.compareTo(daysB)
                val unitsA = a.result.unitsRemaining ?: 0.0
                val unitsB = b.result.unitsRemaining ?: 0.0
                unitsA.compareTo(unitsB)
            }
            DueCalculator.State.DUE_SOON, DueCalculator.State.UPCOMING -> {
                val daysA = a.result.daysRemaining ?: Long.MAX_VALUE
                val daysB = b.result.daysRemaining ?: Long.MAX_VALUE
                if (daysA != daysB) return@Comparator daysA.compareTo(daysB)
                val unitsA = a.result.unitsRemaining ?: Double.MAX_VALUE
                val unitsB = b.result.unitsRemaining ?: Double.MAX_VALUE
                unitsA.compareTo(unitsB)
            }
            DueCalculator.State.DEFERRED -> {
                val dateA = a.schedule.deferredUntil
                val dateB = b.schedule.deferredUntil
                if (dateA != null && dateB != null) dateA.compareTo(dateB) else 0
            }
            else -> 0
        }
    }

    /**
     * Which schedule speaks for the component.
     *
     * DEFERRED sits ABOVE UPCOMING, and that is not a matter of urgency -
     * it is a matter of visibility. `NowViewModel` shows a component when
     * its state is OVERDUE, DUE_SOON or DEFERRED. With UPCOMING ranked
     * higher, a component with one deferred schedule and one upcoming
     * schedule resolved to UPCOMING and vanished from Now entirely, taking
     * with it the thing the owner had explicitly postponed and asked to be
     * reminded about.
     *
     * ⚠️ This ordering is a patch on a lossy projection: N schedules
     * collapse into one card, so ranking them only picks which one to be
     * honest about. The invariant worth reaching for is "a component is
     * shown if ANY of its schedules is worth showing", which this cannot
     * express.
     */
    private fun stateOrder(state: DueCalculator.State): Int = when (state) {
        DueCalculator.State.OVERDUE -> 0
        DueCalculator.State.DUE_SOON -> 1
        DueCalculator.State.DEFERRED -> 2
        DueCalculator.State.UPCOMING -> 3
        DueCalculator.State.DORMANT -> 4
        DueCalculator.State.UNKNOWN -> 5
    }

    /**
     * Evaluates all enabled schedules for a single component and aggregates the result.
     * This suspending version allows asynchronous meter lookups.
     */
    suspend fun evaluate(
        componentId: Long,
        schedules: List<ServiceSchedule>,
        readings: List<DueCalculator.Reading>,
        /**
         * The component's COMPLETE record set - see [indexRecords]. Callers
         * used to hand in a map they built themselves, which is how three
         * of them ended up choosing the OLDEST record per schedule:
         * `associateBy` keeps the LAST entry for a duplicate key, and the
         * queries return newest first.
         */
        records: List<ServiceRecord> = emptyList(),
        readingById: suspend (Long) -> Double?,
        today: LocalDate = LocalDate.now(),
        vessel: Vessel? = null,
        defaultAdvanceDays: Long? = null,
    ): ComponentEvaluation {
        val index = indexRecords(records)
        val enabledSchedules = schedules.filter { it.enabled }
        if (enabledSchedules.isEmpty()) {
            return ComponentEvaluation(
                componentId = componentId,
                primarySchedule = null,
                primaryResult = DueCalculator.Result(
                    state = DueCalculator.State.UNKNOWN,
                    dueDate = null,
                    isPredicted = false,
                    daysRemaining = null,
                    unitsRemaining = null,
                    driver = DueCalculator.Driver.NONE,
                    wearRatePerDay = null,
                    confidence = DueCalculator.Confidence.NONE,
                ),
                allSchedules = emptyList(),
                unknownReason = UnknownReason.NO_SCHEDULES,
            )
        }

        val evaluatedSchedules = mutableListOf<ScheduleEvaluation>()
        for (schedule in enabledSchedules) {
            val lastRecord = index.lastFor(schedule, componentId)
            val baseMeter = lastRecord?.meterReadingId?.let { readingById(it) }

            val input = DueCalculator.Input(
                today = today,
                intervalDays = schedule.intervalDays,
                intervalHours = schedule.intervalHours,
                intervalMiles = schedule.intervalMiles,
                expiresOn = schedule.expiresOn,
                lastServiceDate = lastRecord?.date,
                lastServiceMeter = baseMeter,
                readings = readings,
                deferredUntil = schedule.deferredUntil,
                soonWindowDays = schedule.soonWindowDays?.toLong() ?: defaultAdvanceDays,
                soonWindowHours = schedule.soonWindowHours,
                seasonStartMonth = vessel?.seasonStartMonth,
                seasonEndMonth = vessel?.seasonEndMonth,
            )

            val result = DueCalculator.evaluate(input)
            evaluatedSchedules.add(
                ScheduleEvaluation(
                    schedule = schedule,
                    result = result,
                    lastRecord = lastRecord,
                    baseMeterValue = baseMeter,
                )
            )
        }

        val sortedSchedules = evaluatedSchedules.sortedWith(scheduleEvaluationComparator)
        val primary = sortedSchedules.first()

        val unknownReason = when {
            primary.result.state != DueCalculator.State.UNKNOWN -> UnknownReason.NONE
            enabledSchedules.any { it.intervalHours != null } && readings.isEmpty() && primary.baseMeterValue == null ->
                UnknownReason.NEEDS_INITIAL_METER
            // Nothing to count from and nothing counted to: this is the
            // shape the seeder gives an expiring document.
            enabledSchedules.all {
                it.intervalDays == null && it.intervalHours == null &&
                    it.intervalMiles == null && it.expiresOn == null
            } -> UnknownReason.NEEDS_EXPIRY_DATE
            else -> UnknownReason.INSUFFICIENT_DATA
        }

        return ComponentEvaluation(
            componentId = componentId,
            primarySchedule = primary.schedule,
            primaryResult = primary.result,
            allSchedules = sortedSchedules,
            unknownReason = unknownReason,
        )
    }

    /**
     * In-memory / test overload that evaluates a component without suspending DB lookups.
     */
    fun evaluate(
        component: Component,
        schedules: List<ServiceSchedule>,
        records: List<ServiceRecord> = emptyList(),
        readings: List<DueCalculator.Reading> = emptyList(),
        today: LocalDate = LocalDate.now(),
        vessel: Vessel? = null,
        defaultAdvanceDays: Long? = null,
        readingById: (Long) -> Double? = { null },
    ): ComponentEvaluation {
        // The same derivation the production path uses. It used to build
        // its own map here, with the same broken `associateBy` - so a test
        // written against this overload could pass while every caller of
        // the suspending one stayed wrong.
        val index = indexRecords(records)

        val enabledSchedules = schedules.filter { it.enabled }
        if (enabledSchedules.isEmpty()) {
            return ComponentEvaluation(
                componentId = component.id,
                primarySchedule = null,
                primaryResult = DueCalculator.Result(
                    state = DueCalculator.State.UNKNOWN,
                    dueDate = null,
                    isPredicted = false,
                    daysRemaining = null,
                    unitsRemaining = null,
                    driver = DueCalculator.Driver.NONE,
                    wearRatePerDay = null,
                    confidence = DueCalculator.Confidence.NONE,
                ),
                allSchedules = emptyList(),
                unknownReason = UnknownReason.NO_SCHEDULES,
            )
        }

        val evaluatedSchedules = enabledSchedules.map { schedule ->
            val lastRecord = index.lastFor(schedule, component.id)
            val baseMeter = lastRecord?.meterReadingId?.let { readingById(it) }

            val input = DueCalculator.Input(
                today = today,
                intervalDays = schedule.intervalDays,
                intervalHours = schedule.intervalHours,
                intervalMiles = schedule.intervalMiles,
                expiresOn = schedule.expiresOn,
                lastServiceDate = lastRecord?.date,
                lastServiceMeter = baseMeter,
                readings = readings,
                deferredUntil = schedule.deferredUntil,
                soonWindowDays = schedule.soonWindowDays?.toLong() ?: defaultAdvanceDays,
                soonWindowHours = schedule.soonWindowHours,
                seasonStartMonth = vessel?.seasonStartMonth,
                seasonEndMonth = vessel?.seasonEndMonth,
            )

            val result = DueCalculator.evaluate(input)
            ScheduleEvaluation(
                schedule = schedule,
                result = result,
                lastRecord = lastRecord,
                baseMeterValue = baseMeter,
            )
        }

        val sortedSchedules = evaluatedSchedules.sortedWith(scheduleEvaluationComparator)
        val primary = sortedSchedules.first()

        val unknownReason = when {
            primary.result.state != DueCalculator.State.UNKNOWN -> UnknownReason.NONE
            enabledSchedules.any { it.intervalHours != null } && readings.isEmpty() && primary.baseMeterValue == null ->
                UnknownReason.NEEDS_INITIAL_METER
            // Nothing to count from and nothing counted to: this is the
            // shape the seeder gives an expiring document.
            enabledSchedules.all {
                it.intervalDays == null && it.intervalHours == null &&
                    it.intervalMiles == null && it.expiresOn == null
            } -> UnknownReason.NEEDS_EXPIRY_DATE
            else -> UnknownReason.INSUFFICIENT_DATA
        }

        return ComponentEvaluation(
            componentId = component.id,
            primarySchedule = primary.schedule,
            primaryResult = primary.result,
            allSchedules = sortedSchedules,
            unknownReason = unknownReason,
        )
    }
}
