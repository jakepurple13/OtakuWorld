package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.roundToInt

/*
 * Ported from the `Next episode` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.banner`, `.banner b`, `.banner small`, `.black`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** `.banner small` colour (white at `opacity: .85`). */
private val AnimeBannerSubtitle = Color.White.copy(alpha = .85f)

/** `translateX(-110%)`: the banner fully off-screen to the left. */
private const val BANNER_AWAY = -1.1f

/** Still filter steps between none and the full `grayscale(1) sepia(.65) contrast(1.1)`. */
private const val STILL_STEPS = 30

/** One frame of Next episode, in the mockup's units. */
internal class NextEpisodeFrame(
    /** How far the app's filter is from none (0) to the full sepia still (1). */
    val still: Float,
    /** App scale about its centre. */
    val appScale: Float,
    /** App and banner opacity (they cut out together), 0..1. */
    val stillAlpha: Float,
    /** `.banner` translateX, fraction of the banner's own width. */
    val bannerShift: Float,
    /** `.black` opacity, 0..1. */
    val blackAlpha: Float,
)

/** The frame freezes into a sepia still, a NEXT EPISODE arrow slides in from the left, then the screen cuts to black. */
internal object NextEpisode : DesktopMotion {
    override val key = "next-episode"
    override val title = "Next episode"
    override val durationMillis = 2300

    fun frame(time: Int) = NextEpisodeFrame(
        still = track(time, Keyframe(0, 0f, MockupEasing.Out), Keyframe(300, 1f)),
        appScale = track(time, Keyframe(0, 1f, MockupEasing.Out), Keyframe(300, 1.03f)),
        stillAlpha = track(time, Keyframe(1900, 1f), Keyframe(1910, 0f)),
        bannerShift = track(time, Keyframe(400, BANNER_AWAY, MockupEasing.Out), Keyframe(800, 0f)),
        blackAlpha = track(time, Keyframe(1500, 0f, MockupEasing.In), Keyframe(1900, 1f), Keyframe(2300, 0f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = this
        .graphicsLayer {
            val frame = frame(time())
            scaleX = frame.appScale
            scaleY = frame.appScale
            alpha = frame.stillAlpha
        }
        .drawWithCache {
            val bounds = Rect(Offset.Zero, size)
            val paint = Paint()
            onDrawWithContent {
                val step = (frame(time()).still * STILL_STEPS).roundToInt().coerceIn(0, STILL_STEPS)
                if (step == 0) {
                    drawContent()
                } else {
                    paint.colorFilter = stillFilters[step]
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(bounds, paint)
                        drawContent()
                        canvas.restore()
                    }
                }
            }
        }

    /** Every still filter step, built once on first use; step 0 is never drawn through a layer. */
    private val stillFilters: Array<ColorFilter> by lazy { Array(STILL_STEPS + 1) { stillFilter(it.toFloat() / STILL_STEPS) } }

    /** The banner, then black over everything. */
    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        val text = rememberTextMeasurer()
        // `.banner`: left 0, bottom 8cqw, padding 1.6cqw 8cqw 1.6cqw 4cqw, arrow-clipped; shifted by its own width.
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = frame(time()).stillAlpha }
                .drawWithCache {
                    val u = size.cqw
                    // `.banner b`: 800 italic 5cqw/1 display, letter-spacing .02em, its own line.
                    val heading = text.measure(
                        "NEXT EPISODE",
                        TextStyle(
                            color = Color.White,
                            fontSize = (5 * u).toSp(),
                            lineHeight = (5 * u).toSp(),
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = .02.em,
                        ),
                    )
                    // `.banner small`: 500 1.8cqw mono, letter-spacing .2em, opacity .85.
                    val subtitle = text.measure(
                        "SEE YOU SOON",
                        TextStyle(
                            color = AnimeBannerSubtitle,
                            fontSize = (1.8f * u).toSp(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = .2.em,
                        ),
                    )
                    val width = 4 * u + maxOf(heading.size.width, subtitle.size.width) + 8 * u
                    val height = 1.6f * u + heading.size.height + subtitle.size.height + 1.6f * u
                    val top = size.height - 8 * u - height
                    // clip-path: polygon(0 0, 90% 0, 100% 50%, 90% 100%, 0 100%)
                    val arrow = Path().apply {
                        moveTo(0f, top)
                        lineTo(.9f * width, top)
                        lineTo(width, top + height / 2)
                        lineTo(.9f * width, top + height)
                        lineTo(0f, top + height)
                        close()
                    }
                    val headingAt = Offset(4 * u, top + 1.6f * u)
                    val subtitleAt = Offset(4 * u, headingAt.y + heading.size.height)
                    onDrawBehind {
                        val shift = frame(time()).bannerShift
                        if (shift > BANNER_AWAY) {
                            translate(left = shift * width) {
                                clipPath(arrow) {
                                    drawRect(AnimeAccent)
                                    drawText(heading, topLeft = headingAt)
                                    drawText(subtitle, topLeft = subtitleAt)
                                }
                            }
                        }
                    }
                }
        )
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val alpha = frame(time()).blackAlpha
                    if (alpha > 0f) drawRect(Color.Black, alpha = alpha)
                }
        )
    }
}

/**
 * CSS `filter: grayscale(p) sepia(.65p) contrast(1 + .1p)` as one colour matrix, using the Filter Effects spec
 * matrices composed in order. Browsers clamp between filters; the composed matrix only clamps at the end, which
 * differs only where sepia pushes a channel past white.
 */
private fun stillFilter(progress: Float): ColorFilter {
    val g = 1 - progress
    val grayscale = floatArrayOf(
        .2126f + .7874f * g, .7152f - .7152f * g, .0722f - .0722f * g,
        .2126f - .2126f * g, .7152f + .2848f * g, .0722f - .0722f * g,
        .2126f - .2126f * g, .7152f - .7152f * g, .0722f + .9278f * g,
    )
    val s = 1 - .65f * progress
    val sepia = floatArrayOf(
        .393f + .607f * s, .769f - .769f * s, .189f - .189f * s,
        .349f - .349f * s, .686f + .314f * s, .168f - .168f * s,
        .272f - .272f * s, .534f - .534f * s, .131f + .869f * s,
    )
    val contrast = 1 + .1f * progress
    // ColorMatrix offsets are on the 0..255 scale.
    val offset = (.5f - .5f * contrast) * 255
    val values = FloatArray(20)
    for (row in 0 until 3) {
        for (col in 0 until 3) {
            var sum = 0f
            for (k in 0 until 3) sum += sepia[row * 3 + k] * grayscale[k * 3 + col]
            values[row * 5 + col] = sum * contrast
        }
        values[row * 5 + 4] = offset
    }
    values[18] = 1f
    return ColorFilter.colorMatrix(ColorMatrix(values))
}
