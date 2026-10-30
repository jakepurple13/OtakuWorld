package com.programmersbox.kmpuiviews.presentation.history

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.RecentModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class HistoryViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: HistoryDatabase

    // The ViewModel observes HistoryDao's Room-generated Flow, which emits on Room's own
    // dispatcher, so wait for it with real time.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun recent(
        url: String,
        title: String = "Title",
        source: String = "ExampleService",
        timestamp: Long = 0L,
    ) = RecentModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = source,
        timestamp = timestamp,
    )

    private fun viewModel() = HistoryViewModel(dao = database.historyDao()).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun insert(vararg items: RecentModel) {
        items.forEach { database.historyDao().insertRecentlyViewed(it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("history-viewmodel-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<HistoryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun `stops loading with an empty history`() = runTest {
        val vm = viewModel()
        awaitCondition { !vm.isLoading }

        assertEquals(0, vm.historyCount)
        assertTrue(vm.filteredItems.isEmpty())
    }

    @Test fun `items are sorted newest first`() = runTest {
        insert(recent("1", timestamp = 100), recent("2", timestamp = 300), recent("3", timestamp = 200))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        assertEquals(listOf("2", "3", "1"), vm.filteredItems.map { it.url })
        assertEquals(3, vm.historyCount)
    }

    @Test fun `search matches titles ignoring case`() = runTest {
        insert(recent("1", title = "Sakamoto Days"), recent("2", title = "Dandadan"), recent("3", title = "Solo Leveling"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.updateSearchQuery(" sa ")

        assertEquals(listOf("1"), vm.filteredItems.map { it.url })
    }

    @Test fun `source filter combines with search`() = runTest {
        insert(
            recent("1", title = "Alpha", source = "A"),
            recent("2", title = "Alpha", source = "B"),
            recent("3", title = "Beta", source = "A"),
        )
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.updateSourceFilter("A")
        vm.updateSearchQuery("alp")

        assertEquals(listOf("1"), vm.filteredItems.map { it.url })
        assertEquals(setOf("A", "B"), vm.sources.toSet())
    }

    @Test fun `removeWithUndo hides items and undo brings them back`() = runTest {
        insert(recent("1", timestamp = 1), recent("2", timestamp = 2))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        val batch = vm.removeWithUndo(vm.filteredItems.filter { it.url == "1" })
        assertEquals(listOf("2"), vm.filteredItems.map { it.url })
        assertEquals(1, vm.historyCount)

        vm.undoRemoval(batch)
        assertEquals(listOf("2", "1"), vm.filteredItems.map { it.url })
    }

    @Test fun `commitRemoval deletes from the database`() = runTest {
        insert(recent("1"), recent("2"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        vm.commitRemoval(vm.removeWithUndo(vm.filteredItems.filter { it.url == "1" }))

        awaitCondition { vm.items.size == 1 }
        assertEquals(listOf("2"), vm.filteredItems.map { it.url })
    }

    @Test fun `source filter resets when its last item is removed`() = runTest {
        insert(recent("1", source = "A"), recent("2", source = "B"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        vm.updateSourceFilter("B")
        vm.removeWithUndo(vm.filteredItems)

        assertNull(vm.sourceFilter)
        assertEquals(listOf("1"), vm.filteredItems.map { it.url })
    }

    @Test fun `selection toggles, selects all visible, and clears on removal`() = runTest {
        insert(recent("1", source = "A"), recent("2", source = "A"), recent("3", source = "B"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.toggleSelection("1")
        assertTrue(vm.isSelecting)
        vm.toggleSelection("1")
        assertFalse(vm.isSelecting)

        vm.updateSourceFilter("A")
        vm.selectAll()
        assertEquals(setOf("1", "2"), vm.selected.toSet())

        vm.removeWithUndo(vm.selectedItems())
        assertFalse(vm.isSelecting)
    }

    @Test fun `clearAll empties the history`() = runTest {
        insert(recent("1"), recent("2"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        vm.clearAll()

        awaitCondition { vm.items.isEmpty() }
        assertEquals(0, vm.historyCount)
    }

    @Test fun `groupByDay buckets today, yesterday, this week, then by month`() {
        val tz = TimeZone.UTC
        val now = LocalDateTime(2026, 10, 30, 12, 0).toInstant(tz)
        val items = listOf(
            recent("today", timestamp = (now - 1.hours).toEpochMilliseconds()),
            recent("yesterday", timestamp = (now - 1.days).toEpochMilliseconds()),
            recent("week", timestamp = (now - 4.days).toEpochMilliseconds()),
            recent("oct", timestamp = (now - 20.days).toEpochMilliseconds()),
            recent("sep", timestamp = (now - 40.days).toEpochMilliseconds()),
        )

        val groups = groupByDay(items.shuffled(), now, tz)

        assertEquals(
            listOf(
                HistoryGroup.Today,
                HistoryGroup.Yesterday,
                HistoryGroup.ThisWeek,
                HistoryGroup.MonthOf(2026, Month.OCTOBER),
                HistoryGroup.MonthOf(2026, Month.SEPTEMBER),
            ),
            groups.map { it.first }
        )
        assertEquals(listOf("today", "yesterday", "week", "oct", "sep"), groups.flatMap { it.second }.map { it.url })
    }

    @Test fun `relativeTimeLabel uses short units under a week`() {
        val tz = TimeZone.UTC
        val now = LocalDateTime(2026, 10, 30, 12, 0).toInstant(tz)

        assertEquals("now", relativeTimeLabel((now - 10.minutes / 20).toEpochMilliseconds(), now, tz))
        assertEquals("12m", relativeTimeLabel((now - 12.minutes).toEpochMilliseconds(), now, tz))
        assertEquals("3h", relativeTimeLabel((now - 3.hours).toEpochMilliseconds(), now, tz))
        assertEquals("2d", relativeTimeLabel((now - 2.days).toEpochMilliseconds(), now, tz))
        assertEquals("Oct 3", relativeTimeLabel((now - 27.days).toEpochMilliseconds(), now, tz))
    }
}
