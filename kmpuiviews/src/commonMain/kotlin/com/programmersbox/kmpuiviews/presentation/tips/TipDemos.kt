package com.programmersbox.kmpuiviews.presentation.tips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DetailsChapterSwipeBehavior
import com.programmersbox.datastore.NewSettingsHandling
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Hold a sample cover until the options sheet slides up. */
@Composable
fun HoldCoverDemo(onDone: () -> Unit) {
    var showOptions by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val longPressMillis = LocalViewConfiguration.current.longPressTimeoutMillis.toInt()
    val scale by animateFloatAsState(if (progress.value > 0f) 0.95f else 1f)

    Box(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Press and hold the cover", style = MaterialTheme.typography.titleMedium)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .size(width = 120.dp, height = 172.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val fill = scope.launch {
                                    progress.animateTo(1f, tween(longPressMillis, easing = LinearEasing))
                                }
                                tryAwaitRelease()
                                fill.cancel()
                                scope.launch { progress.snapTo(0f) }
                            },
                            onLongPress = {
                                showOptions = true
                                onDone()
                            }
                        )
                    }
            ) {
                Box(Modifier.padding(12.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.align(Alignment.Center).size(40.dp)
                    )
                    Text(
                        "Sample title",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.width(120.dp)
            )
        }

        AnimatedVisibility(
            visible = showOptions,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(interactionSource = null, indication = null) { showOptions = false }
            )
        }

        AnimatedVisibility(
            visible = showOptions,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 32.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    listOf(
                        Icons.Default.FavoriteBorder to "Add to favorites",
                        Icons.AutoMirrored.Filled.PlaylistAdd to "Add to lists",
                        Icons.Default.Share to "Share",
                    ).forEach { (icon, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = { Icon(icon, null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showOptions = false }
                        )
                    }
                }
            }
        }
    }
}

/** Swipe a sample chapter using the user's own chapter swipe actions. */
@Composable
fun SwipeChapterDemo(
    onDone: () -> Unit,
    handling: NewSettingsHandling = koinInject(),
) {
    val swipeBehavior by handling.detailsChapterSwipeBehavior.rememberPreference()
    val startToEnd = swipeBehavior.detailsChapterSwipeBehaviorStartToEnd
    val endToStart = swipeBehavior.detailsChapterSwipeBehaviorEndToStart

    if (startToEnd == DetailsChapterSwipeBehavior.Nothing && endToStart == DetailsChapterSwipeBehavior.Nothing) {
        Text(
            "Chapter swipe actions are off. Turn them on in Settings › Details.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    fun label(behavior: DetailsChapterSwipeBehavior) = when (behavior) {
        DetailsChapterSwipeBehavior.MarkAsRead -> "marks it read"
        DetailsChapterSwipeBehavior.Read -> "opens it"
        DetailsChapterSwipeBehavior.Nothing -> "does nothing"
    }

    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text("Swipe Chapter 12", style = MaterialTheme.typography.titleMedium)
        Text(
            "Right ${label(startToEnd)}. Left ${label(endToStart)}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        SampleChapter("Chapter 13", "Today")

        SwipeToDismissBox(
            state = dismissState,
            onDismiss = { value ->
                val behavior = when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> startToEnd
                    SwipeToDismissBoxValue.EndToStart -> endToStart
                    SwipeToDismissBoxValue.Settled -> DetailsChapterSwipeBehavior.Nothing
                }
                result = when (behavior) {
                    DetailsChapterSwipeBehavior.MarkAsRead -> "Marked as read"
                    DetailsChapterSwipeBehavior.Read -> "This would open the chapter"
                    DetailsChapterSwipeBehavior.Nothing -> null
                }
                onDone()
                scope.launch { dismissState.reset() }
            },
            enableDismissFromStartToEnd = startToEnd != DetailsChapterSwipeBehavior.Nothing,
            enableDismissFromEndToStart = endToStart != DetailsChapterSwipeBehavior.Nothing,
            backgroundContent = {
                val (alignment, behavior) = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart to startToEnd
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd to endToStart
                    SwipeToDismissBoxValue.Settled -> Alignment.Center to DetailsChapterSwipeBehavior.Nothing
                }
                Box(
                    contentAlignment = alignment,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp)
                ) {
                    when (behavior) {
                        DetailsChapterSwipeBehavior.MarkAsRead -> Icon(Icons.Default.Check, null)
                        DetailsChapterSwipeBehavior.Read -> Icon(Icons.Default.PlayArrow, null)
                        DetailsChapterSwipeBehavior.Nothing -> {}
                    }
                }
            },
        ) {
            SampleChapter("Chapter 12", result ?: "2 days ago")
        }

        SampleChapter("Chapter 11", "1 week ago")
    }
}

@Composable
private fun SampleChapter(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

/** Tap a sample reader page to show and hide the reader bars. */
@Composable
fun TapBarsDemo(
    onDone: () -> Unit,
    prompt: String = "Tap the page",
    page: @Composable BoxScope.() -> Unit,
) {
    var showBars by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(prompt, style = MaterialTheme.typography.titleMedium)

        ReaderPage(
            showBars = showBars,
            onTap = {
                showBars = !showBars
                onDone()
            },
            page = page,
        )
    }
}

@Composable
private fun ReaderPage(
    showBars: Boolean,
    onTap: () -> Unit,
    page: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 160.dp, height = 230.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(interactionSource = null, indication = null, onClick = onTap)
    ) {
        page()

        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Chapter 12 · 4/20",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(4.dp)
                        .height(24.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, null)
                    Box(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}
