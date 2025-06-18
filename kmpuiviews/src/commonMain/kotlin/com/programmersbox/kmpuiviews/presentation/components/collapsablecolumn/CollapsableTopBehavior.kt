package com.programmersbox.kmpuiviews.presentation.components.collapsablecolumn


import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

//Taken from https://github.com/evant/compose-collapsable/tree/main

@Deprecated(
    "renamed to CollapsableTopBehavior",
    replaceWith = ReplaceWith("CollapsableTopBehavior")
)
typealias CollapsableBehavior = CollapsableTopBehavior

/**
 * Drives the [CollapsableState] with dragging or nested scrolling.
 *
 * @param state the [CollapsableState]
 * @param snapAnimationSpec Animates snapping to the collapsed or expanded state at the end of a
 * drag or nested scroll. Enabled by default, passing null will disable it and
 * leave your view in a partially collapsed state.
 * @param flingAnimationSpec Animates flinging the view at the end of a drag or nested scroll.
 * Enabled by default, passing null will disable reacting to flings.
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 * behavior.
 */
@Stable
class CollapsableTopBehavior(
    val state: CollapsableState,
    internal val snapAnimationSpec: AnimationSpec<Float>?,
    internal val flingAnimationSpec: DecayAnimationSpec<Float>?,
    private val enterAlways: Boolean = false,
    private val canScroll: () -> Boolean = { true },
) {
    /**
     * Pass this connection to [Modifier.nestedScroll] to response to nested scrolling events.
     */
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!canScroll()) return Offset.Zero
            // Don't intercept if scrolling down.
            if (!enterAlways && available.y > 0f) return Offset.Zero

            val consumed = state.drag(available.y)
            return if (consumed != 0f) {
                // We're in the middle of top app bar collapse or expand.
                // Consume only the scroll on the Y axis.
                available.copy(x = 0f)
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!canScroll()) return Offset.Zero
            if (enterAlways) {
                state.heightOffset += consumed.y
            } else {
                if (available.y < 0f || consumed.y < 0f) {
                    // When scrolling up, just update the state's height offset.
                    val consumed = state.drag(consumed.y)
                    return Offset(0f, consumed)
                }

                if (available.y > 0f) {
                    // Adjust the height offset in case the consumed delta Y is less than what was
                    // recorded as available delta Y in the pre-scroll.
                    val consumed = state.drag(available.y)
                    return Offset(0f, consumed)
                }
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            return Velocity(
                x = 0f,
                y = state.fling(
                    velocity = available.y,
                    flingAnimationSpec = flingAnimationSpec,
                    snapAnimationSpec = snapAnimationSpec,
                )
            )
        }
    }
}

/**
 * Remembers a [CollapsableTopBehavior] that can be used to control collapsing. You should use
 *
 * @param state the [CollapsableState]
 * @param snapAnimationSpec animates snapping to the collapsed or expanded state at the end of a
 * drag or nested scroll. Enabled by default, passing null will disable it and
 * leave your view in a partially collapsed state.
 * @param flingAnimationSpec animates flinging the view at the end of a drag or nested scroll.
 * Enabled by default, passing null will disable reacting to flings.
 */
@Composable
@Deprecated(
    "renamed to rememberCollapsableTopBehavior",
    replaceWith = ReplaceWith("rememberCollapsableTopBehavior(state = state, snapAnimationSpec = snapAnimationSpec, flingAnimationSpec = flingAnimationSpec)")
)
fun rememberCollapsableBehavior(
    state: CollapsableState = rememberCollapsableState(),
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): CollapsableTopBehavior {
    return rememberCollapsableTopBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec
    )
}

/**
 * Remembers a [CollapsableTopBehavior] that can be used to control collapsing. You should use
 *
 * @param state the [CollapsableState]
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 * behavior.
 * @param snapAnimationSpec animates snapping to the collapsed or expanded state at the end of a
 * drag or nested scroll. Enabled by default, passing null will disable it and
 * leave your view in a partially collapsed state.
 * @param flingAnimationSpec animates flinging the view at the end of a drag or nested scroll.
 * Enabled by default, passing null will disable reacting to flings.
 * @param enterAlways If true the view will start to expand as soon as you start scrolling up,
 * otherwise it will only start expanding when you reach the top of the scrolling view.
 */
@Composable
fun rememberCollapsableTopBehavior(
    state: CollapsableState = rememberCollapsableState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    enterAlways: Boolean = false,
): CollapsableTopBehavior {
    return CollapsableTopBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        enterAlways = enterAlways,
        canScroll = canScroll,
    )
}

/**
 * A hooks up a [CollapsableTopBehavior] so that dragging the view will expand and collapse it.
 *
 * @param behavior the collapsable behavior
 * @param enabled whether or not drag is enabled
 */
fun Modifier.draggable(behavior: CollapsableTopBehavior, enabled: Boolean = true): Modifier =
    composed {
        this.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta -> behavior.state.drag(delta) },
            enabled = enabled,
            onDragStopped = { velocity ->
                behavior.state.fling(
                    velocity,
                    behavior.flingAnimationSpec,
                    behavior.snapAnimationSpec,
                )
            }
        )
    }