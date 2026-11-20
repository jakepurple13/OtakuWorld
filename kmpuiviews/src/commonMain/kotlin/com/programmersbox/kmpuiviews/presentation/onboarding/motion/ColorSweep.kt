package com.programmersbox.kmpuiviews.presentation.onboarding.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
 * Sweep state. [start] resets [progress] to 0 in the same snapshot that sets [origin], so the first
 * frame of every sweep draws the old frame in full, and bumps [id] so a repeat sweep from an equal
 * offset restarts the animation.
 */
internal class ColorSweepState {
    var origin by mutableStateOf<Offset?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
    var id by mutableIntStateOf(0)
        private set

    fun start(offset: Offset) {
        progress = 0f
        origin = offset
        id++
    }

    fun finish() {
        origin = null
    }
}

/**
 * Hosts [content] and lets it start a theme sweep: call `sweepFrom(offset)` right before changing the
 * theme. The previous frame stays on top, with a circular hole growing from the offset that reveals
 * the new theme underneath over 700 ms. No-op under reduced motion.
 */
@Composable
fun ColorSweepHost(content: @Composable (sweepFrom: (Offset) -> Unit) -> Unit) {
    ColorSweepHost(remember { ColorSweepState() }, content)
}

@Composable
internal fun ColorSweepHost(
    sweep: ColorSweepState,
    content: @Composable (sweepFrom: (Offset) -> Unit) -> Unit,
) {
    val reduced = onboardingReducedMotion()
    val layer = rememberGraphicsLayer()

    LaunchedEffect(sweep.id) {
        if (sweep.origin == null) return@LaunchedEffect
        animate(0f, 1f, animationSpec = tween(SweepMillis, easing = LinearEasing)) { value, _ ->
            sweep.progress = value
        }
        sweep.finish()
    }

    Box(
        Modifier.drawWithContent {
            val origin = sweep.origin
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
                val hole = Path().apply { addOval(Rect(origin, farthest * sweep.progress)) }
                clipPath(hole, ClipOp.Difference) { drawLayer(layer) }
            }
        },
    ) {
        content { offset -> if (!reduced) sweep.start(offset) }
    }
}
