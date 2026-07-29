package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.testing.FakeBackgroundWorkHandler
import com.programmersbox.sharedcomponents.backup.ItemResult
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoreSettingsViewModelTest {

    private val viewModelStore = ViewModelStore()

    private class RecordingBackgroundWorkHandler : BackgroundWorkHandler {
        var backupCalledWith: Pair<PlatformFile, Set<String>>? = null
        var restoreCalledWith: Pair<PlatformFile, Set<String>>? = null

        override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun syncLocalToCloud() {}
        override fun syncCloudToLocal() {}
        override fun setupPeriodicCheckers() {}
        override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())
        override fun sourceUpdate() {}
        override fun cancel(uuid: String) {}
        override fun startBackup(file: PlatformFile, selectedKeys: Set<String>) {
            backupCalledWith = file to selectedKeys
        }

        override fun startRestore(file: PlatformFile, selectedKeys: Set<String>) {
            restoreCalledWith = file to selectedKeys
        }

        override fun backupResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
        override fun restoreResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
    }

    private fun viewModel(
        backgroundWorkHandler: BackgroundWorkHandler = FakeBackgroundWorkHandler(),
        backupProcessors: List<BackupProcessor> = emptyList(),
    ) = MoreSettingsViewModel(
        backgroundWorkHandler = backgroundWorkHandler,
        backupProcessors = backupProcessors,
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

    @Test fun `starts with idle status`() = runTest {
        val vm = viewModel()

        assertEquals(ImportExportListStatus.Idle, vm.importExportListStatus)
    }

    @Test fun `exportFullBackup delegates to backgroundWorkHandler startBackup with all processor keys`() = runTest {
        val handler = RecordingBackgroundWorkHandler()
        val processors = listOf(FakeBackupProcessor("a.json"), FakeBackupProcessor("b.json"))
        val vm = viewModel(handler, processors)
        val file = PlatformFile("backup.zip")

        vm.exportFullBackup(file)

        assertEquals(file, handler.backupCalledWith?.first)
        assertEquals(setOf("a.json", "b.json"), handler.backupCalledWith?.second)
        assertTrue(handler.restoreCalledWith == null)
    }

    @Test fun `importFullBackup delegates to backgroundWorkHandler startRestore with all processor keys`() = runTest {
        val handler = RecordingBackgroundWorkHandler()
        val processors = listOf(FakeBackupProcessor("a.json"))
        val vm = viewModel(handler, processors)
        val file = PlatformFile("backup.zip")

        vm.importFullBackup(file)

        assertEquals(file, handler.restoreCalledWith?.first)
        assertEquals(setOf("a.json"), handler.restoreCalledWith?.second)
        assertTrue(handler.backupCalledWith == null)
    }

    @Test fun `importExportListStatus can be updated directly`() = runTest {
        val vm = viewModel()

        vm.importExportListStatus = ImportExportListStatus.Loading
        assertEquals(ImportExportListStatus.Loading, vm.importExportListStatus)

        vm.importExportListStatus = ImportExportListStatus.Success
        assertEquals(ImportExportListStatus.Success, vm.importExportListStatus)

        val error = ImportExportListStatus.Error(RuntimeException("boom"))
        vm.importExportListStatus = error
        assertEquals(error, vm.importExportListStatus)
    }
}

private class FakeBackupProcessor(name: String) : BackupProcessor() {
    override val fileName: String = name
    override suspend fun backup(sink: okio.BufferedSink): ProcessorResult = ProcessorResult(successCount = 1)
    override suspend fun restore(json: String, bufferedSource: okio.BufferedSource): ProcessorResult = ProcessorResult(successCount = 1)
}
