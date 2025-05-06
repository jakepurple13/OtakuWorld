package com.programmersbox.kmpuiviews.presentation.components.textflow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

@Composable
internal fun TextFlowCanvas(
    text: AnnotatedString,
    obstacleSize: IntSize,
    obstacleAlignment: TextFlowLayoutObstacleAlignment,
    constraints: Constraints,
    style: TextStyle,
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
) {
    // Basically copy-pasta from Text composable
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
            // However we need to disable font padding to align both text paragraphs perfectly
            //platformStyle = PlatformTextStyle(false),
        ),
    )

    // Prepare text measurer instance to measure text based on constraints
    val textMeasurer = rememberTextMeasurer()

    // Measure 2 blocks of texts
    // The "top" one which is affected by the obstacle, thus has smaller width and second "bottom" one
    val result = textMeasurer.measureTextFlow(
        text = text,
        obstacleSize = obstacleSize,
        layoutWidth = constraints.maxWidth,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        mergedStyle = mergedStyle,
    )

    // Report text results to the caller
    onTextLayout(result.topTextResult, result.bottomTextResult)

    // Calculate final canvas size to use in the modifier
    val canvasSize = calculateCanvasSize(
        density = LocalDensity.current,
        result = result,
        obstacleSize = obstacleSize,
        constraints = constraints,
    )

    Canvas(modifier = Modifier.size(canvasSize)) {
        // Paint the top text with a horizontal offset
        translate(left = calculateTopBlockOffset(obstacleSize, obstacleAlignment)) {
            result.topTextResult?.multiParagraph?.paint(
                canvas = drawContext.canvas,
                color = color,
                decoration = textDecoration,
            )
        }

        // Paint the bottom text moved below by the top text's height
        translate(top = result.topTextHeight.toFloat()) {
            result.bottomTextResult?.multiParagraph?.paint(
                canvas = drawContext.canvas,
                color = color,
                decoration = textDecoration,
            )
        }
    }
}

private fun TextMeasurer.measureTextFlow(
    text: AnnotatedString,
    obstacleSize: IntSize,
    layoutWidth: Int,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    mergedStyle: TextStyle,
): TextFlowCanvasLayoutResult {
    var topBlock: TextLayoutResult? = null
    var topBlockVisibleLineCount = 0
    var topBlockLastCharIndex = -1
    var hasBottomBlock = true

    // Measure first block only if obstacle is present
    // Otherwise there is noting to wrap and only bottom block will be painted
    if (obstacleSize.height > 0) {
        topBlock = measure(
            text = text,
            style = mergedStyle,
            constraints = Constraints(
                maxWidth = layoutWidth - obstacleSize.width,
                maxHeight = Int.MAX_VALUE,
            ),
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
        )

        // Calculate real text height and lines count based on visible lines
        val lastVisibleLineIndex = topBlock.lastVisibleLineIndex(obstacleSize.height)
        val topBlockHeight = topBlock.getLineBottom(lastVisibleLineIndex)
        topBlockVisibleLineCount = lastVisibleLineIndex + 1

        // Also get index of the last character to know which part of the original text will belong to the bottom block
        topBlockLastCharIndex = topBlock.getOffsetForPosition(
            Offset(
                topBlock.getLineRight(lastVisibleLineIndex),
                topBlock.getLineTop(lastVisibleLineIndex),
            ),
        )

        // Check if text spans to the bottom block
        hasBottomBlock = topBlockVisibleLineCount < maxLines && topBlockLastCharIndex < text.length

        // Remeasure the top block with it's real height and displayed lines so that we do not have to clip it in canvas
        // and can report the correct text result to the caller
        topBlock = measure(
            text = text,
            style = mergedStyle,
            constraints = Constraints(
                maxWidth = layoutWidth - obstacleSize.width,
                maxHeight = topBlockHeight.toInt(),
            ),
            overflow = if (hasBottomBlock) TextOverflow.Clip else overflow,
            softWrap = softWrap,
            maxLines = topBlockVisibleLineCount,
        )
    }

    var bottomBlock: TextLayoutResult? = null
    if (hasBottomBlock) {
        bottomBlock = measure(
            text = text.subSequence(topBlockLastCharIndex + 1, text.length),
            style = mergedStyle,
            constraints = Constraints(
                maxWidth = layoutWidth,
                maxHeight = Int.MAX_VALUE,
            ),
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines - topBlockVisibleLineCount,
        )
    }

    return TextFlowCanvasLayoutResult(topBlock, bottomBlock)
}

private fun TextLayoutResult.lastVisibleLineIndex(height: Int): Int {
    repeat(lineCount) {
        if (getLineBottom(it) > height) {
            return it
        }
    }
    return lineCount - 1
}

private fun calculateCanvasSize(
    density: Density,
    result: TextFlowCanvasLayoutResult,
    obstacleSize: IntSize,
    constraints: Constraints,
): DpSize {
    val width = if (result.topTextResult != null) {
        obstacleSize.width + result.topTextResult.size.width
    } else if (result.bottomTextResult != null) {
        result.bottomTextResult.size.width
    } else {
        0
    }

    val height = result.topTextHeight + result.bottomTextHeight

    return with(density) {
        DpSize(
            width = constraints.constrainWidth(width).toDp(),
            height = constraints.constrainHeight(height).toDp(),
        )
    }
}

private fun calculateTopBlockOffset(
    obstacleSize: IntSize,
    obstacleAlignment: TextFlowLayoutObstacleAlignment,
): Float = if (obstacleAlignment == TextFlowLayoutObstacleAlignment.TopStart) {
    obstacleSize.width.toFloat()
} else {
    0f
}

private class TextFlowCanvasLayoutResult(
    val topTextResult: TextLayoutResult?,
    val bottomTextResult: TextLayoutResult?,
) {
    val topTextHeight: Int get() = topTextResult?.size?.height ?: 0
    val bottomTextHeight: Int get() = bottomTextResult?.size?.height ?: 0
}