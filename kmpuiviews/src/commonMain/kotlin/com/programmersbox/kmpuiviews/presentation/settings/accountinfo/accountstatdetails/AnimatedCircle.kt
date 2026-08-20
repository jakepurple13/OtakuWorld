package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstatdetails

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val DividerLengthInDegrees = 1.8f

data class CircleInfo(
    val color: Color,
    val value: Float,
    val key: Any? = null,
    val label: String? = null,
    val extraInfo: Map<String, Any?> = emptyMap(),
)

// A wrapper to track the state of each slice during enter/update/exit animations
private class SliceNode(
    val key: Any,
    var info: CircleInfo,
    val animatable: Animatable<Float, AnimationVector1D>,
) {
    var isExiting: Boolean = false
}

@Composable
fun AnimatedCircle(
    data: List<CircleInfo>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    angleAnimationSpec: @Composable Transition.Segment<AnimatedCircleProgress>.() -> FiniteAnimationSpec<Float> = {
        tween(delayMillis = 200, durationMillis = 900, easing = LinearOutSlowInEasing)
    },
    shiftAnimationSpec: @Composable Transition.Segment<AnimatedCircleProgress>.() -> FiniteAnimationSpec<Float> = {
        tween(
            delayMillis = 200,
            durationMillis = 900,
            easing = CubicBezierEasing(0f, 0.75f, 0.35f, 0.85f)
        )
    },
    shiftAngle: Float = 30f,
) {
    // 1. Initial Load Animation
    val currentState = remember {
        MutableTransitionState(AnimatedCircleProgress.START).apply {
            targetState = AnimatedCircleProgress.END
        }
    }
    val stroke = with(LocalDensity.current) { Stroke(strokeWidth.toPx()) }
    val transition = rememberTransition(currentState, label = "circle_transition")

    val angleOffset by transition.animateFloat(
        transitionSpec = angleAnimationSpec,
        label = "angle"
    ) {
        if (it == AnimatedCircleProgress.START) 0f else 360f
    }
    val shift by transition.animateFloat(transitionSpec = shiftAnimationSpec, label = "shift") {
        if (it == AnimatedCircleProgress.START) 0f else shiftAngle
    }

    // 2. Dynamic Data State Management
    val scope = rememberCoroutineScope()
    val activeNodes = remember { mutableStateListOf<SliceNode>() }

    LaunchedEffect(data) {
        val incomingKeys = data.map { it.key ?: it.hashCode() }.toSet()

        // Handle Exits: Mark removed nodes as exiting and animate them to 0
        activeNodes.forEach { node ->
            if (node.key !in incomingKeys && !node.isExiting) {
                node.isExiting = true
                scope.launch {
                    // Animating to 0. If this gets cancelled (e.g., item comes back), it throws a
                    // CancellationException safely, and the remove() line won't execute.
                    node.animatable.animateTo(
                        0f,
                        animationSpec = tween(500, easing = LinearOutSlowInEasing)
                    )
                    activeNodes.remove(node)
                }
            }
        }

        // Handle Enters & Updates: Build up the new node sequence
        val newNodesList = mutableListOf<SliceNode>()
        data.forEach { item ->
            val key = item.key ?: item.hashCode()
            var node = activeNodes.find { it.key == key }

            if (node == null) {
                // New Slice: Starts at 0, animates to target
                node = SliceNode(key, item, Animatable(0f))
                scope.launch {
                    node.animatable.animateTo(
                        item.value,
                        animationSpec = tween(500, easing = LinearOutSlowInEasing)
                    )
                }
            } else {
                // Existing Slice: Update color/target
                node.info = item
                node.isExiting = false
                scope.launch {
                    node.animatable.animateTo(
                        item.value,
                        animationSpec = tween(500, easing = LinearOutSlowInEasing)
                    )
                }
            }
            newNodesList.add(node)
        }

        // Append currently exiting slices to the end so they continue drawing until they hit 0
        newNodesList.addAll(activeNodes.filter { it.isExiting })

        // Update the observable state list
        activeNodes.apply {
            clear()
            addAll(newNodesList)
        }
    }

    // 3. Draw
    Canvas(modifier) {
        val innerRadius = (size.minDimension - stroke.width) / 2
        val halfSize = size / 2.0f
        val topLeft = Offset(halfSize.width - innerRadius, halfSize.height - innerRadius)
        val arcSize = Size(innerRadius * 2, innerRadius * 2)
        var startAngle = shift - 90f

        // Draw directly from our state list
        activeNodes.forEach { node ->
            val sweep = node.animatable.value * angleOffset

            if (sweep > DividerLengthInDegrees) {
                drawArc(
                    color = node.info.color,
                    startAngle = startAngle + DividerLengthInDegrees / 2,
                    sweepAngle = sweep - DividerLengthInDegrees,
                    topLeft = topLeft,
                    size = arcSize,
                    useCenter = false,
                    style = stroke
                )
            }
            startAngle += sweep
        }
    }
}

enum class AnimatedCircleProgress { START, END }