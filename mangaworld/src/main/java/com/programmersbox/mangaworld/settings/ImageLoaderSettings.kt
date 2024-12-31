package com.programmersbox.mangaworld.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.panpf.zoomimage.GlideZoomAsyncImage
import com.programmersbox.mangasettings.ImageLoaderType
import com.programmersbox.mangaworld.MangaSettingsHandling
import com.programmersbox.uiviews.presentation.settings.SettingsScaffold
import com.programmersbox.uiviews.utils.AmoledProvider
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.logFirebaseMessage
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage

const val ImageLoaderSettingsRoute = "imageLoaderSettings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLoaderSettings(
    mangaSettingsHandling: MangaSettingsHandling,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    navigationButton: @Composable () -> Unit = { BackButton() },
) {
    var imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()

    LaunchedEffect(imageLoaderType) {
        logFirebaseMessage("ImageLoader Selected: $imageLoaderType")
    }

    SettingsScaffold(
        title = "Image Loader Settings",
        topBar = {
            InsetSmallTopAppBar(
                title = { Text("Image Loader Settings") },
                scrollBehavior = it,
                navigationIcon = navigationButton,
                insetPadding = windowInsets
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedCard {
                ListItem(
                    leadingContent = {
                        imageLoaderType.Composed(
                            url = "https://picsum.photos/480/360",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    },
                    headlineContent = { Text(imageLoaderType.name) },
                    overlineContent = { Text("Currently Selected Image Loader") }
                )
            }

            ImageLoaderType.entries.dropLast(1).forEach {
                ElevatedCard(
                    onClick = { imageLoaderType = it }
                ) {
                    ListItem(
                        leadingContent = {
                            it.Composed(
                                url = "https://picsum.photos/480/360",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        },
                        headlineContent = { Text(it.name) },
                        trailingContent = if (it == imageLoaderType) {
                            { Icon(Icons.Default.CheckCircleOutline, null) }
                        } else null
                    )
                }

                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ImageLoaderType.Composed(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    when (this) {
        ImageLoaderType.Kamel -> Kamel(url, modifier, contentScale)
        ImageLoaderType.Glide -> Glide(url, modifier, contentScale)
        ImageLoaderType.Coil -> Coil(url, modifier, contentScale)
        ImageLoaderType.Panpf -> Panpf(url, modifier, contentScale)
        ImageLoaderType.Telephoto -> Telephoto(url, modifier, contentScale)
        ImageLoaderType.UNRECOGNIZED -> Image(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
private fun Kamel(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (!LocalInspectionMode.current) {
        KamelImage(
            resource = { asyncPainterResource(url) },
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun Glide(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    GlideImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
private fun Coil(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .lifecycle(LocalLifecycleOwner.current)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

@OptIn(com.github.panpf.zoomimage.compose.glide.ExperimentalGlideComposeApi::class)
@Composable
private fun Panpf(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    GlideZoomAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
private fun Telephoto(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    ZoomableGlideImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
@PreviewLightDark
private fun ImageLoaderSettingsPreview(
    @PreviewParameter(AmoledProvider::class) isAmoledMode: Boolean,
) {
    PreviewTheme(
        isAmoledMode = isAmoledMode
    ) {
        ImageLoaderSettings(MangaSettingsHandling(LocalContext.current))
    }
}