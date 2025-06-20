package com.programmersbox.manga.shared.reader.curl

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Creates a PageCurlConfig with the default properties and memorizes it.
 *
 * @param backPageColor Color of the back-page. In majority of use-cases it should be set to the content background
 * color.
 * @param backPageContentAlpha The alpha which defines how content is "seen through" the back-page. From 0 (nothing
 * is visible) to 1 (everything is visible).
 * @param shadowColor The color of the shadow. In majority of use-cases it should be set to the inverted color to the
 * content background color. Should be a solid color, see [shadowAlpha] to adjust opacity.
 * @param shadowAlpha The alpha of the [color].
 * @param shadowRadius Defines how big the shadow is.
 * @param shadowOffset Defines how shadow is shifted from the page. A little shift may add more realism.
 * @param dragForwardEnabled True if forward drag interaction is enabled or not.
 * @param dragBackwardEnabled True if backward drag interaction is enabled or not.
 * @param tapForwardEnabled True if forward tap interaction is enabled or not.
 * @param tapBackwardEnabled True if backward tap interaction is enabled or not.
 * @param tapCustomEnabled True if custom tap interaction is enabled or not, see [onCustomTap].
 * @param dragInteraction The drag interaction setting.
 * @param tapInteraction The tap interaction setting.
 * @param onCustomTap The lambda to invoke to check if tap is handled by custom tap or not. Receives the density
 * scope, the PageCurl size and tap position. Returns true if tap is handled and false otherwise.
 */
@ExperimentalPageCurlApi
@Composable
public fun rememberPageCurlConfig(
    backPageColor: Color = Color.White,
    backPageContentAlpha: Float = 0.1f,
    shadowColor: Color = Color.Black,
    shadowAlpha: Float = 0.2f,
    shadowRadius: Dp = 15.dp,
    shadowOffset: DpOffset = DpOffset((-5).dp, 0.dp),
    dragForwardEnabled: Boolean = true,
    dragBackwardEnabled: Boolean = true,
    tapForwardEnabled: Boolean = true,
    tapBackwardEnabled: Boolean = true,
    tapCustomEnabled: Boolean = true,
    dragInteraction: PageCurlConfig.DragInteraction = PageCurlConfig.StartEndDragInteraction(),
    tapInteraction: PageCurlConfig.TapInteraction = PageCurlConfig.TargetTapInteraction(),
    onCustomTap: Density.(IntSize, Offset) -> Boolean = { _, _ -> false },
): PageCurlConfig =
    rememberSaveable(
        saver = listSaver(
            save = {
                fun Rect.forSave(): List<Any> =
                    listOf(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)

                fun PageCurlConfig.DragInteraction.getRectList(): List<Rect> =
                    when (this) {
                        is PageCurlConfig.GestureDragInteraction ->
                            listOf(forward.target, backward.target)

                        is PageCurlConfig.StartEndDragInteraction ->
                            listOf(forward.start, forward.end, backward.start, backward.end)
                    }

                fun PageCurlConfig.TapInteraction.getRectList(): List<Rect> =
                    when (this) {
                        is PageCurlConfig.TargetTapInteraction ->
                            listOf(forward.target, backward.target)
                    }

                fun PageCurlConfig.DragInteraction.forSave(): List<Any> =
                    listOf(this::class, pointerBehavior.name) + getRectList().flatMap(Rect::forSave)

                fun PageCurlConfig.TapInteraction.forSave(): List<Any> =
                    listOf(this::class) + getRectList().flatMap(Rect::forSave)

                listOf(
                    (it.backPageColor.value shr 32).toInt(),
                    it.backPageContentAlpha,
                    (it.shadowColor.value shr 32).toInt(),
                    it.shadowAlpha,
                    it.shadowRadius.value,
                    it.shadowOffset.x.value,
                    it.shadowOffset.y.value,
                    it.dragForwardEnabled,
                    it.dragBackwardEnabled,
                    it.tapForwardEnabled,
                    it.tapBackwardEnabled,
                    it.tapCustomEnabled,
                    *it.dragInteraction.forSave().toTypedArray(),
                    *it.tapInteraction.forSave().toTypedArray(),
                )
            },
            restore = {
                val iterator = it.iterator()
                fun Iterator<Any>.nextRect(): Rect =
                    Rect(next() as Float, next() as Float, next() as Float, next() as Float)

                PageCurlConfig(
                    backPageColor = Color(iterator.next() as Int),
                    backPageContentAlpha = iterator.next() as Float,
                    shadowColor = Color(iterator.next() as Int),
                    shadowAlpha = iterator.next() as Float,
                    shadowRadius = Dp(iterator.next() as Float),
                    shadowOffset = DpOffset(Dp(iterator.next() as Float), Dp(iterator.next() as Float)),
                    dragForwardEnabled = iterator.next() as Boolean,
                    dragBackwardEnabled = iterator.next() as Boolean,
                    tapForwardEnabled = iterator.next() as Boolean,
                    tapBackwardEnabled = iterator.next() as Boolean,
                    tapCustomEnabled = iterator.next() as Boolean,
                    dragInteraction = when (iterator.next()) {
                        is PageCurlConfig.GestureDragInteraction -> {
                            PageCurlConfig.GestureDragInteraction(
                                PageCurlConfig.DragInteraction.PointerBehavior.valueOf(iterator.next() as String),
                                PageCurlConfig.GestureDragInteraction.Config(iterator.nextRect()),
                                PageCurlConfig.GestureDragInteraction.Config(iterator.nextRect()),
                            )
                        }

                        is PageCurlConfig.StartEndDragInteraction -> {
                            PageCurlConfig.StartEndDragInteraction(
                                PageCurlConfig.DragInteraction.PointerBehavior.valueOf(iterator.next() as String),
                                PageCurlConfig.StartEndDragInteraction.Config(iterator.nextRect(), iterator.nextRect()),
                                PageCurlConfig.StartEndDragInteraction.Config(iterator.nextRect(), iterator.nextRect()),
                            )
                        }

                        else -> error("Unable to restore PageCurlConfig")
                    },
                    tapInteraction = when (iterator.next()) {
                        is PageCurlConfig.TargetTapInteraction -> {
                            PageCurlConfig.TargetTapInteraction(
                                PageCurlConfig.TargetTapInteraction.Config(iterator.nextRect()),
                                PageCurlConfig.TargetTapInteraction.Config(iterator.nextRect()),
                            )
                        }

                        else -> error("Unable to restore PageCurlConfig")
                    },
                    onCustomTap = onCustomTap
                )
            }
        )
    ) {
        PageCurlConfig(
            backPageColor = backPageColor,
            backPageContentAlpha = backPageContentAlpha,
            shadowColor = shadowColor,
            shadowAlpha = shadowAlpha,
            shadowRadius = shadowRadius,
            shadowOffset = shadowOffset,
            dragForwardEnabled = dragForwardEnabled,
            dragBackwardEnabled = dragBackwardEnabled,
            tapForwardEnabled = tapForwardEnabled,
            tapBackwardEnabled = tapBackwardEnabled,
            tapCustomEnabled = tapCustomEnabled,
            dragInteraction = dragInteraction,
            tapInteraction = tapInteraction,
            onCustomTap = onCustomTap
        )
    }

/**
 * The configuration for PageCurl.
 *
 * @param backPageColor Color of the back-page. In majority of use-cases it should be set to the content background
 * color.
 * @param backPageContentAlpha The alpha which defines how content is "seen through" the back-page. From 0 (nothing
 * is visible) to 1 (everything is visible).
 * @param shadowColor The color of the shadow. In majority of use-cases it should be set to the inverted color to the
 * content background color. Should be a solid color, see [shadowAlpha] to adjust opacity.
 * @param shadowAlpha The alpha of the [shadowColor].
 * @param shadowRadius Defines how big the shadow is.
 * @param shadowOffset Defines how shadow is shifted from the page. A little shift may add more realism.
 * @param dragForwardEnabled True if forward drag interaction is enabled or not.
 * @param dragBackwardEnabled True if backward drag interaction is enabled or not.
 * @param tapForwardEnabled True if forward tap interaction is enabled or not.
 * @param tapBackwardEnabled True if backward tap interaction is enabled or not.
 * @param tapCustomEnabled True if custom tap interaction is enabled or not, see [onCustomTap].
 * @param dragInteraction The drag interaction setting.
 * @param tapInteraction The tap interaction setting.
 * @param onCustomTap The lambda to invoke to check if tap is handled by custom tap or not. Receives the density
 * scope, the PageCurl size and tap position. Returns true if tap is handled and false otherwise.
 */
@ExperimentalPageCurlApi
public class PageCurlConfig(
    backPageColor: Color,
    backPageContentAlpha: Float,
    shadowColor: Color,
    shadowAlpha: Float,
    shadowRadius: Dp,
    shadowOffset: DpOffset,
    dragForwardEnabled: Boolean,
    dragBackwardEnabled: Boolean,
    tapForwardEnabled: Boolean,
    tapBackwardEnabled: Boolean,
    tapCustomEnabled: Boolean,
    dragInteraction: DragInteraction,
    tapInteraction: TapInteraction,
    public val onCustomTap: Density.(IntSize, Offset) -> Boolean,
) {
    /**
     * The color of the back-page. In majority of use-cases it should be set to the content background color.
     */
    public var backPageColor: Color by mutableStateOf(backPageColor)

    /**
     * The alpha which defines how content is "seen through" the back-page. From 0 (nothing is visible) to
     * 1 (everything is visible).
     */
    public var backPageContentAlpha: Float by mutableStateOf(backPageContentAlpha)

    /**
     * The color of the shadow. In majority of use-cases it should be set to the inverted color to the content
     * background color. Should be a solid color, see [shadowAlpha] to adjust opacity.
     */
    public var shadowColor: Color by mutableStateOf(shadowColor)

    /**
     * The alpha of the [shadowColor].
     */
    public var shadowAlpha: Float by mutableStateOf(shadowAlpha)

    /**
     * Defines how big the shadow is.
     */
    public var shadowRadius: Dp by mutableStateOf(shadowRadius)

    /**
     * Defines how shadow is shifted from the page. A little shift may add more realism.
     */
    public var shadowOffset: DpOffset by mutableStateOf(shadowOffset)

    /**
     * True if forward drag interaction is enabled or not.
     */
    public var dragForwardEnabled: Boolean by mutableStateOf(dragForwardEnabled)

    /**
     * True if backward drag interaction is enabled or not.
     */
    public var dragBackwardEnabled: Boolean by mutableStateOf(dragBackwardEnabled)

    /**
     * True if forward tap interaction is enabled or not.
     */
    public var tapForwardEnabled: Boolean by mutableStateOf(tapForwardEnabled)

    /**
     * True if backward tap interaction is enabled or not.
     */
    public var tapBackwardEnabled: Boolean by mutableStateOf(tapBackwardEnabled)

    /**
     * True if custom tap interaction is enabled or not, see [onCustomTap].
     */
    public var tapCustomEnabled: Boolean by mutableStateOf(tapCustomEnabled)

    /**
     * The drag interaction setting.
     */
    public var dragInteraction: DragInteraction by mutableStateOf(dragInteraction)

    /**
     * The tap interaction setting.
     */
    public var tapInteraction: TapInteraction by mutableStateOf(tapInteraction)

    /**
     * The drag interaction setting.
     */
    public sealed interface DragInteraction {

        /**
         * The pointer behavior during drag interaction.
         */
        public val pointerBehavior: PointerBehavior

        /**
         * The enumeration of available pointer behaviors.
         */
        public enum class PointerBehavior {
            /**
             * The default behavior is an original one, where "page flip" is anchored to the user's finger.
             * The "page flip" in this sense is a line which divides the back page of the current page and the front
             * page of the next page. This means that when finger is dragged to the left edge, the next page is fully
             * visible.
             */
            Default,

            /**
             * In the page-edge behavior the right edge of the current page is anchored to the user's finger.
             * This means that when finger is dragged to the left edge, the next page is half visible.
             */
            PageEdge;
        }
    }

    /**
     * The drag interaction setting based on where user start and end drag gesture inside the PageCurl.
     *
     * @property pointerBehavior The pointer behavior during drag interaction.
     * @property forward The forward tap configuration.
     * @property backward The backward tap configuration.
     */
    public data class StartEndDragInteraction(
        override val pointerBehavior: DragInteraction.PointerBehavior = DragInteraction.PointerBehavior.Default,
        val forward: Config = Config(start = rightHalf(), end = leftHalf()),
        val backward: Config = Config(start = leftHalf(), end = rightHalf()),
    ) : DragInteraction {

        /**
         * The drag interaction configuration for forward or backward drag.
         *
         * @property start Defines a rectangle where interaction should start. The rectangle coordinates are relative
         * (from 0 to 1) and then scaled to the PageCurl bounds.
         * @property end Defines a rectangle where interaction should end. The rectangle coordinates are relative
         * (from 0 to 1) and then scaled to the PageCurl bounds.
         */
        public data class Config(val start: Rect, val end: Rect)
    }

    /**
     * The drag interaction setting based on the direction where drag has been started.
     *
     * @property pointerBehavior The pointer behavior during drag interaction.
     * @property forward The forward tap configuration.
     * @property backward The backward tap configuration.
     */
    public data class GestureDragInteraction(
        override val pointerBehavior: DragInteraction.PointerBehavior = DragInteraction.PointerBehavior.Default,
        val forward: Config = Config(target = full()),
        val backward: Config = Config(target = full()),
    ) : DragInteraction {

        /**
         * The drag interaction configuration for forward or backward drag.
         *
         * @property target Defines a rectangle where interaction captured. The rectangle coordinates are relative
         * (from 0 to 1) and then scaled to the PageCurl bounds.
         */
        public data class Config(val target: Rect)
    }

    /**
     * The tap interaction setting.
     */
    public sealed interface TapInteraction

    /**
     * The tap interaction setting based on where user taps inside the PageCurl.
     *
     * @property forward The forward tap configuration.
     * @property backward The backward tap configuration.
     */
    public data class TargetTapInteraction(
        val forward: Config = Config(target = rightHalf()),
        val backward: Config = Config(target = leftHalf()),
    ) : TapInteraction {

        /**
         * The tap interaction configuration for forward or backward tap.
         *
         * @property target Defines a rectangle where interaction captured. The rectangle coordinates are relative
         * (from 0 to 1) and then scaled to the PageCurl bounds.
         */
        public data class Config(val target: Rect)
    }
}

/**
 * The full size of the PageCurl.
 */
private fun full(): Rect = Rect(0.0f, 0.0f, 1.0f, 1.0f)

/**
 * The left half of the PageCurl.
 */
private fun leftHalf(): Rect = Rect(0.0f, 0.0f, 0.5f, 1.0f)

/**
 * The right half of the PageCurl.
 */
private fun rightHalf(): Rect = Rect(0.5f, 0.0f, 1.0f, 1.0f)

@ExperimentalPageCurlApi
internal fun Modifier.drawCurl(
    config: PageCurlConfig,
    posA: Offset,
    posB: Offset,
): Modifier = drawWithCache {
    // Fast-check if curl is in left most position (gesture is fully completed)
    // In such case do not bother and draw nothing
    if (posA == size.toRect().topLeft && posB == size.toRect().bottomLeft) {
        return@drawWithCache drawNothing()
    }

    // Fast-check if curl is in right most position (gesture is not yet started)
    // In such case do not bother and draw the full content
    if (posA == size.toRect().topRight && posB == size.toRect().bottomRight) {
        return@drawWithCache drawOnlyContent()
    }

    // Find the intersection of the curl line ([posA, posB]) and top and bottom sides, so that we may clip and mirror
    // content correctly
    val topIntersection = lineLineIntersection(
        Offset(0f, 0f), Offset(size.width, 0f),
        posA, posB
    )
    val bottomIntersection = lineLineIntersection(
        Offset(0f, size.height), Offset(size.width, size.height),
        posA, posB
    )

    // Should not really happen, but in case there is not intersection (curl line is horizontal), just draw the full
    // content instead
    if (topIntersection == null || bottomIntersection == null) {
        return@drawWithCache drawOnlyContent()
    }

    // Limit x coordinates of both intersections to be at least 0, so that page do not look like teared from the book
    val topCurlOffset = Offset(max(0f, topIntersection.x), topIntersection.y)
    val bottomCurlOffset = Offset(max(0f, bottomIntersection.x), bottomIntersection.y)

    // That is the easy part, prepare a lambda to draw the content clipped by the curl line
    val drawClippedContent = prepareClippedContent(topCurlOffset, bottomCurlOffset)
    // That is the tricky part, prepare a lambda to draw the back-page with the shadow
    val drawCurl = prepareCurl(config, topCurlOffset, bottomCurlOffset)

    onDrawWithContent {
        drawClippedContent()
        drawCurl()
    }
}

/**
 * The simple method to draw the whole unmodified content.
 */
private fun CacheDrawScope.drawOnlyContent(): DrawResult =
    onDrawWithContent {
        drawContent()
    }

/**
 * The simple method to draw nothing.
 */
private fun CacheDrawScope.drawNothing(): DrawResult =
    onDrawWithContent {
        /* Empty */
    }

@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareClippedContent(
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
): ContentDrawScope.() -> Unit {
    // Make a quadrilateral from the left side to the intersection points
    val path = Path()
    path.lineTo(topCurlOffset.x, topCurlOffset.y)
    path.lineTo(bottomCurlOffset.x, bottomCurlOffset.y)
    path.lineTo(0f, size.height)
    return result@{
        // Draw a content clipped by the constructed path
        clipPath(path) {
            this@result.drawContent()
        }
    }
}

@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareCurl(
    config: PageCurlConfig,
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
): ContentDrawScope.() -> Unit {
    // Build a quadrilateral of the part of the page which should be mirrored as the back-page
    // In all cases polygon should have 4 points, even when back-page is only a small "corner" (with 3 points) due to
    // the shadow rendering, otherwise it will create a visual artifact when switching between 3 and 4 points polygon
    val polygon = Polygon(
        sequence {
            // Find the intersection of the curl line and right side
            // If intersection is found adds to the polygon points list
            suspend fun SequenceScope<Offset>.yieldEndSideInterception() {
                val offset = lineLineIntersection(
                    topCurlOffset, bottomCurlOffset,
                    Offset(size.width, 0f), Offset(size.width, size.height)
                ) ?: return
                yield(offset)
                yield(offset)
            }

            // In case top intersection lays in the bounds of the page curl, take 2 points from the top side, otherwise
            // take the interception with a right side
            if (topCurlOffset.x < size.width) {
                yield(topCurlOffset)
                yield(Offset(size.width, topCurlOffset.y))
            } else {
                yieldEndSideInterception()
            }

            // In case bottom intersection lays in the bounds of the page curl, take 2 points from the bottom side,
            // otherwise take the interception with a right side
            if (bottomCurlOffset.x < size.width) {
                yield(Offset(size.width, size.height))
                yield(bottomCurlOffset)
            } else {
                yieldEndSideInterception()
            }
        }.toList()
    )

    // Calculate the angle in radians between X axis and the curl line, this is used to rotate mirrored content to the
    // right position of the curled back-page
    val lineVector = topCurlOffset - bottomCurlOffset
    val angle = PI.toFloat() - atan2(lineVector.y, lineVector.x) * 2

    // Prepare a lambda to draw the shadow of the back-page
    val drawShadow = prepareShadow(config, polygon, angle)

    return result@{
        withTransform({
            // Mirror in X axis the drawing as back-page should be mirrored
            scale(-1f, 1f, pivot = bottomCurlOffset)
            // Rotate the drawing according to the curl line
            rotateRad(angle, pivot = bottomCurlOffset)
        }) {
            // Draw shadow first
            this@result.drawShadow()

            // And finally draw the back-page with an overlay with alpha
            clipPath(polygon.toPath()) {
                this@result.drawContent()

                val overlayAlpha = 1f - config.backPageContentAlpha
                drawRect(config.backPageColor.copy(alpha = overlayAlpha))
            }
        }
    }
}

@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareShadow(
    config: PageCurlConfig,
    polygon: Polygon,
    angle: Float,
): ContentDrawScope.() -> Unit {
    // Quick exit if no shadow is requested
    if (config.shadowAlpha == 0f || config.shadowRadius == 0.dp) {
        return { /* No shadow is requested */ }
    }

    // Prepare shadow parameters
    val radius = config.shadowRadius.toPx()
    val shadowColor = config.shadowColor.copy(alpha = config.shadowAlpha).toArgb()
    val transparent = config.shadowColor.copy(alpha = 0f)
    val shadowOffset = Offset(-config.shadowOffset.x.toPx(), config.shadowOffset.y.toPx())
        .rotate(2 * PI.toFloat() - angle)

    // Prepare shadow paint with a shadow layer
    val paint = Paint().apply {
        color = transparent
        //TODO: Fix this
        /*val frameworkPaint = asFrameworkPaint()
        frameworkPaint.color = transparent
        frameworkPaint.setShadowLayer(
            config.shadowRadius.toPx(),
            shadowOffset.x,
            shadowOffset.y,
            shadowColor
        )*/
    }

    // Hardware acceleration supports setShadowLayer() only on API 28 and above, thus to support previous API versions
    // draw a shadow to the bitmap instead
    /*return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        prepareShadowApi28(radius, paint, polygon)
    } else {
        prepareShadowImage(radius, paint, polygon)
    }*/
    return prepareShadowApi28(radius, paint, polygon)
}

private fun prepareShadowApi28(
    radius: Float,
    paint: Paint,
    polygon: Polygon,
): ContentDrawScope.() -> Unit = {
    drawIntoCanvas {
        it.drawPath(
            polygon
                .offset(radius)
                .toPath(),
            paint
        )
    }
}

//TODO: Fix this
/*private fun CacheDrawScope.prepareShadowImage(
    radius: Float,
    paint: Paint,
    polygon: Polygon,
): ContentDrawScope.() -> Unit {
    // Increase the size a little bit so that shadow is not clipped
    val bitmap = Bitmap.createBitmap(
        (size.width + radius * 4).toInt(),
        (size.height + radius * 4).toInt(),
        Bitmap.Config.ARGB_8888
    )
    Canvas(bitmap).apply {
        drawPath(
            polygon
                // As bitmap size is increased we should translate the polygon so that shadow remains in center
                .translate(Offset(2 * radius, 2 * radius))
                .offset(radius).toPath()
                .asAndroidPath(),
            paint.asFrameworkPaint()
        )
    }

    return {
        drawIntoCanvas {
            // As bitmap size is increased we should shift the drawing so that shadow remains in center
            it.nativeCanvas.drawBitmap(bitmap, -2 * radius, -2 * radius, null)
        }
    }
}*/

internal data class DragConfig(
    val edge: Animatable<Edge, AnimationVector4D>,
    val start: Edge,
    val end: Edge,
    val isEnabled: () -> Boolean,
    val isDragSucceed: (Offset, Offset) -> Boolean,
    val onChange: () -> Unit,
)

internal suspend fun PointerInputScope.detectCurlGestures(
    scope: CoroutineScope,
    newEdgeCreator: NewEdgeCreator,
    getConfig: (Offset, Offset) -> DragConfig?,
) {
    // Use velocity tracker to support flings
    val velocityTracker = VelocityTracker()

    var config: DragConfig? = null
    var startOffset: Offset = Offset.Zero

    detectCustomDragGestures(
        onDragStart = { start, end ->
            startOffset = start
            config = getConfig(start, end)
            config != null
        },
        onDragEnd = { endOffset, complete ->
            config?.apply {
                val velocity = velocityTracker.calculateVelocity()
                val decay = splineBasedDecay<Offset>(this@detectCurlGestures)
                val flingEndOffset = decay.calculateTargetValue(
                    Offset.VectorConverter,
                    endOffset,
                    Offset(velocity.x, velocity.y)
                ).let {
                    Offset(
                        it.x.coerceIn(0f, size.width.toFloat() - 1),
                        it.y.coerceIn(0f, size.height.toFloat() - 1)
                    )
                }

                scope.launch {
                    if (complete && isDragSucceed(startOffset, flingEndOffset)) {
                        try {
                            edge.animateTo(end)
                        } finally {
                            onChange()
                            edge.snapTo(start)
                        }
                    } else {
                        try {
                            edge.animateTo(start)
                        } finally {
                            edge.snapTo(start)
                        }
                    }
                }
            }
        },
        onDrag = { change, _ ->
            config?.apply {
                if (!isEnabled()) {
                    throw CancellationException("")
                }

                velocityTracker.addPosition(Clock.System.now().toEpochMilliseconds(), change.position)

                scope.launch {
                    val target = newEdgeCreator.createNew(size, startOffset, change.position)
                    edge.animateTo(target)
                }
            }
        }
    )
}

internal suspend fun PointerInputScope.detectCustomDragGestures(
    onDragStart: (Offset, Offset) -> Boolean,
    onDragEnd: (Offset, Boolean) -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        var overSlop = Offset.Zero
        do {
            drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                overSlop = over
            }
        } while (drag != null && !drag.isConsumed)
        if (drag != null) {
            if (!onDragStart.invoke(down.position, drag.position)) {
                return@awaitEachGesture
            }
            onDrag(drag, overSlop)
            val completed = drag(drag.id) {
                drag = it
                onDrag(it, it.positionChange())
                it.consume()
            }
            onDragEnd(drag?.position ?: down.position, completed)
        }
    }
}

internal abstract class NewEdgeCreator {

    abstract fun createNew(size: IntSize, startOffset: Offset, currentOffset: Offset): Edge

    protected fun createVectors(size: IntSize, startOffset: Offset, currentOffset: Offset): Pair<Offset, Offset> {
        val vector = Offset(size.width.toFloat(), startOffset.y) - currentOffset
        val rotatedVector = vector.rotate(PI.toFloat() / 2)
        return vector to rotatedVector
    }

    class Default : NewEdgeCreator() {
        override fun createNew(size: IntSize, startOffset: Offset, currentOffset: Offset): Edge {
            val vectors = createVectors(size, startOffset, currentOffset)
            return Edge(currentOffset - vectors.second, currentOffset + vectors.second)
        }
    }

    class PageEdge : NewEdgeCreator() {
        override fun createNew(size: IntSize, startOffset: Offset, currentOffset: Offset): Edge {
            val (vector, rotatedVector) = createVectors(size, startOffset, currentOffset)
            return Edge(currentOffset - rotatedVector + vector / 2f, currentOffset + rotatedVector + vector / 2f)
        }
    }
}

@ExperimentalPageCurlApi
internal fun Modifier.dragGesture(
    dragInteraction: PageCurlConfig.GestureDragInteraction,
    state: PageCurlState.InternalState,
    enabledForward: Boolean,
    enabledBackward: Boolean,
    scope: CoroutineScope,
    onChange: (Int) -> Unit,
): Modifier = this.composed {
    val isEnabledForward = rememberUpdatedState(enabledForward)
    val isEnabledBackward = rememberUpdatedState(enabledBackward)

    pointerInput(state) {
        val forwardTargetRect by lazy { dragInteraction.forward.target.multiply(size) }
        val backwardTargetRect by lazy { dragInteraction.backward.target.multiply(size) }

        val forwardConfig = DragConfig(
            edge = state.forward,
            start = state.rightEdge,
            end = state.leftEdge,
            isEnabled = { isEnabledForward.value },
            isDragSucceed = { start, end -> end.x < start.x },
            onChange = { onChange(+1) }
        )
        val backwardConfig = DragConfig(
            edge = state.backward,
            start = state.leftEdge,
            end = state.rightEdge,
            isEnabled = { isEnabledBackward.value },
            isDragSucceed = { start, end -> end.x > start.x },
            onChange = { onChange(-1) }
        )

        detectCurlGestures(
            scope = scope,
            newEdgeCreator = when (dragInteraction.pointerBehavior) {
                PageCurlConfig.DragInteraction.PointerBehavior.Default -> NewEdgeCreator.Default()
                PageCurlConfig.DragInteraction.PointerBehavior.PageEdge -> NewEdgeCreator.PageEdge()
            },
            getConfig = { start, end ->
                val config = if (forwardTargetRect.contains(start) && end.x < start.x) {
                    forwardConfig
                } else if (backwardTargetRect.contains(start) && end.x > start.x) {
                    backwardConfig
                } else {
                    null
                }

                if (config != null) {
                    scope.launch {
                        state.animateJob?.cancel()
                        state.reset()
                    }
                }

                config
            },
        )
    }
}

@ExperimentalPageCurlApi
internal fun Modifier.dragStartEnd(
    dragInteraction: PageCurlConfig.StartEndDragInteraction,
    state: PageCurlState.InternalState,
    enabledForward: Boolean,
    enabledBackward: Boolean,
    scope: CoroutineScope,
    onChange: (Int) -> Unit,
): Modifier = this.composed {
    val isEnabledForward = rememberUpdatedState(enabledForward)
    val isEnabledBackward = rememberUpdatedState(enabledBackward)

    pointerInput(state) {
        val forwardStartRect by lazy { dragInteraction.forward.start.multiply(size) }
        val forwardEndRect by lazy { dragInteraction.forward.end.multiply(size) }
        val backwardStartRect by lazy { dragInteraction.backward.start.multiply(size) }
        val backwardEndRect by lazy { dragInteraction.backward.end.multiply(size) }

        val forwardConfig = DragConfig(
            edge = state.forward,
            start = state.rightEdge,
            end = state.leftEdge,
            isEnabled = { isEnabledForward.value },
            isDragSucceed = { _, end -> forwardEndRect.contains(end) },
            onChange = { onChange(+1) }
        )
        val backwardConfig = DragConfig(
            edge = state.backward,
            start = state.leftEdge,
            end = state.rightEdge,
            isEnabled = { isEnabledBackward.value },
            isDragSucceed = { _, end -> backwardEndRect.contains(end) },
            onChange = { onChange(-1) }
        )

        detectCurlGestures(
            scope = scope,
            newEdgeCreator = when (dragInteraction.pointerBehavior) {
                PageCurlConfig.DragInteraction.PointerBehavior.Default -> NewEdgeCreator.Default()
                PageCurlConfig.DragInteraction.PointerBehavior.PageEdge -> NewEdgeCreator.PageEdge()
            },
            getConfig = { start, _ ->
                val config = if (forwardStartRect.contains(start)) {
                    forwardConfig
                } else if (backwardStartRect.contains(start)) {
                    backwardConfig
                } else {
                    null
                }

                if (config != null) {
                    scope.launch {
                        state.animateJob?.cancel()
                        state.reset()
                    }
                }

                config
            },
        )
    }
}

/**
 * Shows the pages which may be turned by drag or tap gestures.
 *
 * @param count The count of pages.
 * @param modifier The modifier for this composable.
 * @param state The state of the PageCurl. Use this to programmatically change the current page or observe changes.
 * @param config The configuration for PageCurl.
 * @param content The content lambda to provide the page composable. Receives the page number.
 */
@ExperimentalPageCurlApi
@Composable
public fun PageCurl(
    count: Int,
    modifier: Modifier = Modifier,
    state: PageCurlState = rememberPageCurlState(),
    config: PageCurlConfig = rememberPageCurlConfig(),
    content: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier) {
        state.setup(count, constraints)

        val updatedCurrent by rememberUpdatedState(state.current)
        val internalState by rememberUpdatedState(state.internalState ?: return@BoxWithConstraints)

        val updatedConfig by rememberUpdatedState(config)

        val dragGestureModifier = when (val interaction = updatedConfig.dragInteraction) {
            is PageCurlConfig.GestureDragInteraction ->
                Modifier
                    .dragGesture(
                        dragInteraction = interaction,
                        state = internalState,
                        enabledForward = updatedConfig.dragForwardEnabled && updatedCurrent < state.max - 1,
                        enabledBackward = updatedConfig.dragBackwardEnabled && updatedCurrent > 0,
                        scope = scope,
                        onChange = { state.current = updatedCurrent + it }
                    )

            is PageCurlConfig.StartEndDragInteraction ->
                Modifier
                    .dragStartEnd(
                        dragInteraction = interaction,
                        state = internalState,
                        enabledForward = updatedConfig.dragForwardEnabled && updatedCurrent < state.max - 1,
                        enabledBackward = updatedConfig.dragBackwardEnabled && updatedCurrent > 0,
                        scope = scope,
                        onChange = { state.current = updatedCurrent + it }
                    )
        }

        Box(
            Modifier
                .then(dragGestureModifier)
                .tapGesture(
                    config = updatedConfig,
                    scope = scope,
                    onTapForward = state::next,
                    onTapBackward = state::prev,
                )
        ) {
            // Wrap in key to synchronize state updates
            key(updatedCurrent, internalState.forward.value, internalState.backward.value) {
                if (updatedCurrent + 1 < state.max) {
                    content(updatedCurrent + 1)
                }

                if (updatedCurrent < state.max) {
                    val forward = internalState.forward.value
                    Box(Modifier.drawCurl(updatedConfig, forward.top, forward.bottom)) {
                        content(updatedCurrent)
                    }
                }

                if (updatedCurrent > 0) {
                    val backward = internalState.backward.value
                    Box(Modifier.drawCurl(updatedConfig, backward.top, backward.bottom)) {
                        content(updatedCurrent - 1)
                    }
                }
            }
        }
    }
}

/**
 * Shows the pages which may be turned by drag or tap gestures.
 *
 * @param count The count of pages.
 * @param key The lambda to provide stable key for each item. Useful when adding and removing items before current page.
 * @param modifier The modifier for this composable.
 * @param state The state of the PageCurl. Use this to programmatically change the current page or observe changes.
 * @param config The configuration for PageCurl.
 * @param content The content lambda to provide the page composable. Receives the page number.
 */
@ExperimentalPageCurlApi
@Composable
public fun PageCurl(
    count: Int,
    key: (Int) -> Any,
    modifier: Modifier = Modifier,
    state: PageCurlState = rememberPageCurlState(),
    config: PageCurlConfig = rememberPageCurlConfig(),
    content: @Composable (Int) -> Unit,
) {
    var lastKey by remember(state.current) { mutableStateOf(if (count > 0) key(state.current) else null) }

    remember(count) {
        val newKey = if (count > 0) key(state.current) else null
        if (newKey != lastKey) {
            val index = List(count, key).indexOf(lastKey).coerceIn(0, count - 1)
            lastKey = newKey
            state.current = index
        }
        count
    }

    PageCurl(
        count = count,
        state = state,
        config = config,
        content = content,
        modifier = modifier,
    )
}

/**
 * Shows the pages which may be turned by drag or tap gestures.
 *
 * @param state The state of the PageCurl. Use this to programmatically change the current page or observe changes.
 * @param modifier The modifier for this composable.
 * @param content The content lambda to provide the page composable. Receives the page number.
 */
@ExperimentalPageCurlApi
@Composable
@Deprecated("Specify 'max' as 'count' in PageCurl composable.")
public fun PageCurl(
    state: PageCurlState,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    PageCurl(
        count = state.max,
        state = state,
        modifier = modifier,
        content = content,
    )
}

/**
 * Remembers the [PageCurlState].
 *
 * @param initialCurrent The initial current page.
 * @return The remembered [PageCurlState].
 */
@ExperimentalPageCurlApi
@Composable
public fun rememberPageCurlState(
    initialCurrent: Int = 0,
): PageCurlState =
    rememberSaveable(
        initialCurrent,
        saver = Saver(
            save = { it.current },
            restore = { PageCurlState(initialCurrent = it) }
        )
    ) {
        PageCurlState(
            initialCurrent = initialCurrent,
        )
    }

/**
 * Remembers the [PageCurlState].
 *
 * @param initialCurrent The initial current page.
 * @param config The configuration for PageCurl.
 * @return The remembered [PageCurlState].
 */
@ExperimentalPageCurlApi
@Composable
@Deprecated(
    message = "Specify 'config' as 'config' in PageCurl composable.",
    level = DeprecationLevel.ERROR,
)
@Suppress("UnusedPrivateMember")
public fun rememberPageCurlState(
    initialCurrent: Int = 0,
    config: PageCurlConfig,
): PageCurlState =
    rememberSaveable(
        initialCurrent,
        saver = Saver(
            save = { it.current },
            restore = { PageCurlState(initialCurrent = it) }
        )
    ) {
        PageCurlState(
            initialCurrent = initialCurrent,
        )
    }

/**
 * Remembers the [PageCurlState].
 *
 * @param max The max number of pages.
 * @param initialCurrent The initial current page.
 * @param config The configuration for PageCurl.
 * @return The remembered [PageCurlState].
 */
@ExperimentalPageCurlApi
@Composable
@Deprecated(
    message = "Specify 'max' as 'count' in PageCurl composable and 'config' as 'config' in PageCurl composable.",
    level = DeprecationLevel.ERROR,
)
@Suppress("UnusedPrivateMember")
public fun rememberPageCurlState(
    max: Int,
    initialCurrent: Int = 0,
    config: PageCurlConfig = rememberPageCurlConfig(),
): PageCurlState =
    rememberSaveable(
        max, initialCurrent,
        saver = Saver(
            save = { it.current },
            restore = {
                PageCurlState(
                    initialCurrent = it,
                    initialMax = max,
                )
            }
        )
    ) {
        PageCurlState(
            initialCurrent = initialCurrent,
            initialMax = max,
        )
    }

/**
 * The state of the PageCurl.
 *
 * @param initialMax The initial max number of pages.
 * @param initialCurrent The initial current page.
 */
@ExperimentalPageCurlApi
public class PageCurlState(
    initialMax: Int = 0,
    initialCurrent: Int = 0,
) {
    /**
     * The observable current page.
     */
    public var current: Int by mutableStateOf(initialCurrent)
        internal set

    /**
     * The observable progress as page is turned.
     * When going forward it changes from 0 to 1, when going backward it is going from 0 to -1.
     */
    public val progress: Float get() = internalState?.progress ?: 0f

    internal var max: Int = initialMax
        private set

    internal var internalState: InternalState? by mutableStateOf(null)
        private set

    internal fun setup(count: Int, constraints: Constraints) {
        max = count
        if (current >= count) {
            current = (count - 1).coerceAtLeast(0)
        }

        if (internalState?.constraints == constraints) {
            return
        }

        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        val left = Edge(Offset(0f, 0f), Offset(0f, maxHeightPx))
        val right = Edge(Offset(maxWidthPx, 0f), Offset(maxWidthPx, maxHeightPx))

        val forward = Animatable(right, Edge.VectorConverter, Edge.VisibilityThreshold)
        val backward = Animatable(left, Edge.VectorConverter, Edge.VisibilityThreshold)

        internalState = InternalState(constraints, left, right, forward, backward)
    }

    /**
     * Instantly snaps the state to the given page.
     *
     * @param value The page to snap to.
     */
    public suspend fun snapTo(value: Int) {
        current = value.coerceIn(0, max - 1)
        internalState?.reset()
    }

    /**
     * Go forward with an animation.
     *
     * @param block The animation block to animate a change.
     */
    public suspend fun next(block: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = DefaultNext) {
        internalState?.animateTo(
            target = { current + 1 },
            animate = { forward.block(it) }
        )
    }

    /**
     * Go backward with an animation.
     *
     * @param block The animation block to animate a change.
     */
    public suspend fun prev(block: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = DefaultPrev) {
        internalState?.animateTo(
            target = { current - 1 },
            animate = { backward.block(it) }
        )
    }

    internal inner class InternalState(
        val constraints: Constraints,
        val leftEdge: Edge,
        val rightEdge: Edge,
        val forward: Animatable<Edge, AnimationVector4D>,
        val backward: Animatable<Edge, AnimationVector4D>,
    ) {

        var animateJob: Job? = null

        val progress: Float by derivedStateOf {
            if (forward.value != rightEdge) {
                1f - forward.value.centerX / constraints.maxWidth
            } else if (backward.value != leftEdge) {
                -backward.value.centerX / constraints.maxWidth
            } else {
                0f
            }
        }

        suspend fun reset() {
            forward.snapTo(rightEdge)
            backward.snapTo(leftEdge)
        }

        suspend fun animateTo(
            target: () -> Int,
            animate: suspend InternalState.(Size) -> Unit,
        ) {
            animateJob?.cancel()

            val targetIndex = target()
            if (targetIndex < 0 || targetIndex >= max) {
                return
            }

            coroutineScope {
                animateJob = launch {
                    try {
                        reset()
                        animate(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()))
                    } finally {
                        withContext(NonCancellable) {
                            snapTo(target())
                        }
                    }
                }
            }
        }
    }
}

/**
 * The wrapper to represent a line with 2 points: [top] and [bottom].
 */
public data class Edge(val top: Offset, val bottom: Offset) {

    internal val centerX: Float = (top.x + bottom.x) * 0.5f

    internal companion object {
        val VectorConverter: TwoWayConverter<Edge, AnimationVector4D> =
            TwoWayConverter(
                convertToVector = { AnimationVector4D(it.top.x, it.top.y, it.bottom.x, it.bottom.y) },
                convertFromVector = { Edge(Offset(it.v1, it.v2), Offset(it.v3, it.v4)) }
            )

        val VisibilityThreshold: Edge =
            Edge(Offset.VisibilityThreshold, Offset.VisibilityThreshold)
    }
}

private val DefaultNext: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = { size ->
    animateTo(
        targetValue = size.start,
        animationSpec = keyframes {
            durationMillis = DefaultAnimDuration
            size.end at 0
            size.middle at DefaultMidPointDuration
        }
    )
}

private val DefaultPrev: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = { size ->
    animateTo(
        targetValue = size.end,
        animationSpec = keyframes {
            durationMillis = DefaultAnimDuration
            size.start at 0
            size.middle at DefaultAnimDuration - DefaultMidPointDuration
        }
    )
}

private const val DefaultAnimDuration: Int = 450
private const val DefaultMidPointDuration: Int = 150

private val Size.start: Edge
    get() = Edge(Offset(0f, 0f), Offset(0f, height))

private val Size.middle: Edge
    get() = Edge(Offset(width, height / 2f), Offset(width / 2f, height))

private val Size.end: Edge
    get() = Edge(Offset(width, height), Offset(width, height))

@ExperimentalPageCurlApi
internal fun Modifier.tapGesture(
    config: PageCurlConfig,
    scope: CoroutineScope,
    onTapForward: suspend () -> Unit,
    onTapBackward: suspend () -> Unit,
): Modifier = pointerInput(config) {
    val tapInteraction = config.tapInteraction as? PageCurlConfig.TargetTapInteraction ?: return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown().also { it.consume() }
        val up = waitForUpOrCancellation() ?: return@awaitEachGesture

        if ((down.position - up.position).getDistance() > viewConfiguration.touchSlop) {
            return@awaitEachGesture
        }

        if (config.tapCustomEnabled && config.onCustomTap(this, size, up.position)) {
            return@awaitEachGesture
        }

        if (config.tapForwardEnabled && tapInteraction.forward.target.multiply(size).contains(up.position)) {
            scope.launch {
                onTapForward()
            }
            return@awaitEachGesture
        }

        if (config.tapBackwardEnabled && tapInteraction.backward.target.multiply(size).contains(up.position)) {
            scope.launch {
                onTapBackward()
            }
            return@awaitEachGesture
        }
    }
}

internal fun Offset.rotate(angle: Float): Offset {
    val sin = sin(angle)
    val cos = cos(angle)
    return Offset(x * cos - y * sin, x * sin + y * cos)
}

internal fun lineLineIntersection(
    line1a: Offset,
    line1b: Offset,
    line2a: Offset,
    line2b: Offset,
): Offset? {
    val denominator = (line1a.x - line1b.x) * (line2a.y - line2b.y) - (line1a.y - line1b.y) * (line2a.x - line2b.x)
    if (denominator == 0f) return null

    val x1 = (line1a.x * line1b.y - line1a.y * line1b.x) * (line2a.x - line2b.x)
    val x2 = (line1a.x - line1b.x) * (line2a.x * line2b.y - line2a.y * line2b.x)
    val x = (x1 - x2) / denominator

    val y1 = (line1a.x * line1b.y - line1a.y * line1b.x) * (line2a.y - line2b.y)
    val y2 = (line1a.y - line1b.y) * (line2a.x * line2b.y - line2a.y * line2b.x)
    val y = (y1 - y2) / denominator
    return Offset(x, y)
}

internal data class Polygon(val vertices: List<Offset>) {

    private val size: Int = vertices.size

    fun translate(offset: Offset): Polygon =
        Polygon(vertices.map { it + offset })

    fun offset(value: Float): Polygon {
        val edgeNormals = List(size) {
            val edge = vertices[index(it + 1)] - vertices[index(it)]
            Offset(edge.y, -edge.x).normalized()
        }

        val vertexNormals = List(size) {
            (edgeNormals[index(it - 1)] + edgeNormals[index(it)]).normalized()
        }

        return Polygon(
            vertices.mapIndexed { index, vertex ->
                vertex + vertexNormals[index] * value
            }
        )
    }

    fun toPath(): Path =
        Path().apply {
            vertices.forEachIndexed { index, vertex ->
                if (index == 0) {
                    moveTo(vertex.x, vertex.y)
                } else {
                    lineTo(vertex.x, vertex.y)
                }
            }
        }

    private fun index(i: Int) = ((i % size) + size) % size
}

private fun Offset.normalized(): Offset {
    val distance = getDistance()
    return if (distance != 0f) this / distance else this
}

internal fun Rect.multiply(size: IntSize): Rect =
    Rect(
        topLeft = Offset(size.width * left, size.height * top),
        bottomRight = Offset(size.width * right, size.height * bottom),
    )

/**
 * Used for annotating experimental page curl API that is likely to change or be removed in the future.
 */
@RequiresOptIn(
    "This API is experimental and is likely to change or to be removed in the future."
)
public annotation class ExperimentalPageCurlApi