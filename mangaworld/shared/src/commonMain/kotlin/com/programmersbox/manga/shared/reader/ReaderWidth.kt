package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal const val MIN_READER_WIDTH_FRACTION = 0.3f
private val DividerTouchWidth = 32.dp

/**
 * Reader types that honor the adjustable page width.
 * To support another type, add it here and apply [readerSidePadding] to its pages.
 */
internal val ReaderType.supportsWidthAdjust: Boolean
    get() = this == ReaderType.List

/** The adjustable width only applies to expanded windows (tablet landscape, unfolded foldables, desktop). */
internal val isReaderWidthAdjustable: Boolean
    @Composable get() = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded

/** Horizontal padding on each side so the pages take up [fraction] of [containerWidth]. */
internal fun readerSidePadding(containerWidth: Dp, fraction: Float): Dp =
    containerWidth * (1f - fraction.coerceIn(MIN_READER_WIDTH_FRACTION, 1f)) / 2

/**
 * Where a divider sits for [fraction]. Dividers never go closer to the screen edge than [edgeInsetPx]
 * so they stay out of the system back-gesture area and can always be grabbed.
 */
internal fun dividerPosition(
    fraction: Float,
    containerPx: Float,
    edgeInsetPx: Float,
    isLeftDivider: Boolean,
): Float {
    val side = maxOf(containerPx * (1f - fraction) / 2, edgeInsetPx)
    return if (isLeftDivider) side else containerPx - side
}

/**
 * The page width for a divider dragged to [positionPx]. The dividers mirror each other around the center,
 * and dragging a divider into the edge inset means full width.
 */
internal fun fractionForDividerPosition(
    positionPx: Float,
    containerPx: Float,
    edgeInsetPx: Float,
): Float {
    if (containerPx <= 0f) return 1f
    val distanceFromCenter = (positionPx - containerPx / 2).absoluteValue
    val side = containerPx / 2 - distanceFromCenter
    if (side <= edgeInsetPx) return 1f
    return (distanceFromCenter * 2 / containerPx).coerceIn(MIN_READER_WIDTH_FRACTION, 1f)
}

@Composable
internal fun ReaderWidthDividers(
    visible: Boolean,
    widthFraction: () -> Float,
    onWidthFractionChange: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var containerPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionPx by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val gestureInsets = WindowInsets.systemGestures
    val edgeInsetPx = with(density) {
        maxOf(
            gestureInsets.getLeft(this, layoutDirection),
            gestureInsets.getRight(this, layoutDirection)
        ) + DividerTouchWidth.toPx() / 2
    }

    AnimatedVisibility(
        visible = visible || isDragging,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerPx = it.width.toFloat() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            listOf(true, false).forEach { isLeft ->
                val draggableState = rememberDraggableState { delta ->
                    dragPositionPx += delta
                    onWidthFractionChange(fractionForDividerPosition(dragPositionPx, containerPx, edgeInsetPx))
                }
                WidthDivider(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            val x = dividerPosition(widthFraction(), containerPx, edgeInsetPx, isLeft)
                            IntOffset(x.roundToInt() - DividerTouchWidth.roundToPx() / 2, 0)
                        }
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                dragPositionPx = dividerPosition(widthFraction(), containerPx, edgeInsetPx, isLeft)
                                isDragging = true
                            },
                            onDragStopped = {
                                isDragging = false
                                onDragStopped(widthFraction())
                            }
                        )
                )
            }

            AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        "${(widthFraction() * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidthDivider(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .width(DividerTouchWidth)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 14.dp, height = 56.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .size(width = 2.dp, height = 22.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
            )
        }
    }
}
