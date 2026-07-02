package com.programmersbox.kmpuiviews.presentation.settings.notifications

import androidx.lifecycle.ViewModelStore
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.repository.WorkRepository
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeWorkRepository : WorkRepository {
    override val manualCheck: Flow<List<WorkInfoKmp>> = flowOf(emptyList())
    override val allWorkCheck: Flow<List<WorkInfoKmp>> = flowOf(emptyList())
    override fun pruneWork() {}
    override fun checkManually() {}
}

class NotificationSettingsViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var database: ItemDatabase
    private lateinit var dataStoreHandling: DataStoreHandling
    private lateinit var settingsHandling: NewSettingsHandling

    // The ViewModel observes ItemDao's Room-generated Flow and DataStore-backed flows, which
    // emit on real (non-test-controlled) dispatchers. A test-dispatcher virtual-clock advance
    // doesn't drive those emissions, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun notification(id: Int, url: String = "https://example.com/$id") = NotificationItem(
        id = id,
        url = url,
        summaryText = "Summary",
        notiTitle = "Title",
        imageUrl = "https://example.com/$id.jpg",
        source = "ExampleService",
        contentTitle = "Content Title",
    )

    private fun viewModel() = NotificationSettingsViewModel(
        dao = database.itemDao(),
        dataStoreHandling = dataStoreHandling,
        settingsHandling = settingsHandling,
        dateTimeFormatHandler = DateTimeFormatHandler(),
        workRepository = FakeWorkRepository(),
        notificationRepository = NotificationRepository(database.itemDao()),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        database = createTestItemDatabase()
        dataStoreHandling = createTestDataStoreHandling()
        settingsHandling = createTestNewSettingsHandling()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
    }

    @Test fun `starts with no saved notifications`() = runTest {
        val vm = viewModel()

        awaitCondition { vm.time.isNotEmpty() }

        assertEquals(0, vm.savedNotifications)
    }

    @Test fun `saved notification count reflects dao contents after collection`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(notification(1))
        dao.insertNotification(notification(2))

        val vm = viewModel()
        awaitCondition { vm.savedNotifications == 2 }

        assertEquals(2, vm.savedNotifications)
    }

    @Test fun `default media checker settings are loaded into canCheck and updateHourCheck`() = runTest {
        val vm = viewModel()

        awaitCondition { vm.time.isNotEmpty() }

        assertTrue(vm.canCheck)
        assertEquals(1L, vm.updateHourCheck)
    }

    @Test fun `updateShouldCheck persists and updates canCheck`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.time.isNotEmpty() }

        vm.updateShouldCheck(false)
        awaitCondition { !vm.canCheck }

        assertFalse(vm.canCheck)
        assertFalse(settingsHandling.mediaCheckerSettings.get().shouldRun)
    }

    @Test fun `updateHourCheck persists and updates updateHourCheck`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.time.isNotEmpty() }

        vm.updateHourCheck(5L)
        awaitCondition { vm.updateHourCheck == 5L }

        assertEquals(5L, vm.updateHourCheck)
        assertEquals(5L, settingsHandling.mediaCheckerSettings.get().interval)
    }

    @Test fun `updateNetworkType persists to settings`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.time.isNotEmpty() }

        vm.updateNetworkType(MediaCheckerNetworkType.Metered)
        awaitCondition { settingsHandling.mediaCheckerSettings.get().networkType == MediaCheckerNetworkType.Metered }

        assertEquals(MediaCheckerNetworkType.Metered, settingsHandling.mediaCheckerSettings.get().networkType)
    }

    @Test fun `updateRequiresCharging persists to settings`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.time.isNotEmpty() }

        vm.updateRequiresCharging(true)
        awaitCondition { settingsHandling.mediaCheckerSettings.get().requiresCharging }

        assertTrue(settingsHandling.mediaCheckerSettings.get().requiresCharging)
    }

    @Test fun `updateRequiresBatteryNotLow persists to settings`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.time.isNotEmpty() }

        vm.updateRequiresBatteryNotLow(true)
        awaitCondition { settingsHandling.mediaCheckerSettings.get().requiresBatteryNotLow }

        assertTrue(settingsHandling.mediaCheckerSettings.get().requiresBatteryNotLow)
    }

    @Test fun `time reflects update checking start and end from data store`() = runTest {
        val vm = viewModel()

        awaitCondition { vm.time.isNotEmpty() }

        assertTrue(vm.time.contains("Start:"))
        assertTrue(vm.time.contains("End:"))
    }

    @Test fun `notifyOnBoot exposes the settings handler value`() = runTest {
        val vm = viewModel()

        val sub = backgroundScope.launch { vm.notifyOnBoot.asFlow().collect {} }

        awaitCondition { true }
        assertEquals(true, vm.notifyOnBoot.get())
        sub.cancel()
    }

    @Test fun `cancelGroup does not throw`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.time.isNotEmpty() }

        vm.cancelGroup()
    }
}
