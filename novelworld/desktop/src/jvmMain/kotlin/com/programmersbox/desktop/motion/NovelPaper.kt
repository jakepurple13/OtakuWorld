package com.programmersbox.desktop.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.programmersbox.kmpuiviews.motion.cqw
import kotlin.math.ceil
import kotlin.math.floor

/*
 * The ruled paper shared by `Typewriter`, `Ink drop` and `The End` in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.cream`), the ink colour of their text, and the typed-substring line shared by `Typewriter` and `The End`.
 */

/** `.cream` background. */
internal val NovelPaperCream = Color(0xFFF3ECDD)

/** `.cream` ruling (`#b7a98c33`). */
private val NovelPaperRule = Color(0x33B7A98C)

/** `.tw` / `.ttl` / `.end` text colour. */
internal val NovelTypeInk = Color(0xFF2A2219)

/**
 * `.cream`: `#f3ecdd` under `repeating-linear-gradient(transparent 0 3.9cqw, #b7a98c33 3.9cqw 4cqw)`, a .1cqw rule
 * at the bottom of every 4cqw band, for a host of [size]; built in `drawWithCache`.
 */
internal class NovelPaper(size: Size) {
    private val u = size.cqw
    private val ruleSize = Size(size.width, .1f * u)

    /** Top of every rule that starts on the page. */
    private val rules = FloatArray(ceil(size.height / (4 * u)).toInt()) { (3.9f + 4 * it) * u }

    /** The paper, in the current draw coordinates. */
    fun DrawScope.drawPaper() {
        drawRect(NovelPaperCream)
        for (top in rules) drawRect(NovelPaperRule, Offset(0f, top), ruleSize)
    }
}

/**
 * CSS `width: Nch` animated with `steps(count, end)` from [start] to [end] ms: how many of [count] characters show
 * at [time].
 */
internal fun typedChars(time: Int, start: Int, end: Int, count: Int): Int =
    floor(count * ((time - start).toFloat() / (end - start)).coerceIn(0f, 1f)).toInt()

/**
 * A line typed out by revealing its first characters (`.tw span` / `.end span`: `overflow: hidden` on a growing
 * `width`). [layout] is measured once in `drawWithCache`; the edge after each character is looked up once here.
 */
internal class NovelTypedLine(private val layout: TextLayoutResult) {
    private val edges = FloatArray(layout.layoutInput.text.length + 1) { layout.getHorizontalPosition(it, usePrimaryDirection = true) }

    val height = layout.size.height.toFloat()

    /** Width of the first [chars] characters. */
    fun width(chars: Int) = edges[chars]

    /** The first [chars] characters at [alpha], with the line's top-left at [topLeft]. */
    fun DrawScope.drawTyped(topLeft: Offset, chars: Int, alpha: Float = 1f) {
        if (chars <= 0) return
        clipRect(topLeft.x, topLeft.y, topLeft.x + edges[chars], topLeft.y + height) {
            drawText(layout, topLeft = topLeft, alpha = alpha)
        }
    }
}
