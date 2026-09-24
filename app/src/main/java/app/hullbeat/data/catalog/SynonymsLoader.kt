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
     * Returns combined synonyms for both UK and EN locales for cross-lingual search.
     */
    fun synonymsFor(context: Context, catalogCode: String?): List<String> {
        if (catalogCode == null) return emptyList()
        val baseCode = catalogCode.substringBefore('#')
        val all = get(context)
        val ukSyns = all["uk"]?.get(baseCode) ?: emptyList()
        val enSyns = all["en"]?.get(baseCode) ?: emptyList()
        return (ukSyns + enSyns).distinct()
    }
}
