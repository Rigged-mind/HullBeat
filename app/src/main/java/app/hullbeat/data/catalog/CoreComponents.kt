package app.hullbeat.data.catalog

/**
 * Baseline core components that should appear on a newly created vessel.
 *
 * All other catalog components remain in the catalog library (archived)
 * until selected by the owner or until service is performed on them.
 */
object CoreComponents {
    val CODES: Set<String> = setOf(
        // Hull & Steering
        "hull_gelcoat",
        "antifouling",
        "rudder",
        "steering_system",
        "seacocks",
        "through_hulls",
        "anodes_hull",
        // Engine & Fuel
        "engine_oil",
        "engine_oil_filter",
        "raw_water_impeller",
        "fuel_prefilter",
        "fuel_tank",
        "propeller",
        // Electrical & Bilge
        "house_batteries",
        "start_battery",
        "bilge_pump_auto",
        // Ground tackle & Safety
        "primary_anchor",
        "anchor_chain",
        "lifejackets",
        "liferaft",
        // Sails & Rigging
        "mainsail",
        "headsail",
        "mast",
        "boom",
    )

    fun isCore(catalogCode: String?): Boolean {
        if (catalogCode == null) return false
        val base = catalogCode.substringBefore('#')
        return base in CODES
    }
}
