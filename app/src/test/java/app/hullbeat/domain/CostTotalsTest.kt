package app.hullbeat.domain

import app.hullbeat.data.preferences.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class CostTotalsTest {

    private val plain: (Double) -> String = { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }

    @Test
    fun `one currency is one sum`() {
        val totals = costTotals(listOf(100.0 to "UAH", 23.0 to "UAH", null to "UAH"))
        assertEquals(listOf(CurrencyTotal("UAH", 123.0)), totals)
        assertEquals("123 ₴", formatCostTotals(totals, plain))
    }

    @Test
    fun `mixed currencies are summed apart, largest first`() {
        val totals = costTotals(
            listOf(450.0 to "EUR", 243_000.0 to "UAH", 23.0 to "UAH", null to "EUR")
        )
        assertEquals(listOf(CurrencyTotal("UAH", 243_023.0), CurrencyTotal("EUR", 450.0)), totals)
        assertEquals("243023 ₴ · 450 €", formatCostTotals(totals, plain))
    }

    @Test
    fun `a custom currency prints as the owner wrote it`() {
        val totals = costTotals(listOf(80.0 to "CHF", 20.0 to "EUR"))
        assertEquals("80 CHF · 20 €", formatCostTotals(totals, plain))
    }

    @Test
    fun `currencies that share a symbol print their codes`() {
        val totals = costTotals(listOf(500.0 to "SEK", 300.0 to "NOK", 10.0 to "EUR"))
        assertEquals("500 SEK · 300 NOK · 10 €", formatCostTotals(totals, plain))
    }

    @Test
    fun `no costs is a bare zero`() {
        assertEquals("0", formatCostTotals(costTotals(listOf(null to "UAH")), plain))
    }

    @Test
    fun `a listed symbol stores its code, a shared or unknown one stores itself`() {
        assertEquals("UAH", AppPreferences.currencyCodeFor("₴"))
        assertEquals("EUR", AppPreferences.currencyCodeFor("€"))
        // SEK or NOK - the symbol cannot say, so it is not guessed.
        assertEquals("kr", AppPreferences.currencyCodeFor("kr"))
        assertEquals("CHF", AppPreferences.currencyCodeFor("CHF"))
    }

    @Test
    fun `a stored code prints its symbol, anything else prints as stored`() {
        assertEquals("₴", AppPreferences.currencySymbol("UAH"))
        assertEquals("kr", AppPreferences.currencySymbol("NOK"))
        assertEquals("CHF", AppPreferences.currencySymbol("CHF"))
        assertEquals("", AppPreferences.currencySymbol(null))
    }
}
