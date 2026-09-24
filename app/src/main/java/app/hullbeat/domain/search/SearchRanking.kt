package app.hullbeat.domain.search

import app.hullbeat.domain.search.SearchConfig.MAX_DROPPED_CHARS
import app.hullbeat.domain.search.SearchConfig.MIN_STEM_LENGTH
import app.hullbeat.domain.search.SearchConfig.STEM_COST
import app.hullbeat.domain.search.SearchConfig.Scores

enum class MatchSource {
    EXACT_TITLE,
    EXACT_EN_TITLE,
    PREFIX_TITLE,
    PREFIX_EN_TITLE,
    WORD_PREFIX_TITLE,
    WORD_PREFIX_EN_TITLE,
    EXACT_SYNONYM,
    PREFIX_SYNONYM,
    CONTAINS_SYNONYM,
    SUBSTRING_TITLE,
    CATEGORY,
    SPEC_OR_NOTES,
    NONE,
}

data class RankResult(
    val score: Int,
    val via: String? = null,
    val source: MatchSource = MatchSource.NONE,
)

/**
 * Ranks search candidates per docs/search-spec.md §6 & §9.
 *
 * All constants (stem cost, thresholds, scores) are generated from
 * design/search-ranking.json into SearchConfig.
 */
object SearchRanking {

    private val WORD_SPLIT = Regex("""[\s,()/·—\-]+""")

    /**
     * Builds variants with stemming penalties.
     * Drops up to MAX_DROPPED_CHARS (2) characters, never below MIN_STEM_LENGTH (3).
     */
    fun variants(query: String): List<Pair<String, Int>> {
        val q = query.trim().lowercase()
        val result = mutableListOf(Pair(q, 0))
        if (q.length - 1 >= MIN_STEM_LENGTH && MAX_DROPPED_CHARS >= 1) {
            result.add(Pair(q.dropLast(1), STEM_COST))
        }
        if (q.length - 2 >= MIN_STEM_LENGTH && MAX_DROPPED_CHARS >= 2) {
            result.add(Pair(q.dropLast(2), STEM_COST * 2))
        }
        return result
    }

    /**
     * Formats FTS4 match query from raw query and variants.
     */
    fun buildFtsQuery(query: String): String {
        val q = query.trim().lowercase()
        val vs = variants(q)
        val terms = mutableListOf<String>()

        val words = q.split(WORD_SPLIT).filter { it.isNotEmpty() }
        if (words.size > 1) {
            terms.add(words.joinToString(" ") { "$it*" })
            for (w in words) {
                if (w.length >= MIN_STEM_LENGTH) {
                    terms.add("$w*")
                }
            }
        } else {
            for ((v, _) in vs) {
                val clean = v.replace(Regex("""[^\p{L}\p{Nd}]"""), "")
                if (clean.length >= MIN_STEM_LENGTH) {
                    terms.add("$clean*")
                }
            }
        }

        if (q.contains("-")) {
            // An FTS phrase query: the whole hyphenated word in quotes,
            // so "sail-drive" matches as one term and not as two.
            terms.add("\"$q\"")
            val subWords = q.split("-").filter { it.isNotEmpty() }
            if (subWords.size > 1) {
                terms.add(subWords.joinToString(" ") { "$it*" })
            }
        }

        val uniq = terms.distinct()
        return if (uniq.isNotEmpty()) uniq.joinToString(" OR ") else "$q*"
    }

    fun pfx(text: String, variants: List<Pair<String, Int>>): Int {
        var best = -1
        for ((v, cost) in variants) {
            if (text.startsWith(v) && (best < 0 || cost < best)) {
                best = cost
            }
        }
        return best
    }

    fun wordPfx(text: String, variants: List<Pair<String, Int>>): Int {
        var best = -1
        for (w in text.split(WORD_SPLIT)) {
            if (w.isEmpty()) continue
            val cost = pfx(w, variants)
            if (cost >= 0 && (best < 0 || cost < best)) {
                best = cost
            }
        }
        return best
    }

    fun rank(
        query: String,
        title: String,
        englishTitle: String? = null,
        synonyms: List<String> = emptyList(),
        categoryName: String? = null,
        specJson: String? = null,
        notes: String? = null,
    ): RankResult {
        val q = query.trim().lowercase()
        if (q.length < 2) return RankResult(0)

        val vs = variants(q)
        val n = title.trim().lowercase()
        val e = englishTitle?.trim()?.lowercase()

        // 120: exact title
        if (n == q) return RankResult(Scores.EXACT_TITLE, source = MatchSource.EXACT_TITLE)

        // 110: exact English title
        if (e != null && e == q) {
            return RankResult(Scores.EXACT_EN_TITLE, via = englishTitle, source = MatchSource.EXACT_EN_TITLE)
        }

        // 90 - c: prefix title
        val cTitlePfx = pfx(n, vs)
        if (cTitlePfx >= 0) {
            return RankResult(Scores.PREFIX_TITLE - cTitlePfx, source = MatchSource.PREFIX_TITLE)
        }

        // 85 - c: prefix English title
        if (e != null) {
            val cEnPfx = pfx(e, vs)
            if (cEnPfx >= 0) {
                return RankResult(Scores.PREFIX_EN_TITLE - cEnPfx, via = englishTitle, source = MatchSource.PREFIX_EN_TITLE)
            }
        }

        // 70 - c: word prefix in title
        val cWordTitle = wordPfx(n, vs)
        if (cWordTitle >= 0) {
            return RankResult(Scores.WORD_PREFIX_TITLE - cWordTitle, source = MatchSource.WORD_PREFIX_TITLE)
        }

        // 65 - c: word prefix in English title
        if (e != null) {
            val cWordEn = wordPfx(e, vs)
            if (cWordEn >= 0) {
                return RankResult(Scores.WORD_PREFIX_EN_TITLE - cWordEn, via = englishTitle, source = MatchSource.WORD_PREFIX_EN_TITLE)
            }
        }

        // Synonyms
        var bestSynonym: RankResult? = null
        for (syn in synonyms) {
            val sLower = syn.trim().lowercase()
            var s = 0
            var src = MatchSource.NONE
            if (sLower == q) {
                s = Scores.EXACT_SYNONYM
                src = MatchSource.EXACT_SYNONYM
            } else {
                val cSynPfx = pfx(sLower, vs)
                if (cSynPfx >= 0) {
                    s = Scores.PREFIX_SYNONYM - cSynPfx
                    src = MatchSource.PREFIX_SYNONYM
                } else if (sLower.contains(q)) {
                    s = Scores.CONTAINS_SYNONYM
                    src = MatchSource.CONTAINS_SYNONYM
                }
            }
            if (s > 0 && (bestSynonym == null || s > bestSynonym.score)) {
                bestSynonym = RankResult(s, via = syn, source = src)
            }
        }
        if (bestSynonym != null) return bestSynonym

        // 40: substring in title
        if (n.contains(q)) {
            return RankResult(Scores.SUBSTRING_TITLE, source = MatchSource.SUBSTRING_TITLE)
        }

        // 30: category contains
        if (categoryName != null && categoryName.lowercase().contains(q)) {
            return RankResult(Scores.CONTAINS_CATEGORY, source = MatchSource.CATEGORY)
        }

        // 20: specJson or notes contains (Level 20)
        if (specJson != null && specJson.lowercase().contains(q)) {
            return RankResult(Scores.SPEC_OR_NOTES, via = "spec", source = MatchSource.SPEC_OR_NOTES)
        }
        if (notes != null && notes.lowercase().contains(q)) {
            return RankResult(Scores.SPEC_OR_NOTES, via = "notes", source = MatchSource.SPEC_OR_NOTES)
        }

        return RankResult(0)
    }
}
