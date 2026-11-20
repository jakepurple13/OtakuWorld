package com.programmersbox.kmpuiviews.presentation.onboarding.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val ParticleCount = 90
private const val DurationNanos = 1_900_000_000L
private const val Gravity = 900f

private class Particle(
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val spin: Float,
)

/**
 * A one-shot burst of confetti from the upper centre of its area, replayed whenever [trigger] changes.
 * Draws nothing under reduced motion.
 */
@Composable
fun ConfettiBurst(
    trigger: Any,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val reduced = onboardingReducedMotion()
    var active by remember(trigger, reduced) { mutableStateOf(!reduced) }
    val elapsed = remember(trigger) { mutableLongStateOf(0L) }
    val particles = remember(trigger, colors) {
        val random = Random(trigger.hashCode())
        List(ParticleCount) {
            val angle = random.nextFloat() * PI.toFloat() * 0.9f + PI.toFloat() * 0.05f
            val speed = 350f + random.nextFloat() * 650f
            Particle(
                vx = cos(angle) * speed * (if (random.nextBoolean()) 1f else -1f),
                vy = -sin(angle) * speed * 0.8f,
                color = colors.getOrElse(it % colors.size.coerceAtLeast(1)) { Color.Magenta },
                size = 6f + random.nextFloat() * 8f,
                spin = (random.nextFloat() - 0.5f) * 720f,
            )
        }
    }

    LaunchedEffect(trigger, reduced) {
        if (reduced) return@LaunchedEffect
        active = true
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it } - start
            elapsed.longValue = now
            if (now >= DurationNanos) break
        }
        active = false
    }

    val count = if (active && !reduced) particles.size else 0
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { stateDescription = "particles=$count" },
    ) {
        if (count == 0) return@Canvas
        val t = elapsed.longValue / 1_000_000_000f
        val alpha = (1f - ((elapsed.longValue.toFloat() / DurationNanos - 0.6f) / 0.4f)).coerceIn(0f, 1f)
        val origin = Offset(this.size.width / 2f, this.size.height * 0.25f)
        particles.forEach { p ->
            val pos = Offset(origin.x + p.vx * t, origin.y + p.vy * t + 0.5f * Gravity * t * t)
            rotate(p.spin * t, pos) {
                drawRect(p.color.copy(alpha = alpha), topLeft = pos, size = Size(p.size, p.size * 0.6f))
            }
        }
    }
}
