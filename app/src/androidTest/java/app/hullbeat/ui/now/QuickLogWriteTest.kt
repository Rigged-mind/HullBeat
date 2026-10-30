package app.hullbeat.ui.now

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.hullbeat.R
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.MeterUnit
import app.hullbeat.data.db.ServiceSchedule
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.WorkType
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a quick log writes, read back from Room: the record, the meter
 * reading, the renewed expiry. On Android's SQLite and Room's converters, in
 * a database that lives in memory and dies with the test - the owner's
 * database is never opened. Run it with `tools/emu.sh itest`, not
 * `connectedAndroidTest`, which uninstalls the app and the owner's data.
 */
@RunWith(AndroidJUnit4::class)
class QuickLogWriteTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase

    private val serviceDate = LocalDate.of(2026, 10, 30)
    private val oldExpiry = LocalDate.of(2026, 11, 15)
    private val newExpiry = LocalDate.of(2027, 11, 15)

    private var vesselId = 0L
    private var componentId = 0L
    private var closedScheduleId = 0L
    private var otherScheduleId = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        vesselId = db.vesselDao().insert(
            Vessel(name = "Test", hullType = "monohull_sail", engine = "inboard_diesel", drive = "shaft")
        )
        componentId = db.componentDao().insert(
            Component(vesselId = vesselId, categoryCode = "engine_main", name = "Engine")
        )
        // Runs on hours AND carries a printed date, and was put off: the
        // save has to write both and lift the deferral.
        closedScheduleId = db.scheduleDao().insert(
            ServiceSchedule(
                componentId = componentId,
                intervalHours = 250.0,
                expiresOn = oldExpiry,
                deferredUntil = LocalDate.of(2026, 11, 1),
                deferReason = "waiting for parts",
            )
        )
        // A second dated schedule on the same node, which this save does not close.
        otherScheduleId = db.scheduleDao().insert(
            ServiceSchedule(componentId = componentId, expiresOn = oldExpiry)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun renewalWithHoursIsReadBackFieldByField() = runBlocking {
        val undo = writeQuickLog(
            db, context, componentId, vesselId, closedScheduleId, serviceDate,
            WorkType.SCHEDULED, meterHours = 512.5, cost = null, notes = "new impeller",
            newExpiry = newExpiry, savedUris = emptyList(), failedPhotoCount = 0,
        )

        val record = db.serviceRecordDao().byId(undo.recordId)
        assertNotNull(record)
        record!!
        assertEquals(componentId, record.componentId)
        assertEquals(closedScheduleId, record.scheduleId)
        assertEquals(serviceDate, record.date)
        assertEquals(WorkType.SCHEDULED.name, record.workTypes)
        assertEquals(context.getString(R.string.action_renew), record.description)
        assertEquals("new impeller", record.notes)
        assertNull(record.cost)
        assertNull(record.currency)
        assertEquals(undo.meterReadingId, record.meterReadingId)

        // The node had no meter: one was made, in hours, and the reading is on it.
        val meters = db.meterDao().forComponent(componentId)
        assertEquals(1, meters.size)
        assertEquals(MeterUnit.HOURS, meters[0].unit)
        assertEquals(meters[0].id, undo.createdMeterId)
        val readings = db.meterDao().readingsForVessel(vesselId)
        assertEquals(1, readings.size)
        assertEquals(record.meterReadingId, readings[0].id)
        assertEquals(meters[0].id, readings[0].meterId)
        assertEquals(serviceDate, readings[0].date)
        assertEquals(512.5, readings[0].value, 0.0)

        val closed = db.scheduleDao().byId(closedScheduleId)!!
        assertEquals(newExpiry, closed.expiresOn)
        assertNull(closed.deferredUntil)
        assertNull(closed.deferReason)

        // The other dated schedule keeps its date; undo holds the closed one as it was.
        assertEquals(oldExpiry, db.scheduleDao().byId(otherScheduleId)!!.expiresOn)
        assertEquals(1, undo.expiryBefore.size)
        assertEquals(closedScheduleId, undo.expiryBefore[0].id)
        assertEquals(oldExpiry, undo.expiryBefore[0].expiresOn)
        assertEquals("waiting for parts", undo.expiryBefore[0].deferReason)
    }

    @Test
    fun plainDoneWritesNoReadingAndTouchesNoDate() = runBlocking {
        val undo = writeQuickLog(
            db, context, componentId, vesselId, closedScheduleId, serviceDate,
            WorkType.INSPECTION, meterHours = null, cost = null, notes = null,
            newExpiry = null, savedUris = emptyList(), failedPhotoCount = 0,
        )

        val record = db.serviceRecordDao().byId(undo.recordId)!!
        assertEquals(context.getString(R.string.action_mark_done), record.description)
        assertEquals(WorkType.INSPECTION.name, record.workTypes)
        assertNull(record.meterReadingId)
        assertNull(record.notes)
        assertNull(undo.createdMeterId)
        assertTrue(db.meterDao().forComponent(componentId).isEmpty())
        assertTrue(undo.expiryBefore.isEmpty())
        assertEquals(oldExpiry, db.scheduleDao().byId(closedScheduleId)!!.expiresOn)
        assertEquals(oldExpiry, db.scheduleDao().byId(otherScheduleId)!!.expiresOn)
    }
}
