package com.programmersbox.kmpuiviews.presentation.settings.workerinfo

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkerInfoViewModelTest {

    private val viewModelStore = ViewModelStore()

    // The ViewModel collects the handler's workerInfoFlow() on viewModelScope, which runs on
    // a test-dispatcher virtual clock. A raw StateFlow update isn't guaranteed to be observed
    // synchronously, so wait for it with real time instead of virtual-time advancing.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private class FakeBackgroundWorkHandler(
        private val workerInfo: MutableStateFlow<List<WorkerInfoModel>> = MutableStateFlow(emptyList()),
    ) : BackgroundWorkHandler {
        val cancelledIds = mutableListOf<String>()

        override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun syncLocalToCloud() {}
        override fun syncCloudToLocal() {}
        override fun setupPeriodicCheckers() {}
        override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = workerInfo
        override fun sourceUpdate() {}
        override fun cancel(uuid: String) {
            cancelledIds.add(uuid)
        }

        override fun startBackup(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {}
        override fun startRestore(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {}
        override fun backupResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
        override fun restoreResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
    }

    private fun workerInfoModel(id: String, workerName: String = "Worker $id") = WorkerInfoModel(
        id = id,
        progress = emptyMap(),
        status = "RUNNING",
        nextScheduleTimeMillis = LocalDateTime(2024, 1, 1, 0, 0),
        tags = emptySet(),
        isPeriodic = false,
        workerName = workerName,
    )

    private fun viewModel(handler: FakeBackgroundWorkHandler) = WorkerInfoViewModel(
        backgroundWorkHandler = handler,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    @Test fun `starts with no workers`() = runTest {
        val vm = viewModel(FakeBackgroundWorkHandler())

        assertTrue(vm.workers.isEmpty())
    }

    @Test fun `workers reflects emissions from workerInfoFlow`() = runTest {
        val stateFlow = MutableStateFlow<List<WorkerInfoModel>>(emptyList())
        val handler = FakeBackgroundWorkHandler(stateFlow)
        val vm = viewModel(handler)

        stateFlow.value = listOf(workerInfoModel("1"))
        awaitCondition { vm.workers.isNotEmpty() }

        assertEquals(1, vm.workers.size)
        assertEquals("1", vm.workers[0].id)
        assertEquals("Worker 1", vm.workers[0].workerName)
    }

    @Test fun `workers is replaced, not appended, on new emission`() = runTest {
        val stateFlow = MutableStateFlow<List<WorkerInfoModel>>(listOf(workerInfoModel("1")))
        val handler = FakeBackgroundWorkHandler(stateFlow)
        val vm = viewModel(handler)

        awaitCondition { vm.workers.size == 1 }

        stateFlow.value = listOf(workerInfoModel("2"), workerInfoModel("3"))
        awaitCondition { vm.workers.size == 2 }

        assertEquals(listOf("2", "3"), vm.workers.map { it.id })
    }

    @Test fun `cancelWorker delegates to backgroundWorkHandler cancel`() = runTest {
        val handler = FakeBackgroundWorkHandler()
        val vm = viewModel(handler)

        vm.cancelWorker("worker-1")

        assertEquals(listOf("worker-1"), handler.cancelledIds)
    }
}
