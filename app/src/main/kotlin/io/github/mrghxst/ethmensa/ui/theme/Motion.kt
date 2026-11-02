// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.theme

import android.provider.Settings
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import kotlin.math.roundToInt

/** UI spring from docs/port-spec.md §9.5. */
fun <T> uiSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

/** True when the user turned on "Remove animations" (animator duration scale 0). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Shrinks a tappable surface slightly while it's pressed and springs it back on release.
 * Scrolling doesn't trigger it, because clickables in a scrolling container delay the press.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier {
    if (rememberReducedMotion()) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) spring(stiffness = 1500f) else spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "press-scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Screen transitions. Push and pop are Material's shared X axis: the screens slide a tenth of the
 * width and fade through each other. The back gesture shrinks the top screen into a rounded card
 * over the screen below it, which follows the finger, and fades it out once the gesture commits.
 */
object ScreenMotion {
    private const val SlideFraction = 0.1f

    /** Length of the back-gesture timeline: the system's progress 0…1 seeks through it. */
    const val PredictiveMillis = 320

    /** Corner radius of the shrinking card during the back gesture. */
    val PredictiveCorner: Dp = 28.dp

    private val slide = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 700f,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
    private val fadeInThrough = fadeIn(tween(210, delayMillis = 60, easing = LinearOutSlowInEasing))
    private val fadeOutThrough = fadeOut(tween(90, easing = FastOutLinearInEasing))

    // Keeps the screen below visible while an overlay opens over it.
    private val holdUnder = fadeOut(tween(300), targetAlpha = 1f)

    private fun offset(width: Int) = (width * SlideFraction).roundToInt()

    fun push(): ContentTransform =
        (slideInHorizontally(slide) { offset(it) } + fadeInThrough) togetherWith
            (slideOutHorizontally(slide) { -offset(it) } + fadeOutThrough)

    fun pop(): ContentTransform =
        (slideInHorizontally(slide) { -offset(it) } + fadeInThrough) togetherWith
            (slideOutHorizontally(slide) { offset(it) } + fadeOutThrough)

    /** [swipeEdge] is [NavigationEvent.EDGE_LEFT] or [NavigationEvent.EDGE_RIGHT]. */
    fun predictivePop(swipeEdge: Int): ContentTransform {
        // The card drifts away from the edge the finger started at.
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        val linear = tween<Float>(PredictiveMillis, easing = LinearEasing)
        val linearOffset = tween<IntOffset>(PredictiveMillis, easing = LinearEasing)
        val exit = scaleOut(linear, targetScale = 0.9f) +
            slideOutHorizontally(linearOffset) { direction * it / 20 } +
            // Opaque while the finger is down, then a quick fade as the commit finishes.
            fadeOut(
                keyframes {
                    durationMillis = PredictiveMillis
                    1f at PredictiveMillis / 2 using FastOutLinearInEasing
                    0f at PredictiveMillis * 3 / 4
                },
            )
        val enter = slideInHorizontally(linearOffset) { -direction * offset(it) }
        return ContentTransform(enter, exit, targetContentZIndex = -1f)
    }

    /** Full-screen overlays (photo viewer, Legi card): zoom in over the screen below. */
    fun overlayIn(): ContentTransform = ContentTransform(
        fadeIn(tween(160)) + scaleIn(spring(Spring.DampingRatioNoBouncy, 900f), initialScale = 0.94f),
        holdUnder,
    )

    fun overlayOut(): ContentTransform = ContentTransform(
        EnterTransition.None,
        fadeOut(tween(150)) + scaleOut(tween(200, easing = FastOutLinearInEasing), targetScale = 0.94f),
        targetContentZIndex = -1f,
    )

    fun overlayPredictiveOut(): ContentTransform = ContentTransform(
        EnterTransition.None,
        scaleOut(tween(PredictiveMillis, easing = LinearEasing), targetScale = 0.9f) +
            fadeOut(
                keyframes {
                    durationMillis = PredictiveMillis
                    1f at PredictiveMillis / 2 using FastOutLinearInEasing
                    0f at PredictiveMillis * 3 / 4
                },
            ),
        targetContentZIndex = -1f,
    )

    /** Legi card: rises from the bottom like a sheet. */
    fun sheetIn(): ContentTransform = ContentTransform(
        slideInVertically(slide) { it / 8 } + fadeIn(tween(180, easing = LinearOutSlowInEasing)),
        holdUnder,
    )

    fun sheetOut(): ContentTransform = ContentTransform(
        EnterTransition.None,
        slideOutVertically(slide) { it / 8 } + fadeOut(tween(150, easing = FastOutLinearInEasing)),
        targetContentZIndex = -1f,
    )

    /** Shared photo between the list and the detail: settles with the screen, no overshoot. */
    val sharedBounds = BoundsTransform { _, _ ->
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f, visibilityThreshold = Rect.VisibilityThreshold)
    }
}
