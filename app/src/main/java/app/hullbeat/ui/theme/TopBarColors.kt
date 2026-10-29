package app.hullbeat.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * The one place a top bar takes its colours from, so the main screen and the
 * component screen cannot disagree about the header.
 *
 * Colorful fills the header with the brand seed; every other scheme keeps a
 * surface-coloured bar. HullBeatTheme flips the status-bar glyphs to match,
 * which is why a screen must not pick its own header colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appTopBarColors(): TopAppBarColors {
    val colors = MaterialTheme.colorScheme
    return if (LocalScheme.current == Scheme.COLORFUL) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = colors.primary,
            // A bar that scrolls repaints itself in this colour once content
            // slides under it; left unset it is Material's stock lavender.
            scrolledContainerColor = colors.primary,
            titleContentColor = colors.onPrimary,
            navigationIconContentColor = colors.onPrimary,
            actionIconContentColor = colors.onPrimary,
        )
    } else {
        TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surface,
            scrolledContainerColor = colors.surface,
            titleContentColor = colors.onSurface,
            navigationIconContentColor = colors.onSurface,
            actionIconContentColor = colors.onSurfaceVariant,
        )
    }
}
