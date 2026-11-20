package com.programmersbox.desktop.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track

/*
 * Ported from the `Play button` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.room`, `.bw`, `.pbtn`, `.pbtn::after`, `.ring`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** One frame of Play button, in the mockup's units. */
internal class PlayButtonFrame(
    /** `.bw` opacity; overshoots past 1 on the `E.back` pop, so clamp before use. */
    val buttonAlpha: Float,
    /** `.bw` scale about the host centre. */
    val buttonScale: Float,
    /** `.pbtn` scale about the host centre: the press. */
    val pressScale: Float,
    /** `.ring circle` drawn fraction, 0..1 (1 − `stroke-dashoffset`). */
    val ringSweep: Float,
    /** `.ring` opacity, 0..1. */
    val ringAlpha: Float,
    /** `.ring` scale about the host centre. */
    val ringScale: Float,
    /** App `clip-path: circle(r% at 50% 50%)` radius, CSS percent. */
    val irisPercent: Float,
)

/** A play button pops in while a ring loads around it, gets pressed, and the app opens outward from it in a circle. */
internal object PlayButton : DesktopMotion {
    override val key = "play-button"
    override val title = "Play button"
    override val durationMillis = 1900

    fun frame(time: Int) = PlayButtonFrame(
        buttonAlpha = track(
            time,
            Keyframe(0, 0f, MockupEasing.Back), Keyframe(350, 1f), Keyframe(1150, 1f, MockupEasing.In), Keyframe(1500, 0f),
        ),
        buttonScale = track(
            time,
            Keyframe(0, .6f, MockupEasing.Back), Keyframe(350, 1f), Keyframe(1150, 1f, MockupEasing.In), Keyframe(1500, 1.5f),
        ),
        pressScale = track(time, Keyframe(950, 1f), Keyframe(1050, .86f, MockupEasing.Back), Keyframe(1250, 1f)),
        ringSweep = track(time, Keyframe(200, 0f, MockupEasing.InOut), Keyframe(950, 1f)),
        ringAlpha = track(time, Keyframe(1150, 1f, MockupEasing.Out), Keyframe(1600, 0f)),
        ringScale = track(time, Keyframe(1150, 1f, MockupEasing.Out), Keyframe(1600, 2f)),
        irisPercent = track(time, Keyframe(1150, 0f, MockupEasing.InOut), Keyframe(1800, 75f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = drawWithCache {
        val iris = AnimeIris(size)
        onDrawWithContent { with(iris) { drawThrough(frame(time()).irisPercent) } }
    }

    /** Room, button and ring all sit under the app, which opens over them. */
    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        Spacer(Modifier.matchParentSize().background(AnimeRoom))
        // `.bw`: glow and button fade and scale as one group.
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val frame = frame(time())
                    alpha = frame.buttonAlpha.coerceIn(0f, 1f)
                    scaleX = frame.buttonScale
                    scaleY = frame.buttonScale
                }
                .drawWithCache {
                    val button = AnimePlayButton(size)
                    onDrawBehind {
                        val press = frame(time()).pressScale
                        scale(press, center) { with(button) { draw() } }
                    }
                }
        )
        // `.ring`: 26cqw box, viewBox 40, r 18, stroke 1.2, round caps, starting at 12 o'clock.
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val frame = frame(time())
                    alpha = frame.ringAlpha
                    scaleX = frame.ringScale
                    scaleY = frame.ringScale
                }
                .drawWithCache {
                    val u = size.cqw
                    val radius = 26 * u * 18 / 40
                    val stroke = Stroke(width = 26 * u * 1.2f / 40, cap = StrokeCap.Round)
                    val topLeft = size.center - Offset(radius, radius)
                    val ringSize = Size(2 * radius, 2 * radius)
                    onDrawBehind {
                        val sweep = frame(time()).ringSweep
                        if (sweep > 0f) drawArc(AnimeAccent, -90f, 360 * sweep, false, topLeft, ringSize, style = stroke)
                    }
                }
        )
    }
}

/** `.pbtn` (16cqw accent disc with a `0 0 6cqw` accent glow) and its `::after` white triangle, for one host size. */
private class AnimePlayButton(size: Size) {
    private val u = size.cqw
    private val centre = size.center
    private val radius = 8 * u

    /** box-shadow blur 6cqw: a Gaussian with σ = 3cqw, about half strength at the disc edge and gone 6cqw out. */
    private val glowRadius = radius + 6 * u
    private val glow = Brush.radialGradient(
        0f to AnimeAccent.copy(alpha = .5f),
        radius / glowRadius to AnimeAccent.copy(alpha = .5f),
        (radius + 3 * u) / glowRadius to AnimeAccent.copy(alpha = .16f),
        1f to AnimeAccent.copy(alpha = 0f),
        center = centre,
        radius = glowRadius,
    )

    /**
     * `::after`: borders 5cqw wide by 6cqw tall with `margin-left: 1.4cqw`, grid-centred as a 6.4cqw margin box,
     * so the left edge sits 1.8cqw left of centre and the tip 3.2cqw right of it.
     */
    private val triangle = Path().apply {
        moveTo(centre.x - 1.8f * u, centre.y - 3 * u)
        lineTo(centre.x + 3.2f * u, centre.y)
        lineTo(centre.x - 1.8f * u, centre.y + 3 * u)
        close()
    }

    fun DrawScope.draw() {
        drawCircle(glow, glowRadius, centre)
        drawCircle(AnimeAccent, radius, centre)
        drawPath(triangle, Color.White)
    }
}
