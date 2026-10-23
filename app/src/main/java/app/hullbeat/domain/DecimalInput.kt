package app.hullbeat.domain

/**
 * Reading a number the owner typed into a meter or cost field.
 *
 * `String.toDoubleOrNull()` was used directly, and it knows only the dot.
 * A Ukrainian, German or Spanish keyboard offers a comma on the decimal pad,
 * so "1234,5" came back null - and the forms treated null as "left blank":
 * the record saved without its meter reading, and an edited record lost the
 * cost it already had. Nothing on screen said so.
 *
 * Both separators are accepted, one of them, once. Grouping is not guessed:
 * "1.234,5" could be either convention, so it is refused rather than read as
 * the wrong thousand. Spaces are dropped, since "1 234" is how uk and de
 * write a grouped number and nobody means two numbers in one field.
 */
object DecimalInput {

    private val NUMBER = Regex("""^(\d+[.,]?\d*|[.,]\d+)$""")

    /**
     * The value, or null when [raw] is blank or is not a non-negative number.
     * Use [isAcceptable] to tell those two apart before saving.
     */
    fun parse(raw: String): Double? {
        val text = raw.filterNot { it.isWhitespace() || it == ' ' || it == ' ' }
        if (!NUMBER.matches(text)) return null
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /** Blank (the field is optional) or a number [parse] can read. */
    fun isAcceptable(raw: String): Boolean = raw.isBlank() || parse(raw) != null

    /**
     * What a keystroke may leave in the field: digits and at most one
     * separator. Lets "12," stand while the owner is still typing.
     */
    fun isPartialInput(raw: String): Boolean =
        raw.isEmpty() || Regex("""^\d*[.,]?\d*$""").matches(raw)
}
