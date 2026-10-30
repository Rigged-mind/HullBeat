package app.hullbeat.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

/**
 * Every scheme the owner can pick, for `@PreviewParameter`.
 *
 * SYSTEM is left out: it only resolves to LIGHT or DARK, which are here
 * already. Night and Sunlight are the reason this exists - they used to be
 * seen only after a build, an install and a theme switch on the emulator.
 */
class SchemePreviewProvider : PreviewParameterProvider<Scheme> {
    override val values = sequenceOf(
        Scheme.LIGHT, Scheme.DARK, Scheme.SUNLIGHT, Scheme.NIGHT, Scheme.COLORFUL,
    )
}

/**
 * A phone-width preview in Ukrainian, whose strings are the longest.
 * Pair it with `@PreviewParameter(SchemePreviewProvider::class) scheme: Scheme`
 * and wrap the content in [SchemePreview].
 */
@Preview(locale = "uk", widthDp = 360)
annotation class HullBeatPreview

/**
 * The app's own [HullBeatTheme] - same palette.json colours, same extended
 * status colours - on the scheme's background, so a preview cannot drift
 * from what the app draws. Previews are a quick look; the emulator shot is
 * still what counts as checked.
 */
@Composable
fun SchemePreview(scheme: Scheme, content: @Composable () -> Unit) {
    HullBeatTheme(scheme = scheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.padding(16.dp)) { content() }
        }
    }
}
