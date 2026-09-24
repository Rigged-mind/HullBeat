package app.hullbeat.domain.search

import android.content.Context
import app.hullbeat.data.catalog.SynonymsLoader
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.SearchIndex
import app.hullbeat.data.db.displayName

object SearchIndexBuilder {

    /**
     * Builds the SearchIndex row for a component per docs/search-spec.md §8:
     * - SearchIndex.title = canonical display name (customName ?: cmp_<code> ?: name)
     * - SearchIndex.body  = synonyms + english name + category + spec values + notes
     */
    fun build(
        context: Context,
        component: Component,
        categoryName: String? = null,
    ): SearchIndex {
        val title = component.displayName(context)
        val bodyParts = mutableListOf<String>()

        // For renamed catalog components: also include the original catalog name in body
        if (component.customName != null && component.catalogCode != null) {
            val base = component.catalogCode.substringBefore('#')
            val resId = context.resources.getIdentifier("cmp_$base", "string", context.packageName)
            if (resId != 0) {
                bodyParts.add(context.getString(resId))
            }
        }

        // English name from seed
        if (component.name.isNotBlank() && component.name != title) {
            bodyParts.add(component.name)
        }

        // Category name
        categoryName?.takeIf { it.isNotBlank() }?.let { bodyParts.add(it) }

        // Synonyms across both UK and EN
        component.catalogCode?.let { code ->
            val syns = SynonymsLoader.synonymsFor(context, code)
            bodyParts.addAll(syns)
        }

        // Passport specs (keys and values)
        component.specJson?.takeIf { it.isNotBlank() }?.let { spec ->
            bodyParts.add(cleanJson(spec))
        }

        // Notes
        component.notes?.takeIf { it.isNotBlank() }?.let { bodyParts.add(it) }

        return SearchIndex(
            ownerType = "component",
            ownerId = component.id,
            vesselId = component.vesselId,
            title = title,
            body = bodyParts.joinToString(" "),
        )
    }

    private fun cleanJson(jsonStr: String): String =
        jsonStr.replace(Regex("""[{}"\[\]:,]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
