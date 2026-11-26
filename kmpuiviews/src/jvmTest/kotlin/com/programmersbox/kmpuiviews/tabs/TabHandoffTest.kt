package com.programmersbox.kmpuiviews.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Reader tabs hand their chapter over through app singletons; each tab keeps its own copy. */
class TabHandoffTest {

    /** Stands in for an app holder such as `ChapterHolder`. */
    private class FakeHolder : TabHandoff {
        var chapter: String? = null
        override fun capture(): Any? = chapter
        override fun restore(state: Any?) {
            chapter = state as String?
        }
    }

    /** Like a reader ViewModel, which nulls the holder when it is cleared. */
    private class ReaderViewModel(private val holder: FakeHolder) : ViewModel() {
        override fun onCleared() {
            holder.chapter = null
        }
    }

    private val holder = FakeHolder()
    private val home = Navigation3Actions(Screen.RecentScreen)
    private val controller = TabsController(
        homeNav = home,
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        handoffs = listOf(holder),
    )
    private val main get() = controller.windows.first()
    private val homeId get() = main.tabs.first().id

    private fun reader(chapter: String) = Screen.WebViewScreen("url-$chapter")

    /** What a chapter click does: write the holder, then navigate. */
    private fun clickChapter(chapter: String, mode: TabOpenMode): TabId {
        holder.chapter = chapter
        return controller.open(reader(chapter), mode, homeId)
    }

    private fun readerOpened(tab: TabId) {
        val owner = controller.windowOf(tab).tabs.first { it.id == tab }.viewModelStoreOwner
        ViewModelProvider.create(owner, viewModelFactory { initializer { ReaderViewModel(holder) } })[ReaderViewModel::class]
    }

    @Test
    fun capturesOnOpenAndRestoresOnActivate() {
        val one = clickChapter("1", TabOpenMode.Background)
        holder.chapter = "home"

        controller.activate(one)

        assertEquals("1", holder.chapter)
    }

    @Test
    fun backgroundTabsKeepTheirOwnSnapshots() {
        val one = clickChapter("1", TabOpenMode.Background)
        val two = clickChapter("2", TabOpenMode.Background)

        controller.activate(one)
        assertEquals("1", holder.chapter)

        controller.activate(two)
        assertEquals("2", holder.chapter)

        controller.activate(one)
        assertEquals("1", holder.chapter)
    }

    @Test
    fun leavingATabKeepsItsLatestState() {
        val one = clickChapter("1", TabOpenMode.Foreground)
        // The reader moved on to the next chapter.
        holder.chapter = "1-next"

        controller.activate(homeId)
        holder.chapter = "home"
        controller.activate(one)

        assertEquals("1-next", holder.chapter)
    }

    @Test
    fun closingOneTabDoesNotAffectAnother() {
        val one = clickChapter("1", TabOpenMode.Background)
        val two = clickChapter("2", TabOpenMode.Background)
        controller.activate(two)
        readerOpened(two)
        controller.activate(one)
        readerOpened(one)

        // Closing a parked reader clears its ViewModel, which nulls the holder; the active tab keeps its chapter.
        controller.close(two)
        assertEquals("1", holder.chapter)

        val three = clickChapter("3", TabOpenMode.Background)
        controller.close(one)
        // The neighbor is active now and has its own chapter back.
        assertEquals(three, main.activeId.value)
        assertEquals("3", holder.chapter)
    }

    @Test
    fun nonReaderTabsKeepTheHolderUnchanged() {
        holder.chapter = "home"
        val settings = controller.open(Screen.Settings, TabOpenMode.Foreground, homeId)
        controller.activate(homeId)
        controller.activate(settings)

        assertEquals("home", holder.chapter)
        controller.close(settings)
        assertEquals("home", holder.chapter)
    }

    @Test
    fun navigatingInAnUnfocusedWindowDoesNotOverwriteOtherTabs() {
        val one = clickChapter("1", TabOpenMode.NewWindow)
        controller.activate(one)
        assertEquals("1", holder.chapter)

        // A chapter click in the main window's Home tab, while the other window has focus.
        holder.chapter = "home"
        home.navigate(reader("home"))
        assertEquals("home", holder.chapter)

        controller.activate(one)
        assertEquals("1", holder.chapter)
        controller.activate(homeId)
        assertEquals("home", holder.chapter)
    }

    @Test
    fun noHandoffsIsANoOp() {
        val plain = TabsController(
            homeNav = Navigation3Actions(Screen.RecentScreen),
            keys = TabKeys(listOf(CoreTabKeyProvider)),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        )
        val id = plain.open(reader("1"), TabOpenMode.Background, plain.windows.first().tabs.first().id)
        plain.activate(id)
        assertNull(holder.chapter)
    }

    @Test
    fun reopenedTabGetsItsOwnChapterBack() {
        val one = clickChapter("1", TabOpenMode.Background)
        val two = clickChapter("2", TabOpenMode.Background)
        controller.activate(one)
        readerOpened(one)
        controller.activate(two)
        readerOpened(two)

        // Close the active reader, then the parked one.
        controller.close(two)
        controller.close(one)
        holder.chapter = "home"

        val reopenedOne = controller.reopenClosed(main.id)!!
        assertEquals(reader("1"), main.tabs.first { it.id == reopenedOne }.current)
        assertEquals("1", holder.chapter)

        val reopenedTwo = controller.reopenClosed(main.id)!!
        assertEquals("2", holder.chapter)

        controller.activate(reopenedOne)
        assertEquals("1", holder.chapter)
        controller.activate(reopenedTwo)
        assertEquals("2", holder.chapter)
    }

    @Test
    fun reopeningAChosenEntryRestoresThatEntrysChapter() {
        val one = clickChapter("1", TabOpenMode.Background)
        val two = clickChapter("2", TabOpenMode.Background)
        controller.close(one)
        controller.close(two)

        val entry = controller.recentlyClosed.first { it.stack.last() == reader("1") }
        controller.reopenClosed(main.id, entry)

        assertEquals("1", holder.chapter)
    }
}
