package app.hullbeat.domain.export

import app.hullbeat.data.db.InventoryItemDetail
import app.hullbeat.data.db.StorageLocation
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets
import kotlin.text.StringBuilder

data class ServiceRecordExportItem(
    val date: String,
    val componentName: String,
    val workType: String,
    val performedBy: String? = null,
    val meterHours: Double? = null,
    val cost: Double? = null,
    val description: String = "",
    val notes: String? = null,
)

object ExcelExporter {

    val DEFAULT_JOURNAL_HEADERS = listOf(
        "Date", "Component", "Work Type", "Performed By",
        "Hours", "Cost", "Description", "Notes"
    )

    val DEFAULT_STORE_HEADERS = listOf(
        "Locker", "Part Name", "Part Number", "Manufacturer",
        "Quantity", "Min Quantity", "Notes"
    )

    fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    val code = ch.code
                    if (code in 0x20..0xD7FF || ch == '\t' || ch == '\r' || ch == '\n' || code in 0xE000..0xFFFD) {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun columnLetter(index: Int): String {
        var temp = index
        val sb = StringBuilder()
        while (temp >= 0) {
            sb.insert(0, ('A'.code + (temp % 26)).toChar())
            temp = (temp / 26) - 1
        }
        return sb.toString()
    }

    private fun writeStringCell(sb: StringBuilder, colLetter: String, rowIndex: Int, value: String, styleId: Int? = null) {
        val cellRef = "$colLetter$rowIndex"
        val styleAttr = if (styleId != null) " s=\"$styleId\"" else ""
        sb.append("<c r=\"").append(cellRef).append("\"").append(styleAttr).append(" t=\"inlineStr\"><is><t>")
            .append(escapeXml(value))
            .append("</t></is></c>")
    }

    private fun writeNumberCell(sb: StringBuilder, colLetter: String, rowIndex: Int, value: Double, styleId: Int? = null) {
        val cellRef = "$colLetter$rowIndex"
        val styleAttr = if (styleId != null) " s=\"$styleId\"" else ""
        val formatted = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        sb.append("<c r=\"").append(cellRef).append("\"").append(styleAttr).append("><v>")
            .append(formatted)
            .append("</v></c>")
    }

    fun exportToXlsx(
        outputStream: OutputStream,
        journalRecords: List<ServiceRecordExportItem>,
        inventoryItems: List<InventoryItemDetail>,
        locations: List<StorageLocation>,
        journalSheetName: String = "Service Log",
        storeSheetName: String = "Store & Lockers",
        journalHeaders: List<String> = DEFAULT_JOURNAL_HEADERS,
        storeHeaders: List<String> = DEFAULT_STORE_HEADERS,
    ): Boolean {
        return try {
            val zipOut = ZipOutputStream(outputStream)

            // 1. [Content_Types].xml
            zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            zipOut.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 2. _rels/.rels
            zipOut.putNextEntry(ZipEntry("_rels/.rels"))
            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            zipOut.write(rootRelsXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zipOut.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            zipOut.write(wbRelsXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 4. xl/styles.xml
            zipOut.putNextEntry(ZipEntry("xl/styles.xml"))
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
  <cellStyles count="1">
    <cellStyle name="Normal" xfId="0" builtinId="0"/>
  </cellStyles>
</styleSheet>"""
            zipOut.write(stylesXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 5. xl/workbook.xml
            zipOut.putNextEntry(ZipEntry("xl/workbook.xml"))
            val escapedJournalName = escapeXml(journalSheetName)
            val escapedStoreName = escapeXml(storeSheetName)
            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$escapedJournalName" sheetId="1" r:id="rId1"/>
    <sheet name="$escapedStoreName" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""
            zipOut.write(workbookXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 6. xl/worksheets/sheet1.xml (Journal)
            zipOut.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheet1Sb = StringBuilder()
            sheet1Sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            sheet1Sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
            sheet1Sb.append("  <sheetData>\n")

            // Header row
            sheet1Sb.append("    <row r=\"1\">\n")
            journalHeaders.forEachIndexed { colIdx, title ->
                writeStringCell(sheet1Sb, columnLetter(colIdx), 1, title, styleId = 1)
            }
            sheet1Sb.append("    </row>\n")

            // Data rows
            var rowIdx = 2
            for (rec in journalRecords) {
                sheet1Sb.append("    <row r=\"").append(rowIdx).append("\">\n")
                writeStringCell(sheet1Sb, "A", rowIdx, rec.date)
                writeStringCell(sheet1Sb, "B", rowIdx, rec.componentName)
                writeStringCell(sheet1Sb, "C", rowIdx, rec.workType)
                writeStringCell(sheet1Sb, "D", rowIdx, rec.performedBy ?: "")
                if (rec.meterHours != null) {
                    writeNumberCell(sheet1Sb, "E", rowIdx, rec.meterHours)
                } else {
                    writeStringCell(sheet1Sb, "E", rowIdx, "")
                }
                if (rec.cost != null) {
                    writeNumberCell(sheet1Sb, "F", rowIdx, rec.cost)
                } else {
                    writeStringCell(sheet1Sb, "F", rowIdx, "")
                }
                writeStringCell(sheet1Sb, "G", rowIdx, rec.description)
                writeStringCell(sheet1Sb, "H", rowIdx, rec.notes ?: "")
                sheet1Sb.append("    </row>\n")
                rowIdx++
            }

            sheet1Sb.append("  </sheetData>\n")
            sheet1Sb.append("</worksheet>")
            zipOut.write(sheet1Sb.toString().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 7. xl/worksheets/sheet2.xml (Store & Lockers)
            zipOut.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
            val sheet2Sb = StringBuilder()
            sheet2Sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            sheet2Sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
            sheet2Sb.append("  <sheetData>\n")

            // Header row
            sheet2Sb.append("    <row r=\"1\">\n")
            storeHeaders.forEachIndexed { colIdx, title ->
                writeStringCell(sheet2Sb, columnLetter(colIdx), 1, title, styleId = 1)
            }
            sheet2Sb.append("    </row>\n")

            // Store items
            var storeRowIdx = 2
            val usedLocationIds = mutableSetOf<Long>()
            for (item in inventoryItems) {
                if (item.locationId != null) {
                    usedLocationIds.add(item.locationId)
                }
                sheet2Sb.append("    <row r=\"").append(storeRowIdx).append("\">\n")
                writeStringCell(sheet2Sb, "A", storeRowIdx, item.locationName ?: "")
                writeStringCell(sheet2Sb, "B", storeRowIdx, item.partName)
                writeStringCell(sheet2Sb, "C", storeRowIdx, item.partNumber ?: "")
                writeStringCell(sheet2Sb, "D", storeRowIdx, item.manufacturer ?: "")
                writeNumberCell(sheet2Sb, "E", storeRowIdx, item.quantity)
                if (item.minQuantity != null) {
                    writeNumberCell(sheet2Sb, "F", storeRowIdx, item.minQuantity)
                } else {
                    writeStringCell(sheet2Sb, "F", storeRowIdx, "")
                }
                writeStringCell(sheet2Sb, "G", storeRowIdx, item.partNote ?: "")
                sheet2Sb.append("    </row>\n")
                storeRowIdx++
            }

            // Empty lockers
            val emptyLocations = locations.filter { it.id !in usedLocationIds }
            for (loc in emptyLocations) {
                sheet2Sb.append("    <row r=\"").append(storeRowIdx).append("\">\n")
                writeStringCell(sheet2Sb, "A", storeRowIdx, loc.name)
                writeStringCell(sheet2Sb, "B", storeRowIdx, "")
                writeStringCell(sheet2Sb, "C", storeRowIdx, "")
                writeStringCell(sheet2Sb, "D", storeRowIdx, "")
                writeNumberCell(sheet2Sb, "E", storeRowIdx, 0.0)
                writeStringCell(sheet2Sb, "F", storeRowIdx, "")
                writeStringCell(sheet2Sb, "G", storeRowIdx, loc.note ?: "")
                sheet2Sb.append("    </row>\n")
                storeRowIdx++
            }

            sheet2Sb.append("  </sheetData>\n")
            sheet2Sb.append("</worksheet>")
            zipOut.write(sheet2Sb.toString().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.finish()
            zipOut.flush()
            true
        } catch (_: Exception) {
            false
        }
    }
}
