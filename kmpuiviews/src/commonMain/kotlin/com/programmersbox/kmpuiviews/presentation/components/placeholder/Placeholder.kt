package com.programmersbox.kmpuiviews.presentation.components.placeholder

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Contains default values used by [Modifier.placeholder] and [PlaceholderHighlight].
 */
public object PlaceholderDefaults {
    /**
     * The default [InfiniteRepeatableSpec] to use for [fade].
     */
    public val fadeAnimationSpec: InfiniteRepeatableSpec<Float> by lazy {
        infiniteRepeatable(
            animation = tween(delayMillis = 200, durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        )
    }

    /**
     * The default [InfiniteRepeatableSpec] to use for [shimmer].
     */
    public val shimmerAnimationSpec: InfiniteRepeatableSpec<Float> by lazy {
        infiniteRepeatable(
            animation = tween(durationMillis = 1700, delayMillis = 200),
            repeatMode = RepeatMode.Restart
        )
    }
}

/**
 * Draws some skeleton UI which is typically used whilst content is 'loading'.
 *
 * A version of this modifier which uses appropriate values for Material themed apps is available
 * in the 'Placeholder Material' library.
 *
 * You can provide a [PlaceholderHighlight] which runs an highlight animation on the placeholder.
 * The [shimmer] and [fade] implementations are provided for easy usage.
 *
 * A cross-fade transition will be applied to the content and placeholder UI when the [visible]
 * value changes. The transition can be customized via the [contentFadeTransitionSpec] and
 * [placeholderFadeTransitionSpec] parameters.
 *
 * You can find more information on the pattern at the Material Theming
 * [Placeholder UI](https://material.io/design/communication/launch-screen.html#placeholder-ui)
 * guidelines.
 *
 * @sample com.google.accompanist.sample.placeholder.DocSample_Foundation_Placeholder
 *
 * @param visible whether the placeholder should be visible or not.
 * @param color the color used to draw the placeholder UI.
 * @param shape desired shape of the placeholder. Defaults to [RectangleShape].
 * @param highlight optional highlight animation.
 * @param placeholderFadeTransitionSpec The transition spec to use when fading the placeholder
 * on/off screen. The boolean parameter defined for the transition is [visible].
 * @param contentFadeTransitionSpec The transition spec to use when fading the content
 * on/off screen. The boolean parameter defined for the transition is [visible].
 */
internal fun Modifier.placeholder(
    visible: Boolean,
    color: Color,
    shape: Shape = RectangleShape,
    highlight: PlaceholderHighlight? = null,
    placeholderFadeTransitionSpec: FiniteAnimationSpec<Float> = spring(),
    contentFadeTransitionSpec: FiniteAnimationSpec<Float> = spring(),
): Modifier = this then PlaceholderElement(
    visible = visible,
    color = color,
    shape = shape,
    highlight = highlight,
    placeholderFadeTransitionSpec = placeholderFadeTransitionSpec,
    contentFadeTransitionSpec = contentFadeTransitionSpec
)

private data class PlaceholderElement(
    val visible: Boolean,
    val color: Color,
    val shape: Shape,
    val highlight: PlaceholderHighlight?,
    val placeholderFadeTransitionSpec: FiniteAnimationSpec<Float>,
    val contentFadeTransitionSpec: FiniteAnimationSpec<Float>,
) : ModifierNodeElement<PlaceholderNode>() {

    override fun create(): PlaceholderNode = PlaceholderNode(
        visible = visible,
        color = color,
        shape = shape,
        highlight = highlight,
        placeholderFadeTransitionSpec = placeholderFadeTransitionSpec,
        contentFadeTransitionSpec = contentFadeTransitionSpec
    )

    override fun update(node: PlaceholderNode) {
        node.update(
            newVisible = visible,
            newColor = color,
            newShape = shape,
            newHighlight = highlight,
            newPlaceholderFadeTransitionSpec = placeholderFadeTransitionSpec,
            newContentFadeTransitionSpec = contentFadeTransitionSpec
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "placeholder"
        value = visible
        properties["visible"] = visible
        properties["color"] = color
        properties["highlight"] = highlight
        properties["shape"] = shape
    }
}

private class PlaceholderNode(
    var visible: Boolean,
    var color: Color,
    var shape: Shape,
    var highlight: PlaceholderHighlight?,
    var placeholderFadeTransitionSpec: FiniteAnimationSpec<Float>,
    var contentFadeTransitionSpec: FiniteAnimationSpec<Float>,
) : Modifier.Node(), DrawModifierNode {

    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    private val placeholderAlpha = Animatable(if (visible) 1f else 0f)
    private val contentAlpha = Animatable(if (visible) 0f else 1f)
    private val highlightProgress = Animatable(0f)

    private val paint = Paint()

    override fun onAttach() {
        // Monitors visibility or fade-out state to smartly start/stop the infinite highlight animation
        coroutineScope.launch {
            snapshotFlow { visible || placeholderAlpha.value >= 0.01f }.collectLatest { shouldRun ->
                if (shouldRun && highlight?.animationSpec != null) {
                    highlightProgress.snapTo(0f)
                    highlightProgress.animateTo(1f, highlight!!.animationSpec!!)
                }
            }
        }
    }

    fun update(
        newVisible: Boolean,
        newColor: Color,
        newShape: Shape,
        newHighlight: PlaceholderHighlight?,
        newPlaceholderFadeTransitionSpec: FiniteAnimationSpec<Float>,
        newContentFadeTransitionSpec: FiniteAnimationSpec<Float>,
    ) {
        val visibleChanged = visible != newVisible

        visible = newVisible
        color = newColor
        if (shape != newShape) {
            shape = newShape
            lastOutline = null // Invalidate cached outline
        }
        highlight = newHighlight
        placeholderFadeTransitionSpec = newPlaceholderFadeTransitionSpec
        contentFadeTransitionSpec = newContentFadeTransitionSpec

        if (visibleChanged) {
            coroutineScope.launch {
                placeholderAlpha.animateTo(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = placeholderFadeTransitionSpec
                )
            }
            coroutineScope.launch {
                contentAlpha.animateTo(
                    targetValue = if (visible) 0f else 1f,
                    animationSpec = contentFadeTransitionSpec
                )
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val pAlpha = placeholderAlpha.value
        val cAlpha = contentAlpha.value
        val hProgress = highlightProgress.value

        // Draw the composable content first
        if (cAlpha in 0.01f..0.99f) {
            paint.alpha = cAlpha
            withLayer(paint) {
                this@draw.drawContent()
            }
        } else if (cAlpha >= 0.99f) {
            drawContent()
        }

        if (pAlpha in 0.01f..0.99f) {
            paint.alpha = pAlpha
            withLayer(paint) {
                lastOutline = drawPlaceholder(
                    shape = shape,
                    color = color,
                    highlight = highlight,
                    progress = hProgress,
                    lastOutline = lastOutline,
                    lastLayoutDirection = lastLayoutDirection,
                    lastSize = lastSize,
                )
            }
        } else if (pAlpha >= 0.99f) {
            lastOutline = drawPlaceholder(
                shape = shape,
                color = color,
                highlight = highlight,
                progress = hProgress,
                lastOutline = lastOutline,
                lastLayoutDirection = lastLayoutDirection,
                lastSize = lastSize,
            )
        }

        lastSize = size
        lastLayoutDirection = layoutDirection
    }
}

private fun DrawScope.drawPlaceholder(
    shape: Shape,
    color: Color,
    highlight: PlaceholderHighlight?,
    progress: Float,
    lastOutline: Outline?,
    lastLayoutDirection: LayoutDirection?,
    lastSize: Size?,
): Outline? {
    if (shape === RectangleShape) {
        drawRect(color = color)
        if (highlight != null) {
            drawRect(
                brush = highlight.brush(progress, size),
                alpha = highlight.alpha(progress),
            )
        }
        return null
    }

    val outline = lastOutline.takeIf {
        size == lastSize && layoutDirection == lastLayoutDirection
    } ?: shape.createOutline(size, layoutDirection, this)

    drawOutline(outline = outline, color = color)

    if (highlight != null) {
        drawOutline(
            outline = outline,
            brush = highlight.brush(progress, size),
            alpha = highlight.alpha(progress),
        )
    }

    return outline
}

private inline fun DrawScope.withLayer(
    paint: Paint,
    drawBlock: DrawScope.() -> Unit,
) = drawIntoCanvas { canvas ->
    canvas.saveLayer(size.toRect(), paint)
    drawBlock()
    canvas.restore()
}

// PlaceholderHighlight -------------------------

@Stable
public interface PlaceholderHighlight {
    public val animationSpec: InfiniteRepeatableSpec<Float>?

    public fun brush(
        @FloatRange(from = 0.0, to = 1.0) progress: Float,
        size: Size,
    ): Brush

    @FloatRange(from = 0.0, to = 1.0)
    public fun alpha(progress: Float): Float

    public companion object
}

public fun PlaceholderHighlight.Companion.fade(
    highlightColor: Color,
    animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.fadeAnimationSpec,
): PlaceholderHighlight = Fade(
    highlightColor = highlightColor,
    animationSpec = animationSpec,
)

public fun PlaceholderHighlight.Companion.shimmer(
    highlightColor: Color,
    animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.shimmerAnimationSpec,
    @FloatRange(from = 0.0, to = 1.0) progressForMaxAlpha: Float = 0.6f,
): PlaceholderHighlight = Shimmer(
    highlightColor = highlightColor,
    animationSpec = animationSpec,
    progressForMaxAlpha = progressForMaxAlpha,
)

private data class Fade(
    private val highlightColor: Color,
    override val animationSpec: InfiniteRepeatableSpec<Float>,
) : PlaceholderHighlight {
    private val brush = SolidColor(highlightColor)

    override fun brush(progress: Float, size: Size): Brush = brush
    override fun alpha(progress: Float): Float = progress
}

private data class Shimmer(
    private val highlightColor: Color,
    override val animationSpec: InfiniteRepeatableSpec<Float>,
    private val progressForMaxAlpha: Float = 0.6f,
) : PlaceholderHighlight {
    override fun brush(
        progress: Float,
        size: Size,
    ): Brush = Brush.radialGradient(
        colors = listOf(
            highlightColor.copy(alpha = 0f),
            highlightColor,
            highlightColor.copy(alpha = 0f),
        ),
        center = Offset(x = 0f, y = 0f),
        radius = (max(size.width, size.height) * progress * 2).coerceAtLeast(0.01f),
    )

    override fun alpha(progress: Float): Float = when {
        progress <= progressForMaxAlpha -> {
            lerp(
                start = 0f,
                stop = 1f,
                fraction = progress / progressForMaxAlpha
            )
        }
        else -> {
            lerp(
                start = 1f,
                stop = 0f,
                fraction = (progress - progressForMaxAlpha) / (1f - progressForMaxAlpha)
            )
        }
    }
}