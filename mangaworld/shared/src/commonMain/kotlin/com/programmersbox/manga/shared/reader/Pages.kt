package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChapterTransitionItem(
    fromChapterName: String?,
    toChapterName: String?,
    toChapterListIndex: Int,
    chapterCount: Int,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    // Saveable state survives the lazy item scrolling away, so the card plays only the first time it shows
    var played by rememberSaveable { mutableStateOf(false) }
    val alreadyPlayed = remember { played }
    var showFinished by remember { mutableStateOf(alreadyPlayed) }
    var showDivider by remember { mutableStateOf(alreadyPlayed) }
    var showNext by remember { mutableStateOf(alreadyPlayed) }
    val check = remember { Animatable(if (alreadyPlayed) 1f else 0f) }
    // Lists and pagers compose this card before it's on screen; wait until it's actually shown
    LaunchedEffect(visible) { if (visible) played = true }
    LaunchedEffect(played) {
        if (!played || alreadyPlayed) return@LaunchedEffect
        launch { check.animateTo(1f, tween(500)) }
        delay(100)
        showFinished = true
        delay(400)
        showDivider = true
        delay(200)
        showNext = true
    }

    val enter = slideInVertically { it / 3 } + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec())
    val dividerWidth by animateFloatAsState(
        targetValue = if (showDivider) .7f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (fromChapterName == null) {
                Text(
                    "No more chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                DrawnCheck(progress = { check.value })
                AnimatedVisibility(visible = showFinished, enter = enter) {
                    ChapterLabel(label = "Finished", name = fromChapterName)
                }
            }

            HorizontalDivider(Modifier.fillMaxWidth(dividerWidth))

            AnimatedVisibility(visible = showNext, enter = enter) {
                if (toChapterName == null) {
                    Text(
                        "No more chapters",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChapterLabel(label = "Next", name = toChapterName)
                        LinearWavyProgressIndicator(
                            progress = { seriesProgress(toChapterListIndex, chapterCount) },
                            modifier = Modifier.fillMaxWidth(.8f)
                        )
                        Text(
                            "Chapter ${chapterPosition(toChapterListIndex, chapterCount)} of $chapterCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterLabel(label: String, name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Circle draws over the first 70% of [progress], the tick over the rest. */
@Composable
private fun DrawnCheck(progress: () -> Float, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    val tick = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier.size(40.dp)) {
        val t = progress()
        val stroke = 3.dp.toPx()
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * (t / .7f).coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val tickT = ((t - .7f) / .3f).coerceIn(0f, 1f)
        if (tickT > 0f) {
            tick.reset()
            tick.moveTo(size.width * .3f, size.height * .51f)
            tick.lineTo(size.width * .44f, size.height * .65f)
            tick.lineTo(size.width * .71f, size.height * .38f)
            measure.setPath(tick, false)
            segment.reset()
            measure.getSegment(0f, measure.length * tickT, segment, true)
            drawPath(
                segment,
                color = color,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
internal fun ChapterPage(
    chapterLink: () -> String,
    isDownloaded: Boolean,
    headers: Map<String, String>,
    contentScale: ContentScale,
    imageLoaderType: ImageLoaderType,
    colorFilter: ColorFilter? = null,
    aspectRatio: Float? = null,
    onAspectRatio: (Float) -> Unit = {},
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
                contentScale = contentScale,
                imageLoaderType = imageLoaderType,
                colorFilter = colorFilter,
                aspectRatio = aspectRatio,
                onAspectRatio = onAspectRatio,
                modifier = Modifier.fillMaxWidth()
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
    colorFilter: ColorFilter? = null,
    imageLoaderType: ImageLoaderType,
    aspectRatio: Float? = null,
    onAspectRatio: (Float) -> Unit = {},
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
        // Bumping the key recreates the loader, which requests the page again
        var retry by remember { mutableIntStateOf(0) }

        key(retry) {
            imageLoaderType.Composed(
                painter = painter,
                colorFilter = colorFilter,
                onRefresh = { retry++ },
                contentScale = contentScale,
                headers = headers,
                aspectRatio = aspectRatio,
                onAspectRatio = onAspectRatio,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun clampOffset(centerPoint: Offset, offset: Offset, scale: Float): Offset {
    val maxPosition = centerPoint * (scale - 1)

    return offset.copy(
        x = offset.x.coerceIn(-maxPosition.x, maxPosition.x),
        y = offset.y.coerceIn(-maxPosition.y, maxPosition.y)
    )
}
