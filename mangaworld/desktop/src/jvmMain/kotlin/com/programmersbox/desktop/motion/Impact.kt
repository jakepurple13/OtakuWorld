package com.programmersbox.desktop.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.em
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.EaseOut
import com.programmersbox.kmpuiviews.motion.Key
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Ported from the `Impact` entry in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** One frame of Impact, in the mockup's units. */
internal class ImpactFrame(
    /** `.lines` scale about the host centre. */
    val linesScale: Float,
    /** `.lines` rotate, degrees clockwise. */
    val linesRotation: Float,
    /** `.sfx` opacity, 0..1. */
    val sfxAlpha: Float,
    /** `.sfx` scale about its anchor (50%, 44%). */
    val sfxScale: Float,
    /** `.sfx` rotate, degrees clockwise. */
    val sfxRotation: Float,
    /** `.fx` translateX shake, cqw. */
    val shakeX: Float,
    /** `.fx` translateY shake, cqw. */
    val shakeY: Float,
    /** `.fx` opacity (paper, lines, SFX and subtitle together), 0..1. */
    val fxAlpha: Float,
    /** `.sub` opacity, 0..1. */
    val subAlpha: Float,
    /** `.sub` translateY, cqw. */
    val subRise: Float,
    /** `.flash` opacity, 0..1. */
    val flashAlpha: Float,
    /** App scale about its centre. */
    val appScale: Float,
)

/** Speed lines rush in, ドン! slams down and shakes the frame, and a white flash cuts to the app. */
internal object Impact : DesktopMotion {
    override val key = "impact"
    override val title = "Impact"
    override val durationMillis = 2200

    /** `.sfx` slam: `cubic-bezier(.6,0,.9,.5)`. */
    private val Slam = CubicBezierEasing(.6f, 0f, .9f, .5f)

    fun frame(time: Int) = ImpactFrame(
        linesScale = track(time, Key(0, 1.6f, EaseOut), Key(500, 1f), Key(2200, 1.04f)),
        linesRotation = track(time, Key(0, -6f, EaseOut), Key(500, 0f), Key(2200, 2f)),
        sfxAlpha = track(time, Key(250, 0f, Slam), Key(480, 1f)),
        sfxScale = track(time, Key(250, 4f, Slam), Key(480, 1f), Key(2200, 1.06f)),
        sfxRotation = track(time, Key(250, -14f, Slam), Key(480, -6f)),
        shakeX = track(time, Key(480, 0f), Key(520, -1.4f), Key(560, 1.2f), Key(600, -.8f), Key(640, .5f), Key(690, 0f)),
        shakeY = track(time, Key(480, 0f), Key(520, .9f), Key(560, -1f), Key(600, .6f), Key(640, -.3f), Key(690, 0f)),
        fxAlpha = track(time, Key(1640, 1f), Key(1650, 0f)),
        subAlpha = track(time, Key(650, 0f, EaseOut), Key(950, 1f)),
        subRise = track(time, Key(650, 1.5f, EaseOut), Key(950, 0f)),
        flashAlpha = track(time, Key(1530, 0f), Key(1650, 1f, EaseOut), Key(2000, 0f)),
        appScale = track(time, Key(1650, 1.08f, EaseOut), Key(2200, 1f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = graphicsLayer {
        val scale = frame(time()).appScale
        scaleX = scale
        scaleY = scale
    }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val frame = frame(time())
                    alpha = frame.fxAlpha
                    translationX = frame.shakeX * size.cqw
                    translationY = frame.shakeY * size.cqw
                }
                .drawWithCache {
                    val u = size.cqw
                    val lines = SpeedLines(size)
                    val sfx = text.measure("ドン!", displayStyle(16 * u, Ink))
                    val sub = text.measure(
                        "MANGAWORLD",
                        TextStyle(color = Ink, fontSize = (3 * u).toSp(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = .5.em),
                    )
                    val sfxAnchor = Offset(size.width / 2, size.height * .44f)
                    val sfxTopLeft = sfxAnchor - Offset(sfx.size.width / 2f, sfx.size.height / 2f)
                    val sfxOutline = Stroke(width = .5f * u, join = StrokeJoin.Round)
                    val layerPaint = Paint()
                    onDrawBehind {
                        val frame = frame(time())
                        drawRect(Paper)
                        withTransform({
                            rotate(frame.linesRotation, center)
                            scale(frame.linesScale, frame.linesScale, center)
                        }) {
                            clipRect(lines.bounds.left, lines.bounds.top, lines.bounds.right, lines.bounds.bottom) {
                                drawPath(lines.wedges, lines.mask)
                            }
                        }
                        if (frame.sfxAlpha > 0f) {
                            // Shadow, white outline, then ink fill, faded as one group like the element's opacity.
                            drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), layerPaint.apply { alpha = frame.sfxAlpha }) }
                            withTransform({
                                rotate(frame.sfxRotation, sfxAnchor)
                                scale(frame.sfxScale, frame.sfxScale, sfxAnchor)
                            }) {
                                drawText(sfx, color = MangaAccent, topLeft = sfxTopLeft + Offset(u, u))
                                drawText(sfx, color = Color.White, topLeft = sfxTopLeft, drawStyle = sfxOutline)
                                drawText(sfx, topLeft = sfxTopLeft)
                            }
                            drawIntoCanvas { it.restore() }
                        }
                        if (frame.subAlpha > 0f) {
                            drawText(
                                sub,
                                topLeft = Offset((size.width - sub.size.width) / 2, size.height * .68f + frame.subRise * u),
                                alpha = frame.subAlpha,
                            )
                        }
                    }
                }
        )
        Canvas(Modifier.matchParentSize()) {
            val alpha = frame(time()).flashAlpha
            if (alpha > 0f) drawRect(Color.White, alpha = alpha)
        }
    }
}

/**
 * `.lines`: a box 30% bigger than the host on every side holding a conic gradient of 1.6° ink wedges every 7°
 * (clockwise from 12 o'clock), masked by `radial-gradient(circle, transparent 16%, #000 48%)` so the centre stays clear.
 * Built once per host size.
 */
private class SpeedLines(size: Size) {
    val bounds = Rect(-.3f * size.width, -.3f * size.height, 1.3f * size.width, 1.3f * size.height)

    /** The mask's circle reaches the box's farthest corner. */
    private val radius = hypot(bounds.width / 2, bounds.height / 2)

    val wedges = Path().apply {
        val centre = bounds.center
        // Reach past the corners so each wedge's straight outer edge still covers the box.
        val reach = radius * 1.01f
        fun point(degrees: Float): Offset {
            val radians = Math.toRadians(degrees.toDouble())
            return centre + Offset(sin(radians).toFloat(), -cos(radians).toFloat()) * reach
        }
        var start = 0f
        while (start < 360f) {
            moveTo(centre.x, centre.y)
            point(start).let { lineTo(it.x, it.y) }
            point(minOf(start + 1.6f, 360f)).let { lineTo(it.x, it.y) }
            close()
            start += 7f
        }
    }

    val mask = Brush.radialGradient(
        0f to Ink.copy(alpha = 0f),
        .16f to Ink.copy(alpha = 0f),
        .48f to Ink,
        1f to Ink,
        center = bounds.center,
        radius = radius,
    )
}
