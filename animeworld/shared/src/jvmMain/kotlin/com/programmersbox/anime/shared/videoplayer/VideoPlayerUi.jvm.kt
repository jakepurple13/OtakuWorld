package com.programmersbox.anime.shared.videoplayer

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import com.programmersbox.kmpuiviews.theme.SeedColorTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.tabs.LocalTabId
import com.programmersbox.kmpuiviews.tabs.MiniPlayerRegistry
import com.programmersbox.kmpuiviews.tabs.TabId
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import java.io.File

@Composable
actual fun VideoPlayerUi(videoScreen: VideoScreen) {
    SeedColorTheme(videoScreen.seedColor) { VideoPlayerContent(videoScreen) }
}

@Composable
private fun VideoPlayerContent(videoScreen: VideoScreen) {
    val storageHolder = koinInject<StorageHolder>()
    // The first load scans libVLC's plugins, so keep it off the UI thread.
    // Already loaded when a parked tab returns, so it goes straight back to its player.
    val factory by produceState(VlcRuntime.loadedFactory()) {
        value = withContext(Dispatchers.IO) { VlcRuntime.factory() }
    }
    // DesktopPlayerScreen ends the loading overlay's hand-off; without libVLC it never shows, so end it here.
    val genericInfo = koinInject<KmpGenericInfo>()
    LaunchedEffect(factory) {
        if (factory?.isFailure == true) (genericInfo as? GenericSharedAnime)?.episodeLoader?.finishHandoff()
    }

    // Black behind both states so the swap from the startup spinner to the player never flashes
    Crossfade(
        targetState = factory,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) { result ->
        when (result) {
            null -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }

            else -> result.fold(
                onSuccess = {
                    val source = remember(videoScreen) { playerSource(videoScreen, storageHolder) }
                    val subtitles = remember(source) { externalSubtitles(source, storageHolder) }
                    PlayerHost(it, videoScreen, source, subtitles, storageHolder.storageModel?.headers.orEmpty())
                },
                onFailure = { PlayerUnavailable("The video player couldn't start: libVLC failed to load (${it.message}).") },
            )
        }
    }
}

@Composable
private fun PlayerHost(factory: MediaPlayerFactory, videoScreen: VideoScreen, source: PlayerSource, subtitles: List<KmpSubtitle>, streamHeaders: Map<String, String>) {
    val tab = LocalTabId.current
    val player = if (tab == null) rememberScreenPlayer(factory, source) else rememberTabPlayer(tab, factory, source, videoScreen)
    DesktopPlayerScreen(
        player,
        title = videoScreen.showName,
        externalSubtitles = subtitles,
        referer = videoScreen.referer,
        streamHeaders = streamHeaders,
    )
}

/** Outside the tab shell: the player lives as long as the screen. */
@Composable
private fun rememberScreenPlayer(factory: MediaPlayerFactory, source: PlayerSource): DesktopPlayer {
    val proxy = remember(source) { LocalStreamProxy() }
    val player = remember(factory, source) { DesktopPlayer(factory, proxy) }
    DisposableEffect(player) {
        player.open(source)
        onDispose {
            player.close()
            proxy.close()
        }
    }
    return player
}

/**
 * In a tab: the player lives in the tab's ViewModel store, one per episode, so it keeps playing (in the mini
 * player) while the tab is parked. It is registered while this screen is composed and unregistered when cleared.
 */
@Composable
private fun rememberTabPlayer(tab: TabId, factory: MediaPlayerFactory, source: PlayerSource, videoScreen: VideoScreen): DesktopPlayer {
    val holder = viewModel(key = "player:$videoScreen") { PlayerHolderViewModel(factory, source, videoScreen.showName) }
    val registry = koinInject<MiniPlayerRegistry>()
    LaunchedEffect(holder, tab) { holder.attach(tab, registry) }
    return holder.player
}

/** A download's sidecar files, else the subtitles the source returned. */
private fun externalSubtitles(source: PlayerSource, storageHolder: StorageHolder): List<KmpSubtitle> = when (source) {
    is PlayerSource.LocalFile -> OfflineSubtitles.find(File(source.path))
    is PlayerSource.Stream -> storageHolder.storageModel?.subtitles.orEmpty()
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
