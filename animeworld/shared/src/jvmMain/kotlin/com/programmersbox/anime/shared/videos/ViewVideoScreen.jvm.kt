package com.programmersbox.anime.shared.videos

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.videoexport.VideoExportRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import org.koin.compose.koinInject
import java.io.File

@Composable
internal actual fun VideoPermissionGate(content: @Composable () -> Unit) {
    // No runtime permissions are needed on desktop - the video library is just a scan of
    // user-configured folders.
    content()
}

/** Where a desktop thumbnail is: libVLC grabs can take a while, so loading is shown. */
private sealed interface ThumbnailState {
    data object Loading : ThumbnailState
    data class Loaded(val image: ImageBitmap) : ThumbnailState
    data object Unavailable : ThumbnailState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun VideoThumbnail(path: String, modifier: Modifier) {
    val source = koinInject<VideoLibrarySource>()
    // libVLC grabs the frame (cached on disk); decoding stays off the main thread.
    val thumbnail by produceState<ThumbnailState>(ThumbnailState.Loading, path) {
        value = withContext(Dispatchers.IO) {
            source.thumbnailer.thumbnail(File(path))
                ?.let { file -> runCatching { SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap() }.getOrNull() }
                ?.let { ThumbnailState.Loaded(it) }
                ?: ThumbnailState.Unavailable
        }
    }

    Crossfade(targetState = thumbnail, label = "videoThumbnail", modifier = modifier) { state ->
        when (state) {
            is ThumbnailState.Loaded -> Image(
                bitmap = state.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (state == ThumbnailState.Loading) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

internal actual fun videoExportRoute(path: String): NavKey? = VideoExportRoute(path)
