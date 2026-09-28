package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class NotificationScreenViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var database: ItemDatabase

    // The ViewModel observes ItemDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun notification(
        id: Int,
        url: String = "https://example.com/$id",
        source: String = "ExampleService",
        createdAt: Long = id.toLong(),
        isShowing: Boolean = false,
    ) = NotificationItem(
        id = id,
        url = url,
        summaryText = "Summary $id",
        notiTitle = "Title $id",
        imageUrl = "https://example.com/$id.jpg",
        source = source,
        contentTitle = "Content $id",
        isShowing = isShowing,
        createdAt = createdAt,
    )

    private fun viewModel() = NotificationScreenViewModel(
        db = database.itemDao(),
        notificationRepository = NotificationRepository(database.itemDao()),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun insert(vararg items: NotificationItem) {
        items.forEach { database.itemDao().insertNotification(it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        database = createTestItemDatabase()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
    }

    @Test fun `starts with no notifications`() = runTest {
        val vm = viewModel()

        assertTrue(vm.items.isEmpty())
        assertTrue(vm.filteredItems.isEmpty())
        assertEquals(NotificationFilter.All, vm.filter)
    }

    @Test fun `filteredItems are sorted newest first`() = runTest {
        insert(notification(1, createdAt = 100), notification(2, createdAt = 300), notification(3, createdAt = 200))

        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        assertEquals(listOf(2, 3, 1), vm.filteredItems.map { it.id })
    }

    @Test fun `source filter keeps only that source`() = runTest {
        insert(
            notification(1, source = "A"),
            notification(2, source = "B"),
            notification(3, source = "A"),
        )
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.updateFilter(NotificationFilter.Source("A"))

        assertEquals(listOf(3, 1), vm.filteredItems.map { it.id })
        assertEquals(listOf("A" to 2, "B" to 1), vm.sourceCounts)
    }

    @Test fun `in tray filter keeps only showing notifications`() = runTest {
        insert(notification(1, isShowing = true), notification(2), notification(3, isShowing = true))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.updateFilter(NotificationFilter.InTray)

        assertEquals(listOf(3, 1), vm.filteredItems.map { it.id })
        assertEquals(2, vm.trayCount)
    }

    @Test fun `filter resets to All when its last item is deleted`() = runTest {
        insert(notification(1, source = "A"), notification(2, source = "B"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        vm.updateFilter(NotificationFilter.Source("B"))
        vm.deleteWithUndo(vm.filteredItems)

        assertEquals(NotificationFilter.All, vm.filter)
        assertEquals(listOf(1), vm.filteredItems.map { it.id })
    }

    @Test fun `selection toggles, selects all visible, and clears`() = runTest {
        insert(notification(1, source = "A"), notification(2, source = "B"), notification(3, source = "A"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.toggleSelection("https://example.com/1")
        assertTrue(vm.isSelecting)
        vm.toggleSelection("https://example.com/1")
        assertTrue(!vm.isSelecting)

        vm.updateFilter(NotificationFilter.Source("A"))
        vm.selectAll()
        assertEquals(setOf("https://example.com/1", "https://example.com/3"), vm.selected.toSet())

        vm.clearSelection()
        assertTrue(vm.selected.isEmpty())
    }

    @Test fun `deleteWithUndo hides items and clears their selection until committed`() = runTest {
        insert(notification(1), notification(2))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        vm.toggleSelection("https://example.com/1")
        val batch = vm.deleteWithUndo(vm.selectedItems())

        assertEquals(listOf(2), vm.filteredItems.map { it.id })
        assertTrue(vm.selected.isEmpty())
        assertEquals(2, database.itemDao().getAllNotifications().size)

        vm.commitDeletion(batch)
        awaitCondition { database.itemDao().getAllNotifications().size == 1 }
    }

    @Test fun `undoDeletion brings items back without touching the database`() = runTest {
        insert(notification(1), notification(2))
        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        val batch = vm.deleteWithUndo(listOf(vm.items.first { it.id == 1 }))
        vm.undoDeletion(batch)

        assertEquals(listOf(2, 1), vm.filteredItems.map { it.id })
        assertEquals(2, database.itemDao().getAllNotifications().size)
    }

    @Test fun `pending deletions are committed when the ViewModel is cleared`() = runTest {
        insert(notification(1))
        val vm = viewModel()
        awaitCondition { vm.items.size == 1 }

        vm.deleteWithUndo(vm.filteredItems)
        viewModelStore.clear()

        awaitCondition { database.itemDao().getAllNotifications().isEmpty() }
    }

    @Test fun `catch up snapshots the filtered list and advances`() = runTest {
        insert(notification(1, source = "A"), notification(2, source = "B"), notification(3, source = "A"))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.updateFilter(NotificationFilter.Source("A"))
        vm.startCatchUp()
        assertEquals(listOf(3, 1), vm.catchUpRemaining.map { it.id })

        vm.advanceCatchUp()
        assertEquals(listOf(1), vm.catchUpRemaining.map { it.id })
        assertEquals(1, vm.catchUp?.index)

        vm.advanceCatchUp()
        assertTrue(vm.catchUpRemaining.isEmpty())

        vm.endCatchUp()
        assertNull(vm.catchUp)
    }

    @Test fun `catch up delete confirmation can be turned off until the next catch up`() = runTest {
        insert(notification(1))
        val vm = viewModel()
        awaitCondition { vm.items.size == 1 }

        vm.startCatchUp()
        assertEquals(true, vm.catchUp?.confirmDeletes)

        vm.stopConfirmingCatchUpDeletes()
        vm.advanceCatchUp()
        assertEquals(false, vm.catchUp?.confirmDeletes)

        vm.endCatchUp()
        vm.startCatchUp()
        assertEquals(true, vm.catchUp?.confirmDeletes)
    }

    @Test fun `catch up skips items deleted from the deck`() = runTest {
        insert(notification(1), notification(2), notification(3))
        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        vm.startCatchUp()
        vm.deleteWithUndo(listOf(vm.catchUpRemaining.first()))
        vm.advanceCatchUp()

        assertEquals(listOf(2, 1), vm.catchUpRemaining.map { it.id })
    }

    @Test fun `deleteAllNotifications clears the database and returns count`() = runTest {
        insert(notification(1), notification(2))

        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        val deletedCount = vm.deleteAllNotifications()

        assertEquals(2, deletedCount)
        assertTrue(database.itemDao().getAllNotifications().isEmpty())
    }

    @Test fun `cancelNotificationById does not throw`() = runTest {
        val vm = viewModel()

        vm.cancelNotificationById(1)
    }
}

class BucketByDayTest {
    private val zone = TimeZone.UTC
    private val now = LocalDateTime(2026, 9, 28, 15, 0).toInstant(zone)

    private fun item(id: Int, createdAt: Long) = NotificationItem(
        id = id,
        url = "u$id",
        summaryText = "",
        notiTitle = "",
        imageUrl = null,
        source = "",
        contentTitle = "",
        createdAt = createdAt,
    )

    @Test fun `groups items into today, yesterday, this week and older`() {
        val items = listOf(
            item(1, (now - 1.hours).toEpochMilliseconds()),
            item(2, (now - 14.hours).toEpochMilliseconds()), // 01:00 same day
            item(3, (now - 16.hours).toEpochMilliseconds()), // 23:00 yesterday
            item(4, (now - 3.days).toEpochMilliseconds()),
            item(5, (now - 10.days).toEpochMilliseconds()),
            item(6, 0L),
        )

        val buckets = bucketByDay(items, now, zone)

        assertEquals(
            listOf(
                NotificationDay.Today to listOf(1, 2),
                NotificationDay.Yesterday to listOf(3),
                NotificationDay.ThisWeek to listOf(4),
                NotificationDay.Older to listOf(5, 6),
            ),
            buckets.map { (day, list) -> day to list.map { it.id } }
        )
    }

    @Test fun `empty buckets are left out`() {
        val buckets = bucketByDay(listOf(item(1, (now - 1.hours).toEpochMilliseconds())), now, zone)

        assertEquals(listOf(NotificationDay.Today), buckets.map { it.first })
    }
}
