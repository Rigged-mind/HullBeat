package app.hullbeat.domain.export

import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.AttachmentKind
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.Vessel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PdfPassportExporterTest {

    @Test
    fun testPreparePassportData_vesselMapping() {
        val vessel = Vessel(
            id = 1L,
            name = "Sea Breeze",
            hullType = "GRP / Fiberglass",
            engine = "diesel",
            drive = "saildrive",
            cooling = "raw_water",
            year = 2018,
            hin = "FR-SPB12345D818",
            make = "Beneteau",
            model = "Oceanis 38.1",
            lengthMetres = 11.5,
            photoUri = "content://media/vessel.jpg"
        )

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = emptyList(),
            componentCategoryNames = emptyMap(),
            componentDisplayNames = emptyMap(),
            records = emptyList(),
            meters = emptyMap(),
            vesselProfile = VesselProfileLabels(
                hullType = "Sailing monohull",
                engineType = "Diesel",
                driveType = "Saildrive",
                length = "11.5 m"
            ),
            workTypeNames = emptyMap(),
            recordAttachments = emptyMap(),
            componentAttachments = emptyMap(),
            generatedDate = "2026-09-15"
        )

        assertEquals("Sea Breeze", report.vessel.name)
        assertEquals("Beneteau Oceanis 38.1", report.vessel.makeModel)
        assertEquals(2018, report.vessel.year)
        assertEquals("FR-SPB12345D818", report.vessel.hin)
        assertEquals(11.5, report.vessel.lengthMetres ?: 0.0, 0.001)
        assertEquals("Sailing monohull", report.vessel.hullType)
        assertEquals("Diesel", report.vessel.engineType)
        assertEquals("11.5 m", report.vessel.lengthLabel)
        assertEquals("Saildrive", report.vessel.driveType)
        assertEquals("content://media/vessel.jpg", report.vessel.photoUri)
        assertEquals(0.0, report.totalCost, 0.001)
        assertEquals("2026-09-15", report.generatedDate)
    }

    @Test
    fun testPreparePassportData_componentsSortedAndMapped() {
        val vessel = Vessel(
            id = 1L,
            name = "Odyssey",
            hullType = "Steel",
            engine = "diesel",
            drive = "shaft"
        )

        val comp1 = Component(
            id = 10L,
            vesselId = 1L,
            categoryCode = "rigging",
            name = "Standing Rigging",
            make = "Selden",
            model = "Standard",
            serial = "SR-992",
            yearInstalled = 2020
        )
        val comp2 = Component(
            id = 20L,
            vesselId = 1L,
            categoryCode = "engine",
            name = "Main Propulsion Engine",
            make = "Yanmar",
            model = "3YM30",
            serial = "YM-10293",
            yearInstalled = 2015
        )

        val catNames = mapOf(
            10L to "Rigging",
            20L to "Engine"
        )
        val displayNames = mapOf(
            10L to "Forestay and Shrouds",
            20L to "Yanmar 3YM30"
        )

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = listOf(comp1, comp2),
            componentCategoryNames = catNames,
            componentDisplayNames = displayNames,
            records = emptyList(),
            meters = emptyMap(),
            vesselProfile = VesselProfileLabels(),
            workTypeNames = emptyMap(),
            recordAttachments = emptyMap(),
            componentAttachments = emptyMap()
        )

        assertEquals(2, report.components.size)
        val first = report.components[0]
        assertEquals(20L, first.id)
        assertEquals("Engine", first.categoryName)
        assertEquals("Yanmar 3YM30", first.name)
        assertEquals("Yanmar", first.make)
        assertEquals("3YM30", first.model)
        assertEquals("YM-10293", first.serial)
        assertEquals(2015, first.yearInstalled)

        val second = report.components[1]
        assertEquals(10L, second.id)
        assertEquals("Rigging", second.categoryName)
        assertEquals("Forestay and Shrouds", second.name)
    }

    @Test
    fun testPreparePassportData_serviceRecordsAndCostAggregation() {
        val vessel = Vessel(
            id = 1L,
            name = "Calypso",
            hullType = "Wood",
            engine = "diesel",
            drive = "shaft"
        )

        val rec1 = ServiceRecord(
            id = 101L,
            componentId = 5L,
            date = LocalDate.of(2026, 5, 10),
            workTypes = "SCHEDULED",
            description = "Engine oil change",
            cost = 150.0,
            meterReadingId = 501L,
            performedBy = "Skipper"
        )
        val rec2 = ServiceRecord(
            id = 102L,
            componentId = 5L,
            date = LocalDate.of(2026, 8, 20),
            workTypes = "REPAIR",
            description = "Impeller replacement",
            cost = 85.5,
            notes = "Spare from locker A"
        )
        val rec3 = ServiceRecord(
            id = 103L,
            componentId = 6L,
            date = LocalDate.of(2026, 1, 15),
            workTypes = "INSPECTION",
            description = "Anode check",
            cost = null
        )

        val meters = mapOf(501L to 350.5)
        val att1 = Attachment(
            id = 1L,
            parentType = ParentType.SERVICE_RECORD,
            parentId = 102L,
            kind = AttachmentKind.RECEIPT,
            fileUri = "content://media/receipt_102.jpg"
        )
        val attMap = mapOf(102L to listOf(att1))

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = emptyList(),
            componentCategoryNames = emptyMap(),
            componentDisplayNames = mapOf(5L to "Engine", 6L to "Hull"),
            records = listOf(rec1, rec2, rec3),
            meters = meters,
            vesselProfile = VesselProfileLabels(),
            workTypeNames = emptyMap(),
            recordAttachments = attMap,
            componentAttachments = emptyMap()
        )

        assertEquals(3, report.serviceRecords.size)
        assertEquals(102L, report.serviceRecords[0].id)
        assertEquals("2026-08-20", report.serviceRecords[0].date)
        assertEquals(listOf("content://media/receipt_102.jpg"), report.serviceRecords[0].photoUris)

        assertEquals(101L, report.serviceRecords[1].id)
        assertEquals(350.5, report.serviceRecords[1].meterHours ?: 0.0, 0.001)
        assertEquals("Skipper", report.serviceRecords[1].performedBy)

        assertEquals(103L, report.serviceRecords[2].id)
        assertNull(report.serviceRecords[2].cost)

        assertEquals(235.5, report.totalCost, 0.001)
    }

    @Test
    fun testPreparePassportData_blankValuesFallbackToNull() {
        val vessel = Vessel(
            id = 1L,
            name = "Ghost",
            hullType = "",
            engine = "",
            drive = "",
            make = "  ",
            model = "",
            hin = "   "
        )

        val comp = Component(
            id = 1L,
            vesselId = 1L,
            categoryCode = "general",
            name = "Test",
            make = " ",
            model = "",
            serial = "   "
        )

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = listOf(comp),
            componentCategoryNames = emptyMap(),
            componentDisplayNames = emptyMap(),
            records = emptyList(),
            meters = emptyMap(),
            vesselProfile = VesselProfileLabels(),
            workTypeNames = emptyMap(),
            recordAttachments = emptyMap(),
            componentAttachments = emptyMap()
        )

        assertNull(report.vessel.makeModel)
        assertNull(report.vessel.hin)
        assertNull(report.vessel.hullType)
        assertNull(report.vessel.engineType)
        assertNull(report.vessel.driveType)

        val compReport = report.components[0]
        assertNull(compReport.make)
        assertNull(compReport.model)
        assertNull(compReport.serial)
    }

    /**
     * A component id and a service-record id are drawn from different
     * sequences and collide constantly - component 102 and record 102 both
     * exist on almost any boat. One map keyed by "parentId" would have
     * handed a record's receipt to a component and called it equipment.
     */
    @Test
    fun testPreparePassportData_photosDoNotCrossBetweenIdSpaces() {
        val vessel = Vessel(
            id = 1L,
            name = "Bug",
            hullType = "GRP",
            engine = "diesel",
            drive = "saildrive"
        )
        val comp = Component(
            id = 102L,
            vesselId = 1L,
            categoryCode = "bilge",
            name = "Automatic bilge pump"
        )
        val record = ServiceRecord(
            id = 102L,
            componentId = 102L,
            date = LocalDate.of(2026, 5, 1),
            workTypes = "SERVICE",
            description = "Impeller"
        )

        val onTheComponent = Attachment(
            id = 1L,
            parentType = ParentType.COMPONENT,
            parentId = 102L,
            kind = AttachmentKind.PHOTO,
            fileUri = "file:///pump.jpg"
        )
        val onTheRecord = Attachment(
            id = 2L,
            parentType = ParentType.SERVICE_RECORD,
            parentId = 102L,
            kind = AttachmentKind.RECEIPT,
            fileUri = "file:///receipt.jpg"
        )

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = listOf(comp),
            componentCategoryNames = mapOf(102L to "Bilge"),
            componentDisplayNames = mapOf(102L to "Automatic bilge pump"),
            records = listOf(record),
            meters = emptyMap(),
            vesselProfile = VesselProfileLabels(),
            workTypeNames = emptyMap(),
            recordAttachments = mapOf(102L to listOf(onTheRecord)),
            componentAttachments = mapOf(102L to listOf(onTheComponent))
        )

        assertEquals(listOf("file:///pump.jpg"), report.components[0].photoUris)
        assertEquals(listOf("file:///receipt.jpg"), report.serviceRecords[0].photoUris)
    }

    /**
     * Everything attached used to count as a photograph. It only worked
     * because all three writers store PHOTO today; the enum has carried
     * MANUAL_PDF, VIDEO and AUDIO_NOTE the whole time, and each of them
     * would have reserved a strip of the page and drawn nothing into it.
     */
    @Test
    fun testPreparePassportData_onlyImagesBecomePhotos() {
        val vessel = Vessel(
            id = 1L,
            name = "Bug",
            hullType = "GRP",
            engine = "diesel",
            drive = "saildrive"
        )
        val comp = Component(
            id = 7L,
            vesselId = 1L,
            categoryCode = "sails",
            name = "Mainsail"
        )

        fun attachment(id: Long, kind: AttachmentKind, uri: String) = Attachment(
            id = id,
            parentType = ParentType.COMPONENT,
            parentId = 7L,
            kind = kind,
            fileUri = uri
        )

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = listOf(comp),
            componentCategoryNames = emptyMap(),
            componentDisplayNames = emptyMap(),
            records = emptyList(),
            meters = emptyMap(),
            vesselProfile = VesselProfileLabels(),
            workTypeNames = emptyMap(),
            recordAttachments = emptyMap(),
            componentAttachments = mapOf(
                7L to listOf(
                    attachment(1L, AttachmentKind.PHOTO, "file:///sail.jpg"),
                    attachment(2L, AttachmentKind.AUDIO_NOTE, "file:///note.m4a"),
                    attachment(3L, AttachmentKind.MANUAL_PDF, "file:///manual.pdf"),
                    attachment(4L, AttachmentKind.DIAGRAM, "file:///plan.png"),
                    attachment(5L, AttachmentKind.VIDEO, "file:///clip.mp4")
                )
            )
        )

        assertEquals(
            listOf("file:///sail.jpg", "file:///plan.png"),
            report.components[0].photoUris
        )
    }

    /**
     * The passport printed `[SCHEDULED]` and `monohull_sail` at an owner
     * running the app in Ukrainian. `formatWorkTypes` had existed the whole
     * time - it simply had one caller instead of two.
     */
    @Test
    fun testPreparePassportData_workTypesUseTheWordsGiven() {
        val vessel = Vessel(
            id = 1L,
            name = "Bug",
            hullType = "monohull_sail",
            engine = "none",
            drive = "none"
        )
        val scheduled = ServiceRecord(
            id = 1L,
            componentId = 1L,
            date = LocalDate.of(2026, 3, 2),
            workTypes = "SCHEDULED",
            description = "Seacock service"
        )
        val untranslated = ServiceRecord(
            id = 2L,
            componentId = 1L,
            date = LocalDate.of(2026, 3, 1),
            workTypes = "SOMETHING_NEW",
            description = "Whatever this turns out to be"
        )

        val report = PdfPassportExporter.preparePassportData(
            vessel = vessel,
            components = emptyList(),
            componentCategoryNames = emptyMap(),
            componentDisplayNames = emptyMap(),
            records = listOf(scheduled, untranslated),
            meters = emptyMap(),
            vesselProfile = VesselProfileLabels(hullType = "Вітрильний однокорпусник"),
            workTypeNames = mapOf("SCHEDULED" to "Планове обслуговування"),
            recordAttachments = emptyMap(),
            componentAttachments = emptyMap()
        )

        assertEquals("Вітрильний однокорпусник", report.vessel.hullType)
        assertEquals(
            "Планове обслуговування",
            report.serviceRecords.first { it.id == 1L }.workTypes
        )
        // Nothing disappears just because nobody has translated it yet.
        assertEquals(
            "SOMETHING_NEW",
            report.serviceRecords.first { it.id == 2L }.workTypes
        )
    }
}
