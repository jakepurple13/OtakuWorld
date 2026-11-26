package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** A tab's video, as the mini player sees it. AnimeWorld adapts its player to this; nothing here knows vlcj. */
interface MiniPlayerSource {
    /** The latest decoded frame. */
    val frame: StateFlow<ImageBitmap?>
    val title: String

    /** The player's real playing state; the tab's speaker icon follows it. */
    val playing: StateFlow<Boolean>
    fun pause()
    fun resume()
    fun stop()
}

/**
 * The app's one mini player. A tab registers its video while its player screen is composed and unregisters when
 * the player is cleared (not when the screen leaves: parking a tab disposes the screen).
 *
 * Leaving a playing tab shows it in the mini player of the window that owns the tab ([enabled]), or pauses it.
 * A second tab taking over pauses the first. Returning to the tab hides the mini player, and closing the tab
 * stops it. The mini player follows its tab across [TabsController.move] and [TabsController.tearOut].
 */
class MiniPlayerRegistry(
    internal val controller: TabsController,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    /**
     * "Mini player when leaving a playing tab"; off pauses the video instead. Turning it off pauses and hides the
     * mini player. Set it on the UI thread.
     */
    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) _showing.value?.let { tab ->
                entries[tab]?.source?.pause()
                _showing.value = null
            }
        }

    /** "Pause when the window loses focus": the app losing focus pauses the mini player, which stays paused. */
    @Volatile
    var pauseUnfocused: Boolean = false

    private class Entry(val source: MiniPlayerSource, var onScreen: Boolean) {
        var job: Job? = null
    }

    private val entries = mutableMapOf<TabId, Entry>()

    private val _showing = MutableStateFlow<TabId?>(null)

    /** The tab whose video is in the mini player, if any. */
    val showing: StateFlow<TabId?> = _showing.asStateFlow()

    /** The window that owns [showing]'s tab; only that window draws the mini player. */
    val showingWindow: WindowId? get() = _showing.value?.let { windowOf(it)?.id }

    /** Where a drag moved the mini player from its corner, in px. Session only; the corner setting is unchanged. */
    val drag = mutableStateOf(Offset.Zero)

    /** App windows that have focus now. */
    private val focusedWindows = mutableSetOf<WindowId>()

    init {
        controller.addListener { event ->
            if (event is TabEvent.Closed) entries[event.tab]?.let { stopAndForget(event.tab, it) }
            reconcile()
        }
    }

    fun register(tab: TabId, source: MiniPlayerSource) {
        if (entries[tab]?.source === source) return
        entries.remove(tab)?.job?.cancel()
        val state = tabState(tab) ?: return
        state.playing.value = source.playing.value
        // In the map before collecting: the first value may arrive at once.
        val entry = Entry(source, onScreen = isOnScreen(tab) ?: true)
        entries[tab] = entry
        entry.job = scope.launch {
            source.playing.collect {
                state.playing.value = it
                if (it) startedPlaying(tab)
            }
        }
    }

    fun unregister(tab: TabId) {
        val entry = entries.remove(tab) ?: return
        entry.job?.cancel()
        tabState(tab)?.playing?.value = false
        if (_showing.value == tab) _showing.value = null
    }

    /** The mini player's ×: stops the video and takes it out of the mini player. */
    fun dismiss(tab: TabId) {
        entries[tab]?.let { stopAndForget(tab, it) }
    }

    /** True while [tab]'s video plays. A playing tab must never be put to sleep. */
    fun isPlaying(tab: TabId): Boolean = entries[tab]?.source?.playing?.value == true

    fun sourceOf(tab: TabId): MiniPlayerSource? = entries[tab]?.source

    /**
     * An app window gained or lost focus. When none is focused any more and [pauseUnfocused] is on, the mini player
     * pauses. Callers report a loss a moment late, so focus moving between app windows never looks like the app
     * losing it.
     */
    fun windowFocusChanged(window: WindowId, focused: Boolean) {
        if (focused) {
            focusedWindows += window
            return
        }
        focusedWindows -= window
        if (focusedWindows.isEmpty() && pauseUnfocused) _showing.value?.let { entries[it]?.source?.pause() }
    }

    /** A window closed; it no longer counts as focused, and closing it is not the app losing focus. */
    fun forgetWindow(window: WindowId) {
        focusedWindows -= window
    }

    private fun stopAndForget(tab: TabId, entry: Entry) {
        entry.source.stop()
        unregister(tab)
    }

    private fun reconcile() {
        for ((tab, entry) in entries.toList()) {
            // Mid-move the tab is in no window; the move's own event settles it.
            val onScreen = isOnScreen(tab) ?: continue
            if (onScreen == entry.onScreen) continue
            entry.onScreen = onScreen
            if (onScreen) {
                if (_showing.value == tab) _showing.value = null
            } else {
                left(tab, entry)
            }
        }
    }

    /**
     * Only one video is audible. On screen, a tab that starts playing pauses the mini player (which stays, paused).
     * Parked, it was left while opening or buffering, or something resumed it: it goes to the mini player as if it
     * were left now, or pauses.
     */
    private fun startedPlaying(tab: TabId) {
        val entry = entries[tab] ?: return
        val onScreen = isOnScreen(tab) ?: return
        if (onScreen) {
            _showing.value?.takeIf { it != tab }?.let { entries[it]?.source?.pause() }
        } else if (_showing.value != tab) {
            left(tab, entry)
        }
    }

    private fun left(tab: TabId, entry: Entry) {
        if (!entry.source.playing.value) return
        if (!enabled) {
            entry.source.pause()
            return
        }
        _showing.value?.takeIf { it != tab }?.let { entries[it]?.source?.pause() }
        _showing.value = tab
    }

    private fun windowOf(tab: TabId) = controller.windows.firstOrNull { w -> w.tabs.any { it.id == tab } }

    private fun tabState(tab: TabId) = windowOf(tab)?.tabs?.firstOrNull { it.id == tab }

    /** Whether [tab] is its window's active tab, or null when it is in no window. */
    private fun isOnScreen(tab: TabId): Boolean? = windowOf(tab)?.let { it.activeId.value == tab }
}

/**
 * The mini player's top-left in its container: [corner] with [inset], moved by [drag], and kept inside the
 * container. All values are px.
 */
fun miniPlayerOffset(corner: MiniPlayerCorner, container: IntSize, player: IntSize, inset: Int, drag: Offset): IntOffset {
    val left = corner == MiniPlayerCorner.TopLeft || corner == MiniPlayerCorner.BottomLeft
    val top = corner == MiniPlayerCorner.TopLeft || corner == MiniPlayerCorner.TopRight
    val x = if (left) inset else container.width - player.width - inset
    val y = if (top) inset else container.height - player.height - inset
    return IntOffset(
        x = (x + drag.x).roundToInt().coerceIn(0, max(0, container.width - player.width)),
        y = (y + drag.y).roundToInt().coerceIn(0, max(0, container.height - player.height)),
    )
}
