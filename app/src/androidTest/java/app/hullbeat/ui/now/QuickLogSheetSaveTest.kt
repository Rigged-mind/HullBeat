package app.hullbeat.ui.now

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hullbeat.R
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.WorkType
import app.hullbeat.domain.DueCalculator
import app.hullbeat.ui.theme.HullBeatTheme
import app.hullbeat.ui.theme.Scheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the quick-log sheet hands to its caller when Save is pressed: the
 * typed expiry and hours, parsed. QuickLogForm only reports the tap; the
 * sheet turns the typed text into the values a record is written from, so
 * that is what is tested here. Nothing is written anywhere - the callback
 * only records what it was given. Run with `tools/emu.sh itest`.
 */
@RunWith(AndroidJUnit4::class)
class QuickLogSheetSaveTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private data class Saved(
        val date: LocalDate,
        val workType: WorkType,
        val meterHours: Double?,
        val cost: Double?,
        val notes: String?,
        val newExpiry: LocalDate?,
        val photoUris: List<Uri>,
    )

    private val today = LocalDate.now()

    /** A dated certificate that also runs on hours: the sheet asks for both. */
    private val item = NowItem(
        component = Component(id = 1, vesselId = 1, categoryCode = "engine_main", name = "Engine"),
        schedule = ServiceSchedule(
            id = 1, componentId = 1, intervalHours = 250.0, expiresOn = today.plusDays(10),
        ),
        vessel = Vessel(id = 1, name = "Test", hullType = "monohull_sail", engine = "inboard_diesel", drive = "shaft"),
        state = DueCalculator.State.DUE_SOON,
        result = DueCalculator.Result(
            state = DueCalculator.State.DUE_SOON, dueDate = today.plusDays(10), isPredicted = false,
            daysRemaining = 10, unitsRemaining = null, driver = DueCalculator.Driver.EXPIRY,
            wearRatePerDay = null, confidence = DueCalculator.Confidence.HIGH,
        ),
        needsNewExpiry = true,
        isDeferred = false,
        deferredUntil = null,
        deferReason = null,
        criticality = Criticality.HIGH,
        hasMeter = true,
    )

    private val saved = mutableListOf<Saved>()

    private fun showSheet() {
        compose.setContent {
            HullBeatTheme(scheme = Scheme.COLORFUL) {
                QuickLogSheet(
                    item = item,
                    onDismiss = {},
                    onSave = { date, workType, hours, cost, notes, newExpiry, photos ->
                        saved += Saved(date, workType, hours, cost, notes, newExpiry, photos)
                    },
                    currency = "€",
                )
            }
        }
    }

    private fun field(labelRes: Int) = compose.onNode(
        hasSetTextAction() and hasText(compose.activity.getString(labelRes))
    )

    @Test
    fun typedExpiryAndHoursReachTheCallbackParsed() {
        val expiry = today.plusYears(1)
        showSheet()

        field(R.string.field_new_expiry).performScrollTo().performTextInput(expiry.toString())
        // A comma, as an owner in the uk locale types it.
        field(R.string.field_meter_hours).performScrollTo().performTextInput("512,5")
        compose.onNodeWithText(compose.activity.getString(R.string.action_save))
            .performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(1, saved.size)
        val s = saved[0]
        assertEquals(today, s.date)
        assertEquals(WorkType.SCHEDULED, s.workType)
        assertEquals(512.5, s.meterHours!!, 0.0)
        assertNull(s.cost)
        assertNull(s.notes)
        assertEquals(expiry, s.newExpiry)
        assertTrue(s.photoUris.isEmpty())
    }

    @Test
    fun expiryBeforeTheServiceDateKeepsSaveOff() {
        showSheet()

        field(R.string.field_new_expiry).performScrollTo()
            .performTextInput(today.minusDays(1).toString())
        field(R.string.field_meter_hours).performScrollTo().performTextInput("512,5")
        compose.onNodeWithText(compose.activity.getString(R.string.action_save))
            .performScrollTo().assertIsNotEnabled().performClick()
        compose.waitForIdle()

        assertTrue(saved.isEmpty())
    }
}
