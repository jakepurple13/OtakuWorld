package com.programmersbox.manga.shared.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.serialization.Serializable

@Serializable
data object ImageLoaderSettingsRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLoaderSettings(
    mangaSettingsHandling: MangaNewSettingsHandling,
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
            TopAppBar(
                title = { Text("Image Loader Settings") },
                scrollBehavior = it,
                navigationIcon = navigationButton,
                windowInsets = windowInsets
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
fun ImageLoaderType.Composed(
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

@Composable
private fun Glide(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Text("Not working right now")
    /*GlideImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )*/
}

@Composable
private fun Coil(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
private fun Panpf(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Text("Not working right now")
    /*GlideZoomAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )*/
}

@Composable
private fun Telephoto(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Text("Not working right now")
    /*ZoomableGlideImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )*/
}