package app.hullbeat.data.search

import android.content.Context
import app.hullbeat.data.catalog.SynonymsLoader
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.displayName
import app.hullbeat.domain.search.MatchSource
import app.hullbeat.domain.search.SearchIndexBuilder
import app.hullbeat.domain.search.SearchRanking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchSuggestion(
    val component: Component,
    val title: String,
    val categoryName: String,
    val score: Int,
    val via: String? = null,
    val source: MatchSource = MatchSource.NONE,
)

/**
 * Executes FTS queries and performs ranking in Kotlin per docs/search-spec.md §6.
 */
class SearchRepository(
    private val db: AppDatabase,
    private val context: Context,
) {

    /**
     * Resolves category name by code from Android string resources (cat_<code>).
     */
    fun categoryNameFor(catCode: String): String {
        val normalizedCode = when (catCode) {
            "engine" -> "engine_main"
            "electrical" -> "elec_dc"
            "hull_deck" -> "hull"
            else -> catCode
        }
        val resId = context.resources.getIdentifier("cat_$normalizedCode", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else catCode
    }

    /**
     * Search with 150ms debounce and min 2 characters, returning up to [limit] suggestions.
     * Tie-breaks on:
     *   1. score DESC
     *   2. criticality DESC (HIGH > MED > LOW)
     *   3. id DESC (favors recently created custom components)
     */
    suspend fun search(vesselId: Long, rawQuery: String, limit: Int = 8): List<SearchSuggestion> =
        withContext(Dispatchers.IO) {
            val q = rawQuery.trim()
            if (q.length < 2) return@withContext emptyList()

            val ftsQuery = SearchRanking.buildFtsQuery(q)
            val ftsHits = try {
                db.searchDao().search(vesselId, ftsQuery, limit = 100)
            } catch (_: Exception) {
                emptyList()
            }

            if (ftsHits.isEmpty()) return@withContext emptyList()

            val componentIds = ftsHits.map { it.ownerId }.distinct()
            val candidateComponents = componentIds.mapNotNull { db.componentDao().byId(it) }
                .filter { !it.archived && it.vesselId == vesselId }

            val scored = mutableListOf<SearchSuggestion>()
            for (comp in candidateComponents) {
                val title = comp.displayName(context)
                val catName = categoryNameFor(comp.categoryCode)
                val synonyms = SynonymsLoader.synonymsFor(context, comp.catalogCode)

                val rank = SearchRanking.rank(
                    query = q,
                    title = title,
                    englishTitle = comp.name,
                    synonyms = synonyms,
                    categoryName = catName,
                    specJson = comp.specJson,
                    notes = comp.notes,
                )

                if (rank.score > 0) {
                    scored.add(
                        SearchSuggestion(
                            component = comp,
                            title = title,
                            categoryName = catName,
                            score = rank.score,
                            via = rank.via,
                            source = rank.source,
                        )
                    )
                }
            }

            scored.sortWith(
                compareByDescending<SearchSuggestion> { it.score }
                    .thenByDescending { critWeight(it.component.criticality) }
                    .thenByDescending { it.component.id }
            )

            scored.take(limit)
        }

    suspend fun indexComponent(component: Component) = withContext(Dispatchers.IO) {
        val catName = categoryNameFor(component.categoryCode)
        val index = SearchIndexBuilder.build(context, component, catName)
        db.searchDao().reindex(index)
    }

    suspend fun removeComponent(componentId: Long) = withContext(Dispatchers.IO) {
        db.searchDao().removeFor("component", componentId)
    }

    suspend fun indexAllForVessel(vesselId: Long) = withContext(Dispatchers.IO) {
        db.searchDao().clearForVessel(vesselId)
        val components = db.componentDao().forVessel(vesselId)
        val indexes = components.map { comp ->
            val catName = categoryNameFor(comp.categoryCode)
            SearchIndexBuilder.build(context, comp, catName)
        }
        db.searchDao().indexAll(indexes)
    }

    private fun critWeight(crit: Criticality): Int = when (crit) {
        Criticality.HIGH -> 2
        Criticality.MED -> 1
        Criticality.LOW -> 0
    }
}
