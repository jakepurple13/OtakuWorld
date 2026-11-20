package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.em
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

/*
 * Ported from the `Typewriter` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.cream`, `.sheet`, `.tw`, `.tw span`, `.tw .l1`, `.tw .l2`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** `.sheet` box-shadow colour (`#0008`). */
private val NovelSheetShadow = Color(0x88000000)

/** `.tw .l1` text. */
private const val TYPEWRITER_TITLE = "NovelWorld"

/** `.tw .l2` text. */
private const val TYPEWRITER_LINE = "Chapter One, page 1"

/** `translateY(-105%)`: the sheet fed fully out of the top of the window, as a fraction of its height. */
private const val SHEET_FED = 1.05f

/** One frame of Typewriter, in the mockup's units. */
internal class TypewriterFrame(
    /** Characters of `.l1` typed. */
    val titleChars: Int,
    /** `.l1` caret (border-right) opacity, 0..1. */
    val titleCaret: Float,
    /** Characters of `.l2` typed. */
    val lineChars: Int,
    /** `.l2` caret opacity, 0..1. */
    val lineCaret: Float,
    /** Sheet translateY upwards, fraction of the host height. */
    val sheetLift: Float,
    /** App translateY, cqw. */
    val appDrop: Float,
)

/** NovelWorld and a chapter line type out on ruled paper, then the sheet feeds up and out to reveal the app. */
internal object Typewriter : DesktopMotion {
    override val key = "typewriter"
    override val title = "Typewriter"
    override val durationMillis = 2600

    fun frame(time: Int) = TypewriterFrame(
        titleChars = typedChars(time, 200, 1100, TYPEWRITER_TITLE.length),
        titleCaret = track(time, Keyframe(1100, 1f), Keyframe(1150, 0f)),
        lineChars = typedChars(time, 1200, 1850, TYPEWRITER_LINE.length),
        // The border colour interpolates from transparent to ink over the whole wait before the line starts.
        lineCaret = track(time, Keyframe(0, 0f), Keyframe(1200, 1f)),
        sheetLift = track(time, Keyframe(2000, 0f, MockupEasing.InOut), Keyframe(2600, SHEET_FED)),
        appDrop = track(time, Keyframe(2000, 8f, MockupEasing.Out), Keyframe(2600, 0f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = graphicsLayer {
        translationY = frame(time()).appDrop * size.cqw
    }

    /** `.cream.sheet` with `.tw` on it. */
    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val u = size.cqw
                    val paper = NovelPaper(size)
                    // `.sheet`: box-shadow 0 2cqw 4cqw #0008; the CSS blur radius is twice the Gaussian sigma.
                    val shadow = Paint().apply {
                        color = NovelSheetShadow
                        skiaPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 2 * u)
                    }
                    val shadowRect = Rect(Offset(0f, 2 * u), size)
                    // `.tw .l1`: 700 7cqw mono, line-height 1.1.
                    val heading = NovelTypedLine(
                        text.measure(
                            TYPEWRITER_TITLE,
                            TextStyle(
                                color = NovelTypeInk,
                                fontSize = (7 * u).toSp(),
                                lineHeight = 1.1.em,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    )
                    // `.tw .l2`: 3cqw mono, line-height 1.3.
                    val line = NovelTypedLine(
                        text.measure(
                            TYPEWRITER_LINE,
                            TextStyle(
                                color = NovelTypeInk,
                                fontSize = (3 * u).toSp(),
                                lineHeight = 1.3.em,
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    )
                    // `.tw`: left 12cqw, top 20cqw, a grid with a 2cqw gap.
                    val headingAt = Offset(12 * u, 20 * u)
                    val lineAt = Offset(12 * u, headingAt.y + heading.height + 2 * u)
                    // `.tw span`: border-right .35cqw, drawn at the edge of the typed width.
                    val caretWidth = .35f * u
                    val headingCaret = Size(caretWidth, heading.height)
                    val lineCaret = Size(caretWidth, line.height)
                    onDrawBehind {
                        val f = frame(time())
                        if (f.sheetLift < SHEET_FED) {
                            translate(top = -f.sheetLift * size.height) {
                                drawIntoCanvas { it.drawRect(shadowRect, shadow) }
                                with(paper) { drawPaper() }
                                with(heading) { drawTyped(headingAt, f.titleChars) }
                                if (f.titleCaret > 0f) {
                                    drawRect(NovelTypeInk, headingAt + Offset(heading.width(f.titleChars), 0f), headingCaret, f.titleCaret)
                                }
                                with(line) { drawTyped(lineAt, f.lineChars) }
                                if (f.lineCaret > 0f) {
                                    drawRect(NovelTypeInk, lineAt + Offset(line.width(f.lineChars), 0f), lineCaret, f.lineCaret)
                                }
                            }
                        }
                    }
                }
        )
    }
}
