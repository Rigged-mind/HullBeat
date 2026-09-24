package app.hullbeat.data.catalog

import android.content.Context
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.search.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.room.withTransaction

/**
 * Turns an answered profile into a boat with a catalog under it.
 *
 * ⚠️ This is the piece that was missing when the app first ran on a phone.
 * `CatalogSeeder` existed, 329 nodes existed, the whole `appliesTo` grammar
 * existed - and nothing anywhere called `seedFor`, nothing anywhere inserted
 * a `Vessel`. The only code path that ever wrote a row was "create your own
 * node", so that is all the owner was offered: an empty screen and a button
 * to type the boat in by hand.
 *
 * Everything the catalog decides hangs off the object created here.
 */
object VesselSetup {

    /** Insert the boat, then seed everything the catalog says she has. */
    suspend fun create(context: Context, db: AppDatabase, vessel: Vessel): Long =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val vesselId = db.vesselDao().insert(vessel)
                seed(context, db, vessel.copy(id = vesselId))
                vesselId
            }
        }

    /**
     * Seed, or top up after a catalog update.
     *
     * `allCatalogCodesForVessel` is the right source for `existingCodes`
     * precisely because it does NOT filter archived rows: a node the owner
     * archived must stay archived across releases.
     */
    suspend fun seed(context: Context, db: AppDatabase, vessel: Vessel): Int =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val existing = db.componentDao().allCatalogCodesForVessel(vessel.id).toSet()
                val seeded = CatalogSeeder(CatalogLoader.get(context)).seedFor(vessel, existing)

                // Pass one: the rows. A child cannot point at a parent that has
                // no id yet, so parents are wired in pass two.
                val inserted = mutableMapOf<String, Component>()
                for (component in seeded.components) {
                    val row = component.copy(vesselId = vessel.id)
                    val id = db.componentDao().insert(row)
                    row.catalogCode?.let { inserted[it] = row.copy(id = id) }
                }

                // Pass two: parents.
                // Look up in freshly inserted components, falling back to components
                // already in the database from a previous catalog release.
                val allVesselComponents = db.componentDao().allForVessel(vessel.id)
                    .filter { it.catalogCode != null }
                    .associateBy { it.catalogCode!! }

                for ((childCode, parentCode) in seeded.parentByCode) {
                    val child = inserted[childCode] ?: continue
                    val parent = inserted[parentCode] ?: allVesselComponents[parentCode] ?: continue
                    db.componentDao().update(child.copy(parentId = parent.id))
                }

                // Pass three: the schedules that make anything become due at all.
                val schedules = seeded.schedulesByCode.mapNotNull { entry ->
                    inserted[entry.key]?.let { entry.value.copy(componentId = it.id) }
                }
                if (schedules.isNotEmpty()) {
                    db.scheduleDao().insertAll(schedules)
                }

                // And the search index, or every seeded node is unfindable.
                SearchRepository(db, context).indexAllForVessel(vessel.id)
                inserted.size
            }
        }
}
