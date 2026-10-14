package app.hullbeat.data.search

import android.content.Context
import app.hullbeat.data.db.AppDatabase
import java.util.zip.CRC32

/**
 * Rebuilds the search index when what it was built from has changed.
 *
 * The FTS body of a component holds its synonyms from every locale and its
 * category name in the language of the moment it was indexed, and it was only
 * ever built once - when the vessel was set up. So an install from before
 * German and Spanish shipped never learned "Ankerwinde" or "molinete", and an
 * owner who switched the app to another language kept searching category
 * names in the old one. Search ranks only what FTS returns, so a word missing
 * from the index is a search that finds nothing.
 *
 * The signature is the app language plus a checksum of synonyms.json. When
 * either differs from the one the index was built with, every active vessel
 * is re-indexed - a few hundred rows, once.
 */
object SearchIndexFreshness {

    private const val PREFS_NAME = "hullbeat_prefs"
    private const val KEY_SIGNATURE = "search_index_signature"
    private const val SYNONYMS_ASSET = "catalog/synonyms.json"

    fun signature(context: Context): String {
        val crc = CRC32()
        try {
            context.assets.open(SYNONYMS_ASSET).use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    crc.update(buf, 0, n)
                }
            }
        } catch (_: Exception) {
            // No asset: the language alone still decides.
        }
        val language = context.resources.configuration.locales[0].toLanguageTag()
        return "$language|${crc.value}"
    }

    /** Re-index if stale. Call from a background coroutine with a localized Context. */
    suspend fun refreshIfStale(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = signature(context)
        if (prefs.getString(KEY_SIGNATURE, null) == current) return
        val repo = SearchRepository(db, context)
        db.vesselDao().allActive().forEach { repo.indexAllForVessel(it.id) }
        prefs.edit().putString(KEY_SIGNATURE, current).apply()
    }
}
