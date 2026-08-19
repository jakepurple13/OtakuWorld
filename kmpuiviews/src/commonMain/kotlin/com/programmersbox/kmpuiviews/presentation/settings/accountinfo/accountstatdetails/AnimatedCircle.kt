package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstatdetails

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val DividerLengthInDegrees = 1.8f

data class CircleInfo(
    val color: Color,
    val value: Float,
    val key: Any? = null,
    val label: String? = null,
)

/**
 * A donut chart that animates when loaded.
 */
@Composable
fun AnimatedCircle(
    data: List<CircleInfo>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    angleAnimationSpec: @Composable Transition.Segment<AnimatedCircleProgress>.() -> FiniteAnimationSpec<Float> = {
        tween(
            delayMillis = 200,
            durationMillis = 900,
            easing = LinearOutSlowInEasing
        )
    },
    shiftAnimationSpec: @Composable Transition.Segment<AnimatedCircleProgress>.() -> FiniteAnimationSpec<Float> = {
        tween(
            delayMillis = 200,
            durationMillis = 900,
            easing = CubicBezierEasing(0f, 0.75f, 0.35f, 0.85f)
        )
    },
) {
    val currentState = remember {
        MutableTransitionState(AnimatedCircleProgress.START)
            .apply { targetState = AnimatedCircleProgress.END }
    }
    val stroke = with(LocalDensity.current) { Stroke(strokeWidth.toPx()) }
    val transition = rememberTransition(currentState)
    val angleOffset by transition.animateFloat(
        transitionSpec = angleAnimationSpec,
        label = ""
    ) { progress ->
        if (progress == AnimatedCircleProgress.START) {
            0f
        } else {
            360f
        }
    }
    val shift by transition.animateFloat(
        transitionSpec = shiftAnimationSpec,
        label = ""
    ) { progress ->
        if (progress == AnimatedCircleProgress.START) {
            0f
        } else {
            30f
        }
    }

    Canvas(modifier) {
        val innerRadius = (size.minDimension - stroke.width) / 2
        val halfSize = size / 2.0f
        val topLeft = Offset(
            halfSize.width - innerRadius,
            halfSize.height - innerRadius
        )
        val size = Size(innerRadius * 2, innerRadius * 2)
        var startAngle = shift - 90f
        data.forEach { proportion ->
            val sweep = proportion.value * angleOffset
            drawArc(
                color = proportion.color,
                startAngle = startAngle + DividerLengthInDegrees / 2,
                sweepAngle = sweep - DividerLengthInDegrees,
                topLeft = topLeft,
                size = size,
                useCenter = false,
                style = stroke
            )
            startAngle += sweep
        }
    }
}

enum class AnimatedCircleProgress { START, END }
