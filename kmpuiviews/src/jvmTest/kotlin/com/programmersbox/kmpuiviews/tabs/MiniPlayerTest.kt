package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MiniPlayerTest {

    /** Stands in for AnimeWorld's player adapter; no vlcj. */
    private class FakeSource(override val title: String = "Episode 1") : MiniPlayerSource {
        override val frame = MutableStateFlow<ImageBitmap?>(null)
        override val playing = MutableStateFlow(true)
        var stopped = false

        override fun pause() {
            playing.value = false
        }

        override fun resume() {
            playing.value = true
        }

        override fun stop() {
            stopped = true
            playing.value = false
        }
    }

    private val controller = TabsController(
        homeNav = Navigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
    )
    private val registry = MiniPlayerRegistry(controller, scope = CoroutineScope(Dispatchers.Unconfined))
    private val main get() = controller.windows.first()
    private val homeId get() = main.tabs.first().id

    private fun tab(id: TabId) = controller.windowOf(id).tabs.first { it.id == id }

    /** Opens an episode in a foreground tab and starts it playing there. */
    private fun play(name: String, source: FakeSource = FakeSource(name)): Pair<TabId, FakeSource> {
        val id = controller.open(Screen.WebViewScreen("url-$name"), TabOpenMode.Foreground, homeId)
        registry.register(id, source)
        return id to source
    }

    @Test
    fun registeringFlagsTheTabFromThePlayersState() {
        val (a, source) = play("a")
        assertTrue(tab(a).playing.value)
        assertTrue(registry.isPlaying(a))

        source.pause()
        assertFalse(tab(a).playing.value)
        assertFalse(registry.isPlaying(a))

        source.resume()
        registry.unregister(a)
        assertFalse(tab(a).playing.value)
        assertFalse(registry.isPlaying(a))
    }

    @Test
    fun leavingPlayingTabShowsMini() {
        val (a, source) = play("a")
        controller.activate(homeId)

        assertEquals(a, registry.showing.value)
        assertEquals(main.id, registry.showingWindow)
        assertTrue(source.playing.value)
    }

    @Test
    fun leavingPausedTabShowsNothing() {
        val (a, source) = play("a")
        source.pause()
        controller.activate(homeId)

        assertNull(registry.showing.value)
    }

    @Test
    fun leavingWithPrefOffPauses() {
        registry.enabled = false
        val (_, source) = play("a")
        controller.activate(homeId)

        assertNull(registry.showing.value)
        assertFalse(source.playing.value)
    }

    @Test
    fun secondPlayingTabTakesOverAndPausesFirst() {
        val (a, first) = play("a")
        // Opening b in the foreground leaves a.
        val (b, second) = play("b")
        assertEquals(a, registry.showing.value)
        // b playing in the foreground pauses a; it stays in the mini player.
        assertFalse(first.playing.value)

        controller.activate(homeId)

        assertEquals(b, registry.showing.value)
        assertFalse(first.playing.value)
        assertTrue(second.playing.value)
    }

    @Test
    fun resumingAForegroundTabPausesTheMiniPlayer() {
        val (_, first) = play("a")
        val (_, second) = play("b", FakeSource("b").apply { playing.value = false })
        assertTrue(first.playing.value)

        second.resume()

        assertFalse(first.playing.value)
        assertTrue(second.playing.value)
    }

    @Test
    fun parkedTabThatStartsPlayingGoesToTheMiniPlayer() {
        val (a, source) = play("a")
        source.pause()
        controller.activate(homeId)
        assertNull(registry.showing.value)

        // Opening or buffering finished, or a dialog's resume ran, after the tab was parked.
        source.resume()

        assertEquals(a, registry.showing.value)
        assertTrue(source.playing.value)
    }

    @Test
    fun parkedTabThatStartsPlayingWithPrefOffIsPaused() {
        registry.enabled = false
        val (_, source) = play("a")
        source.pause()
        controller.activate(homeId)

        source.resume()

        assertNull(registry.showing.value)
        assertFalse(source.playing.value)
    }

    @Test
    fun twoParkedTabsStartingLeavesOnlyTheNewestAudible() {
        val (_, first) = play("a")
        first.pause()
        val (b, second) = play("b")
        second.pause()
        controller.activate(homeId)

        first.resume()
        second.resume()

        assertEquals(b, registry.showing.value)
        assertFalse(first.playing.value)
        assertTrue(second.playing.value)
    }

    @Test
    fun turningThePrefOffPausesAndHidesTheMiniPlayer() {
        val (_, source) = play("a")
        controller.activate(homeId)

        registry.enabled = false

        assertNull(registry.showing.value)
        assertFalse(source.playing.value)
    }

    @Test
    fun returningHidesMini() {
        val (a, source) = play("a")
        controller.activate(homeId)
        controller.activate(a)

        assertNull(registry.showing.value)
        assertTrue(source.playing.value)
    }

    @Test
    fun closingPlayingTabStopsMiniPlayer() {
        val (a, source) = play("a")
        controller.activate(homeId)
        controller.close(a)

        assertTrue(source.stopped)
        assertNull(registry.showing.value)
        assertFalse(registry.isPlaying(a))
    }

    @Test
    fun closingTheMiniPlayerStopsAndUnregisters() {
        val (a, source) = play("a")
        controller.activate(homeId)
        registry.dismiss(a)

        assertTrue(source.stopped)
        assertNull(registry.showing.value)
        assertFalse(tab(a).playing.value)
    }

    @Test
    fun miniFollowsTabOnMove() {
        val (a, _) = play("a")
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        // Torn out, it is still on screen.
        assertNull(registry.showing.value)

        // Leaving it in its new window shows the mini player there.
        val b = controller.open(Screen.WebViewScreen("url-b"), TabOpenMode.Foreground, a)
        assertEquals(w2, controller.windowOf(b).id)
        assertEquals(a, registry.showing.value)
        assertEquals(w2, registry.showingWindow)

        // Moved back, it shows in the main window when left again.
        controller.move(a, main.id, 1)
        assertNull(registry.showing.value)
        controller.activate(homeId)
        assertEquals(a, registry.showing.value)
        assertEquals(main.id, registry.showingWindow)
    }

    @Test
    fun pauseOnFocusLossPausesOnlyWhenEnabled() {
        val (_, source) = play("a")
        controller.activate(homeId)

        registry.windowFocusChanged(main.id, focused = false)
        assertTrue(source.playing.value)

        registry.windowFocusChanged(main.id, focused = true)
        registry.pauseUnfocused = true
        registry.windowFocusChanged(main.id, focused = false)
        assertFalse(source.playing.value)

        // It doesn't resume on its own.
        registry.windowFocusChanged(main.id, focused = true)
        assertFalse(source.playing.value)
    }

    @Test
    fun focusMovingBetweenAppWindowsDoesNotPause() {
        registry.pauseUnfocused = true
        val (a, source) = play("a")
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        controller.open(Screen.WebViewScreen("url-b"), TabOpenMode.Foreground, a)

        registry.windowFocusChanged(main.id, focused = true)
        registry.windowFocusChanged(w2, focused = true)
        registry.windowFocusChanged(main.id, focused = false)
        assertTrue(source.playing.value)
    }

    @Test
    fun cornerPlacementMath() {
        val container = IntSize(1000, 800)
        val size = IntSize(250, 180)
        fun at(corner: MiniPlayerCorner, drag: Offset = Offset.Zero) =
            miniPlayerOffset(corner, container, size, inset = 16, drag = drag)

        assertEquals(IntOffset(734, 604), at(MiniPlayerCorner.BottomRight))
        assertEquals(IntOffset(16, 604), at(MiniPlayerCorner.BottomLeft))
        assertEquals(IntOffset(734, 16), at(MiniPlayerCorner.TopRight))
        assertEquals(IntOffset(16, 16), at(MiniPlayerCorner.TopLeft))
        // Dragging moves it, but never out of the window.
        assertEquals(IntOffset(634, 584), at(MiniPlayerCorner.BottomRight, Offset(-100f, -20f)))
        assertEquals(IntOffset(0, 620), at(MiniPlayerCorner.BottomRight, Offset(-5000f, 5000f)))
    }
}
