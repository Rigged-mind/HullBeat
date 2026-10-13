package app.hullbeat.ui.theme

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.CancellationException
import kotlin.math.abs

/**
 * CompositionLocal providing the application-wide [FlingBehavior].
 */
val LocalAppFlingBehavior = staticCompositionLocalOf<FlingBehavior?> { null }

/**
 * Returns the application-wide [FlingBehavior] from [LocalAppFlingBehavior] if provided,
 * or creates an instance of [rememberAppFlingBehavior] with slightly increased friction.
 */
@Composable
fun appFlingBehavior(frictionFactor: Float = 1.35f): FlingBehavior {
    return LocalAppFlingBehavior.current ?: rememberAppFlingBehavior(frictionFactor)
}

/**
 * A custom [FlingBehavior] that provides slightly increased friction and tighter
 * deceleration control compared to stock Android Compose fling.
 *
 * This eliminates the floaty "sliding on a wet surface / ice rink" feeling by
 * damping the fling velocity and smoothly bringing the list to a rest.
 */
class AppFlingBehavior(
    private val decaySpec: DecayAnimationSpec<Float>,
    private val frictionFactor: Float = 1.35f,
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // Damped velocity: scale down the initial launch velocity by frictionFactor
        val effectiveVelocity = initialVelocity / frictionFactor

        if (abs(effectiveVelocity) <= 1f) {
            return initialVelocity
        }

        var velocityLeft = effectiveVelocity
        var lastValue = 0f
        val animationState = AnimationState(
            initialValue = 0f,
            initialVelocity = effectiveVelocity,
        )

        try {
            animationState.animateDecay(decaySpec) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = this.velocity

                // If the delta was not consumed (e.g. hit the top/bottom edge), abort
                if (abs(delta - consumed) > 0.5f) {
                    cancelAnimation()
                }
            }
        } catch (_: CancellationException) {
            velocityLeft = animationState.velocity
        }

        return velocityLeft
    }
}

/**
 * Creates and remembers an [AppFlingBehavior] with a given [frictionFactor].
 *
 * @param frictionFactor Values > 1.0f increase friction (e.g. 1.35f = ~35% more resistance).
 */
@Composable
fun rememberAppFlingBehavior(frictionFactor: Float = 1.35f): FlingBehavior {
    val splineDecay = rememberSplineBasedDecay<Float>()
    return remember(splineDecay, frictionFactor) {
        AppFlingBehavior(
            decaySpec = splineDecay,
            frictionFactor = frictionFactor,
        )
    }
}
