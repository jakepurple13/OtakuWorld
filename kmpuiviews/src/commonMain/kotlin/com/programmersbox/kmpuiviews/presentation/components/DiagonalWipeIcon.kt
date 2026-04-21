package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Defaults for [DiagonalWipeIcon].
 */
@Immutable
object DiagonalWipeIconDefaults {
    const val WipeInDurationMillis: Int = 530
    const val WipeOutDurationMillis: Int = 800

    val WipeInEasing: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val WipeOutEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val SeamOverlapPx: Float = 0.8f
    val defaultMotion: DiagonalWipeMotion = gentle()

    /**
     * Tween-based motion builder.
     */
    fun tween(
        direction: WipeDirection = WipeDirection.TopLeftToBottomRight,
        wipeInDurationMillis: Int = WipeInDurationMillis,
        wipeOutDurationMillis: Int = WipeOutDurationMillis,
        wipeInEasing: Easing = WipeInEasing,
        wipeOutEasing: Easing = WipeOutEasing,
        seamOverlapPx: Float = SeamOverlapPx,
    ): DiagonalWipeMotion {
        require(wipeInDurationMillis >= 0) { "wipeInDurationMillis must be >= 0" }
        require(wipeOutDurationMillis >= 0) { "wipeOutDurationMillis must be >= 0" }
        return DiagonalWipeMotion(
            direction = direction,
            wipeInSpec = tween(durationMillis = wipeInDurationMillis, easing = wipeInEasing),
            wipeOutSpec = tween(durationMillis = wipeOutDurationMillis, easing = wipeOutEasing),
            seamOverlapPx = seamOverlapPx,
        )
    }

    /**
     * Spring-based motion builder for users who want a physics feel.
     */
    fun spring(
        direction: WipeDirection = WipeDirection.TopLeftToBottomRight,
        wipeInStiffness: Float = Spring.StiffnessMediumLow,
        wipeOutStiffness: Float = Spring.StiffnessLow,
        wipeInDampingRatio: Float = Spring.DampingRatioNoBouncy,
        wipeOutDampingRatio: Float = Spring.DampingRatioNoBouncy,
        seamOverlapPx: Float = SeamOverlapPx,
    ): DiagonalWipeMotion = DiagonalWipeMotion(
        direction = direction,
        wipeInSpec = spring(
            stiffness = wipeInStiffness,
            dampingRatio = wipeInDampingRatio,
        ),
        wipeOutSpec = spring(
            stiffness = wipeOutStiffness,
            dampingRatio = wipeOutDampingRatio,
        ),
        seamOverlapPx = seamOverlapPx,
    )

    /**
     * Preset: fast and snappy interactions.
     */
    fun snappy(
        direction: WipeDirection = WipeDirection.TopLeftToBottomRight,
        seamOverlapPx: Float = SeamOverlapPx,
    ): DiagonalWipeMotion = tween(
        direction = direction,
        wipeInDurationMillis = 220,
        wipeOutDurationMillis = 300,
        wipeInEasing = FastOutSlowInEasing,
        wipeOutEasing = LinearOutSlowInEasing,
        seamOverlapPx = seamOverlapPx,
    )

    /**
     * Preset: balanced and readable. This is the default.
     */
    fun gentle(
        direction: WipeDirection = WipeDirection.TopLeftToBottomRight,
        seamOverlapPx: Float = SeamOverlapPx,
    ): DiagonalWipeMotion = tween(
        direction = direction,
        wipeInDurationMillis = WipeInDurationMillis,
        wipeOutDurationMillis = WipeOutDurationMillis,
        wipeInEasing = WipeInEasing,
        wipeOutEasing = WipeOutEasing,
        seamOverlapPx = seamOverlapPx,
    )

    /**
     * Preset: spring-driven with more personality.
     */
    fun expressive(
        direction: WipeDirection = WipeDirection.TopLeftToBottomRight,
        seamOverlapPx: Float = SeamOverlapPx,
    ): DiagonalWipeMotion = spring(
        direction = direction,
        wipeInStiffness = Spring.StiffnessMediumLow,
        wipeOutStiffness = Spring.StiffnessLow,
        wipeInDampingRatio = Spring.DampingRatioNoBouncy,
        wipeOutDampingRatio = Spring.DampingRatioNoBouncy,
        seamOverlapPx = seamOverlapPx,
    )
}

enum class WipeDirection {
    TopLeftToBottomRight,
    BottomRightToTopLeft,
    TopRightToBottomLeft,
    BottomLeftToTopRight,
    TopToBottom,
    BottomToTop,
    LeftToRight,
    RightToLeft,
}

/**
 * Full motion configuration for [DiagonalWipeIcon].
 * Supports any finite animation spec (e.g. tween, spring, keyframes).
 */
@Immutable
data class DiagonalWipeMotion(
    val direction: WipeDirection = WipeDirection.TopLeftToBottomRight,
    val wipeInSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = DiagonalWipeIconDefaults.WipeInDurationMillis,
        easing = DiagonalWipeIconDefaults.WipeInEasing,
    ),
    val wipeOutSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = DiagonalWipeIconDefaults.WipeOutDurationMillis,
        easing = DiagonalWipeIconDefaults.WipeOutEasing,
    ),
    val seamOverlapPx: Float = DiagonalWipeIconDefaults.SeamOverlapPx,
) {
    init {
        require(seamOverlapPx >= 0f) { "seamOverlapPx must be >= 0" }
    }
}

/**
 * Two-layer icon morph using a diagonal (or axis-aligned) wipe boundary.
 *
 * @param isWiped Target state: true = shows [wipedIcon]; false = shows [baseIcon].
 * @param baseIcon Icon shown when [isWiped] is false.
 * @param wipedIcon Icon shown when [isWiped] is true.
 * @param baseTint Tint for [baseIcon].
 * @param wipedTint Tint for [wipedIcon].
 * @param contentDescription Optional semantics description.
 * @param modifier Standard modifier.
 * @param motion Full motion configuration (specs, seam, direction).
 */
@Composable
fun DiagonalWipeIcon(
    isWiped: Boolean,
    baseIcon: ImageVector,
    wipedIcon: ImageVector,
    baseTint: Color = Color.Unspecified,
    wipedTint: Color = Color.Unspecified,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    motion: DiagonalWipeMotion = DiagonalWipeIconDefaults.defaultMotion,
) {
    DiagonalWipeIcon(
        isWiped = isWiped,
        basePainter = rememberVectorPainter(baseIcon),
        wipedPainter = rememberVectorPainter(wipedIcon),
        baseTint = baseTint,
        wipedTint = wipedTint,
        contentDescription = contentDescription,
        modifier = modifier,
        motion = motion,
    )
}

/**
 * Painter variant of [DiagonalWipeIcon] for maximum flexibility.
 * Accepts any [Painter] - icons, images, custom drawings, etc.
 */
@Composable
fun DiagonalWipeIcon(
    isWiped: Boolean,
    basePainter: Painter,
    wipedPainter: Painter,
    baseTint: Color = Color.Unspecified,
    wipedTint: Color = Color.Unspecified,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    motion: DiagonalWipeMotion = DiagonalWipeIconDefaults.defaultMotion,
) {
    val transition = updateTransition(targetState = isWiped, label = "diagonalWipeIcon")

    val wipeProgress by transition.animateFloat(
        transitionSpec = {
            motionSpec(false isTransitioningTo true, motion)
        },
        label = "diagonalWipeReveal",
    ) { isWipedState ->
        if (isWipedState) 1f else 0f
    }

    DiagonalWipeIconAtProgress(
        progress = wipeProgress,
        basePainter = basePainter,
        wipedPainter = wipedPainter,
        baseTint = baseTint,
        wipedTint = wipedTint,
        contentDescription = contentDescription,
        modifier = modifier,
        motion = motion,
    )
}

@Composable
internal fun DiagonalWipeIconAtProgress(
    progress: Float,
    basePainter: Painter,
    wipedPainter: Painter,
    baseTint: Color = Color.Unspecified,
    wipedTint: Color = Color.Unspecified,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    motion: DiagonalWipeMotion = DiagonalWipeIconDefaults.defaultMotion,
) {
    val resolvedBaseTint = resolveWipeTint(baseTint)
    val resolvedWipedTint = resolveWipeTint(wipedTint)
    val baseColorFilter = remember(resolvedBaseTint) { ColorFilter.tint(resolvedBaseTint) }
    val wipedColorFilter = remember(resolvedWipedTint) { ColorFilter.tint(resolvedWipedTint) }
    val revealPath = remember { Path() }
    val wipePathScratch = remember { WipePathScratch() }
    val clampedProgress = progress.coerceIn(0f, 1f)

    if (clampedProgress <= 0.001f) {
        Image(
            painter = basePainter,
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = baseColorFilter,
        )
        return
    }

    if (clampedProgress >= 0.999f) {
        Image(
            painter = wipedPainter,
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = wipedColorFilter,
        )
        return
    }

    val semanticModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Box(
        modifier = semanticModifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val travelDistance = wipeTravelDistance(
                width = size.width,
                height = size.height,
                direction = motion.direction,
            )
            val adjustedProgress = (
                    (clampedProgress * travelDistance + motion.seamOverlapPx) / travelDistance
                    ).coerceIn(0f, 1f)

            buildWipeRevealPath(
                path = revealPath,
                width = size.width,
                height = size.height,
                progress = adjustedProgress,
                direction = motion.direction,
                scratch = wipePathScratch,
            )

            clipPath(path = revealPath, clipOp = ClipOp.Difference) {
                val basePlacement = fitPainterInBounds(
                    painter = basePainter,
                    boundsSize = size,
                )
                with(basePainter) {
                    translate(
                        left = basePlacement.topLeft.x,
                        top = basePlacement.topLeft.y,
                    ) {
                        draw(size = basePlacement.drawSize, colorFilter = baseColorFilter)
                    }
                }
            }
            clipPath(path = revealPath, clipOp = ClipOp.Intersect) {
                val wipedPlacement = fitPainterInBounds(
                    painter = wipedPainter,
                    boundsSize = size,
                )
                with(wipedPainter) {
                    translate(
                        left = wipedPlacement.topLeft.x,
                        top = wipedPlacement.topLeft.y,
                    ) {
                        draw(size = wipedPlacement.drawSize, colorFilter = wipedColorFilter)
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveWipeTint(tint: Color): Color {
    return if (tint.isSpecified) tint else LocalContentColor.current
}

internal fun motionSpec(
    isWipingIn: Boolean,
    motion: DiagonalWipeMotion,
): FiniteAnimationSpec<Float> {
    return if (isWipingIn) motion.wipeInSpec else motion.wipeOutSpec
}

internal fun buildWipeBoundaryLine(
    width: Float,
    height: Float,
    progress: Float,
    direction: WipeDirection,
): Pair<Offset, Offset>? {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f || p >= 1f) return null

    val axis = wipeAxis(direction)
    val threshold = wipeBoundaryThreshold(width, height, p, axis)
    val points = arrayOfNulls<Offset>(4)
    var pointCount = 0
    val eps = 0.0001f

    fun addIfInBounds(point: Offset) {
        val inBounds = point.x >= -eps && point.x <= width + eps &&
                point.y >= -eps && point.y <= height + eps
        if (!inBounds) return
        for (index in 0 until pointCount) {
            val candidate = points[index] ?: continue
            if ((candidate - point).getDistance() < 0.01f) return
        }
        points[pointCount] = point
        pointCount += 1
    }

    if (kotlin.math.abs(axis.y) > eps) {
        addIfInBounds(Offset(0f, threshold / axis.y))
        addIfInBounds(Offset(width, (threshold - axis.x * width) / axis.y))
    }
    if (kotlin.math.abs(axis.x) > eps) {
        addIfInBounds(Offset(threshold / axis.x, 0f))
        addIfInBounds(Offset((threshold - axis.y * height) / axis.x, height))
    }

    if (pointCount < 2) return null
    if (pointCount == 2) return points[0]!! to points[1]!!

    var bestStart = points[0]!!
    var bestEnd = points[1]!!
    var bestDistance = -1f
    for (i in 0 until pointCount) {
        val first = points[i] ?: continue
        for (j in i + 1 until pointCount) {
            val second = points[j] ?: continue
            val distance = (first - second).getDistanceSquared()
            if (distance > bestDistance) {
                bestDistance = distance
                bestStart = first
                bestEnd = second
            }
        }
    }
    return bestStart to bestEnd
}

internal fun buildWipeRevealPath(
    path: Path,
    width: Float,
    height: Float,
    progress: Float,
    direction: WipeDirection,
    scratch: WipePathScratch,
) {
    val p = progress.coerceIn(0f, 1f)
    path.reset()

    if (p <= 0f) return
    if (p >= 1f) {
        path.moveTo(0f, 0f)
        path.lineTo(width, 0f)
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        return
    }

    val axis = wipeAxis(direction)
    val threshold = wipeBoundaryThreshold(width, height, p, axis)
    val pointCount = clipRectangleWithHalfPlane(
        width = width,
        height = height,
        axis = axis,
        threshold = threshold,
        scratch = scratch,
    )
    if (pointCount == 0) return

    path.moveTo(scratch.outX[0], scratch.outY[0])
    for (i in 1 until pointCount) {
        path.lineTo(scratch.outX[i], scratch.outY[i])
    }
    path.close()
}

internal fun buildWipeRevealPath(
    width: Float,
    height: Float,
    progress: Float,
    direction: WipeDirection,
): Path {
    val path = Path()
    buildWipeRevealPath(
        path = path,
        width = width,
        height = height,
        progress = progress,
        direction = direction,
        scratch = WipePathScratch(),
    )
    return path
}

internal fun wipeTravelDistance(
    width: Float,
    height: Float,
    direction: WipeDirection,
): Float {
    val axis = wipeAxis(direction)
    val p0 = 0f
    val p1 = axis.x * width
    val p2 = axis.x * width + axis.y * height
    val p3 = axis.y * height
    val minValue = minOf(p0, p1, p2, p3)
    val maxValue = maxOf(p0, p1, p2, p3)
    return (maxValue - minValue).coerceAtLeast(1f)
}

private fun wipeAxis(direction: WipeDirection): Offset {
    return when (direction) {
        WipeDirection.TopLeftToBottomRight -> Offset(1f, 1f)
        WipeDirection.BottomRightToTopLeft -> Offset(-1f, -1f)
        WipeDirection.TopRightToBottomLeft -> Offset(-1f, 1f)
        WipeDirection.BottomLeftToTopRight -> Offset(1f, -1f)
        WipeDirection.TopToBottom -> Offset(0f, 1f)
        WipeDirection.BottomToTop -> Offset(0f, -1f)
        WipeDirection.LeftToRight -> Offset(1f, 0f)
        WipeDirection.RightToLeft -> Offset(-1f, 0f)
    }
}

private fun wipeBoundaryThreshold(
    width: Float,
    height: Float,
    progress: Float,
    axis: Offset,
): Float {
    val p0 = 0f
    val p1 = axis.x * width
    val p2 = axis.x * width + axis.y * height
    val p3 = axis.y * height
    val minValue = minOf(p0, p1, p2, p3)
    val maxValue = maxOf(p0, p1, p2, p3)
    return minValue + (maxValue - minValue) * progress.coerceIn(0f, 1f)
}

private data class PainterPlacement(
    val topLeft: Offset,
    val drawSize: Size,
)

private fun fitPainterInBounds(
    painter: Painter,
    boundsSize: Size,
): PainterPlacement {
    val intrinsic = painter.intrinsicSize
    val hasValidIntrinsicSize = intrinsic.width.isFinite() &&
            intrinsic.height.isFinite() &&
            intrinsic.width > 0f &&
            intrinsic.height > 0f
    if (!hasValidIntrinsicSize) {
        return PainterPlacement(topLeft = Offset.Zero, drawSize = boundsSize)
    }

    val scale = minOf(
        boundsSize.width / intrinsic.width,
        boundsSize.height / intrinsic.height,
    )
    val drawWidth = intrinsic.width * scale
    val drawHeight = intrinsic.height * scale
    val left = (boundsSize.width - drawWidth) * 0.5f
    val top = (boundsSize.height - drawHeight) * 0.5f

    return PainterPlacement(
        topLeft = Offset(left, top),
        drawSize = Size(drawWidth, drawHeight),
    )
}

internal class WipePathScratch {
    val inX: FloatArray = FloatArray(4)
    val inY: FloatArray = FloatArray(4)
    val outX: FloatArray = FloatArray(8)
    val outY: FloatArray = FloatArray(8)
}

private fun clipRectangleWithHalfPlane(
    width: Float,
    height: Float,
    axis: Offset,
    threshold: Float,
    scratch: WipePathScratch,
): Int {
    scratch.inX[0] = 0f
    scratch.inY[0] = 0f
    scratch.inX[1] = width
    scratch.inY[1] = 0f
    scratch.inX[2] = width
    scratch.inY[2] = height
    scratch.inX[3] = 0f
    scratch.inY[3] = height

    val eps = 0.0001f
    var outCount = 0

    fun addOutputPoint(x: Float, y: Float) {
        if (outCount > 0) {
            val dx = scratch.outX[outCount - 1] - x
            val dy = scratch.outY[outCount - 1] - y
            if (dx * dx + dy * dy < 0.0001f) return
        }
        scratch.outX[outCount] = x
        scratch.outY[outCount] = y
        outCount += 1
    }

    var prevX = scratch.inX[3]
    var prevY = scratch.inY[3]
    var prevValue = axis.x * prevX + axis.y * prevY - threshold
    var prevInside = prevValue <= eps

    for (index in 0 until 4) {
        val currentX = scratch.inX[index]
        val currentY = scratch.inY[index]
        val currentValue = axis.x * currentX + axis.y * currentY - threshold
        val currentInside = currentValue <= eps

        when {
            prevInside && currentInside -> addOutputPoint(currentX, currentY)
            prevInside && !currentInside -> {
                val denominator = prevValue - currentValue
                if (kotlin.math.abs(denominator) >= eps) {
                    val t = (prevValue / denominator).coerceIn(0f, 1f)
                    addOutputPoint(
                        x = prevX + (currentX - prevX) * t,
                        y = prevY + (currentY - prevY) * t,
                    )
                }
            }

            !prevInside && currentInside -> {
                val denominator = prevValue - currentValue
                if (kotlin.math.abs(denominator) >= eps) {
                    val t = (prevValue / denominator).coerceIn(0f, 1f)
                    addOutputPoint(
                        x = prevX + (currentX - prevX) * t,
                        y = prevY + (currentY - prevY) * t,
                    )
                }
                addOutputPoint(currentX, currentY)
            }
        }

        prevX = currentX
        prevY = currentY
        prevValue = currentValue
        prevInside = currentInside
    }

    if (outCount > 1) {
        val dx = scratch.outX[0] - scratch.outX[outCount - 1]
        val dy = scratch.outY[0] - scratch.outY[outCount - 1]
        if (dx * dx + dy * dy < 0.0001f) {
            outCount -= 1
        }
    }

    return outCount
}