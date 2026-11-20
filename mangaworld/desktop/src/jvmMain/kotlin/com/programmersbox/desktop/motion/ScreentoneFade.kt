package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.programmersbox.kmpuiviews.motion.DesktopMotion

/*
 * Ported from the `Screentone fade` entry in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** One frame of Screentone fade, in the mockup's units. */
internal class ScreentoneFrame(
    /** App `grayscale()`, 0..1. */
    val grayscale: Float,
    /** App `contrast()`, 1 = unchanged. */
    val contrast: Float,
    /** App opacity, 0..1. */
    val appAlpha: Float,
    /** `.tone` dot radius (`--r`), cqw; at 2cqw the 2.4cqw grid is solid. */
    val toneRadius: Float,
    /** `.tone` opacity, 0..1. */
    val toneAlpha: Float,
    /** `.owari` opacity, 0..1. */
    val owariAlpha: Float,
    /** `.owari` scale about the host centre. */
    val owariScale: Float,
)

/** The app drains to grayscale while halftone dots grow to solid black; おわり shows, then everything fades. */
internal object ScreentoneFade : DesktopMotion {
    override val key = "screentone-fade"
    override val title = "Screentone fade"
    override val durationMillis = 2400

    fun frame(time: Int) = ScreentoneFrame(
        grayscale = track(time, Key(0, 0f), Key(600, 1f)),
        contrast = track(time, Key(0, 1f), Key(600, 1.3f)),
        appAlpha = track(time, Key(1300, 1f), Key(1310, 0f)),
        toneRadius = track(time, Key(0, 0f, EaseIn), Key(1250, 2f)),
        toneAlpha = track(time, Key(1900, 1f, EaseIn), Key(2300, 0f)),
        owariAlpha = track(time, Key(1200, 0f, EaseOut), Key(1500, 1f), Key(1900, 1f, EaseIn), Key(2300, 0f)),
        owariScale = track(time, Key(1200, .85f, EaseOut), Key(1500, 1f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = this
        .graphicsLayer { alpha = frame(time()).appAlpha }
        .drawWithContent {
            val frame = frame(time())
            drawInked(frame.grayscale, frame.contrast)
        }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        Spacer(
            Modifier
                .matchParentSize()
                // Layer opacity so overlapping dots fade as one sheet.
                .graphicsLayer { alpha = frame(time()).toneAlpha }
                .drawWithCache {
                    val u = size.cqw
                    val area = size
                    // `0 0 / 2.4cqw 2.4cqw` tiles, one dot centred in each.
                    val dots = buildList {
                        var y = 1.2f * u
                        while (y - 1.2f * u < area.height) {
                            var x = 1.2f * u
                            while (x - 1.2f * u < area.width) {
                                add(Offset(x, y))
                                x += 2.4f * u
                            }
                            y += 2.4f * u
                        }
                    }
                    onDrawBehind {
                        val radius = frame(time()).toneRadius * u
                        if (radius > 0f) drawPoints(dots, PointMode.Points, ToneInk, strokeWidth = 2 * radius, cap = StrokeCap.Round)
                    }
                }
        )
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val owari = text.measure("おわり", displayStyle(9 * size.cqw, Color.White))
                    val topLeft = size.center - Offset(owari.size.width / 2f, owari.size.height / 2f)
                    onDrawBehind {
                        val frame = frame(time())
                        if (frame.owariAlpha > 0f) {
                            scale(frame.owariScale, center) { drawText(owari, topLeft = topLeft, alpha = frame.owariAlpha) }
                        }
                    }
                }
        )
    }
}
