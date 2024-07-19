package com.programmersbox.mangaworld.reader.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.size.Size
import com.bumptech.glide.load.model.GlideUrl
import com.github.panpf.zoomimage.GlideZoomAsyncImage
import com.github.panpf.zoomimage.compose.glide.internal.ExperimentalGlideComposeApi
import com.github.panpf.zoomimage.zoom.ReadMode
import com.programmersbox.mangasettings.ImageLoaderType
import com.programmersbox.mangaworld.R
import com.programmersbox.uiviews.utils.ComposableUtils
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.request.header
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

@Composable
internal fun ImageLoaderType.Composed(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    when (this) {
        ImageLoaderType.Kamel -> Kamel(painter, headers, modifier, contentScale, onRefresh)
        ImageLoaderType.Glide -> Glide(painter, headers, modifier, contentScale, onRefresh)
        ImageLoaderType.Coil -> Coil(painter, headers, modifier, contentScale, onRefresh)
        ImageLoaderType.Telephoto -> Telephoto(painter, headers, modifier, contentScale, onRefresh)
        else -> Image(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
internal fun Kamel(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    KamelImage(
        resource = {
            asyncPainterResource(painter) {
                requestBuilder {
                    headers.forEach { (t, u) -> header(t, u) }
                }
            }
        },
        onLoading = {
            val progress by animateFloatAsState(targetValue = it, label = "")
            CircularProgressIndicator(progress = { progress })
        },
        onFailure = {
            Text(
                stringResource(R.string.pressToRefresh),
                modifier = Modifier.clickable { onRefresh() }
            )
        },
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
            .clipToBounds()
    )
}

@Composable
internal fun Glide(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    val url = remember(painter) { GlideUrl(painter) { headers } }
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
    )
}

@Composable
internal fun Coil(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(painter)
            .apply { headers.forEach { addHeader(it.key, it.value) } }
            .lifecycle(LocalLifecycleOwner.current)
            .crossfade(true)
            .size(Size.ORIGINAL)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxSize()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
            .clipToBounds()
    ) {
        val state = this.painter.state
        when (state) {
            is AsyncImagePainter.State.Error -> Text(
                stringResource(R.string.pressToRefresh),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { onRefresh() }
            )

            is AsyncImagePainter.State.Loading, AsyncImagePainter.State.Empty -> CircularProgressIndicator()
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun Panpf(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
    ) {
        val state = com.github.panpf.zoomimage.compose.rememberZoomState()
        LaunchedEffect(Unit) {
            state.zoomable.readMode = ReadMode(ReadMode.SIZE_TYPE_VERTICAL, ReadMode.Decider.Default)
        }

        GlideZoomAsyncImage(
            model = remember(painter) { GlideUrl(painter) { headers } },
            state = state,
            contentDescription = null,
            contentScale = contentScale,
            scrollBar = null,
            onTap = { onClick() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun Telephoto(
    painter: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onRefresh: () -> Unit,
) {
    val url = remember(painter) { GlideUrl(painter) { headers } }

    val state = rememberZoomableImageState()

    if (state.isImageDisplayed) {
        CircularProgressIndicator()
    }

    /*SubSamplingImage(
        state = rememberSubSamplingImageState(
            imageSource = SubSamplingImageSource.file(""),
            zoomableState = rememberZoomableState()
        ),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
    )*/

    ZoomableGlideImage(
        model = url,
        state = state,
        contentDescription = null,
        contentScale = contentScale,
        gesturesEnabled = false,
        modifier = modifier
            .fillMaxSize()
            .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
    )
}