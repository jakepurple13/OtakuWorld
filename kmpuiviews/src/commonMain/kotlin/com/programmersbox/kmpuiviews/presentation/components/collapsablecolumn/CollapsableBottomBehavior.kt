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
class CollapsableBottomBehavior(
    val state: CollapsableState,
    internal val snapAnimationSpec: AnimationSpec<Float>?,
    internal val flingAnimationSpec: DecayAnimationSpec<Float>?,
    private val canScroll: () -> Boolean = { true },
) {
    /**
     * Pass this connection to [Modifier.nestedScroll] to response to nested scrolling events.
     */
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!canScroll()) return Offset.Zero
            state.drag(consumed.y)
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
 * Remembers a [CollapsableBottomBehavior] that can be used to control collapsing. You should use
 *
 * @param state the [CollapsableState]
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 * behavior.
 * @param snapAnimationSpec animates snapping to the collapsed or expanded state at the end of a
 * drag or nested scroll. Enabled by default, passing null will disable it and
 * leave your view in a partially collapsed state.
 * @param flingAnimationSpec animates flinging the view at the end of a drag or nested scroll.
 * Enabled by default, passing null will disable reacting to flings.
 */
@Composable
fun rememberCollapsableBottomBehavior(
    state: CollapsableState = rememberCollapsableState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): CollapsableBottomBehavior {
    return CollapsableBottomBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll,
    )
}

/**
 * A hooks up a [CollapsableBottomBehavior] so that dragging the view will expand and collapse it.
 *
 * @param behavior the collapsable behavior
 * @param enabled whether or not drag is enabled
 */
fun Modifier.draggable(behavior: CollapsableBottomBehavior, enabled: Boolean = true): Modifier =
    composed {
        this.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta -> behavior.state.drag(-delta) },
            enabled = enabled,
            onDragStopped = { velocity ->
                behavior.state.fling(
                    -velocity,
                    behavior.flingAnimationSpec,
                    behavior.snapAnimationSpec,
                )
            }
        )
    }