package com.programmersbox.kmpuiviews.notifier

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private enum class SwipeValue { Start, Settled, End }

private const val DismissThreshold = 0.4f
private const val UpFlingVelocity = 800f

/** Swipe sideways (past 40% of the width) or swipe up (past 40dp, or flung faster than 800px/s) to dismiss. */
@Composable
internal fun SwipeToDismissBanner(
    enabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val state = remember { AnchoredDraggableState(SwipeValue.Settled) }
    var width by remember { mutableFloatStateOf(0f) }
    val upThresholdPx = with(density) { 40.dp.toPx() }

    LaunchedEffect(width) {
        if (width > 0f) {
            state.updateAnchors(
                DraggableAnchors {
                    SwipeValue.Start at -width
                    SwipeValue.Settled at 0f
                    SwipeValue.End at width
                },
            )
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect {
            if (it != SwipeValue.Settled) currentOnDismiss()
        }
    }

    var upOffset by remember { mutableFloatStateOf(0f) }
    val verticalState = rememberDraggableState { delta ->
        // Upward only: the card follows the finger up and never past its resting place downward.
        upOffset = (upOffset + delta).coerceAtMost(0f)
    }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width.toFloat() }
            .graphicsLayer {
                translationX = if (width > 0f) state.offset.takeUnless { it.isNaN() } ?: 0f else 0f
                translationY = upOffset
            }
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                flingBehavior = androidx.compose.foundation.gestures.AnchoredDraggableDefaults.flingBehavior(
                    state = state,
                    positionalThreshold = { distance -> distance * DismissThreshold },
                ),
            )
            .draggable(
                state = verticalState,
                orientation = Orientation.Vertical,
                enabled = enabled,
                onDragStopped = { velocity ->
                    if (upOffset <= -upThresholdPx || velocity < -UpFlingVelocity) {
                        currentOnDismiss()
                    } else {
                        animate(upOffset, 0f, animationSpec = spring()) { value, _ -> upOffset = value }
                    }
                },
            ),
    ) {
        content()
    }
}
