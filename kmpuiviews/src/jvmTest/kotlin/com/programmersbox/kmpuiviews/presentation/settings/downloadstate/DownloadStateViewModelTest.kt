package com.programmersbox.kmpuiviews.presentation.settings.downloadstate

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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

class DownloadStateViewModelTest {

    private val viewModelStore = ViewModelStore()

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun state(url: String, name: String = "Name $url", id: String = url) = DownloadAndInstallState(
        url = url,
        name = name,
        id = id,
        status = DownloadAndInstallStatus.Downloading(0f),
    )

    private class ControllableDownloadStateInterface(
        val downloadListFlow: MutableStateFlow<List<DownloadAndInstallState>> = MutableStateFlow(emptyList()),
        val installFlow: MutableStateFlow<DownloadAndInstallStatus>? = null,
    ) : DownloadStateInterface {
        override val downloadList: Flow<List<DownloadAndInstallState>> = downloadListFlow
        val cancelledIds = mutableListOf<String>()
        override fun cancelDownload(id: String) {
            cancelledIds.add(id)
        }

        override fun install(url: String): Flow<DownloadAndInstallStatus> = installFlow ?: emptyFlow()
        override fun downloadAndInstall(url: String) {}
        override fun downloadThenInstall(url: String) {}
    }

    private fun viewModel(downloadStateInterface: DownloadStateInterface) = DownloadStateViewModel(
        downloadStateRepository = downloadStateInterface,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModelStore.clear()
    }

    @Test fun `starts with empty downloadList`() = runTest {
        val vm = viewModel(ControllableDownloadStateInterface())

        assertTrue(vm.downloadList.isEmpty())
    }

    @Test fun `downloadList reflects repository emissions`() = runTest {
        val repo = ControllableDownloadStateInterface()
        val vm = viewModel(repo)

        repo.downloadListFlow.value = listOf(state("https://example.com/1"))
        awaitCondition { vm.downloadList.isNotEmpty() }

        assertEquals(1, vm.downloadList.size)
        assertEquals("https://example.com/1", vm.downloadList[0].url)
    }

    @Test fun `downloadList filters out entries with blank name`() = runTest {
        val repo = ControllableDownloadStateInterface()
        val vm = viewModel(repo)

        repo.downloadListFlow.value = listOf(
            state("https://example.com/1", name = "Valid"),
            state("https://example.com/2", name = ""),
        )
        awaitCondition { vm.downloadList.isNotEmpty() }

        assertEquals(1, vm.downloadList.size)
        assertEquals("https://example.com/1", vm.downloadList[0].url)
    }

    @Test fun `downloadList updates when repository emits a new list`() = runTest {
        val repo = ControllableDownloadStateInterface()
        val vm = viewModel(repo)

        repo.downloadListFlow.value = listOf(state("https://example.com/1"))
        awaitCondition { vm.downloadList.size == 1 }

        repo.downloadListFlow.value = listOf(
            state("https://example.com/1"),
            state("https://example.com/2"),
        )
        awaitCondition { vm.downloadList.size == 2 }

        assertEquals(
            listOf("https://example.com/1", "https://example.com/2"),
            vm.downloadList.map { it.url },
        )
    }

    @Test fun `cancelWorker delegates to repository`() = runTest {
        val repo = ControllableDownloadStateInterface()
        val vm = viewModel(repo)

        vm.cancelWorker("some-id")

        assertEquals(listOf("some-id"), repo.cancelledIds)
    }

    @Test fun `install updates status of matching entry in downloadList`() = runTest {
        val installFlow = MutableStateFlow<DownloadAndInstallStatus>(DownloadAndInstallStatus.Installing)
        val repo = ControllableDownloadStateInterface(installFlow = installFlow)
        val vm = viewModel(repo)

        repo.downloadListFlow.value = listOf(state("https://example.com/1"))
        awaitCondition { vm.downloadList.isNotEmpty() }

        vm.install("https://example.com/1")
        awaitCondition { vm.downloadList[0].status == DownloadAndInstallStatus.Installing }

        installFlow.value = DownloadAndInstallStatus.Installed
        awaitCondition { vm.downloadList[0].status == DownloadAndInstallStatus.Installed }
    }

    @Test fun `install does not affect entries with a different url`() = runTest {
        val installFlow = MutableStateFlow<DownloadAndInstallStatus>(DownloadAndInstallStatus.Installing)
        val repo = ControllableDownloadStateInterface(installFlow = installFlow)
        val vm = viewModel(repo)

        repo.downloadListFlow.value = listOf(
            state("https://example.com/1"),
            state("https://example.com/2"),
        )
        awaitCondition { vm.downloadList.size == 2 }

        vm.install("https://example.com/1")
        awaitCondition { vm.downloadList.first { it.url == "https://example.com/1" }.status == DownloadAndInstallStatus.Installing }

        assertEquals(
            DownloadAndInstallStatus.Downloading(0f),
            vm.downloadList.first { it.url == "https://example.com/2" }.status,
        )
    }
}
