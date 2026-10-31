package app.hullbeat.ui.now

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.DueCalculator
import app.hullbeat.ui.theme.HullBeatPreview
import app.hullbeat.ui.theme.Scheme
import app.hullbeat.ui.theme.SchemePreview
import app.hullbeat.ui.theme.SchemePreviewProvider
import java.time.LocalDate

/**
 * The quick-log form in each scheme, in the three states the owner meets:
 * empty with a certificate date still to enter, filled with hours and a cost,
 * and an expiry before the service date shown as a field error.
 *
 * Names come from the catalog and the default boat name, and the typed
 * values are numbers and dates, so the fixture has no interface text of its
 * own for check_hardcoded_strings.
 */
@HullBeatPreview
@Composable
private fun QuickLogFormEmptyPreview(
    @PreviewParameter(SchemePreviewProvider::class) scheme: Scheme,
) {
    QuickLogFormPreview(scheme, liferaft(), QuickLogInput(date = LocalDate.now()))
}

@HullBeatPreview
@Composable
private fun QuickLogFormFilledPreview(
    @PreviewParameter(SchemePreviewProvider::class) scheme: Scheme,
) {
    QuickLogFormPreview(
        scheme,
        engineOil(),
        QuickLogInput(date = LocalDate.now(), meterText = "512,5", costText = "1250"),
    )
}

@HullBeatPreview
@Composable
private fun QuickLogFormErrorPreview(
    @PreviewParameter(SchemePreviewProvider::class) scheme: Scheme,
) {
    val today = LocalDate.now()
    QuickLogFormPreview(
        scheme,
        liferaft(),
        // A slip of the year: the certificate "runs out" before the service.
        QuickLogInput(date = today, newExpiryText = today.minusDays(1).toString()),
    )
}

@Composable
private fun QuickLogFormPreview(scheme: Scheme, item: NowItem, input: QuickLogInput) {
    SchemePreview(scheme) {
        QuickLogForm(
            item = item,
            input = input,
            onInputChange = {},
            onPickExpiry = {},
            onAddPhoto = {},
            onCancel = {},
            onSave = {},
            currency = AppPreferences.getCurrency(LocalContext.current),
            // The form scrolls, and a preview measures its content with no
            // height limit; a scroller given an infinite height throws.
            modifier = Modifier.heightIn(max = 1600.dp),
        )
    }
}

/** A dated certificate: Save waits for the new date off the paper. */
private fun liferaft() = previewItem(
    category = "safety", catalogCode = "liferaft", criticality = Criticality.HIGH,
    schedule = ServiceSchedule(id = 1, componentId = 1, expiresOn = LocalDate.now().plusDays(9)),
    driver = DueCalculator.Driver.EXPIRY, needsNewExpiry = true, hasMeter = false,
)

/** Runs on hours: the reading is required, with the last one stated under it. */
private fun engineOil() = previewItem(
    category = "engine_main", catalogCode = "engine_oil", criticality = Criticality.HIGH,
    schedule = ServiceSchedule(id = 1, componentId = 1, intervalHours = 250.0),
    driver = DueCalculator.Driver.HOURS, needsNewExpiry = false, hasMeter = true,
    lastMeterReading = 498.0,
)

private fun previewItem(
    category: String,
    catalogCode: String,
    criticality: Criticality,
    schedule: ServiceSchedule,
    driver: DueCalculator.Driver,
    needsNewExpiry: Boolean,
    hasMeter: Boolean,
    lastMeterReading: Double? = null,
): NowItem {
    val state = DueCalculator.State.DUE_SOON
    return NowItem(
        component = Component(
            id = 1, vesselId = 1, categoryCode = category, catalogCode = catalogCode,
            name = catalogCode, criticality = criticality,
        ),
        schedule = schedule,
        vessel = Vessel(id = 1, name = "setup_default_name", hullType = "mono", engine = "inboard_diesel", drive = "shaft"),
        state = state,
        result = DueCalculator.Result(
            state = state, dueDate = LocalDate.now().plusDays(9), isPredicted = false, daysRemaining = 9,
            unitsRemaining = null, driver = driver,
            wearRatePerDay = null, confidence = DueCalculator.Confidence.HIGH,
        ),
        needsNewExpiry = needsNewExpiry,
        isDeferred = false,
        deferredUntil = null,
        deferReason = null,
        criticality = criticality,
        hasMeter = hasMeter,
        lastMeterReading = lastMeterReading,
    )
}
