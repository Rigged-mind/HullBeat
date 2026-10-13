package app.hullbeat.domain.export

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Amounts used to be formatted with the locale and then had every ','
 * replaced by a space. That was right for English grouping commas and ate
 * the decimal mark everywhere else: 1234.56 printed "1 234 56" in Ukrainian
 * and "1.234 56" in German and Spanish.
 */
class PdfNumberFormatTest {

    private val uk = Locale.forLanguageTag("uk")
    private val de = Locale.forLanguageTag("de")
    private val es = Locale.forLanguageTag("es")

    @Test
    fun `amount keeps the decimal mark of the language`() {
        assertEquals("1 234.56", formatAmount(1234.56, Locale.ENGLISH))
        assertEquals("1 234,56", formatAmount(1234.56, uk))
        assertEquals("1 234,56", formatAmount(1234.56, de))
        assertEquals("1 234,56", formatAmount(1234.56, es))
    }

    @Test
    fun `whole amount has no decimals and a plain space for thousands`() {
        assertEquals("12 500", formatAmount(12500.0, Locale.ENGLISH))
        assertEquals("12 500", formatAmount(12500.0, uk))
        assertEquals("950", formatAmount(950.0, de))
    }

    @Test
    fun `engine hours follow the language and are not grouped`() {
        assertEquals("1234.5", formatHours(1234.5, Locale.ENGLISH))
        assertEquals("1234,5", formatHours(1234.5, uk))
        assertEquals("1234,5", formatHours(1234.5, de))
        assertEquals("1234", formatHours(1234.0, es))
    }
}
