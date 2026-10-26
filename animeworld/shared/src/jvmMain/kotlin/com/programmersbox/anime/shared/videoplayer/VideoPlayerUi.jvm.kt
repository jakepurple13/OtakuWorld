package com.programmersbox.anime.shared.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import uk.co.caprica.vlcj.factory.MediaPlayerFactory

@Composable
actual fun VideoPlayerUi(videoScreen: VideoScreen) {
    val storageHolder = koinInject<StorageHolder>()
    // The first load scans libVLC's plugins, so keep it off the UI thread.
    val factory by produceState<Result<MediaPlayerFactory>?>(null) {
        value = withContext(Dispatchers.IO) { VlcRuntime.factory() }
    }

    when (val result = factory) {
        null -> Box(Modifier.fillMaxSize().background(Color.Black)) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }

        else -> result.fold(
            onSuccess = { PlayerHost(it, videoScreen, remember(videoScreen) { playerSource(videoScreen, storageHolder) }) },
            onFailure = { PlayerUnavailable("The video player couldn't start: libVLC failed to load (${it.message}).") },
        )
    }
}

@Composable
private fun PlayerHost(factory: MediaPlayerFactory, videoScreen: VideoScreen, source: PlayerSource) {
    val proxy = remember(source) { LocalStreamProxy() }
    val player = remember(factory, source) { DesktopPlayer(factory, proxy) }
    DisposableEffect(player) {
        player.open(source)
        onDispose {
            player.close()
            proxy.close()
        }
    }
    DesktopPlayerScreen(player, title = videoScreen.showName)
}

/** Downloads play from their file; everything else streams the resolved [StorageHolder] video with its headers. */
private fun playerSource(videoScreen: VideoScreen, storageHolder: StorageHolder): PlayerSource {
    if (videoScreen.downloadOrStream) return PlayerSource.LocalFile(videoScreen.showPath)
    val storage = storageHolder.storageModel
    return PlayerSource.Stream(
        url = storage?.link ?: videoScreen.showPath,
        headers = playerHeaders(storage?.headers.orEmpty(), videoScreen.referer),
        mimeType = storage?.let(::streamMime),
    )
}
