package com.programmersbox.desktop.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track

/*
 * Ported from the `The End panel` entry in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** One frame of The End panel, in the mockup's units. */
internal class TheEndFrame(
    /** `.paper` opacity, 0..1. */
    val paperAlpha: Float,
    /** App scale about its centre. */
    val appScale: Float,
    /** App rotate, degrees clockwise. */
    val appRotation: Float,
    /** App `box-shadow: 0 0 0 N #111` spread (the panel border), cqw before the app's own scale. */
    val frameWidth: Float,
    /** App `grayscale()`, 0..1. */
    val grayscale: Float,
    /** App `contrast()`, 1 = unchanged. */
    val contrast: Float,
    /** App opacity (with its panel border), 0..1. */
    val appAlpha: Float,
    /** `.cap` opacity; overshoots past 1 on the `E.back` pop, so clamp before use. */
    val captionAlpha: Float,
    /** `.cap` scale about its top-left corner. */
    val captionScale: Float,
)

/** The app shrinks into an inked panel on a white page, a THE END caption pops onto its corner, and the page fades. */
internal object TheEndPanel : DesktopMotion {
    override val key = "the-end-panel"
    override val title = "The End panel"
    override val durationMillis = 2300

    /** `.cap` rotate, degrees clockwise (constant). */
    private const val CAPTION_ROTATION = -5f

    fun frame(time: Int) = TheEndFrame(
        paperAlpha = track(time, Keyframe(1700, 1f, MockupEasing.In), Keyframe(2200, 0f)),
        appScale = track(time, Keyframe(0, 1f, MockupEasing.InOut), Keyframe(800, .6f)),
        appRotation = track(time, Keyframe(0, 0f, MockupEasing.InOut), Keyframe(800, -2f)),
        frameWidth = track(time, Keyframe(0, 0f, MockupEasing.InOut), Keyframe(800, 1.4f)),
        grayscale = track(time, Keyframe(0, 0f, MockupEasing.InOut), Keyframe(800, 1f)),
        contrast = track(time, Keyframe(0, 1f, MockupEasing.InOut), Keyframe(800, 1.2f)),
        appAlpha = track(time, Keyframe(1700, 1f, MockupEasing.In), Keyframe(2200, 0f)),
        captionAlpha = track(time, Keyframe(850, 0f, MockupEasing.Back), Keyframe(1150, 1f), Keyframe(1700, 1f, MockupEasing.In), Keyframe(2200, 0f)),
        captionScale = track(time, Keyframe(850, .4f, MockupEasing.Back), Keyframe(1150, 1f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = this
        .graphicsLayer {
            val frame = frame(time())
            scaleX = frame.appScale
            scaleY = frame.appScale
            rotationZ = frame.appRotation
            alpha = frame.appAlpha
        }
        .drawWithContent {
            val frame = frame(time())
            drawInked(frame.grayscale, frame.contrast)
        }

    /** Paper, then the panel border just outside the app, transformed like the app so it hugs it. */
    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        Canvas(Modifier.matchParentSize()) {
            val frame = frame(time())
            drawRect(Paper, alpha = frame.paperAlpha)
            val width = frame.frameWidth * cqw
            if (width > 0f && frame.appAlpha > 0f) {
                withTransform({
                    rotate(frame.appRotation, center)
                    scale(frame.appScale, frame.appScale, center)
                }) {
                    drawRect(
                        Ink,
                        topLeft = Offset(-width / 2, -width / 2),
                        size = Size(size.width + width, size.height + width),
                        alpha = frame.appAlpha,
                        style = Stroke(width),
                    )
                }
            }
        }
    }

    /** `.cap`: white box, .5cqw border, padding .8cqw 2cqw, at (16cqw, 8cqw), rotated and scaled about that corner. */
    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val frame = frame(time())
                    val u = size.cqw
                    alpha = frame.captionAlpha.coerceIn(0f, 1f)
                    scaleX = frame.captionScale
                    scaleY = frame.captionScale
                    rotationZ = CAPTION_ROTATION
                    transformOrigin = TransformOrigin(16 * u / size.width, 8 * u / size.height)
                }
                .drawWithCache {
                    val u = size.cqw
                    val label = text.measure("THE END", displayStyle(3.6f * u, Ink))
                    val border = .5f * u
                    val box = Rect(
                        Offset(16 * u, 8 * u),
                        Size(label.size.width + 2 * (2 * u + border), label.size.height + 2 * (.8f * u + border)),
                    )
                    val outline = box.deflate(border / 2)
                    onDrawBehind {
                        drawRect(Color.White, box.topLeft, box.size)
                        drawRect(Ink, outline.topLeft, outline.size, style = Stroke(border))
                        drawText(label, topLeft = box.topLeft + Offset(2 * u + border, .8f * u + border))
                    }
                }
        )
    }
}
