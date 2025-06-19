package com.programmersbox.manga.shared.reader

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LastPageReached(
    isLoading: Boolean,
    currentChapter: Int,
    lastChapter: Int,
    chapterName: String,
    nextChapter: () -> Unit,
    previousChapter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(targetValue = if (isLoading) 0f else 1f, label = "")

    ChangeChapterSwipe(
        nextChapter = nextChapter,
        previousChapter = previousChapter,
        isLoading = isLoading,
        currentChapter = currentChapter,
        lastChapter = lastChapter,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    chapterName,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.graphicsLayer { this.alpha = alpha }
                ) {
                    Text(
                        "Reached Last Page",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    )
                    if (currentChapter <= 0) {
                        Text(
                            "Reached Last Chapter",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Text(
                    "Swipe left or right and up to go to the previous or next chapter",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ChangeChapterSwipe(
    nextChapter: () -> Unit,
    previousChapter: () -> Unit,
    currentChapter: Int,
    lastChapter: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 300.dp)
            .wrapContentHeight()
    ) {
        val density = LocalDensity.current

        val state = remember {
            AnchoredDraggableState<SwipeUpGesture>(
                initialValue = SwipeUpGesture.Settled,
                positionalThreshold = { it },
                velocityThreshold = { with(density) { 125.dp.toPx() } },
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                decayAnimationSpec = exponentialDecay(),
                anchors = DraggableAnchors {
                    SwipeUpGesture.Settled at 0f
                    SwipeUpGesture.Up at -300f
                }
            ) {
                if (it == SwipeUpGesture.Up) previousChapter()
                false
            }
        }

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                when (it) {
                    SwipeToDismissBoxValue.StartToEnd -> nextChapter()
                    SwipeToDismissBoxValue.EndToStart -> previousChapter()
                    else -> Unit
                }
                false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = !isLoading && currentChapter < lastChapter,
            enableDismissFromEndToStart = !isLoading && currentChapter > 0,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.FastRewind
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.FastForward
                    else -> Icons.Default.Pages
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.scale(scale),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = state
                            .requireOffset()
                            .toInt()
                    )
                }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Vertical,
                    enabled = !isLoading && currentChapter > 0,
                ),
            content = {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(this@BoxWithConstraints.maxHeight / 2)
                ) { content() }
            }
        )
    }
}

enum class SwipeUpGesture {
    Up,
    Settled,
}

@Composable
internal fun ChapterPage(
    chapterLink: () -> String,
    isDownloaded: Boolean,
    headers: Map<String, String>,
    contentScale: ContentScale,
    imageLoaderType: ImageLoaderType,
) {
    if (imageLoaderType == ImageLoaderType.Panpf) {
        Panpf(
            painter = chapterLink(),
            headers = headers,
            modifier = Modifier.fillMaxWidth(),
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .requiredHeightIn(min = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            ZoomableImage(
                painter = chapterLink(),
                isDownloaded = isDownloaded,
                headers = headers,
                modifier = Modifier.fillMaxWidth(),
                contentScale = contentScale,
                imageLoaderType = imageLoaderType
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    painter: String,
    isDownloaded: Boolean,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    imageLoaderType: ImageLoaderType,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            /*.clickable(
                indication = null,
                onClick = onClick,
                interactionSource = null
            )*/
    ) {
        val scope = rememberCoroutineScope()
        var showTheThing by remember { mutableStateOf(true) }

        imageLoaderType.Composed(
            painter = painter,
            onRefresh = {
                scope.launch {
                    showTheThing = false
                    delay(1000)
                    showTheThing = true
                }
            },
            contentScale = contentScale,
            headers = headers,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private fun clampOffset(centerPoint: Offset, offset: Offset, scale: Float): Offset {
    val maxPosition = centerPoint * (scale - 1)

    return offset.copy(
        x = offset.x.coerceIn(-maxPosition.x, maxPosition.x),
        y = offset.y.coerceIn(-maxPosition.y, maxPosition.y)
    )
}