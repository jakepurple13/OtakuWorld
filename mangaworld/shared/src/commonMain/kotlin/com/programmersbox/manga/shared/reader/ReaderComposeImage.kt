package com.programmersbox.manga.shared.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfigBuilder
import io.kamel.core.loadImageBitmapResource
import io.kamel.image.asyncPainterResource
import io.kamel.image.config.LocalKamelConfig
import io.ktor.client.request.header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ImageLoaderType.Composed(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    contentScale: ContentScale = ContentScale.Fit,
    aspectRatio: Float? = null,
    onAspectRatio: (Float) -> Unit = {},
    onRefresh: () -> Unit,
) {
    when (this) {
        ImageLoaderType.Kamel -> Kamel(painter, headers, modifier, contentScale, colorFilter, aspectRatio, onAspectRatio, onRefresh)
        ImageLoaderType.Glide -> Glide(painter, headers, modifier, contentScale, onRefresh)
        ImageLoaderType.Telephoto -> Telephoto(painter, headers, modifier, contentScale, onRefresh)
        else -> Image(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/**
 * When [aspectRatio] is set (list reader), the page reserves that height while loading so the list
 * doesn't jump when the image arrives, then sizes itself to the loaded image's real ratio.
 */
@Composable
internal fun Kamel(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
    aspectRatio: Float? = null,
    onAspectRatio: (Float) -> Unit = {},
    onRefresh: () -> Unit,
) {
    val resource = asyncPainterResource(painter) {
        requestBuilder {
            headers.forEach { (t, u) -> header(t, u) }
        }
    }

    val placeholderSize = if (aspectRatio != null) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    } else {
        Modifier
            .fillMaxSize()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
    }

    when (resource) {
        is Resource.Loading -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.then(placeholderSize)
        ) {
            val progress by animateFloatAsState(targetValue = resource.progress, label = "")
            PageLoadSkeleton(progress = { progress }, modifier = Modifier.matchParentSize())
        }

        is Resource.Success -> {
            val image = resource.value
            val imageRatio = image.intrinsicSize
                .let { it.width / it.height }
                .takeIf { it.isFinite() && it > 0f }
            LaunchedEffect(imageRatio) { imageRatio?.let(onAspectRatio) }

            // Memory-cache hits (scrolling back, prefetched pages) show instantly; only fresh loads fade in
            val start = remember(painter) { pageRevealStart(resource.source) }
            val alpha = remember(painter) { Animatable(start.alpha) }
            val scale = remember(painter) { Animatable(start.scale) }
            val alphaSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
            val scaleSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
            LaunchedEffect(alpha, scale) {
                launch { alpha.animateTo(1f, alphaSpec) }
                launch { scale.animateTo(1f, scaleSpec) }
            }

            Image(
                painter = image,
                contentDescription = null,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = modifier
                    .then(
                        if (aspectRatio != null && imageRatio != null) {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(imageRatio)
                        } else {
                            placeholderSize
                        }
                    )
                    .clipToBounds()
                    .graphicsLayer {
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
        }

        is Resource.Failure -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
        ) {
            LaunchedEffect(Unit) { resource.exception.printStackTrace() }
            PageLoadFailed(
                message = resource.exception.message,
                onRetry = onRefresh,
            )
        }
    }
}

/**
 * Failed page: shakes once when it first shows, and spins the retry icon from the tap until
 * the resource leaves the failure state (which removes this composable).
 */
@Composable
internal fun PageLoadFailed(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 500
                -6f at 150
                6f at 250
                -3f at 350
                0f at 500
            }
        )
    }
    var retrying by remember { mutableStateOf(false) }
    val rotation = if (retrying) {
        rememberInfiniteTransition()
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing))
            )
            .value
    } else {
        0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                retrying = true
                onRetry()
            }
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Retry",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
        Text("Page didn't load", style = MaterialTheme.typography.titleSmall)
        Text(
            "Tap to retry",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Decodes the upcoming [urls] into Kamel's memory cache so pages are ready before they scroll into view.
 * Each url is requested once per reader session; failures are retried the next time they're upcoming.
 */
@Composable
internal fun PrefetchPages(
    urls: List<String>,
    headers: Map<String, String>,
    onAspectRatio: (url: String, ratio: Float) -> Unit,
) {
    val kamelConfig = LocalKamelConfig.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val requested = remember { mutableSetOf<String>() }

    LaunchedEffect(urls) {
        urls
            .filter { requested.add(it) }
            .forEach { url ->
                scope.launch {
                    val config = ResourceConfigBuilder(coroutineContext)
                        .apply {
                            this.density = density
                            requestBuilder { headers.forEach { (t, u) -> header(t, u) } }
                        }
                        .build()

                    // Kamel decodes in the collector's context, so collect off the main thread
                    val result = withContext(Dispatchers.Default) {
                        kamelConfig.loadImageBitmapResource(url, config).first { it !is Resource.Loading }
                    }
                    when (result) {
                        is Resource.Success -> onAspectRatio(url, result.value.width.toFloat() / result.value.height)
                        else -> requested.remove(url)
                    }
                }
            }
    }
}

@Composable
internal fun Glide(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    /*val url = remember(painter) { GlideUrl(painter) { headers } }
    GlideImage(
        imageModel = { url },
        imageOptions = ImageOptions(contentScale = contentScale),
        loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
        failure = {
            Text(
                stringResource(R.string.pressToRefresh),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { onRefresh() }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
            .clipToBounds()
    )*/
}

@Composable
internal fun Panpf(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    /*Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
    ) {
        val state = rememberGlideZoomState()
        LaunchedEffect(Unit) {
            state.zoomable.readMode = ReadMode(ReadMode.SIZE_TYPE_VERTICAL, ReadMode.Decider.Default)
        }

        GlideZoomAsyncImage(
            model = remember(painter) { GlideUrl(painter) { headers } },
            zoomState = state,
            contentDescription = null,
            contentScale = contentScale,
            scrollBar = null,
            modifier = Modifier.fillMaxWidth()
        )
    }*/
}

@Composable
internal fun Telephoto(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    /*val url = remember(painter) { GlideUrl(painter) { headers } }

    val state = rememberZoomableImageState()

    if (state.isImageDisplayed) {
        CircularProgressIndicator()
    }

    ZoomableGlideImage(
        model = url,
        state = state,
        contentDescription = null,
        contentScale = contentScale,
        gesturesEnabled = false,
        modifier = modifier
            .fillMaxSize()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
    )*/
}
/** Panel-shaped shimmer that holds a manga page's layout while it loads, with the wavy progress ring on top. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageLoadSkeleton(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val panel = Modifier.m3placeholder(
        visible = true,
        shape = MaterialTheme.shapes.extraSmall,
        highlight = PlaceholderHighlight.shimmer(),
    )
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Box(panel.fillMaxWidth().weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().weight(1.2f)
            ) {
                Box(panel.fillMaxHeight().weight(1.3f))
                Box(panel.fillMaxHeight().weight(1f))
            }
            Box(panel.fillMaxWidth().weight(.9f))
        }
        CircularWavyProgressIndicator(progress = progress)
    }
}
