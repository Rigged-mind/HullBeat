package app.hullbeat.domain.importer

import app.hullbeat.R
import app.hullbeat.data.db.DatePrecision
import java.text.Normalizer
import java.time.LocalDate

enum class TargetField(val titleRes: Int) {
    DATE(R.string.field_map_date),
    DESCRIPTION(R.string.field_map_desc),
    WORK_TYPE(R.string.field_map_work_type),
    HOURS(R.string.field_map_hours),
    COST(R.string.field_map_cost),
    PERFORMED_BY(R.string.field_map_who),
    NOTES(R.string.field_map_notes),
    COMPONENT(R.string.field_map_component),
    IGNORE(R.string.field_map_ignore),
}

object ColumnMapper {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    /**
     * A synonym this short is matched as a whole word, never as a substring.
     * "h" is a fine header for engine hours on its own, but as a substring it
     * claimed "Mechanic", "Mechaniker", "Fecha" and "Beschreibung" for the
     * engine-hours column - every one of them a header that has an h in it.
     */
    private const val WHOLE_WORD_MAX = 3

    /**
     * ES and DE aliases below are a draft, pending review by a native-speaking
     * sailor (catalog/i18n/ marks the catalog terms `check` for the same
     * reason). A wrong one here only costs a column the owner re-maps by hand.
     * They are written without accents because headers and synonyms go
     * through the same [words] folding.
     */
    private val DATE_SYNONYMS = syn(
        "date", "datum", "when", "day",
        "\u0434\u0430\u0442\u0430", "\u0434\u0435\u043d\u044c", "\u0447\u0430\u0441",
        "fecha", "dia", "tag", "wann"
    )

    private val DESC_SYNONYMS = syn(
        "task", "description", "work", "job", "service", "maintenance", "action",
        "\u043e\u043f\u0438\u0441", "\u0440\u043e\u0431\u043e\u0442\u0430",
        "\u0440\u043e\u0431\u043e\u0442\u0438", "\u0437\u0430\u0432\u0434\u0430\u043d\u043d\u044f",
        "descripcion", "trabajo", "tarea", "servicio", "mantenimiento",
        "beschreibung", "arbeit", "taetigkeit", "tatigkeit", "wartung", "aufgabe", "massnahme"
    )

    private val WORK_TYPE_SYNONYMS = syn(
        "worktype", "type", "kind", "category",
        "\u0442\u0438\u043f", "\u0432\u0438\u0434", "\u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0456\u044f",
        "tipo", "clase", "categoria", "art", "typ", "kategorie", "arbeitsart", "wartungsart"
    )

    private val HOURS_SYNONYMS = syn(
        "hours", "hrs", "h", "engine hours", "eng hrs", "meter", "motohours",
        "\u0433\u043e\u0434\u0438\u043d\u0438", "\u043c\u043e\u0442\u043e\u0433\u043e\u0434\u0438\u043d\u0438",
        "\u043c\u043e\u0442\u043e\u0447\u0430\u0441\u0438", "\u0433\u043e\u0434",
        "horas", "horometro", "contador", "stunden", "zahlerstand", "zaehlerstand"
    )

    private val COST_SYNONYMS = syn(
        "cost", "price", "amount", "total", "sum", "expense",
        "\u0432\u0430\u0440\u0442\u0456\u0441\u0442\u044c", "\u0446\u0456\u043d\u0430",
        "\u0441\u0443\u043c\u0430", "\u0432\u0438\u0442\u0440\u0430\u0442\u0438",
        "coste", "costo", "precio", "importe", "kosten", "preis", "betrag", "summe", "gesamt"
    )

    private val PERFORMED_BY_SYNONYMS = syn(
        "by", "who", "vendor", "mechanic", "yard", "technician", "performed by",
        "\u0432\u0438\u043a\u043e\u043d\u0430\u0432\u0435\u0446\u044c", "\u0445\u0442\u043e",
        "\u043c\u0430\u0439\u0441\u0442\u0435\u0440", "\u0441\u0435\u0440\u0432\u0456\u0441",
        "realizadopor", "hechopor", "mecanico", "taller", "tecnico", "quien", "proveedor",
        "durchgefuhrtvon", "mechaniker", "werft", "techniker", "wer", "firma"
    )

    private val NOTES_SYNONYMS = syn(
        "notes", "remarks", "comments", "anmerkungen", "info",
        "\u043d\u043e\u0442\u0430\u0442\u043a\u0438", "\u043f\u0440\u0438\u043c\u0456\u0442\u043a\u0438",
        "\u043a\u043e\u043c\u0435\u043d\u0442\u0430\u0440\u0456",
        "notas", "observaciones", "comentarios", "bemerkungen", "notizen", "kommentar", "hinweis"
    )

    private val COMPONENT_SYNONYMS = syn(
        "component", "system", "part", "equipment", "item",
        "\u0432\u0443\u0437\u043e\u043b", "\u0430\u0433\u0440\u0435\u0433\u0430\u0442",
        "\u0434\u0435\u0442\u0430\u043b\u044c", "\u0441\u0438\u0441\u0442\u0435\u043c\u0430",
        "componente", "sistema", "pieza", "equipo", "elemento",
        "komponente", "bauteil", "teil", "ausrustung", "gerat"
    )

    fun suggestMapping(headers: List<String>, sampleRows: List<List<String>>): Map<Int, TargetField> {
        val mapping = mutableMapOf<Int, TargetField>()
        val assignedFields = mutableSetOf<TargetField>()

        // 1. First pass: exact header matching
        headers.forEachIndexed { index, header ->
            val matchedField = matchHeader(Header(words(header)))
            if (matchedField != null && matchedField !in assignedFields) {
                mapping[index] = matchedField
                assignedFields.add(matchedField)
            }
        }

        // 2. Second pass: value analysis for unassigned columns
        headers.forEachIndexed { index, _ ->
            if (index !in mapping) {
                val values = sampleRows.mapNotNull { if (index < it.size) it[index].trim() else null }
                    .filter { it.isNotEmpty() }
                if (values.isNotEmpty()) {
                    val field = analyzeValues(values, assignedFields)
                    if (field != null) {
                        mapping[index] = field
                        assignedFields.add(field)
                    } else {
                        mapping[index] = TargetField.IGNORE
                    }
                } else {
                    mapping[index] = TargetField.IGNORE
                }
            }
        }

        return mapping
    }

    /**
     * Words of a header or a synonym: letters and digits of any script, lower
     * case, accents folded and ß spelled ss - so "Descripción", "descripcion"
     * and "DESCRIPCION" are one header. The old filter kept only a-z and
     * Cyrillic, which dropped the ó from "Descripción" and the ü from
     * "Ausrüstung" and left words no synonym could match.
     */
    private fun words(str: String): List<String> =
        Normalizer.normalize(str.lowercase().replace("\u00df", "ss"), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .split(NON_WORD)
            .filter { it.isNotEmpty() }

    /** A synonym is stored the way a header's words are compared: joined. */
    private fun syn(vararg phrases: String): List<String> =
        phrases.map { words(it).joinToString("") }

    private class Header(val words: List<String>) {
        val joined = words.joinToString("")
    }

    private fun List<String>.matches(header: Header): Boolean = any { synonym ->
        if (synonym.length <= WHOLE_WORD_MAX) synonym in header.words
        else header.joined.contains(synonym)
    }

    private fun matchHeader(header: Header): TargetField? {
        if (header.words.isEmpty()) return null
        return when {
            DATE_SYNONYMS.matches(header) -> TargetField.DATE
            HOURS_SYNONYMS.matches(header) -> TargetField.HOURS
            COST_SYNONYMS.matches(header) -> TargetField.COST
            WORK_TYPE_SYNONYMS.matches(header) -> TargetField.WORK_TYPE
            PERFORMED_BY_SYNONYMS.matches(header) -> TargetField.PERFORMED_BY
            NOTES_SYNONYMS.matches(header) -> TargetField.NOTES
            COMPONENT_SYNONYMS.matches(header) -> TargetField.COMPONENT
            DESC_SYNONYMS.matches(header) -> TargetField.DESCRIPTION
            else -> null
        }
    }

    private fun analyzeValues(values: List<String>, assigned: Set<TargetField>): TargetField? {
        // Date detection
        if (TargetField.DATE !in assigned) {
            val dateMatches = values.count { parseDate(it) != null }
            if (dateMatches.toDouble() / values.size >= 0.6) {
                return TargetField.DATE
            }
        }

        // Hours detection
        if (TargetField.HOURS !in assigned) {
            val hourMatches = values.count { parseHours(it) != null }
            if (hourMatches.toDouble() / values.size >= 0.7) {
                return TargetField.HOURS
            }
        }

        // Cost detection
        if (TargetField.COST !in assigned) {
            val costMatches = values.count {
                it.contains('$') || it.contains('€') || it.contains('£') || it.contains('₴')
            }
            if (costMatches > 0 && costMatches.toDouble() / values.size >= 0.4) {
                return TargetField.COST
            }
        }

        // Description detection
        if (TargetField.DESCRIPTION !in assigned) {
            val avgLen = values.map { it.length }.average()
            if (avgLen > 6) {
                return TargetField.DESCRIPTION
            }
        }

        return null
    }

    fun parseDate(raw: String): Pair<LocalDate, DatePrecision>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // ISO YYYY-MM-DD
        try {
            return Pair(LocalDate.parse(trimmed), DatePrecision.DAY)
        } catch (_: Exception) {}

        // DD.MM.YYYY
        val dotParts = trimmed.split('.')
        if (dotParts.size == 3) {
            val d = dotParts[0].toIntOrNull()
            val m = dotParts[1].toIntOrNull()
            val y = dotParts[2].toIntOrNull()
            if (d != null && m != null && y != null && d in 1..31 && m in 1..12) {
                val fullYear = if (y < 100) 2000 + y else y
                try {
                    return Pair(LocalDate.of(fullYear, m, d), DatePrecision.DAY)
                } catch (_: Exception) {}
            }
        }

        // DD/MM/YYYY or MM/DD/YYYY
        val slashParts = trimmed.split('/')
        if (slashParts.size == 3) {
            val p0 = slashParts[0].toIntOrNull()
            val p1 = slashParts[1].toIntOrNull()
            val p2 = slashParts[2].toIntOrNull()
            if (p0 != null && p1 != null && p2 != null) {
                val fullYear = if (p2 < 100) 2000 + p2 else p2
                val (d, m) = if (p0 > 12 && p1 <= 12) {
                    Pair(p0, p1)
                } else if (p1 > 12 && p0 <= 12) {
                    Pair(p1, p0)
                } else {
                    Pair(p0, p1)
                }
                if (d in 1..31 && m in 1..12) {
                    try {
                        return Pair(LocalDate.of(fullYear, m, d), DatePrecision.DAY)
                    } catch (_: Exception) {}
                }
            }
        }

        // Year only e.g. "2019"
        val yearOnly = trimmed.toIntOrNull()
        if (yearOnly != null && yearOnly in 1970..2040) {
            return Pair(LocalDate.of(yearOnly, 7, 1), DatePrecision.YEAR)
        }

        return null
    }

    fun parseHours(raw: String): Double? {
        val cleaned = raw.replace(',', '.').replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull()
    }

    fun parseCost(raw: String): Double? {
        val cleaned = raw.replace(',', '.').replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull()
    }
}