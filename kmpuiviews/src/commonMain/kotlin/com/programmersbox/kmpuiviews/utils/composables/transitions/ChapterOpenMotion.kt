package com.programmersbox.kmpuiviews.utils.composables.transitions

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import org.koin.compose.koinInject
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay

/**
 * Motion for opening a chapter's reader: a circle that grows from where the chapter was tapped, or, when there is
 * no tap point, a "depth" move where Details sinks back and the reader rises.
 *
 * NavDisplay only keeps Details on screen; the reader animates itself from its entry's transition, which
 * NavDisplay seeks during predictive back, so the circle follows the finger.
 */
object ChapterOpenMotion {
    const val REVEAL_MS = 450
    const val DEPTH_MS = 380
    private const val DEPTH_OUT_MS = 110

    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    private val depthPush
        get() = EnterTransition.None togetherWith
                (scaleOut(tween(DEPTH_MS, easing = Emphasized), targetScale = 0.92f) + fadeOut(tween(DEPTH_OUT_MS)))

    // Holds the outgoing screen until the reader's own motion is done (ExitTransition.KeepUntilTransitionsFinished is internal)
    private val keepBehind get() = EnterTransition.None togetherWith fadeOut(tween(1, delayMillis = REVEAL_MS))

    /** Reader entries: reveal when Details recorded a tap point, depth otherwise. */
    fun readerMetadata(): Map<String, Any> =
        NavDisplay.transitionSpec { if (ChapterRevealOrigins.peek() != null) keepBehind else depthPush } +
                NavDisplay.popTransitionSpec { keepBehind } +
                NavDisplay.predictivePopTransitionSpec { keepBehind }

    /** Entries that never reveal (the video player). */
    fun depthMetadata(): Map<String, Any> =
        NavDisplay.transitionSpec { depthPush } +
                NavDisplay.popTransitionSpec { keepBehind } +
                NavDisplay.predictivePopTransitionSpec { keepBehind }
}

/** Takes the tap point recorded for [key] once, and keeps it across rotation and process death. */
@Composable
fun rememberChapterRevealOrigin(key: String?): Offset? {
    val saved = rememberSaveable { ChapterRevealOrigins.take(key)?.let { listOf(it.x, it.y) } ?: emptyList() }
    return remember(saved) { if (saved.size == 2) Offset(saved[0], saved[1]) else null }
}

/**
 * How open the reader is in each state of its entry's transition. It only opens when first added and only closes
 * when removed; while another screen covers it ([covered]) or is closed back to it ([hasEntered]) it stays open.
 */
internal fun chapterOpenTarget(state: EnterExitState, hasEntered: Boolean, covered: Boolean): Float = when (state) {
    EnterExitState.Visible -> 1f
    EnterExitState.PreEnter -> if (hasEntered) 1f else 0f
    EnterExitState.PostExit -> if (covered) 1f else 0f
}

/**
 * Animates the reader in and out with its navigation entry [key]. [origin] is a fraction of the window size:
 * non-null reveals a circle from it, null plays the depth move.
 */
@Composable
fun Modifier.chapterOpenTransition(origin: Offset?, key: NavKey): Modifier {
    val transition = LocalNavAnimatedContentScope.current.transition
    val backStack = koinInject<Navigation3Actions>().backStack
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(transition.currentState, transition.targetState) {
        if (transition.currentState == EnterExitState.Visible && transition.targetState == EnterExitState.Visible) hasEntered = true
    }
    val progress by transition.animateFloat(
        transitionSpec = {
            tween(if (origin != null) ChapterOpenMotion.REVEAL_MS else ChapterOpenMotion.DEPTH_MS, easing = ChapterOpenMotion.Emphasized)
        },
        label = "chapterOpen"
    ) { state ->
        // Covered: another screen was pushed above this entry, which stays in the back stack
        val index = backStack.indexOf(key)
        chapterOpenTarget(state, hasEntered, covered = index in 0 until backStack.lastIndex)
    }

    if (origin == null) {
        return graphicsLayer {
            val scale = lerp(1.08f, 1f, progress)
            scaleX = scale
            scaleY = scale
            alpha = ((progress - 0.24f) / 0.76f).coerceIn(0f, 1f)
        }
    }

    val window = LocalWindowInfo.current.containerSize
    var positionInWindow by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { positionInWindow = it.positionInWindow() }
        .graphicsLayer {
            if (progress >= 1f) return@graphicsLayer
            val center = Offset(origin.x * window.width, origin.y * window.height) - positionInWindow
            clip = true
            shape = CircleRevealShape(center, revealRadius(size, center) * progress)
        }
}

private class CircleRevealShape(private val center: Offset, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addOval(Rect(center, radius)) })
}

/** The last point a pointer went down on the element, as a fraction of the window size. */
class RevealTapState {
    var lastFraction: Offset? = null
    internal var coordinates: LayoutCoordinates? = null
}

@Composable
fun rememberRevealTapState(): RevealTapState = remember { RevealTapState() }

/** Records where a pointer goes down on this element into [state] without consuming the event. */
@Composable
fun Modifier.trackRevealTap(state: RevealTapState): Modifier {
    val window = LocalWindowInfo.current.containerSize
    return this
        .onGloballyPositioned { state.coordinates = it }
        .pointerInput(state, window) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val coords = state.coordinates
                if (coords != null && coords.isAttached && window.width > 0 && window.height > 0) {
                    val p = coords.localToWindow(down.position)
                    state.lastFraction = Offset(p.x / window.width, p.y / window.height)
                }
            }
        }
}
