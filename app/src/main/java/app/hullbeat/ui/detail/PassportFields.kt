package app.hullbeat.ui.detail

import android.content.Context
import app.hullbeat.R
import app.hullbeat.data.catalog.CatalogLoader
import app.hullbeat.data.db.Component
import app.hullbeat.domain.SpecQuantity
import app.hullbeat.domain.SpecUnit
import app.hullbeat.domain.SpecUnits
import org.json.JSONObject

data class PassportField(
    val key: String,
    val value: String,
    val displayLabel: String = key,
    /** What the value measures, when Settings has a unit for it; null for text fields. */
    val quantity: SpecQuantity? = null,
    /** The unit [value] was typed in. Null while the field is empty. */
    val unit: SpecUnit? = null,
) {
    /**
     * The label without a unit in it: "Обʼєм (л)" would contradict a value
     * shown in gallons, so the unit is printed after the value instead.
     */
    fun displayLabel(context: Context): String =
        if (key == "Litres used") context.getString(R.string.spec_label_amount_used)
        else SpecUnits.stripLabelUnit(key, app.hullbeat.data.db.displayPassportLabel(context, key))

    /**
     * The value as typed, in the unit it was typed in - not converted to
     * today's Settings, so the screen and the PDF show what was recorded.
     * Blank when the field is empty.
     */
    fun displayValue(context: Context): String {
        if (value.isBlank()) return ""
        val unit = unit?.takeUnless { SpecUnits.mentionsUnit(value, it.quantity) } ?: return value
        return value + " " + context.getString(unit.shortNameRes)
    }
}

/**
 * A node's passport: catalog hints first, in catalog order and shown even
 * while empty, then any field the owner added. One source for the node
 * screen and the PDF passport, so the two cannot say different things.
 *
 * [includeIdentity] puts make, model, serial and year in front; the PDF
 * leaves them out because its equipment table has columns for them.
 */
fun resolvePassportFields(
    context: Context,
    component: Component,
    includeIdentity: Boolean = true,
): List<PassportField> {
    val fields = mutableListOf<PassportField>()
    if (includeIdentity) {
        val res = context.resources
        if (!component.make.isNullOrBlank()) fields += PassportField(res.getString(R.string.passport_make), component.make)
        if (!component.model.isNullOrBlank()) fields += PassportField(res.getString(R.string.passport_model), component.model)
        if (!component.serial.isNullOrBlank()) fields += PassportField(res.getString(R.string.passport_serial), component.serial)
        if (component.yearInstalled != null) fields += PassportField(res.getString(R.string.passport_year), component.yearInstalled.toString())
    }

    val rawHints = try {
        CatalogLoader.rawSpecHintsFor(context, component.catalogCode)
    } catch (_: Exception) {
        emptyList()
    }

    val jsonObj = if (!component.specJson.isNullOrBlank()) {
        runCatching { JSONObject(component.specJson) }.getOrNull()
    } else null

    val units = SpecUnits.readUnits(component.specUnitsJson)
    val processedKeys = mutableSetOf<String>()

    // 1. Catalog hints first (always visible!)
    for (hint in rawHints) {
        val label = CatalogLoader.hintString(context, hint)
        val sourceKey = jsonObj?.let {
            if (it.has(hint)) hint else if (it.has(label)) label else null
        }
        val value = sourceKey?.let { jsonObj.optString(it, "") } ?: ""
        fields += PassportField(
            key = hint,
            value = value,
            displayLabel = label,
            quantity = SpecUnits.quantityOf(hint),
            unit = if (value.isBlank()) null else sourceKey?.let { units[it] },
        )
        processedKeys.add(hint)
        processedKeys.add(label)
    }

    // 2. Any additional custom fields from specJson
    if (jsonObj != null) {
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k !in processedKeys) {
                val v = jsonObj.optString(k, "")
                val label = CatalogLoader.hintString(context, k)
                fields += PassportField(
                    key = k,
                    value = v,
                    displayLabel = label,
                    quantity = SpecUnits.quantityOf(k),
                    unit = if (v.isBlank()) null else units[k],
                )
                processedKeys.add(k)
                processedKeys.add(label)
            }
        }
    }
    return fields
}
