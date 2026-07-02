package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.lifecycle.ViewModelStore
import com.programmersbox.datastore.NotificationSortBy
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        url: String,
        source: String = "ExampleService",
    ) = NotificationItem(
        id = id,
        url = url,
        summaryText = "Summary $id",
        notiTitle = "Title $id",
        imageUrl = "https://example.com/$id.jpg",
        source = source,
        contentTitle = "Content $id",
    )

    private fun viewModel() = NotificationScreenViewModel(
        db = database.itemDao(),
        settingsHandling = createTestNewSettingsHandling(),
        sourceRepository = SourceRepository(),
        notificationRepository = NotificationRepository(database.itemDao()),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

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
        assertTrue(vm.groupedList.isEmpty())
        assertEquals(NotificationSortBy.Date, vm.sortedBy)
    }

    @Test fun `inserted notification shows up in items after collection`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(notification(id = 1, url = "https://example.com/1"))

        val vm = viewModel()
        awaitCondition { vm.items.isNotEmpty() }

        assertEquals(1, vm.items.size)
        assertEquals("Title 1", vm.items[0].notiTitle)
    }

    @Test fun `groupedList groups notifications by source`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(notification(id = 1, url = "https://example.com/1", source = "SourceA"))
        dao.insertNotification(notification(id = 2, url = "https://example.com/2", source = "SourceA"))
        dao.insertNotification(notification(id = 3, url = "https://example.com/3", source = "SourceB"))

        val vm = viewModel()
        awaitCondition { vm.items.size == 3 }

        assertEquals(2, vm.groupedList.size)
        val sourceAGroup = vm.groupedList.first { it.first == "SourceA" }
        assertEquals(2, sourceAGroup.second.size)
        val sourceBGroup = vm.groupedList.first { it.first == "SourceB" }
        assertEquals(1, sourceBGroup.second.size)
    }

    @Test fun `toggleGroupedState flips state for known source`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(notification(id = 1, url = "https://example.com/1", source = "SourceA"))

        val vm = viewModel()
        awaitCondition { vm.groupedListState.containsKey("SourceA") }

        assertEquals(false, vm.groupedListState["SourceA"]?.value)

        vm.toggleGroupedState("SourceA")
        assertEquals(true, vm.groupedListState["SourceA"]?.value)

        vm.toggleGroupedState("SourceA")
        assertEquals(false, vm.groupedListState["SourceA"]?.value)
    }

    @Test fun `deleteNotification removes it from the database and invokes callback`() = runTest {
        val dao = database.itemDao()
        val item = notification(id = 1, url = "https://example.com/1")
        dao.insertNotification(item)

        val vm = viewModel()
        awaitCondition { vm.items.isNotEmpty() }

        var callbackInvoked = false
        vm.deleteNotification(item) { callbackInvoked = true }

        awaitCondition { callbackInvoked }
        awaitCondition { dao.getAllNotifications().isEmpty() }
    }

    @Test fun `deleteAllNotifications clears the database and returns count`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(notification(id = 1, url = "https://example.com/1"))
        dao.insertNotification(notification(id = 2, url = "https://example.com/2"))

        val vm = viewModel()
        awaitCondition { vm.items.size == 2 }

        val deletedCount = vm.deleteAllNotifications()

        assertEquals(2, deletedCount)
        assertTrue(dao.getAllNotifications().isEmpty())
    }

    @Test fun `toggleSort switches between Date and Grouped`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.sortedBy == NotificationSortBy.Date }

        vm.toggleSort()
        awaitCondition { vm.sortedBy == NotificationSortBy.Grouped }

        vm.toggleSort()
        awaitCondition { vm.sortedBy == NotificationSortBy.Date }
    }

    @Test fun `cancelNotificationById does not throw`() = runTest {
        val vm = viewModel()

        vm.cancelNotificationById(1)
    }
}
