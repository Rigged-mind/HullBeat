package app.hullbeat.domain.importer

import app.hullbeat.data.db.DatePrecision
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ColumnMapperTest {

    @Test
    fun `parses various date formats accurately`() {
        val iso = ColumnMapper.parseDate("2026-09-13")
        assertNotNull(iso)
        assertEquals(LocalDate.of(2026, 9, 13), iso?.first)
        assertEquals(DatePrecision.DAY, iso?.second)

        val euroDot = ColumnMapper.parseDate("15.04.2025")
        assertNotNull(euroDot)
        assertEquals(LocalDate.of(2025, 4, 15), euroDot?.first)

        val euroSlash = ColumnMapper.parseDate("28/05/2024")
        assertNotNull(euroSlash)
        assertEquals(LocalDate.of(2024, 5, 28), euroSlash?.first)

        val yearOnly = ColumnMapper.parseDate("2020")
        assertNotNull(yearOnly)
        assertEquals(LocalDate.of(2020, 7, 1), yearOnly?.first)
        assertEquals(DatePrecision.YEAR, yearOnly?.second)

        assertNull(ColumnMapper.parseDate("not a date"))
    }

    @Test
    fun `parses hours and costs with dirty formatting`() {
        assertEquals(150.5, ColumnMapper.parseHours("150,5 h"))
        assertEquals(2400.0, ColumnMapper.parseHours("2400 hrs."))
        assertEquals(450.0, ColumnMapper.parseCost("$ 450,00"))
        assertEquals(120.75, ColumnMapper.parseCost("120.75 EUR"))
    }

    @Test
    fun `suggests mapping based on standard headers`() {
        val headers = listOf("Date", "Description", "Engine Hours", "Cost", "Performed By")
        val mapping = ColumnMapper.suggestMapping(headers, emptyList())

        assertEquals(TargetField.DATE, mapping[0])
        assertEquals(TargetField.DESCRIPTION, mapping[1])
        assertEquals(TargetField.HOURS, mapping[2])
        assertEquals(TargetField.COST, mapping[3])
        assertEquals(TargetField.PERFORMED_BY, mapping[4])
    }

    @Test
    fun `suggests mapping based on ukrainian headers`() {
        val headers = listOf(
            "\u0414\u0430\u0442\u0430",
            "\u041e\u043f\u0438\u0441",
            "\u041c\u043e\u0442\u043e\u0433\u043e\u0434\u0438\u043d\u0438",
            "\u0412\u0430\u0440\u0442\u0456\u0441\u0442\u044c"
        )
        val mapping = ColumnMapper.suggestMapping(headers, emptyList())

        assertEquals(TargetField.DATE, mapping[0])
        assertEquals(TargetField.DESCRIPTION, mapping[1])
        assertEquals(TargetField.HOURS, mapping[2])
        assertEquals(TargetField.COST, mapping[3])
    }
}