package app.hullbeat.domain.export

import app.hullbeat.data.db.InventoryItemDetail
import app.hullbeat.data.db.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlin.text.Charsets

class ExcelExporterTest {

    @Test
    fun testEscapeXml() {
        val raw = "Engine & Transmission <100hp> \"Volvo\" 'D2-55'\nValid"
        val escaped = ExcelExporter.escapeXml(raw)
        assertEquals("Engine &amp; Transmission &lt;100hp&gt; &quot;Volvo&quot; &apos;D2-55&apos;\nValid", escaped)
    }

    @Test
    fun testColumnLetter() {
        assertEquals("A", ExcelExporter.columnLetter(0))
        assertEquals("B", ExcelExporter.columnLetter(1))
        assertEquals("Z", ExcelExporter.columnLetter(25))
        assertEquals("AA", ExcelExporter.columnLetter(26))
        assertEquals("AB", ExcelExporter.columnLetter(27))
    }

    @Test
    fun testExportToXlsxGeneratesValidZipAndWorksheets() {
        val records = listOf(
            ServiceRecordExportItem(
                date = "2026-09-13",
                componentName = "Main Engine",
                workType = "SCHEDULED",
                performedBy = "Skipper",
                meterHours = 450.5,
                cost = 120.0,
                description = "Oil and filter replacement",
                notes = "Used 15W40 mineral oil",
            )
        )

        val items = listOf(
            InventoryItemDetail(
                id = 1L,
                vesselId = 1L,
                partId = 10L,
                partName = "Oil Filter",
                partNumber = "875812",
                manufacturer = "Volvo Penta",
                partNote = "Engine spare",
                locationId = 101L,
                locationName = "Engine Bay Locker",
                quantity = 2.0,
                minQuantity = 1.0,
            )
        )

        val locations = listOf(
            StorageLocation(id = 101L, vesselId = 1L, name = "Engine Bay Locker", qrCode = null, note = "Port side"),
            StorageLocation(id = 102L, vesselId = 1L, name = "Empty Aft Locker", qrCode = null, note = "Starboard empty"),
        )

        val out = ByteArrayOutputStream()
        val success = ExcelExporter.exportToXlsx(
            outputStream = out,
            journalRecords = records,
            inventoryItems = items,
            locations = locations,
            journalSheetName = "Service Log",
            storeSheetName = "Store & Lockers",
        )

        assertTrue(success)

        val zipBytes = out.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        val zipIn = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entryContents = mutableMapOf<String, String>()

        var entry = zipIn.nextEntry
        while (entry != null) {
            val bytes = zipIn.readBytes()
            entryContents[entry.name] = String(bytes, Charsets.UTF_8)
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()

        assertTrue(entryContents.containsKey("[Content_Types].xml"))
        assertTrue(entryContents.containsKey("_rels/.rels"))
        assertTrue(entryContents.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entryContents.containsKey("xl/styles.xml"))
        assertTrue(entryContents.containsKey("xl/workbook.xml"))
        assertTrue(entryContents.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entryContents.containsKey("xl/worksheets/sheet2.xml"))

        val wbXml = entryContents["xl/workbook.xml"]!!
        assertTrue(wbXml.contains("Service Log"))
        assertTrue(wbXml.contains("Store &amp; Lockers"))

        val sheet1Xml = entryContents["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet1Xml.contains("Main Engine"))
        assertTrue(sheet1Xml.contains("Oil and filter replacement"))
        assertTrue(sheet1Xml.contains("<v>450.5</v>"))
        assertTrue(sheet1Xml.contains("<v>120</v>"))

        val sheet2Xml = entryContents["xl/worksheets/sheet2.xml"]!!
        assertTrue(sheet2Xml.contains("Oil Filter"))
        assertTrue(sheet2Xml.contains("875812"))
        assertTrue(sheet2Xml.contains("Empty Aft Locker"))
        assertTrue(sheet2Xml.contains("Starboard empty"))
    }
}
