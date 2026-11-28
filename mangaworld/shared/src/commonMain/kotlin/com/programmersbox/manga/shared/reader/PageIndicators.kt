package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.State
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
        PageIndicatorStyle.Ring -> "Progress ring"
        PageIndicatorStyle.Wavy -> "Wavy line"
        PageIndicatorStyle.Dots -> "Dot window"
    }

/**
 * Everything the reader draws to show the current page: the top progress line (every style; Segments splits it
 * per page) plus the chosen [style]'s overlay. Tapping an indicator calls [onOpenPages]; the scrubber and the
 * rail jump with [onJumpToPage] (0-based, within the chapter).
 *
 * [pageThumbnail] draws a page (0-based) for the scrubber's drag bubble.
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
    pageThumbnail: @Composable (index: Int) -> Unit = {},
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
        } else if (style == PageIndicatorStyle.Wavy) {
            WavyProgressLine(
                page = page,
                pages = pages,
                visible = !showItems,
                turning = recentlyTurned,
                modifier = Modifier.align(Alignment.TopStart)
            )
        } else {
            ReadingProgressLine(
                progress = chapterProgress(currentPage, pages),
                visible = !showItems,
                page = if (style == PageIndicatorStyle.Classic) page else null,
                pages = pages,
                showChip = recentlyTurned,
                swellOnTurn = style == PageIndicatorStyle.LineOnly,
                quarterTicks = style == PageIndicatorStyle.LineOnly,
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
                    PageScrubber(index = index, pages = pages, onJumpToPage = onJumpToPage, pageThumbnail = pageThumbnail)
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
                pageThumbnail = pageThumbnail,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(aboveBars)
                    .padding(horizontal = 16.dp)
            )

            PageIndicatorStyle.Ring -> PageRing(
                page = page,
                pages = pages,
                active = active,
                onClick = onOpenPages,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .then(aboveBars)
                    .padding(end = 10.dp, bottom = 2.dp)
            )

            PageIndicatorStyle.Dots -> PageDotWindow(
                index = index,
                pages = pages,
                active = active,
                onClick = onOpenPages,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(aboveBars)
                    .padding(bottom = 4.dp)
            )

            PageIndicatorStyle.Classic, PageIndicatorStyle.Segments, PageIndicatorStyle.LineOnly,
            PageIndicatorStyle.Wavy -> Unit
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

/** Digits all the same width, so counts don't shift as pages change. */
internal fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** "[prefix]12 / 24": the page rolls, the total is dimmed, digits are tabular. */
@Composable
internal fun PageCount(
    page: Int,
    pages: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    prefix: String? = null,
) {
    val tabular = style.tabular()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (prefix != null) Text(prefix, style = tabular)
        RollingNumber(page, tabular)
        Text(" / $pages", style = tabular, color = LocalContentColor.current.copy(alpha = .7f))
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
    val alpha by animateFloatAsState(if (active) 1f else .7f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val scale by animateFloatAsState(if (active) 1f else .9f, MaterialTheme.motionScheme.defaultSpatialSpec())
    val shown = animatePageProgress(page, pages)
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = .26f)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = IndicatorContainer,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }
    ) {
        PageCount(
            page = page,
            pages = pages,
            modifier = Modifier
                .drawBehind { drawRect(fill, size = size.copy(width = size.width * shown.value)) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/** [page] / [pages] as a fraction, springing like the top progress line. */
@Composable
private fun animatePageProgress(page: Int, pages: Int): State<Float> = animateFloatAsState(
    targetValue = if (pages <= 0) 0f else (page.toFloat() / pages).coerceIn(0f, 1f),
    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
)

/**
 * Slider over the chapter's pages, with a tick per page in short chapters. Dragging shows the target page's
 * [pageThumbnail] and number in a bubble; releasing jumps there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageScrubber(
    index: Int,
    pages: Int,
    onJumpToPage: (Int) -> Unit,
    pageThumbnail: @Composable (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val target = dragValue?.roundToInt() ?: index
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val labelStyle = MaterialTheme.typography.labelLarge.tabular()
    val ticks = showScrubberTicks(pages)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 18.dp)
    ) {
        RollingNumber(target + 1, labelStyle, Modifier.widthIn(min = 20.dp))
        val lastIndex = (pages - 1).coerceAtLeast(1)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
        ) {
            // Plain overload on purpose: the thumb/track overload differs between the CMP and Android material3
            // artifacts and fails at runtime with NoSuchMethodError. Steps draw a tick per page on top of the
            // track and snap the thumb to whole pages.
            Slider(
                value = dragValue ?: index.toFloat(),
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { onJumpToPage(it.roundToInt()) }
                    dragValue = null
                },
                valueRange = 0f..lastIndex.toFloat(),
                steps = if (ticks) pages - 2 else 0,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth()
            )
            // Qualified: inside the Row, the RowScope overload would otherwise be picked
            androidx.compose.animation.AnimatedVisibility(
                visible = dragged || pressed,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                        scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = .9f),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                modifier = Modifier.layout { measurable, constraints ->
                    // Zero-size so the bubble floats above the thumb without resizing the slider
                    val placeable = measurable.measure(Constraints())
                    val inset = ScrubberThumbInset.roundToPx()
                    val x = inset + (constraints.maxWidth - inset * 2) * target / lastIndex
                    layout(0, 0) {
                        placeable.place(x - placeable.width / 2, -placeable.height - 20.dp.roundToPx())
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(40.dp, 56.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) { pageThumbnail(target) }
                    Text(
                        "p. ${target + 1}",
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
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
    val alpha by animateFloatAsState(if (active) 1f else .5f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val background by animateColorAsState(
        MaterialTheme.colorScheme.scrim.copy(alpha = if (active) .4f else .18f),
        MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val primary = MaterialTheme.colorScheme.primary
    val read = lerp(primary, Color.White, .45f)
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
                .padding(vertical = RailPadding, horizontal = 3.dp)
        ) {
            repeat(dots) { dot ->
                val height by animateDpAsState(if (dot == current) RailCurrentDot else RailDot, dotSpring)
                val color by animateColorAsState(
                    when {
                        dot == current -> primary
                        dot < current -> read
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
                shape = RoundedCornerShape(12.dp, 3.dp, 3.dp, 12.dp),
                color = primary,
            ) {
                PageCount(
                    page = index + 1,
                    pages = pages,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
    val start = folioOnStart(index)
    val shift = { width: Int -> if (start) -width / 4 else width / 4 }
    AnimatedVisibility(
        visible = recent || showItems,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + slideInHorizontally(initialOffsetX = shift),
        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + slideOutHorizontally(targetOffsetX = shift),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = if (start) Alignment.Start else Alignment.End,
            modifier = Modifier
                .clip(FolioShape)
                .clickable(onClick = onClick)
                .background(FolioScrim)
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
        ) {
            Box(
                Modifier
                    .size(22.dp, 1.dp)
                    .background(Color.White.copy(alpha = .6f))
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                RollingNumber(
                    index + 1,
                    MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 30.sp,
                        color = Color.White
                    )
                )
                Text(
                    "OF $pages",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
                    color = Color.White.copy(alpha = .75f),
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }
        }
    }
}

/** Soft dark glow behind the folio, densest at the bottom edge, so it reads over any art without a box. */
private val FolioScrim = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = RadialGradientShader(
        center = Offset(size.width / 2, size.height),
        radius = 1.2f * maxOf(size.width, size.height),
        colors = listOf(Color(0xC70A0A0C), Color(0x730A0A0C), Color.Transparent),
        colorStops = listOf(0f, .6f, 1f)
    )
}
private val FolioShape = RoundedCornerShape(14.dp)

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
    pageThumbnail: @Composable (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (active) 1f else .7f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val shown = animatePageProgress(index + 1, pages)
    val primary = MaterialTheme.colorScheme.primary
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
                PageScrubber(index = index, pages = pages, onJumpToPage = onJumpToPage, pageThumbnail = pageThumbnail)
            } else {
                // Count with a 2dp progress bar drawn in the 5dp below it
                PageCount(
                    page = index + 1,
                    pages = pages,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenPages)
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .drawBehind {
                            val bar = 2.dp.toPx()
                            val top = size.height - bar
                            val radius = CornerRadius(bar / 2)
                            drawRoundRect(Color.White.copy(alpha = .18f), Offset(0f, top), Size(size.width, bar), radius)
                            drawRoundRect(primary, Offset(0f, top), Size(size.width * shown.value, bar), radius)
                        }
                        .padding(bottom = 5.dp)
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

internal val IndicatorContainer: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .9f)

/** Distance from the slider's edge to the thumb's center at either end of the track. */
private val ScrubberThumbInset = 10.dp

private val RailDot = 4.dp
private val RailCurrentDot = 20.dp
private val RailGap = 5.dp
private val RailPadding = 10.dp
