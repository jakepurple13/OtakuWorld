package com.programmersbox.desktop.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.em
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track
import com.programmersbox.kmpuiviews.motion.verticalCqw

/*
 * Ported from the `Ink panels` entry in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** Whole-page layers of one Ink panels frame. */
internal class InkPanelsFrame(
    /** `.paper` opacity, 0..1. */
    val paperAlpha: Float,
    /** `.panelsvg` opacity, 0..1; fades the inked borders once the panels sit on them. */
    val strokeAlpha: Float,
)

/** One panel in one Ink panels frame. */
internal class InkPanelFrame(
    /** How much of the border has been inked (1 − `stroke-dashoffset`), 0..1 of its perimeter. */
    val drawn: Float,
    /** `.pnl` opacity, 0..1. */
    val alpha: Float,
    /** How far along its fly-away the panel is, 0..1 of [InkPanel.flyX] / [InkPanel.flyY] / [InkPanel.flyRotation]. */
    val fly: Float,
)

/**
 * One manga panel: `G[i]` box in percent of the host (viewBox 0..100 stretched both ways), and its `fly[i]`
 * exit as cqw (x), vertical cqw (y, see [verticalCqw]) and degrees clockwise.
 */
internal class InkPanel(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val flyX: Float,
    val flyY: Float,
    val flyRotation: Float,
)

/** Panel borders ink themselves onto a blank page, fill with screentone and the title, then fly apart. */
internal object InkPanels : DesktopMotion {
    override val key = "ink-panels"
    override val title = "Ink panels"
    override val durationMillis = 2600

    val panels = listOf(
        InkPanel(2f, 3f, 56f, 45f, -70f, -40f, -9f),
        InkPanel(60f, 3f, 38f, 45f, 70f, -40f, 9f),
        InkPanel(2f, 51f, 30f, 46f, -70f, 40f, -7f),
        InkPanel(34f, 51f, 30f, 46f, 0f, 60f, 5f),
        InkPanel(66f, 51f, 32f, 46f, 70f, 40f, 8f),
    )

    fun frame(time: Int) = InkPanelsFrame(
        paperAlpha = track(time, Keyframe(1800, 1f, MockupEasing.InOut), Keyframe(2250, 0f)),
        strokeAlpha = track(time, Keyframe(1350, 1f), Keyframe(1500, 0f)),
    )

    fun panel(index: Int, time: Int) = InkPanelFrame(
        drawn = track(time, Keyframe(index * 140, 0f, MockupEasing.InOut), Keyframe(index * 140 + 500, 1f)),
        alpha = track(time, Keyframe(950 + index * 70, 0f), Keyframe(1200 + index * 70, 1f), Keyframe(1800 + index * 40, 1f, MockupEasing.In), Keyframe(2400 + index * 40, 0f)),
        fly = track(time, Keyframe(1800 + index * 40, 0f, MockupEasing.In), Keyframe(2400 + index * 40, 1f)),
    )

    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        Canvas(Modifier.matchParentSize()) { drawRect(Paper, alpha = frame(time()).paperAlpha) }
        Spacer(Modifier.matchParentSize().drawWithCache { drawStrokes(time) })
        val text = rememberTextMeasurer()
        panels.forEachIndexed { index, panel ->
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val frame = panel(index, time())
                        val box = panel.box(size)
                        alpha = frame.alpha
                        translationX = panel.flyX * frame.fly * size.cqw
                        translationY = panel.flyY * frame.fly * size.verticalCqw
                        rotationZ = panel.flyRotation * frame.fly
                        transformOrigin = TransformOrigin(box.center.x / size.width, box.center.y / size.height)
                    }
                    .drawWithCache { drawPanel(index, panel, text) }
            )
        }
    }

    /** `.panelsvg`: each border inked along its perimeter from the top-left corner, clockwise. */
    private fun CacheDrawScope.drawStrokes(time: () -> Int): DrawResult {
        val u = size.cqw
        val outlines = panels.map { panel ->
            Path().apply {
                val left = panel.x * size.width / 100
                val top = panel.y * size.height / 100
                val right = (panel.x + panel.width) * size.width / 100
                val bottom = (panel.y + panel.height) * size.height / 100
                moveTo(left, top)
                lineTo(right, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
        }
        val measures = outlines.map { outline -> PathMeasure().apply { setPath(outline, false) } }
        val segment = Path()
        val stroke = Stroke(width = .75f * u, join = StrokeJoin.Miter)
        return onDrawBehind {
            val t = time()
            val alpha = frame(t).strokeAlpha
            if (alpha == 0f) return@onDrawBehind
            outlines.forEachIndexed { index, outline ->
                val drawn = panel(index, t).drawn
                when {
                    drawn >= 1f -> drawPath(outline, PanelInk, alpha, stroke)
                    drawn > 0f -> {
                        val measure = measures[index]
                        segment.reset()
                        measure.getSegment(0f, drawn * measure.length, segment, true)
                        drawPath(segment, PanelInk, alpha, stroke)
                    }
                }
            }
        }
    }

    /** `.pnl`: white, screentone dots, .75cqw border, plus the panel's own contents. Size-only; no time read. */
    private fun CacheDrawScope.drawPanel(index: Int, panel: InkPanel, text: TextMeasurer): DrawResult {
        val u = size.cqw
        val border = .75f * u
        val box = panel.box(size)
        val inner = box.deflate(border)
        val dots = buildList {
            // radial-gradient(circle, #0000002b .34cqw, transparent .4cqw) 0 0 / 1.3cqw 1.3cqw
            var y = inner.top + .65f * u
            while (y - .4f * u < inner.bottom) {
                var x = inner.left + .65f * u
                while (x - .4f * u < inner.right) {
                    add(Offset(x, y))
                    x += 1.3f * u
                }
                y += 1.3f * u
            }
        }
        val title = if (index == 0) {
            text.measure("マンガ", displayStyle(5.6f * u, PanelInk)) to
                text.measure("MANGAWORLD", TextStyle(color = PanelInk, fontSize = (2 * u).toSp(), fontFamily = FontFamily.Monospace, letterSpacing = .2.em))
        } else {
            null
        }
        return onDrawBehind {
            drawRect(Color.White, inner.topLeft, inner.size)
            clipRect(inner.left, inner.top, inner.right, inner.bottom) {
                drawPoints(dots, PointMode.Points, DotShade, strokeWidth = .74f * u, cap = StrokeCap.Round)
                when (index) {
                    0 -> title?.let { (kana, name) ->
                        // .t: kana at line-height 1, then the name 1cqw below, centred as one block.
                        val gap = 1 * u
                        val top = inner.center.y - (kana.size.height + gap + name.size.height) / 2
                        drawText(kana, topLeft = Offset(inner.center.x - kana.size.width / 2, top))
                        drawText(name, topLeft = Offset(inner.center.x - name.size.width / 2, top + kana.size.height + gap))
                    }

                    // .bub: 60% × 46% ellipse with a .5cqw border outside that box (content-box).
                    1 -> {
                        val bubbleSize = Size(inner.width * .6f + u, inner.height * .46f + u)
                        val bubble = Rect(inner.center - Offset(bubbleSize.width / 2, bubbleSize.height / 2), bubbleSize)
                        val outline = bubble.deflate(.25f * u)
                        drawOval(Color.White, bubble.topLeft, bubble.size)
                        drawOval(PanelInk, outline.topLeft, outline.size, style = Stroke(.5f * u))
                    }

                    // .spd: repeating-linear-gradient(90deg, #121212 0 .25cqw, transparent .25cqw 2.4cqw), opacity .6
                    3 -> {
                        var x = inner.left
                        while (x < inner.right) {
                            drawRect(PanelInk, Offset(x, inner.top), Size(.25f * u, inner.height), alpha = .6f)
                            x += 2.4f * u
                        }
                    }
                }
            }
            val frame = box.deflate(border / 2)
            drawRect(PanelInk, frame.topLeft, frame.size, style = Stroke(border))
        }
    }
}

/** `#0000002b` screentone dot. */
private val DotShade = Color(0x2B000000)

/** The panel's border box in pixels: the `G` box plus its .75cqw border on the right and bottom (content-box). */
private fun InkPanel.box(size: Size): Rect {
    val border = .75f * size.cqw
    val left = x * size.width / 100
    val top = y * size.height / 100
    return Rect(left, top, left + width * size.width / 100 + 2 * border, top + height * size.height / 100 + 2 * border)
}
