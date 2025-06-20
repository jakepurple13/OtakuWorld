package com.programmersbox.manga.shared.reader

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

val TopShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rectangle(Rect(0f, 0f, size.width, size.height / 2))
}

val BottomShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rectangle(Rect(0f, size.height / 2, size.width, size.height))
}

val LeftShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rectangle(Rect(0f, 0f, size.width / 2, size.height))
}

val RightShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rectangle(Rect(size.width / 2, 0f, size.width, size.height))
}

internal sealed class PageFlapType(val shape: Shape) {
    data object Top : PageFlapType(TopShape)
    data object Bottom : PageFlapType(BottomShape)
    data object Left : PageFlapType(LeftShape)
    data object Right : PageFlapType(RightShape)
}

@Composable
internal fun BoxScope.PageFlap(
    modifier: Modifier = Modifier,
    pageFlap: PageFlapType,
    imageBitmap: () -> ImageBitmap?,
    state: PagerState,
    page: Int,
    animatedOverscrollAmount: () -> Float = { 0f },
) {
    val density = LocalDensity.current
    val size by remember {
        derivedStateOf {
            imageBitmap()?.let {
                with(density) {
                    DpSize(it.width.toDp(), it.height.toDp())
                }
            } ?: DpSize.Zero
        }
    }
    Canvas(
        modifier
            .size(size)
            .align(Alignment.TopStart)
            .graphicsLayer {
                shape = pageFlap.shape
                clip = true

                cameraDistance = 65f
                when (pageFlap) {
                    is PageFlapType.Top -> {
                        rotationX = min(
                            (state.endOffsetForPage(page) * 180f).coerceIn(-90f..0f),
                            animatedOverscrollAmount().coerceAtLeast(0f) * -20f
                        )
                    }

                    is PageFlapType.Bottom -> {
                        rotationX = max(
                            (state.startOffsetForPage(page) * 180f).coerceIn(0f..90f),
                            animatedOverscrollAmount().coerceAtMost(0f) * -20f
                        )
                    }

                    is PageFlapType.Left -> {
                        rotationY = -min(
                            (state.endOffsetForPage(page) * 180f).coerceIn(-90f..0f),
                            animatedOverscrollAmount().coerceAtLeast(0f) * -20f
                        )
                    }

                    is PageFlapType.Right -> {
                        rotationY = -max(
                            (state.startOffsetForPage(page) * 180f).coerceIn(0f..90f),
                            animatedOverscrollAmount().coerceAtMost(0f) * -20f
                        )
                    }
                }
            }
    ) {
        imageBitmap()?.let { imageBitmap ->
            drawImage(imageBitmap)
            drawImage(
                imageBitmap,
                colorFilter = ColorFilter.tint(
                    Color.Black.copy(
                        alpha = when (pageFlap) {
                            PageFlapType.Top, PageFlapType.Left -> max(
                                (state.endOffsetForPage(page).absoluteValue * .9f).coerceIn(
                                    0f..1f
                                ), animatedOverscrollAmount() * .3f
                            )

                            PageFlapType.Bottom, PageFlapType.Right -> max(
                                (state.startOffsetForPage(page) * .9f).coerceIn(
                                    0f..1f
                                ), (animatedOverscrollAmount() * -1) * .3f
                            )
                        },
                    )
                )
            )
        }
    }
}

sealed class FlipPagerOrientation {
    data object Vertical : FlipPagerOrientation()
    data object Horizontal : FlipPagerOrientation()
}

fun PagerState.offsetForPage(page: Int) = (currentPage - page) + currentPageOffsetFraction

fun PagerState.startOffsetForPage(page: Int): Float {
    return offsetForPage(page).coerceAtLeast(0f)
}

fun PagerState.endOffsetForPage(page: Int): Float {
    return offsetForPage(page).coerceAtMost(0f)
}

@Composable
fun FlipPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    orientation: FlipPagerOrientation,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable (Int) -> Unit,
) {
    val overscrollAmount = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { state.isScrollInProgress }.collect {
            if (!it) overscrollAmount.floatValue = 0f
        }
    }
    val animatedOverscrollAmount by animateFloatAsState(
        targetValue = overscrollAmount.floatValue / 500,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = ""
    )
    val nestedScrollConnection = rememberFlipPagerOverscroll(
        orientation = orientation,
        overscrollAmount = overscrollAmount
    )

    when (orientation) {
        FlipPagerOrientation.Vertical -> {
            VerticalPager(
                state = state,
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = contentPadding,
                pageSize = pageSize,
                beyondViewportPageCount = beyondViewportPageCount,
                horizontalAlignment = horizontalAlignment,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                key = key,
                snapPosition = snapPosition,
                pageContent = {
                    Content(
                        it,
                        state,
                        orientation,
                        pageContent,
                        animatedOverscrollAmount
                    )
                }
            )
        }

        FlipPagerOrientation.Horizontal -> {
            HorizontalPager(
                state = state,
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = contentPadding,
                pageSize = pageSize,
                beyondViewportPageCount = beyondViewportPageCount,
                verticalAlignment = verticalAlignment,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                key = key,
                snapPosition = snapPosition,
                pageContent = {
                    Content(
                        it,
                        state,
                        orientation,
                        pageContent,
                        animatedOverscrollAmount
                    )
                }
            )
        }
    }
}


@Composable
private fun Content(
    page: Int,
    state: PagerState,
    orientation: FlipPagerOrientation,
    pageContent: @Composable (Int) -> Unit,
    animatedOverscrollAmount: Float,
) {
    var zIndex by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { state.offsetForPage(page) }.collect {
            zIndex = when (state.offsetForPage(page)) {
                in -.5f..(.5f) -> 3f
                in -1f..1f -> 2f
                else -> 1f
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .zIndex(zIndex)
            .graphicsLayer {
                val pageOffset = state.offsetForPage(page)
                when (orientation) {
                    FlipPagerOrientation.Vertical -> translationY = size.height * pageOffset
                    FlipPagerOrientation.Horizontal -> translationX = size.width * pageOffset
                }
            },
        contentAlignment = Alignment.Center,
    ) {

        var imageBitmap: ImageBitmap? by remember { mutableStateOf(null) }
        val graphicsLayer = rememberGraphicsLayer()
        val isImageBitmapNull by remember {
            derivedStateOf {
                imageBitmap == null
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .alpha(if (state.isScrollInProgress && !isImageBitmapNull) 0f else 1f)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                },
            contentAlignment = Alignment.Center
        ) {
            pageContent(page)
        }

        LaunchedEffect(state.isScrollInProgress) {
            while (true) {
                if (graphicsLayer.size.width != 0)
                    imageBitmap = graphicsLayer.toImageBitmap()
                delay(if (state.isScrollInProgress) 16 else 300)
            }
        }

        LaunchedEffect(MaterialTheme.colorScheme.surface) {
            if (graphicsLayer.size.width != 0)
                imageBitmap = graphicsLayer.toImageBitmap()
        }

        PageFlap(
            modifier = Modifier.fillMaxSize(),
            pageFlap = when (orientation) {
                FlipPagerOrientation.Vertical -> PageFlapType.Top
                FlipPagerOrientation.Horizontal -> PageFlapType.Left
            },
            imageBitmap = { imageBitmap },
            state = state,
            page = page,
            animatedOverscrollAmount = { animatedOverscrollAmount }
        )

        PageFlap(
            modifier = Modifier.fillMaxSize(),
            pageFlap = when (orientation) {
                FlipPagerOrientation.Vertical -> PageFlapType.Bottom
                FlipPagerOrientation.Horizontal -> PageFlapType.Right
            },
            imageBitmap = { imageBitmap },
            state = state,
            page = page,
            animatedOverscrollAmount = { animatedOverscrollAmount }
        )
    }
}


@Composable
private fun rememberFlipPagerOverscroll(
    orientation: FlipPagerOrientation,
    overscrollAmount: MutableFloatState,
): NestedScrollConnection {
    val nestedScrollConnection = remember(orientation) {
        object : NestedScrollConnection {

            private fun calculateOverscroll(available: Float) {
                val previous = overscrollAmount.floatValue
                overscrollAmount.floatValue += available * (.3f)
                overscrollAmount.floatValue = when {
                    previous > 0 -> overscrollAmount.floatValue.coerceAtLeast(0f)
                    previous < 0 -> overscrollAmount.floatValue.coerceAtMost(0f)
                    else -> overscrollAmount.floatValue
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollAmount.floatValue != 0f) {
                    when (orientation) {
                        FlipPagerOrientation.Vertical -> calculateOverscroll(available.y)
                        FlipPagerOrientation.Horizontal -> calculateOverscroll(available.x)
                    }
                    return available
                }

                return super.onPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                when (orientation) {
                    FlipPagerOrientation.Vertical -> calculateOverscroll(available.y)
                    FlipPagerOrientation.Horizontal -> calculateOverscroll(available.x)
                }
                return available
            }
        }
    }
    return nestedScrollConnection
}