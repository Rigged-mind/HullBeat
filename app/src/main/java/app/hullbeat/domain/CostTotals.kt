package app.hullbeat.domain

import app.hullbeat.data.preferences.AppPreferences

/** Everything spent in one currency. [currency] is as stored on the record. */
data class CurrencyTotal(val currency: String?, val amount: Double)

/**
 * Costs summed per currency, never across them: 243 023 hryvnia and 450 euro
 * are two numbers, not 243 473 of anything. Largest first, so a boat kept
 * mostly in one currency leads with it.
 */
fun costTotals(costs: List<Pair<Double?, String?>>): List<CurrencyTotal> =
    costs
        .filter { it.first != null }
        .groupBy({ it.second }, { it.first!! })
        .map { (currency, amounts) -> CurrencyTotal(currency, amounts.sum()) }
        .sortedWith(compareByDescending<CurrencyTotal> { it.amount }.thenBy { it.currency.orEmpty() })

/**
 * "243 023 ₴ · 450 €", or one amount when there is one currency. Two listed
 * currencies that share a symbol (SEK and NOK are both "kr") print their
 * codes instead, otherwise the line would show "kr" twice with no way to
 * tell which is which. An empty list is a zero with no currency.
 */
fun formatCostTotals(totals: List<CurrencyTotal>, formatAmount: (Double) -> String): String {
    if (totals.isEmpty()) return formatAmount(0.0)
    val symbols = totals.map { AppPreferences.currencySymbol(it.currency) }
    val clashing = symbols.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return totals.zip(symbols).joinToString(" · ") { (total, symbol) ->
        val label = if (symbol in clashing) total.currency.orEmpty() else symbol
        if (label.isBlank()) formatAmount(total.amount) else "${formatAmount(total.amount)} $label"
    }
}
