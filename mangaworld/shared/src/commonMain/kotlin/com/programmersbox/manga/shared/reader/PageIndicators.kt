package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.PageIndicatorStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** How long a page indicator stays fully shown after the last page turn. */
private const val TURN_LINGER_MS = 600L

/** Name shown in the settings pickers. */
internal val PageIndicatorStyle.label: String
    get() = when (this) {
        PageIndicatorStyle.Classic -> "Classic"
        PageIndicatorStyle.Pill -> "Rolling pill"
        PageIndicatorStyle.Scrubber -> "Scrubber"
        PageIndicatorStyle.Rail -> "Edge rail"
        PageIndicatorStyle.Segments -> "Story segments"
        PageIndicatorStyle.Folio -> "Printed folio"
        PageIndicatorStyle.Morph -> "Morphing pill"
        PageIndicatorStyle.LineOnly -> "Line only"
    }

/**
 * Everything the reader draws to show the current page: the top progress line (every style; Segments splits it
 * per page) plus the chosen [style]'s overlay. Tapping an indicator calls [onOpenPages]; the scrubber and the
 * rail jump with [onJumpToPage] (0-based, within the chapter).
 *
 * [topPadding] and [bottomPadding] are the space the reader's bars take while [showItems] is true.
 */
@Composable
internal fun PageIndicatorOverlay(
    style: PageIndicatorStyle,
    currentPage: Int,
    pages: Int,
    showItems: Boolean,
    scrolling: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    onOpenPages: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = displayPage(currentPage, pages)
    val index = page - 1

    var recentlyTurned by remember { mutableStateOf(true) }
    LaunchedEffect(scrolling, index) {
        recentlyTurned = true
        if (!scrolling) {
            delay(TURN_LINGER_MS)
            recentlyTurned = false
        }
    }
    val active = showItems || recentlyTurned

    Box(modifier.fillMaxSize()) {
        if (style == PageIndicatorStyle.Segments && usesSegments(pages)) {
            SegmentedProgressLine(
                index = index,
                pages = pages,
                showChip = active,
                onChipClick = onOpenPages,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = topPadding)
            )
        } else {
            ReadingProgressLine(
                progress = chapterProgress(currentPage, pages),
                visible = !showItems,
                pageLabel = if (style == PageIndicatorStyle.Classic) pageLabel(currentPage, pages) else null,
                showChip = recentlyTurned,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        val aboveBars = Modifier.padding(bottom = bottomPadding + 8.dp)
        when (style) {
            PageIndicatorStyle.Pill -> PagePill(
                page = page,
                pages = pages,
                active = active,
                onClick = onOpenPages,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(aboveBars)
            )

            PageIndicatorStyle.Scrubber -> AnimatedVisibility(
                visible = showItems && pages > 1,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + slideInVertically { it / 2 },
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(aboveBars)
                    .padding(horizontal = 16.dp)
            ) {
                UnclippedPill {
                    PageScrubber(index = index, pages = pages, onJumpToPage = onJumpToPage)
                }
            }

            PageIndicatorStyle.Rail -> PageRail(
                index = index,
                pages = pages,
                active = active,
                onOpenPages = onOpenPages,
                onJumpToPage = onJumpToPage,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
            )

            PageIndicatorStyle.Folio -> PageFolio(
                index = index,
                pages = pages,
                showItems = showItems,
                onClick = onOpenPages,
                modifier = Modifier
                    .align(if (folioOnStart(index)) Alignment.BottomStart else Alignment.BottomEnd)
                    .then(aboveBars)
                    .padding(horizontal = 12.dp)
            )

            PageIndicatorStyle.Morph -> MorphingPageIndicator(
                index = index,
                pages = pages,
                expanded = showItems && pages > 1,
                active = active,
                onOpenPages = onOpenPages,
                onJumpToPage = onJumpToPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(aboveBars)
                    .padding(horizontal = 16.dp)
            )

            PageIndicatorStyle.Classic, PageIndicatorStyle.Segments, PageIndicatorStyle.LineOnly -> Unit
        }
    }
}

/** A number that rolls up when it grows and down when it shrinks. */
@Composable
internal fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInVertically { it * direction } + fadeIn()) togetherWith
                    (slideOutVertically { -it * direction } + fadeOut()) using SizeTransform(clip = true)
        },
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier,
    ) { Text("$it", style = style) }
}

@Composable
private fun PageCount(
    page: Int,
    pages: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        RollingNumber(page, style)
        Text(" / $pages", style = style, color = LocalContentColor.current.copy(alpha = .7f))
    }
}

@Composable
private fun PagePill(
    page: Int,
    pages: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (active) 1f else .45f, MaterialTheme.motionScheme.defaultEffectsSpec())
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = IndicatorContainer,
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    ) {
        PageCount(page, pages, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

/** Slider over the chapter's pages. Dragging shows the target page in a bubble; releasing jumps there. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageScrubber(
    index: Int,
    pages: Int,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val target = dragValue?.roundToInt() ?: index
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val labelStyle = MaterialTheme.typography.labelLarge

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
    ) {
        RollingNumber(target + 1, labelStyle)
        val lastIndex = (pages - 1).coerceAtLeast(1)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
        ) {
            // Plain overload on purpose: the thumb/track overload differs between the CMP and Android material3
            // artifacts and fails at runtime with NoSuchMethodError.
            Slider(
                value = dragValue ?: index.toFloat(),
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { onJumpToPage(it.roundToInt()) }
                    dragValue = null
                },
                valueRange = 0f..lastIndex.toFloat(),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth()
            )
            if (dragged || pressed) {
                Text(
                    "${target + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            // Zero-size so the bubble floats above the thumb without resizing the slider
                            val placeable = measurable.measure(Constraints())
                            val inset = ScrubberThumbInset.roundToPx()
                            val x = inset + (constraints.maxWidth - inset * 2) * target / lastIndex
                            layout(0, 0) {
                                placeable.place(x - placeable.width / 2, -placeable.height - 20.dp.roundToPx())
                            }
                        }
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Text("$pages", style = labelStyle, color = LocalContentColor.current.copy(alpha = .7f))
    }
}

/** One dot per page (or per range of pages past [MAX_RAIL_DOTS]) on the screen edge, with a number flag. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageRail(
    index: Int,
    pages: Int,
    active: Boolean,
    onOpenPages: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dots = railDotCount(pages)
    val current = railDotFor(index, pages)
    val alpha by animateFloatAsState(if (active) 1f else .35f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val background by animateColorAsState(
        if (active) MaterialTheme.colorScheme.scrim.copy(alpha = .4f) else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val primary = MaterialTheme.colorScheme.primary
    val dotSpring = spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy)
    val flagCenter = animateDpAsState(
        RailPadding + (RailDot + RailGap) * current + RailCurrentDot / 2,
        MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    Box(modifier.graphicsLayer { this.alpha = alpha }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RailGap),
            modifier = Modifier
                .background(background, CircleShape)
                .padding(vertical = RailPadding, horizontal = 2.dp)
        ) {
            repeat(dots) { dot ->
                val height by animateDpAsState(if (dot == current) RailCurrentDot else RailDot, dotSpring)
                val color by animateColorAsState(
                    when {
                        dot == current -> primary
                        dot < current -> Color.White.copy(alpha = .87f)
                        else -> Color.White.copy(alpha = .55f)
                    }
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(12.dp)
                        .height(height)
                        .clickable(interactionSource = null, indication = null) { onJumpToPage(railFirstPage(dot, pages)) }
                ) {
                    Box(
                        Modifier
                            .width(RailDot)
                            .fillMaxHeight()
                            .background(color, CircleShape)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = active,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.layout { measurable, constraints ->
                // Zero-size so the flag sits left of the rail without widening it
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(0, 0) {
                    placeable.place(
                        x = -placeable.width - 4.dp.roundToPx(),
                        y = flagCenter.value.roundToPx() - placeable.height / 2
                    )
                }
            }
        ) {
            Surface(
                onClick = onOpenPages,
                shape = RoundedCornerShape(10.dp, 10.dp, 2.dp, 10.dp),
                color = primary,
            ) {
                PageCount(
                    page = index + 1,
                    pages = pages,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/** Page number in the outer corner like a printed book; shows on each turn, then fades. */
@Composable
private fun PageFolio(
    index: Int,
    pages: Int,
    showItems: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var recent by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        recent = true
        delay(1500)
        recent = false
    }
    AnimatedVisibility(
        visible = recent || showItems,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + slideInVertically { it / 4 },
        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + slideOutVertically { it / 4 },
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .85f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            RollingNumber(
                index + 1,
                MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                " of $pages",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
    }
}

/** A small pill while reading that stretches into the scrubber when the bars show. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MorphingPageIndicator(
    index: Int,
    pages: Int,
    expanded: Boolean,
    active: Boolean,
    onOpenPages: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (active) 1f else .5f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val sizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    UnclippedPill(modifier.graphicsLayer { this.alpha = alpha }) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) using SizeTransform(clip = false) { _, _ -> sizeSpec }
            },
            contentAlignment = Alignment.Center,
        ) { isExpanded ->
            if (isExpanded) {
                PageScrubber(index = index, pages = pages, onJumpToPage = onJumpToPage)
            } else {
                PageCount(
                    page = index + 1,
                    pages = pages,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenPages)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Pill background that doesn't clip its content, so the scrubber's drag bubble can rise above it.
 * A Surface would clip to its shape.
 */
@Composable
private fun UnclippedPill(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = IndicatorContainer
    Box(modifier.background(color, CircleShape)) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.contentColorFor(color.copy(alpha = 1f)),
            content = content
        )
    }
}

private val IndicatorContainer: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .9f)

/** Distance from the slider's edge to the thumb's center at either end of the track. */
private val ScrubberThumbInset = 10.dp

private val RailDot = 4.dp
private val RailCurrentDot = 18.dp
private val RailGap = 5.dp
private val RailPadding = 8.dp
