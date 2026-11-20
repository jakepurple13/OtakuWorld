package com.programmersbox.kmpuiviews.presentation.onboarding.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlin.math.hypot
import kotlin.math.max

private const val SweepMillis = 700

/**
 * Hosts [content] and lets it start a theme sweep: call `sweepFrom(offset)` right before changing the
 * theme. The previous frame stays on top, with a circular hole growing from the offset that reveals
 * the new theme underneath over 700 ms. No-op under reduced motion.
 */
@Composable
fun ColorSweepHost(content: @Composable (sweepFrom: (Offset) -> Unit) -> Unit) {
    val reduced = onboardingReducedMotion()
    val layer = rememberGraphicsLayer()
    var sweepOrigin by remember { mutableStateOf<Offset?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(sweepOrigin) {
        if (sweepOrigin == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SweepMillis, easing = LinearEasing))
        sweepOrigin = null
    }

    Box(
        Modifier.drawWithContent {
            val origin = sweepOrigin
            if (origin == null) {
                // Keep the latest frame so it can be frozen the moment a sweep starts.
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            } else {
                drawContent()
                val farthest = max(
                    max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
                    max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
                )
                val hole = Path().apply { addOval(Rect(origin, farthest * progress.value)) }
                clipPath(hole, ClipOp.Difference) { drawLayer(layer) }
            }
        },
    ) {
        content { offset -> if (!reduced) sweepOrigin = offset }
    }
}
