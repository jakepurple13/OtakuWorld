package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.manga.shared.reader.curl.ExperimentalPageCurlApi
import com.programmersbox.manga.shared.reader.curl.PageCurlState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal const val READER_RAIL_TAG = "readerPageRail"
private const val RAIL_IDLE_HIDE_MS = 1500L
private val RailTouchWidth = 24.dp
private val RailIdleWidth = 4.dp
private val RailActiveWidth = 10.dp
private val RailEndInset = 6.dp
private val MinThumbHeight = 18.dp

internal data class RailThumb(val offsetPx: Float, val sizePx: Float)

/**
 * Thumb placement for a rail over [count] items when [visible] items fit on screen and the first one shown is at [position]
 * (fractional, e.g. 3.5 = halfway through item 3).
 */
internal fun railThumb(count: Int, position: Float, visible: Float, trackPx: Float, minThumbPx: Float): RailThumb {
    val total = count.coerceAtLeast(1).toFloat()
    val shown = visible.coerceIn(0f, total)
    val size = (shown / total * trackPx).coerceIn(minThumbPx.coerceAtMost(trackPx), trackPx)
    val scrollable = total - shown
    val fraction = if (scrollable <= 0f) 0f else (position / scrollable).coerceIn(0f, 1f)
    return RailThumb(offsetPx = fraction * (trackPx - size), sizePx = size)
}

/** The position that puts the thumb's top at [thumbOffsetPx]; the inverse of [railThumb]. */
internal fun railPositionForThumb(thumbOffsetPx: Float, count: Int, visible: Float, trackPx: Float, thumbPx: Float): Float {
    val room = trackPx - thumbPx
    val fraction = if (room <= 0f) 0f else (thumbOffsetPx / room).coerceIn(0f, 1f)
    return fraction * (count - visible).coerceAtLeast(0f)
}

/** The item under [yPx] on the track. */
internal fun railIndexAt(yPx: Float, trackPx: Float, count: Int): Int =
    if (trackPx <= 0f || count <= 0) 0 else (yPx / trackPx * count).toInt().coerceIn(0, count - 1)

/**
 * A scrollbar for the reader on large screens: one tick per item, a stronger tick where a chapter starts.
 * It fades out [RAIL_IDLE_HIDE_MS] after scrolling stops; hovering or dragging widens it and shows a page bubble.
 * Dragging calls [onScrub] with a fractional position; clicking the track calls [onJump] with an item index.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderPageRail(
    count: Int,
    position: () -> Float,
    visible: () -> Float,
    chapterStarts: List<Int>,
    scrolling: Boolean,
    label: (index: Int) -> Pair<String, String?>,
    onScrub: (position: Float) -> Unit,
    onJump: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val currentPosition by rememberUpdatedState(position)
    // Page changes count as scrolling too: pagers jump between pages without a long scroll
    var pageChanges by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        snapshotFlow { currentPosition().roundToInt() }.collect { pageChanges++ }
    }
    var recentlyScrolled by remember { mutableStateOf(true) }
    LaunchedEffect(scrolling, pageChanges) {
        recentlyScrolled = true
        if (!scrolling) {
            delay(RAIL_IDLE_HIDE_MS)
            recentlyScrolled = false
        }
    }
    val active = hovered || dragging
    val alpha by animateFloatAsState(
        targetValue = if (active || recentlyScrolled) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
    )
    val trackWidth by animateDpAsState(
        targetValue = if (active) RailActiveWidth else RailIdleWidth,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )

    val currentCount by rememberUpdatedState(count)
    val currentVisible by rememberUpdatedState(visible)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentOnJump by rememberUpdatedState(onJump)

    var trackPx by remember { mutableFloatStateOf(0f) }
    var bubbleHeight by remember { mutableIntStateOf(0) }
    val minThumbPx = with(LocalDensity.current) { MinThumbHeight.toPx() }
    fun thumb() = railThumb(currentCount, currentPosition(), currentVisible(), trackPx, minThumbPx)

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val chapterColor = MaterialTheme.colorScheme.primary
    val thumbColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.width(RailTouchWidth).fillMaxHeight().alpha(alpha)) {
        AnimatedVisibility(
            visible = active,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Hangs off to the left so showing it never resizes the rail out from under the pointer
                .wrapContentWidth(Alignment.End, unbounded = true)
                .padding(end = RailTouchWidth + 4.dp)
                .offset {
                    val t = thumb()
                    IntOffset(0, (t.offsetPx + t.sizePx / 2 - bubbleHeight / 2f).roundToInt())
                }
                .onSizeChanged { bubbleHeight = it.height },
        ) {
            val index = currentPosition().roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0))
            val (title, subtitle) = label(index)
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = MaterialTheme.shapes.small,
                shadowElevation = 4.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(RailTouchWidth)
                .fillMaxHeight()
                .testTag(READER_RAIL_TAG)
                .onSizeChanged { trackPx = it.height.toFloat() }
                .hoverable(interactionSource)
                .pointerInput(Unit) {
                    detectTapGestures { currentOnJump(railIndexAt(it.y, size.height.toFloat(), currentCount)) }
                }
                .pointerInput(Unit) {
                    var grab = 0f
                    detectVerticalDragGestures(
                        onDragStart = { start ->
                            dragging = true
                            val t = thumb()
                            grab = if (start.y in t.offsetPx..(t.offsetPx + t.sizePx)) start.y - t.offsetPx else t.sizePx / 2
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        val t = thumb()
                        currentOnScrub(
                            railPositionForThumb(change.position.y - grab, currentCount, currentVisible(), trackPx, t.sizePx)
                        )
                    }
                }
        ) {
            val width = trackWidth.toPx()
            val left = size.width - RailEndInset.toPx() - width
            val radius = CornerRadius(width / 2)
            drawRoundRect(trackColor, Offset(left, 0f), Size(width, size.height), radius)
            // Skip per-page ticks when they would smear into a solid bar
            if (currentCount > 1 && size.height / currentCount >= 3.dp.toPx()) {
                for (i in 1 until currentCount) {
                    val y = i.toFloat() / currentCount * size.height
                    drawRect(tickColor, Offset(left, y), Size(width, 1.dp.toPx()))
                }
            }
            chapterStarts.forEach { i ->
                if (i <= 0) return@forEach
                val y = i.toFloat() / currentCount * size.height
                val extra = 3.dp.toPx()
                drawRect(chapterColor, Offset(left - extra, y), Size(width + extra * 2, 2.dp.toPx()))
            }
            val t = thumb()
            drawRoundRect(thumbColor, Offset(left, t.offsetPx), Size(width, t.sizePx), radius)
        }
    }
}

/** [ReaderPageRail] wired to whichever reader is showing. List and Pager cover every loaded chapter; Flip and Curl only the current one. */
@OptIn(ExperimentalPageCurlApi::class)
@Composable
internal fun ReaderRail(
    readerType: ReaderType,
    viewModel: ReadViewModel,
    listState: LazyListState,
    pagerState: PagerState,
    curlState: PageCurlState,
    pagesInCurrentChapter: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val chapterPageCounts by remember {
        derivedStateOf {
            viewModel.pageItems.filterIsInstance<PageItem.Page>().groupingBy { it.chapterListIndex }.eachCount()
        }
    }
    val chapterStarts by remember {
        derivedStateOf {
            viewModel.pageItems.indices.filter { (viewModel.pageItems[it] as? PageItem.Page)?.pageIndex == 0 }
        }
    }

    fun itemLabel(index: Int): Pair<String, String?> = when (val item = viewModel.pageItems.getOrNull(index)) {
        is PageItem.Page -> "Page ${item.pageIndex + 1} / ${chapterPageCounts[item.chapterListIndex] ?: 1}" to
                viewModel.chapterName(item.chapterListIndex)

        is PageItem.ChapterTransition -> "Chapter break" to viewModel.chapterName(item.toChapterListIndex)
        null -> "" to null
    }

    fun chapterLabel(index: Int): Pair<String, String?> =
        "Page ${index + 1} / $pagesInCurrentChapter" to viewModel.chapterName(viewModel.currentChapter)

    val count = when (readerType) {
        ReaderType.List, ReaderType.Pager -> viewModel.pageItems.size
        ReaderType.FlipPager, ReaderType.CurlPager -> pagesInCurrentChapter
    }
    if (count <= 1) return

    when (readerType) {
        ReaderType.List -> ReaderPageRail(
            count = count,
            position = { listState.railPosition() },
            visible = { listState.railVisibleItems() },
            chapterStarts = chapterStarts,
            scrolling = listState.isScrollInProgress,
            label = ::itemLabel,
            onScrub = { position ->
                scope.launch {
                    val index = position.toInt()
                    val size = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                    listState.scrollToItem(index, ((position - index) * size).roundToInt())
                }
            },
            onJump = { scope.launch { listState.animateScrollToItem(it) } },
            modifier = modifier,
        )

        ReaderType.Pager, ReaderType.FlipPager -> ReaderPageRail(
            count = count,
            position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            visible = { 1f },
            chapterStarts = if (readerType == ReaderType.Pager) chapterStarts else emptyList(),
            scrolling = pagerState.isScrollInProgress,
            label = if (readerType == ReaderType.Pager) ::itemLabel else ::chapterLabel,
            onScrub = { scope.launch { pagerState.scrollToPage(it.roundToInt()) } },
            onJump = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = modifier,
        )

        ReaderType.CurlPager -> ReaderPageRail(
            count = count,
            position = { curlState.current.toFloat() },
            visible = { 1f },
            chapterStarts = emptyList(),
            scrolling = false,
            label = ::chapterLabel,
            onScrub = { scope.launch { curlState.snapTo(it.roundToInt()) } },
            onJump = { scope.launch { curlState.snapTo(it) } },
            modifier = modifier,
        )
    }
}

/** First visible item plus how far it has scrolled off, e.g. 3.5 = halfway through item 3. */
private fun LazyListState.railPosition(): Float {
    val first = layoutInfo.visibleItemsInfo.firstOrNull { it.index == firstVisibleItemIndex } ?: return firstVisibleItemIndex.toFloat()
    return firstVisibleItemIndex + (firstVisibleItemScrollOffset.toFloat() / first.size.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/** How many items fit on screen, counting partly shown ones by the share that shows. */
private fun LazyListState.railVisibleItems(): Float {
    val info = layoutInfo
    return info.visibleItemsInfo.sumOf { item ->
        val top = maxOf(item.offset, info.viewportStartOffset)
        val bottom = minOf(item.offset + item.size, info.viewportEndOffset)
        (bottom - top).coerceAtLeast(0).toDouble() / item.size.coerceAtLeast(1)
    }.toFloat()
}
