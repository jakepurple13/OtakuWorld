package com.programmersbox.desktop.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.em
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track
import com.programmersbox.kmpuiviews.motion.verticalCqw
import kotlin.math.hypot
import kotlin.math.sqrt

/*
 * Ported from the `Ink drop` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.cream`, `.ttl`, `.drop`, `.splash`, `.ink`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** `.drop`, `.splash` and `.ink` colour. */
internal val NovelInk = Color(0xFF1B2030)

/** The drop's fall: `cubic-bezier(.55,0,1,.45)`. */
private val NovelDropFall = CubicBezierEasing(.55f, 0f, 1f, .45f)

/** `.drop` translateY before it falls, in vertical cqw so tall windows still start it off-screen. */
private const val DROP_HEIGHT = -70f

/** One frame of Ink drop, in the mockup's units. */
internal class InkDropFrame(
    /** `.ttl` opacity, 0..1. */
    val titleAlpha: Float,
    /** `.ttl` translateY, cqw. */
    val titleDrop: Float,
    /** How far the drop has fallen: 0 at -70cqw above, 1 centred. */
    val fall: Float,
    /** `.drop` opacity, 0..1. */
    val dropAlpha: Float,
    /** `.splash` scale. */
    val splashScale: Float,
    /** `.splash` opacity, 0..1. */
    val splashAlpha: Float,
    /** `.ink` `clip-path: circle(r% at 50% 50%)` radius, CSS percent. */
    val inkPercent: Float,
    /** App `clip-path: circle(r% at 50% 50%)` radius, CSS percent. */
    val appPercent: Float,
)

/** The title appears on paper, a drop of ink falls and lands, and the spreading ink stain opens into the app. */
internal object InkDrop : DesktopMotion {
    override val key = "ink-drop"
    override val title = "Ink drop"
    override val durationMillis = 1900

    fun frame(time: Int) = InkDropFrame(
        titleAlpha = track(time, Keyframe(0, 0f, MockupEasing.Out), Keyframe(500, 1f)),
        titleDrop = track(time, Keyframe(0, 1f, MockupEasing.Out), Keyframe(500, 0f)),
        fall = track(time, Keyframe(350, 0f, NovelDropFall), Keyframe(800, 1f)),
        dropAlpha = track(time, Keyframe(800, 1f), Keyframe(820, 0f)),
        splashScale = track(time, Keyframe(790, 0f), Keyframe(800, .4f, MockupEasing.Out), Keyframe(1250, 6f)),
        splashAlpha = track(time, Keyframe(790, 0f), Keyframe(800, 1f, MockupEasing.Out), Keyframe(1250, 0f)),
        inkPercent = track(time, Keyframe(800, 0f, MockupEasing.Out), Keyframe(1500, 80f)),
        appPercent = track(time, Keyframe(1050, 0f, MockupEasing.InOut), Keyframe(1800, 80f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = drawWithCache {
        val circle = NovelInkCircle(size)
        onDrawWithContent { with(circle) { drawThrough(frame(time()).appPercent) } }
    }

    /** `.cream`, `.ttl`, `.drop`, `.splash` and `.ink`, back to front. */
    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val u = size.cqw
                    val paper = NovelPaper(size)
                    val circle = NovelInkCircle(size)
                    val centre = size.center
                    // `.ttl`: italic 600 8cqw/1 serif, centred, top 22%.
                    val title = text.measure(
                        "NovelWorld",
                        TextStyle(
                            color = NovelTypeInk,
                            fontSize = (8 * u).toSp(),
                            lineHeight = 1.em,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    val titleAt = Offset((size.width - title.size.width) / 2, size.height * .22f)
                    // `.drop`: 3cqw square, `border-radius: 0 50% 50% 50%`, drawn about its centre.
                    val drop = Path().apply {
                        val round = CornerRadius(1.5f * u)
                        addRoundRect(RoundRect(Rect(Offset.Zero, 1.5f * u), CornerRadius.Zero, round, round, round))
                    }
                    // The drop's centre starts 70 vertical cqw above `top: 50%` plus half its height, and lands centred.
                    val dropFrom = centre.y + 1.5f * u + DROP_HEIGHT * size.verticalCqw
                    // `.splash`: a 4cqw circle with a .4cqw border inside it, scaled about its centre.
                    val ring = Stroke(.4f * u)
                    val ringRadius = 1.8f * u
                    onDrawBehind {
                        val f = frame(time())
                        with(paper) { drawPaper() }
                        if (f.titleAlpha > 0f) drawText(title, topLeft = titleAt + Offset(0f, f.titleDrop * u), alpha = f.titleAlpha)
                        if (f.dropAlpha > 0f) {
                            translate(centre.x, dropFrom + (centre.y - dropFrom) * f.fall) {
                                rotate(45f, Offset.Zero) { drawPath(drop, NovelInk, f.dropAlpha) }
                            }
                        }
                        if (f.splashAlpha > 0f && f.splashScale > 0f) {
                            scale(f.splashScale, centre) { drawCircle(NovelInk, ringRadius, centre, f.splashAlpha, ring) }
                        }
                        with(circle) { drawInk(f.inkPercent) }
                    }
                }
        )
    }
}

/**
 * CSS `clip-path: circle(r% at 50% 50%)` for a host of [size]; built in `drawWithCache`.
 * One circle path is built per size and scaled per frame, so drawing allocates nothing.
 */
private class NovelInkCircle(size: Size) {
    private val centre = size.center

    /** The radius that reaches the host corners: a larger circle clips nothing. */
    private val coverRadius = hypot(size.width, size.height) / 2

    /** 1% of a CSS `circle()` radius: percentages resolve against sqrt(w² + h²) / sqrt(2). */
    private val percent = hypot(size.width, size.height) / sqrt(2f) / 100

    private val cover = Path().apply { addOval(Rect(centre, coverRadius)) }

    /** `.ink`: a solid ink disc of [radiusPercent]% (CSS units). */
    fun DrawScope.drawInk(radiusPercent: Float) {
        if (radiusPercent > 0f) drawCircle(NovelInk, minOf(radiusPercent * percent, coverRadius), centre)
    }

    /** Draws the content through a centred circle of [radiusPercent]% (CSS units); nothing at 0. */
    fun ContentDrawScope.drawThrough(radiusPercent: Float) {
        val radius = radiusPercent * percent
        when {
            radius >= coverRadius -> drawContent()
            radius <= 0f -> Unit
            else -> {
                val k = radius / coverRadius
                scale(k, centre) {
                    clipPath(cover) {
                        scale(1 / k, centre) { this@drawThrough.drawContent() }
                    }
                }
            }
        }
    }
}
