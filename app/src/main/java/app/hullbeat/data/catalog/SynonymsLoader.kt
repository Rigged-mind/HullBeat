package app.hullbeat.data.catalog

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Loads synonyms from assets/catalog/synonyms.json for cross-lingual search (docs/search-spec.md §4 & §8).
 */
object SynonymsLoader {

    private const val ASSET_PATH = "catalog/synonyms.json"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Volatile
    private var cached: Map<String, Map<String, List<String>>>? = null

    fun load(context: Context): Map<String, Map<String, List<String>>> =
        try {
            context.assets.open(ASSET_PATH).bufferedReader().use {
                json.decodeFromString(it.readText())
            }
        } catch (_: Exception) {
            emptyMap()
        }

    fun get(context: Context): Map<String, Map<String, List<String>>> =
        cached ?: synchronized(this) {
            cached ?: load(context).also { cached = it }
        }

    /**
     * Synonyms from every locale in the asset, for cross-lingual search: an
     * owner reading the app in English still types "Impeller" or "rodete".
     *
     * This used to read `uk` and `en` by name, so a third locale's aliases
     * would have shipped in the asset and matched nothing. UK and EN still
     * come first, in their old order, so the index body of an existing
     * install does not change; any other locale follows in tag order.
     */
    fun synonymsFor(context: Context, catalogCode: String?): List<String> {
        if (catalogCode == null) return emptyList()
        val baseCode = catalogCode.substringBefore('#')
        val all = get(context)
        val order = listOf("uk", "en") + (all.keys - setOf("uk", "en")).sorted()
        return order.flatMap { all[it]?.get(baseCode).orEmpty() }.distinct()
    }
}
