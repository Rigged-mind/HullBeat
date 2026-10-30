package app.hullbeat.ui.now

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.domain.DueCalculator
import app.hullbeat.ui.theme.HullBeatPreview
import app.hullbeat.ui.theme.Scheme
import app.hullbeat.ui.theme.SchemePreview
import app.hullbeat.ui.theme.SchemePreviewProvider
import java.time.LocalDate

/**
 * Overdue, due soon and deferred cards in each scheme: the three states whose
 * colour carries meaning and must stay readable in Sunlight and Night.
 *
 * Every name resolves from resources - a catalog code and the default boat
 * name - so the fixture has no text of its own for check_hardcoded_strings.
 */
@HullBeatPreview
@Composable
private fun NowCardStatesPreview(
    @PreviewParameter(SchemePreviewProvider::class) scheme: Scheme,
) {
    val today = LocalDate.now()
    SchemePreview(scheme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                previewItem(1, "engine_main", "engine_oil", Criticality.HIGH, DueCalculator.State.OVERDUE, -12),
                previewItem(2, "safety", "liferaft", Criticality.MED, DueCalculator.State.DUE_SOON, 9),
                previewItem(3, "hull", "seacocks", Criticality.LOW, DueCalculator.State.DUE_SOON, 4, deferredUntil = today.plusDays(20)),
            ).forEach { item ->
                NowCard(
                    item = item,
                    showVesselBadge = false,
                    onDone = {},
                    onDefer = {},
                    onUndefer = {},
                    onOpenDetail = {},
                )
            }
        }
    }
}

private fun previewItem(
    id: Long,
    category: String,
    catalogCode: String,
    criticality: Criticality,
    state: DueCalculator.State,
    days: Long,
    deferredUntil: LocalDate? = null,
): NowItem {
    val due = LocalDate.now().plusDays(days)
    val schedule = ServiceSchedule(id = id, componentId = id, intervalDays = 365, deferredUntil = deferredUntil)
    return NowItem(
        component = Component(
            id = id, vesselId = 1, categoryCode = category, catalogCode = catalogCode,
            name = catalogCode, criticality = criticality,
        ),
        schedule = schedule,
        vessel = Vessel(id = 1, name = "setup_default_name", hullType = "mono", engine = "inboard_diesel", drive = "shaft"),
        state = state,
        result = DueCalculator.Result(
            state = state, dueDate = due, isPredicted = false, daysRemaining = days,
            unitsRemaining = null, driver = DueCalculator.Driver.CALENDAR,
            wearRatePerDay = null, confidence = DueCalculator.Confidence.HIGH,
        ),
        needsNewExpiry = false,
        isDeferred = deferredUntil != null,
        deferredUntil = deferredUntil,
        deferReason = null,
        criticality = criticality,
        hasMeter = false,
    )
}
