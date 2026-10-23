package android.text

import android.graphics.Canvas
import android.graphics.Typeface
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.text.AttributedString

abstract class Layout protected constructor(
    private val text: CharSequence,
    private val paint: TextPaint,
    private val width: Int,
    private val alignment: Alignment,
) {
    enum class Alignment { ALIGN_NORMAL, ALIGN_OPPOSITE, ALIGN_CENTER }

    fun getText(): CharSequence = text
    fun getPaint(): TextPaint = paint
    fun getWidth(): Int = width
    fun getAlignment(): Alignment = alignment

    abstract fun getLineCount(): Int
    abstract fun getHeight(): Int
    abstract fun draw(canvas: Canvas)
}

/**
 * Word-wraps [source] to [width] with Java2D (font from the paint's typeface and text size) so extensions
 * that render text into page images (e.g. author notes) get a real height and visible text.
 */
open class StaticLayout(
    source: CharSequence,
    paint: TextPaint,
    width: Int,
    align: Alignment,
    private val spacingMult: Float,
    private val spacingAdd: Float,
    includePad: Boolean,
) : Layout(source, paint, width, align) {

    private val font: Font = (paint.getTypeface() ?: Typeface.DEFAULT).let { tf ->
        Font(tf.family, (if (tf.isBold()) Font.BOLD else 0) or (if (tf.isItalic()) Font.ITALIC else 0), 1)
            .deriveFont(paint.getTextSize())
    }

    private val lines: List<TextLayout> = run {
        val frc = FontRenderContext(null, true, true)
        source.toString().split('\n').flatMap { paragraph ->
            if (paragraph.isEmpty()) return@flatMap listOf(TextLayout(" ", font, frc))
            val attributed = AttributedString(paragraph).apply { addAttribute(TextAttribute.FONT, font) }
            val measurer = LineBreakMeasurer(attributed.iterator, frc)
            buildList {
                while (measurer.position < paragraph.length) add(measurer.nextLayout(width.coerceAtLeast(1).toFloat()))
            }
        }
    }

    private fun lineHeight(line: TextLayout): Float = (line.ascent + line.descent + line.leading) * spacingMult + spacingAdd

    override fun getLineCount(): Int = lines.size

    override fun getHeight(): Int = kotlin.math.ceil(lines.sumOf { lineHeight(it).toDouble() }).toInt()

    override fun draw(canvas: Canvas) {
        val g = canvas.graphics()
        val oldColor = g.color
        val oldHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING)
        g.color = Color(getPaint().getColor(), true)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        var y = 0f
        lines.forEach { line ->
            val x = when (getAlignment()) {
                Alignment.ALIGN_NORMAL -> 0f
                Alignment.ALIGN_OPPOSITE -> getWidth() - line.advance
                Alignment.ALIGN_CENTER -> (getWidth() - line.advance) / 2
            }
            line.draw(g, x, y + line.ascent)
            y += lineHeight(line)
        }
        g.color = oldColor
        oldHint?.let { g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, it) }
    }
}
