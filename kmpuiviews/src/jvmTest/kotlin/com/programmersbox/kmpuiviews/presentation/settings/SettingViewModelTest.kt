package com.programmersbox.kmpuiviews.presentation.settings

import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
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

class SettingViewModelTest {

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

    private fun notification(url: String) = NotificationItem(
        id = url.hashCode(),
        url = url,
        summaryText = "Summary",
        notiTitle = "Title",
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
        contentTitle = "Content $url",
    )

    private fun viewModel() = SettingViewModel(dao = database.itemDao()).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

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

    @Test fun `starts with zero saved notifications`() = runTest {
        val vm = viewModel()

        assertEquals(0, vm.savedNotifications)
    }

    @Test fun `savedNotifications reflects notification count after collection`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(notification("https://example.com/1"))
        dao.insertNotification(notification("https://example.com/2"))

        val vm = viewModel()
        awaitCondition { vm.savedNotifications == 2 }

        assertEquals(2, vm.savedNotifications)
    }

    @Test fun `savedNotifications updates when a new notification is inserted`() = runTest {
        val dao = database.itemDao()
        val vm = viewModel()
        awaitCondition { vm.savedNotifications == 0 }

        dao.insertNotification(notification("https://example.com/1"))
        awaitCondition { vm.savedNotifications == 1 }

        assertEquals(1, vm.savedNotifications)
    }
}
