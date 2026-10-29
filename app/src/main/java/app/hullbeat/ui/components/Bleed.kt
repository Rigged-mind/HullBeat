package app.hullbeat.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Widens a list item past the list's side padding by [edge] on each side.
 *
 * A screen header moved into a LazyColumn would otherwise sit 16 dp further
 * in than before, and its sideways-scrolling chip rows would stop short of
 * the screen edge instead of running under it.
 */
fun Modifier.bleedHorizontally(edge: Dp): Modifier = layout { measurable, constraints ->
    val extra = edge.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + 2 * extra,
            maxWidth = constraints.maxWidth + 2 * extra,
        ),
    )
    layout(placeable.width - 2 * extra, placeable.height) {
        placeable.place(-extra, 0)
    }
}
