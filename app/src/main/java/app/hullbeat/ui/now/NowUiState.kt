package app.hullbeat.ui.now

import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.domain.DueCalculator
import java.time.LocalDate

data class NowItem(
    val component: Component,
    val schedule: ServiceSchedule,
    val vessel: Vessel,
    val state: DueCalculator.State,
    val result: DueCalculator.Result,
    val urgencyText: String = "",
    val actionVerb: String = "",
    val needsNewExpiry: Boolean,
    val isDeferred: Boolean,
    val deferredUntil: LocalDate?,
    val deferReason: String?,
    val criticality: Criticality,
    val hasMeter: Boolean,
) {
    val isOverdue: Boolean
        get() = state == DueCalculator.State.OVERDUE

    val isDueSoon: Boolean
        get() = state == DueCalculator.State.DUE_SOON

    val isUpcoming: Boolean
        get() = state == DueCalculator.State.UPCOMING
}

data class RecentActivityItem(
    val record: app.hullbeat.data.db.ServiceRecord,
    val component: Component? = null,
    val componentName: String = "",
    val categoryCode: String,
    val vessel: Vessel? = null,
    val vesselName: String? = null,
)

data class VesselWaitingMeters(
    val vesselId: Long,
    val vessel: Vessel? = null,
    val vesselName: String = "",
    val count: Int,
    val currentMeterHours: Double?,
)

data class VesselMeterInfo(
    val vesselId: Long,
    val vessel: Vessel? = null,
    val vesselName: String = "",
    val meterId: Long?,
    val hours: Double?,
)

data class NowUiState(
    val vessels: List<Vessel> = emptyList(),
    val selectedVesselId: Long? = null,
    val items: List<NowItem> = emptyList(),
    val upcomingItems: List<NowItem> = emptyList(),
    val recentActivity: RecentActivityItem? = null,
    val unrecordedComponents: List<Component> = emptyList(),
    val overdueCount: Int = 0,
    val soonCount: Int = 0,
    /**
     * Put off on purpose, and counted apart from the two above: a deferral
     * is not urgent work. It is stated all the same, because the card for it
     * sits in the list below - "no urgent work today" over a visible
     * "deferred until October" read as a contradiction.
     */
    val deferredCount: Int = 0,
    /** Items on the selected boat's list; meaningless in the fleet view, where each boat has its own. */
    val departureCheckCount: Int = 8,
    /** For the selected boat, or - in the fleet view - for every boat at once. */
    val departureCheckDoneToday: Boolean = false,
    val departureDoneVesselIds: Set<Long> = emptySet(),
    val lastDepartedVesselId: Long? = null,
    /**
     * Components whose only obstacle is a first meter reading. One number
     * typed once clears every one of them, which is why this is a row with
     * an action and not a wall of cards.
     */
    val needsInitialMeterCount: Int = 0,
    /**
     * Components that have a schedule and no history at all. On a freshly
     * seeded boat this is most of the catalog - roughly 230 of 249 on a
     * Bavaria 38 - so it is stated as a number, never enumerated here.
     */
    val needsFirstRecordCount: Int = 0,
    val waitingMetersByVessel: List<VesselWaitingMeters> = emptyList(),
    val vesselMeterInfos: List<VesselMeterInfo> = emptyList(),
    val primaryMeterHours: Double? = null,
    val primaryMeterId: Long? = null,
    val isLoading: Boolean = false,
) {
    val activeVessel: Vessel?
        get() = vessels.firstOrNull { it.id == selectedVesselId } ?: vessels.firstOrNull()

    /** "All" over more than one boat. With a single boat, "All" is that boat. */
    val isFleetView: Boolean
        get() = vessels.size > 1 && selectedVesselId == null

    /**
     * Which boat the pre-departure sheet opens on. In the fleet view nothing
     * says which boat is about to leave, so it is the one that left last -
     * not whichever sorts first by name - and the sheet lets the owner switch.
     * Once that boat is done today, the next open is for another one, so the
     * first boat still to do comes before it.
     */
    val departureDefaultVessel: Vessel?
        get() = vessels.firstOrNull { it.id == selectedVesselId }
            ?: vessels.firstOrNull { it.id == lastDepartedVesselId && it.id !in departureDoneVesselIds }
            ?: vessels.firstOrNull { it.id !in departureDoneVesselIds }
            ?: vessels.firstOrNull { it.id == lastDepartedVesselId }
            ?: vessels.firstOrNull()
}
