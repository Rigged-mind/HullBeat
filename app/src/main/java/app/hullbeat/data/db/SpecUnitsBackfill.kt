package app.hullbeat.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import app.hullbeat.data.catalog.CatalogLoader
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.SpecUnits

/**
 * Gives passport values saved before they kept a unit the one they were
 * typed in - see [SpecUnits.backfill] for the order it decides by.
 *
 * Every open, not once per install, for the same reason as the currency
 * backfill: a backup made before this restores the same values without
 * units. A row that already has all its units is not rewritten.
 */
object SpecUnitsBackfill {

    fun run(db: SupportSQLiteDatabase, context: Context) {
        val settings = AppPreferences.getUnitSettings(context)
        // Values were once saved under the translated label rather than the
        // hint; the label in today's language is the one we can recognise.
        val hintByLabel = SpecUnits.QUANTITY_BY_HINT.keys.associateBy { CatalogLoader.hintString(context, it) }
        val hintOf: (String) -> String? = { key ->
            if (key in SpecUnits.QUANTITY_BY_HINT) key else hintByLabel[key]
        }

        val updates = mutableListOf<Triple<Long, String, String?>>()
        db.query("SELECT id, specJson, specUnitsJson FROM component WHERE specJson IS NOT NULL AND specJson != ''")
            .use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val spec = c.getString(1)
                    val units = if (c.isNull(2)) null else c.getString(2)
                    val result = runCatching { SpecUnits.backfill(spec, units, settings, hintOf) }
                        .onFailure { Log.w("SpecUnitsBackfill", "component $id", it) }
                        .getOrNull() ?: continue
                    updates += Triple(id, result.first, result.second)
                }
            }
        for ((id, spec, units) in updates) {
            val values = ContentValues().apply {
                put("specJson", spec)
                put("specUnitsJson", units)
            }
            db.update("component", SQLiteDatabase.CONFLICT_NONE, values, "id = ?", arrayOf(id))
        }
    }
}
