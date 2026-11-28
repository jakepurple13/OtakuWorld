package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/** "Put unused tabs to sleep": who sleeps, what sleeping drops, and what it keeps. */
class TabSleepTest {

    private class FakeHolder : TabHandoff {
        var chapter: String? = null
        override fun capture(): Any? = chapter
        override fun restore(state: Any?) {
            chapter = state as String?
        }
    }

    /** Like a page ViewModel; a reader one also nulls the handoff holder when cleared. */
    private class PageViewModel(private val holder: FakeHolder? = null) : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
            holder?.chapter = null
        }
    }

    private class FakeSource : MiniPlayerSource {
        override val frame = MutableStateFlow<ImageBitmap?>(null)
        override val title = "Episode 1"
        override val playing = MutableStateFlow(true)
        override fun pause() {
            playing.value = false
        }

        override fun resume() {
            playing.value = true
        }

        override fun stop() {
            playing.value = false
        }
    }

    private var time = 0L
    private val holder = FakeHolder()
    private val controller = TabsController(
        homeNav = TabNavigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        now = { time },
        handoffs = listOf(holder),
    )
    private val main get() = controller.windows.first()
    private val homeId get() = main.tabs.first().id
    private val after = SleepAfter.M15.millis!!

    private fun tab(id: TabId) = controller.windowOf(id).tabs.first { it.id == id }

    private fun open(name: String, mode: TabOpenMode = TabOpenMode.Background): TabId {
        holder.chapter = name
        return controller.open(Screen.WebViewScreen("url-$name"), mode, homeId)
    }

    /** A page ViewModel in [id]'s page store, the way `Nav3` creates them. */
    private fun pageViewModel(id: TabId, withHolder: Boolean = false): PageViewModel {
        val owner = tab(id).viewModelStoreProvider.getOrCreateOwner("page")
        return ViewModelProvider.create(
            owner,
            viewModelFactory { initializer { PageViewModel(if (withHolder) holder else null) } },
        )[PageViewModel::class]
    }

    private fun sleepAt(t: Long, after: Long? = this.after) {
        time = t
        controller.sleepIdleTabs(time, after)
    }

    @Test
    fun neverDoesNothing() {
        val a = open("a")
        sleepAt(10 * HOUR, after = null)
        assertFalse(tab(a).sleeping.value)
    }

    @Test
    fun idleBackgroundTabSleeps() {
        val a = open("a")
        sleepAt(after - 1)
        assertFalse(tab(a).sleeping.value)
        sleepAt(after)
        assertTrue(tab(a).sleeping.value)
    }

    @Test
    fun idleTimeCountsFromWhenTheTabWasLeft() {
        val a = open("a", TabOpenMode.Foreground)
        time = 10 * MINUTE
        controller.activate(homeId)

        sleepAt(10 * MINUTE + after - 1)
        assertFalse(tab(a).sleeping.value)
        sleepAt(10 * MINUTE + after)
        assertTrue(tab(a).sleeping.value)
    }

    @Test
    fun activeTabNeverSleeps() {
        val a = open("a", TabOpenMode.Foreground)
        val b = open("b")
        // The active tab of another window, too.
        controller.tearOut(b, TabsController.DefaultWindowBounds)

        sleepAt(10 * HOUR)

        assertFalse(tab(a).sleeping.value)
        assertFalse(tab(b).sleeping.value)
    }

    @Test
    fun playingTabNeverSleeps() {
        val a = open("a")
        tab(a).playing.value = true
        sleepAt(10 * HOUR)
        assertFalse(tab(a).sleeping.value)
    }

    @Test
    fun miniPlayerTabNeverSleeps() {
        val registry = MiniPlayerRegistry(controller, scope = CoroutineScope(Dispatchers.Unconfined))
        val a = open("a", TabOpenMode.Foreground)
        val source = FakeSource()
        registry.register(a, source)
        controller.activate(homeId)
        assertEquals(a, registry.showing.value)

        sleepAt(10 * HOUR)
        assertFalse(tab(a).sleeping.value)

        // Paused, but still in the mini player.
        source.pause()
        sleepAt(20 * HOUR)
        assertFalse(tab(a).sleeping.value)
        assertEquals(a, registry.showing.value)

        registry.dismiss(a)
        sleepAt(30 * HOUR)
        assertTrue(tab(a).sleeping.value)
    }

    @Test
    fun homeNeverSleeps() {
        open("a", TabOpenMode.Foreground)
        sleepAt(10 * HOUR)
        assertFalse(tab(homeId).sleeping.value)
    }

    @Test
    fun sleepClearsViewModels() {
        val a = open("a")
        val slept = mutableListOf<TabEvent.Slept>()
        controller.addListener { if (it is TabEvent.Slept) slept += it }
        val first = pageViewModel(a)

        sleepAt(after)

        assertTrue(first.cleared)
        // Its window drops the tab's saved UI state.
        assertEquals(listOf(TabEvent.Slept(a, main.id)), slept)

        // Woken, it gets new ViewModels, and closing still clears them.
        controller.activate(a)
        val second = pageViewModel(a)
        assertNotSame(first, second)
        assertFalse(second.cleared)
        controller.close(a)
        assertTrue(second.cleared)
    }

    @Test
    fun attachedContentDefersTheClear() {
        val a = open("a")
        val vm = pageViewModel(a)
        // A crossfade frame still shows it.
        controller.attachContent(a)

        sleepAt(after)
        assertTrue(tab(a).sleeping.value)
        assertFalse(vm.cleared)

        controller.detachContent(a)
        assertTrue(vm.cleared)
    }

    @Test
    fun wakingBeforeTheDeferredClearKeepsViewModels() {
        val a = open("a")
        val vm = pageViewModel(a)
        controller.attachContent(a)
        sleepAt(after)

        controller.activate(a)
        controller.detachContent(a)

        assertFalse(tab(a).sleeping.value)
        assertFalse(vm.cleared)
    }

    @Test
    fun wakingKeepsBackStackAndSnapshot() {
        val a = open("1")
        val extra = Screen.WebViewScreen("url-1b")
        tab(a).nav.navBackStack.add(extra)
        val stack = tab(a).nav.backStack.toList()
        controller.activate(a)
        pageViewModel(a, withHolder = true)
        controller.activate(homeId)
        holder.chapter = "home"

        sleepAt(after)
        assertTrue(tab(a).sleeping.value)
        // Clearing the reader's ViewModel nulled nothing the active tab owns.
        assertEquals("home", holder.chapter)

        controller.activate(a)

        assertFalse(tab(a).sleeping.value)
        assertEquals(stack, tab(a).nav.backStack.toList())
        assertEquals("1", holder.chapter)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun sleepRunsWhenPrefChanges() = runTest {
        val pref = MutableStateFlow(SleepAfter.Never)
        val clock = { testScheduler.currentTime }
        time = clock()
        val a = open("a")
        backgroundScope.launch { controller.runSleepChecks(pref, clock) }

        advanceTimeBy(20 * MINUTE)
        runCurrent()
        assertFalse(tab(a).sleeping.value)

        // Applies at once, without waiting for the next check.
        pref.value = SleepAfter.M15
        runCurrent()
        assertTrue(tab(a).sleeping.value)

        // Then every minute.
        time = clock()
        val b = open("b")
        advanceTimeBy(after - 1)
        runCurrent()
        assertFalse(tab(b).sleeping.value)
        // The check at exactly 15 minutes idle, one interval after the one before it.
        advanceTimeBy(1)
        runCurrent()
        assertTrue(tab(b).sleeping.value)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
