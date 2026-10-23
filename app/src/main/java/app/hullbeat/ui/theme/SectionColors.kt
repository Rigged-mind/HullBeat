package app.hullbeat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Authentic maritime colors for vessel systems and equipment categories.
 *
 * Each zone uses authentic nautical materials and standards rather than
 * generic flat UI colors:
 * - Propulsion & Fuel: Marine cast bronze & copper (propellers, shafts, machinery)
 * - Electrical: Warm amber & brass (terminals, masthead lamps)
 * - Hull & Anchoring: Baltic emerald / dark petrol teal (HullBeat brand)
 * - Plumbing & Bilge: Deep fjord cyan (seawater, seacocks, bilges)
 * - Rigging & Sails: Dacron navy / offshore midnight (storm sky & sails)
 * - Safety: deep magenta. NOT the SOLAS orange or red it would naturally be:
 *   red is "overdue" and amber is "due soon" in every scheme, and a liferaft
 *   card framed in either reads as a status it does not have. Hue 316 sits
 *   in the one band no status and no other section uses.
 * - Navigation: Chart ultramarine (night navigation displays & charts)
 * - General / Other: Weathered teak & canvas slate
 */
@Immutable
data class SectionColor(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

object SectionColors {
    val Propulsion = SectionColor(
        accent = Color(0xFF8D4A16),
        container = Color(0xFFFBF1E8),
        onContainer = Color(0xFF5A2A06),
    )

    val Electrical = SectionColor(
        accent = Color(0xFF966200),
        container = Color(0xFFFDF6E2),
        onContainer = Color(0xFF5E3C00),
    )

    val HullDeck = SectionColor(
        accent = Color(0xFF14403A),
        container = Color(0xFFE6F0ED),
        onContainer = Color(0xFF0E2E29),
    )

    val Plumbing = SectionColor(
        accent = Color(0xFF0E6476),
        container = Color(0xFFE8F6F8),
        onContainer = Color(0xFF08414D),
    )

    val Rigging = SectionColor(
        accent = Color(0xFF1E3A6E),
        container = Color(0xFFEDF2FB),
        onContainer = Color(0xFF102244),
    )

    val Safety = SectionColor(
        accent = Color(0xFF9C1F7A),
        container = Color(0xFFFAEDF6),
        onContainer = Color(0xFF5C0F47),
    )

    val Navigation = SectionColor(
        accent = Color(0xFF53388F),
        container = Color(0xFFF4EFFB),
        onContainer = Color(0xFF33205E),
    )

    val Slate = SectionColor(
        accent = Color(0xFF4D5C58),
        container = Color(0xFFEFF3F2),
        onContainer = Color(0xFF2A3633),
    )

    fun forCategory(code: String): SectionColor = when (code) {
        "engine_main", "engine", "genset", "drivetrain", "fuel" -> Propulsion
        "elec_dc", "elec_ac" -> Electrical
        "hull", "deck", "ground_tackle" -> HullDeck
        "plumbing", "gas", "galley", "hvac" -> Plumbing
        "standing_rig", "running_rig", "sails" -> Rigging
        "safety" -> Safety
        "nav" -> Navigation
        else -> Slate
    }
}

/**
 * Returns the [SectionColor] for a given category code depending on the active [Scheme].
 * When in [Scheme.COLORFUL], authentic maritime section hues are used.
 * In other schemes, standard theme tones are preserved.
 */
@Composable
@ReadOnlyComposable
fun categorySectionColor(code: String, scheme: Scheme = LocalScheme.current): SectionColor {
    return if (scheme == Scheme.COLORFUL) {
        SectionColors.forCategory(code)
    } else {
        SectionColor(
            accent = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
