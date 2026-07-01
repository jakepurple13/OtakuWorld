package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.testing.FakeBackgroundWorkHandler
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
        var backupCalledWith: PlatformFile? = null
        var restoreCalledWith: PlatformFile? = null

        override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun syncLocalToCloud() {}
        override fun syncCloudToLocal() {}
        override fun setupPeriodicCheckers() {}
        override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())
        override fun sourceUpdate() {}
        override fun cancel(uuid: String) {}
        override fun startBackup(file: PlatformFile) {
            backupCalledWith = file
        }

        override fun startRestore(file: PlatformFile) {
            restoreCalledWith = file
        }
    }

    private fun viewModel(backgroundWorkHandler: BackgroundWorkHandler = FakeBackgroundWorkHandler()) =
        MoreSettingsViewModel(
            backgroundWorkHandler = backgroundWorkHandler,
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

    @Test fun `starts with idle status`() = runTest {
        val vm = viewModel()

        assertEquals(ImportExportListStatus.Idle, vm.importExportListStatus)
    }

    @Test fun `exportFullBackup delegates to backgroundWorkHandler startBackup`() = runTest {
        val handler = RecordingBackgroundWorkHandler()
        val vm = viewModel(handler)
        val file = PlatformFile("backup.zip")

        vm.exportFullBackup(file)

        assertEquals(file, handler.backupCalledWith)
        assertTrue(handler.restoreCalledWith == null)
    }

    @Test fun `importFullBackup delegates to backgroundWorkHandler startRestore`() = runTest {
        val handler = RecordingBackgroundWorkHandler()
        val vm = viewModel(handler)
        val file = PlatformFile("backup.zip")

        vm.importFullBackup(file)

        assertEquals(file, handler.restoreCalledWith)
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
