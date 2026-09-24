package app.hullbeat.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSnifferTest {

    @Test
    fun `parses standard comma separated values`() {
        val csv = "Date,Task,Hours\n2026-05-01,Oil Change,120.5\n2026-06-15,Impeller,135.0"
        val rows = CsvSniffer.parse(csv, ',')
        assertEquals(3, rows.size)
        assertEquals(listOf("Date", "Task", "Hours"), rows[0])
        assertEquals(listOf("2026-05-01", "Oil Change", "120.5"), rows[1])
        assertEquals(listOf("2026-06-15", "Impeller", "135.0"), rows[2])
    }

    @Test
    fun `parses semicolon separated european formatted rows`() {
        val csv = "Datum;Arbeit;Kosten\n01.05.2026;Motorinspektion;450.00\n15.06.2026;Anoden;120.50"
        val delim = CsvSniffer.detectDelimiter(csv)
        assertEquals(';', delim)
        val rows = CsvSniffer.parse(csv, delim)
        assertEquals(3, rows.size)
        assertEquals("Motorinspektion", rows[1][1])
    }

    @Test
    fun `handles quoted fields with embedded commas and quotes`() {
        val csv = "Date,Description,Cost\n2026-07-01,\"Replaced oil, filter and \"\"O-rings\"\"\",150.00"
        val rows = CsvSniffer.parse(csv, ',')
        assertEquals(2, rows.size)
        assertEquals("Replaced oil, filter and \"O-rings\"", rows[1][1])
        assertEquals("150.00", rows[1][2])
    }

    @Test
    fun `detects tab delimiter correctly`() {
        val tsv = "Date\tDescription\tHours\n2026-01-01\tCheck\t50.0\n2026-02-01\tService\t60.0"
        val delim = CsvSniffer.detectDelimiter(tsv)
        assertEquals('\t', delim)
    }
}