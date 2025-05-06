package com.programmersbox.kmpuiviews.presentation.components.textflow

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * Ported from [Here](https://github.com/oleksandrbalan/textflow/tree/main)
 *
 * The composable to draw a text which flows around an "obstacle". Obstacle can be placed to the start top corner or to
 * the end top corner based on [obstacleAlignment]. Use [obstacleContent] lambda to provide a composable for an
 * "obstacle".
 *
 * @param text The text to be displayed.
 * @param modifier The modifier for root composable.
 * @param obstacleAlignment The alignment for an "obstacle" inside the text.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set, this will be
 * [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic). See [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter. See [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline). See [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph. See [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the text will be
 * positioned as if there was unlimited horizontal space. If [softWrap] is false, [overflow] and TextAlign may have
 * unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary. If the text
 * exceeds the given number of lines, it will be truncated according to [overflow] and [softWrap]. If it is not null,
 * then it must be greater than zero.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A [TextLayoutResult] object
 * that callback provides contains paragraph information, size of the text, baselines and other details. The callback
 * can be used to add additional decoration or functionality to the text. For example, to draw selection around
 * the text.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param obstacleContent The slot for an "obstacle".
 */
@Composable
public fun TextFlow(
    text: String,
    modifier: Modifier = Modifier,
    obstacleAlignment: TextFlowObstacleAlignment = TextFlowObstacleAlignment.TopStart,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult?, TextLayoutResult?) -> Unit = { _, _ -> },
    style: TextStyle = LocalTextStyle.current,
    obstacleContent: @Composable () -> Unit = {},
) {
    TextFlow(
        text = remember(text) { AnnotatedString(text) },
        modifier = modifier,
        obstacleAlignment = obstacleAlignment,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = style,
        obstacleContent = obstacleContent,
    )
}

/**
 * The composable to draw a text which flows around an "obstacle". Obstacle can be placed to the start top corner or to
 * the end top corner based on [obstacleAlignment]. Use [obstacleContent] lambda to provide a composable for an
 * "obstacle".
 *
 * @param text The text to be displayed.
 * @param modifier The modifier for root composable.
 * @param obstacleAlignment The alignment for an "obstacle" inside the text.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set, this will be
 * [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic). See [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter. See [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline). See [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph. See [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the text will be
 * positioned as if there was unlimited horizontal space. If [softWrap] is false, [overflow] and TextAlign may have
 * unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary. If the text
 * exceeds the given number of lines, it will be truncated according to [overflow] and [softWrap]. If it is not null,
 * then it must be greater than zero.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A [TextLayoutResult] object
 * that callback provides contains paragraph information, size of the text, baselines and other details. The callback
 * can be used to add additional decoration or functionality to the text. For example, to draw selection around
 * the text.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param obstacleContent The slot for an "obstacle".
 */
@Composable
public fun TextFlow(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    obstacleAlignment: TextFlowObstacleAlignment = TextFlowObstacleAlignment.TopStart,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult?, TextLayoutResult?) -> Unit = { _, _ -> },
    style: TextStyle = LocalTextStyle.current,
    obstacleContent: @Composable () -> Unit = {},
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current
        }
    }

    val layoutObstacleAlignment: TextFlowLayoutObstacleAlignment =
        when (obstacleAlignment) {
            TextFlowObstacleAlignment.TopStart -> TextFlowLayoutObstacleAlignment.TopStart
            TextFlowObstacleAlignment.TopEnd -> TextFlowLayoutObstacleAlignment.TopEnd
        }

    TextFlowLayout(
        text = text,
        style = style,
        modifier = modifier,
        obstacleAlignment = layoutObstacleAlignment,
        color = textColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        obstacleContent = obstacleContent,
    )
}

/**
 * The allowed alignment for an "obstacle" inside the [TextFlow] composable.
 */
public enum class TextFlowObstacleAlignment {
    /**
     * Obstacle is aligned in the top start corner.
     */
    TopStart,

    /**
     * Obstacle is aligned in the top end corner.
     */
    TopEnd,
}