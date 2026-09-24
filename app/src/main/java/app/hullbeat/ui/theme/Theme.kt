package app.hullbeat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The four schemes the owner can choose between.
 *
 * `Color.kt` defined the palettes and nothing consumed them, so an Activity
 * calling `MaterialTheme` would have rendered in stock Material purple with
 * the whole brand ramp sitting unused beside it.
 *
 * NIGHT is red-on-black and not cosmetic: it exists to keep the helm's dark
 * adaptation (ui-spec.md §8.2). It was designed as CSS in the prototype and
 * had no Kotlin ColorScheme for weeks - Compose had three schemes where the
 * design has four - until design/palette.json became the single source and
 * tools/make_palette.py generated it. It now clears the same WCAG AA floors
 * as every other scheme, which is checked rather than asserted.
 */
enum class Scheme { SYSTEM, LIGHT, DARK, SUNLIGHT, NIGHT }

@Composable
fun HullBeatTheme(
    scheme: Scheme = Scheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val resolved = if (scheme == Scheme.SYSTEM) {
        if (systemDark) Scheme.DARK else Scheme.LIGHT
    } else {
        scheme
    }
    val colors: ColorScheme = when (resolved) {
        Scheme.LIGHT -> LightScheme
        Scheme.DARK -> DarkScheme
        Scheme.SUNLIGHT -> SunlightScheme
        Scheme.NIGHT -> NightScheme
        Scheme.SYSTEM -> LightScheme      // unreachable: resolved above
    }
    // The extended colours are NOT part of ColorScheme: overdue, due soon,
    // deferred, their tint plates and the border width are semantic states,
    // not Material roles, and they must travel with whichever scheme is active
    // or a card renders its urgency in the wrong palette.
    val extended = when (resolved) {
        Scheme.LIGHT -> LightExtended
        Scheme.DARK -> DarkExtended
        Scheme.SUNLIGHT -> SunlightExtended
        Scheme.NIGHT -> NightExtended
        Scheme.SYSTEM -> LightExtended
    }
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
