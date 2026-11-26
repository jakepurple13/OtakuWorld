package com.programmersbox.anime.shared.videoplayer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.tabs.MiniPlayerRegistry
import com.programmersbox.kmpuiviews.tabs.MiniPlayerSource
import com.programmersbox.kmpuiviews.tabs.TabId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uk.co.caprica.vlcj.factory.MediaPlayerFactory

/**
 * Owns a tab's [DesktopPlayer] and its [LocalStreamProxy], so parking the tab (which disposes the player screen)
 * keeps the video going. Lives in the tab's ViewModel store; clearing it (the episode left the tab's back stack,
 * or the tab closed) unregisters the mini player and closes both.
 */
internal class PlayerHolderViewModel(
    factory: MediaPlayerFactory,
    source: PlayerSource,
    title: String,
) : ViewModel() {
    private val proxy = LocalStreamProxy()
    val player = DesktopPlayer(factory, proxy).apply { open(source) }

    private var registered: Pair<TabId, MiniPlayerRegistry>? = null

    private val miniSource = object : MiniPlayerSource {
        override val frame: StateFlow<ImageBitmap?> = player.frame
        override val title: String = title
        override val playing: StateFlow<Boolean> = player.state
            .map { it.playing }
            .stateIn(viewModelScope, SharingStarted.Eagerly, player.state.value.playing)

        override fun pause() = player.pause()
        override fun resume() = player.resume()

        // Pausing keeps the position, so returning to the tab picks up where the × left it.
        override fun stop() = player.pause()
    }

    /** Called while the player screen is composed for [tab]; the registry ignores a repeat. */
    fun attach(tab: TabId, registry: MiniPlayerRegistry) {
        registered = tab to registry
        registry.register(tab, miniSource)
    }

    override fun onCleared() {
        registered?.let { (tab, registry) -> registry.unregister(tab) }
        player.close()
        proxy.close()
    }
}
