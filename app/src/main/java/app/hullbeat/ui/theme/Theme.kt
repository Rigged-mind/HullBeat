package app.hullbeat.ui.theme

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

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
enum class Scheme { SYSTEM, LIGHT, DARK, SUNLIGHT, NIGHT, COLORFUL }

val LocalScheme = androidx.compose.runtime.staticCompositionLocalOf { Scheme.SYSTEM }

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
        Scheme.COLORFUL -> ColorfulScheme
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
        Scheme.COLORFUL -> ColorfulExtended
        Scheme.DARK -> DarkExtended
        Scheme.SUNLIGHT -> SunlightExtended
        Scheme.NIGHT -> NightExtended
        Scheme.SYSTEM -> LightExtended
    }
    // enableEdgeToEdge picks the bar glyphs from the SYSTEM dark mode, not the
    // scheme: Colorful's brand header and an in-app Dark on a light phone
    // would both get dark glyphs on a dark bar. Light glyphs wherever the
    // top bar is dark; the bottom bar is a surface in every light scheme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.matchBarGlyphs(view, resolved)
        }
    }
    val appFling = rememberAppFlingBehavior()
    CompositionLocalProvider(
        LocalScheme provides resolved,
        LocalExtendedColors provides extended,
        LocalAppFlingBehavior provides appFling,
    ) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

/**
 * Call first inside every ModalBottomSheet's content.
 *
 * A sheet is its own dialog window, and Material 1.3 sets that window's bar
 * glyphs once, from the SYSTEM dark mode: under Colorful the status-bar
 * glyphs went dark over the dimmed brand header the moment a sheet opened.
 * This runs after Material's setup and puts back the scheme's rule.
 */
@Composable
fun SheetSystemBars() {
    val view = LocalView.current
    val scheme = LocalScheme.current
    SideEffect {
        var parent = view.parent
        while (parent != null && parent !is DialogWindowProvider) parent = parent.parent
        val window = (parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.matchBarGlyphs(view, scheme)
    }
}

/**
 * Dark glyphs only where the bar behind them is light: the top bar is a
 * surface in Light and Sunlight, the bottom bar in every light scheme.
 * Colorful's top bar is the brand seed, so its status bar gets light glyphs.
 */
private fun Window.matchBarGlyphs(view: View, scheme: Scheme) {
    val lightTopBar = scheme == Scheme.LIGHT || scheme == Scheme.SUNLIGHT
    val lightBottomBar = lightTopBar || scheme == Scheme.COLORFUL
    WindowCompat.getInsetsController(this, view).apply {
        isAppearanceLightStatusBars = lightTopBar
        isAppearanceLightNavigationBars = lightBottomBar
    }
}
