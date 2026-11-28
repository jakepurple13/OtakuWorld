package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Thin line at the top edge showing how far through the chapter the reader is, shown while the bars are
 * hidden. It springs to each new value and turns tertiary on the last page. When [page] is set, a chip that
 * rides the line's tip shows while [showChip] is true. [swellOnTurn] thickens the line while [showChip] is
 * true; [quarterTicks] marks 25, 50 and 75%.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReadingProgressLine(
    progress: Float,
    visible: Boolean,
    page: Int?,
    pages: Int,
    showChip: Boolean,
    modifier: Modifier = Modifier,
    swellOnTurn: Boolean = false,
    quarterTicks: Boolean = false,
) {
    val shown = animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    )
    val color by animateColorAsState(
        targetValue = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val swollen = swellOnTurn && showChip
    val height by animateDpAsState(
        targetValue = when {
            !swellOnTurn -> 3.dp
            swollen -> 5.dp
            else -> 2.dp
        },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val glow by animateDpAsState(if (swollen) 6.dp else 0.dp, MaterialTheme.motionScheme.defaultEffectsSpec())
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (quarterTicks) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .drawBehind {
                            listOf(.25f, .5f, .75f).forEach { f ->
                                drawRect(QuarterTick, Offset(size.width * f, 0f), Size(1.dp.toPx(), size.height))
                            }
                        }
                )
            }
            Box(
                Modifier
                    .fillMaxWidth(shown.value.coerceIn(0f, 1f))
                    .height(height)
                    .shadow(glow, RoundedEndShape, clip = false, ambientColor = color, spotColor = color)
                    .background(color, RoundedEndShape)
            )
            if (page != null) {
                AnimatedVisibility(
                    visible = showChip,
                    enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    modifier = Modifier.fillMaxWidth()
                ) { TipChip(tip = { shown.value.coerceIn(0f, 1f) }, page = page, pages = pages) }
            }
        }
    }
}

/** Page chip 10dp under the line, centered on its tip and kept 8dp from the edges, with a notch at the tip. */
@Composable
private fun TipChip(
    tip: () -> Float,
    page: Int,
    pages: Int,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box(
                Modifier
                    .size(8.dp, 4.dp)
                    .background(ChipColor, NotchShape)
            )
            IndicatorChip { PageCount(page, pages, style = LocalTextStyle.current, prefix = "p.\u00a0") }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val notch = measurables[0].measure(loose)
        val chip = measurables[1].measure(loose)
        val width = constraints.maxWidth
        val tipX = width * tip()
        val x = clampedChipX(tipX, chip.width.toFloat(), width.toFloat(), 8.dp.toPx()).roundToInt()
        val inset = 6.dp.roundToPx()
        val notchMax = maxOf(x + inset, x + chip.width - inset - notch.width)
        val notchX = (tipX - notch.width / 2f).roundToInt().coerceIn(x + inset, notchMax)
        val top = 10.dp.roundToPx()
        layout(width, top + chip.height) {
            notch.place(notchX, top - notch.height)
            chip.place(x, top)
        }
    }
}

/** Dark translucent chip with roomy padding and tabular 12sp text, used for page counts over the art. */
@Composable
internal fun IndicatorChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalTextStyle provides MaterialTheme.typography.labelMedium.tabular(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(CircleShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .background(ChipColor)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            content = content
        )
    }
}

/**
 * The top line split into segments, like a stories viewer, with the page count at the end of the row.
 * Up to [SEGMENT_GROUP_THRESHOLD] pages each segment is one page; longer chapters group pages so a segment
 * fills partway through its range. The count stays visible and dims while [showChip] is false.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SegmentedProgressLine(
    index: Int,
    pages: Int,
    showChip: Boolean,
    onChipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (index >= pages - 1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val fillSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val heightSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val chipAlpha by animateFloatAsState(if (showChip) 1f else .75f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val perSegment = pagesPerSegment(pages)
    val current = index.coerceIn(0, (pages - 1).coerceAtLeast(0)) / perSegment
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            repeat(segmentCount(pages)) { i ->
                val fill = animateFloatAsState(segmentFill(i, index, pages), fillSpec)
                val height by animateDpAsState(if (i == current) 4.dp else 3.dp, heightSpec)
                Box(
                    Modifier
                        .weight(1f)
                        .height(height)
                        .clip(CircleShape)
                        .drawBehind {
                            drawRect(SegmentTrack)
                            drawRect(color, size = size.copy(width = size.width * fill.value))
                            drawRect(SegmentOutline, style = Stroke(0.75.dp.toPx()))
                        }
                )
            }
        }
        IndicatorChip(
            modifier = Modifier.graphicsLayer { alpha = chipAlpha },
            onClick = onChipClick,
        ) { PageCount(index + 1, pages, style = LocalTextStyle.current) }
    }
}

private val RoundedEndShape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
private val SegmentTrack = Color.White.copy(alpha = .42f)
private val SegmentOutline = Color.Black.copy(alpha = .35f)
private val ChipColor = Color(0xFF101114).copy(alpha = .72f)
private val QuarterTick = Color.Gray.copy(alpha = .6f)
private val NotchShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}
