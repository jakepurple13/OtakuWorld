package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.repository.WorkRepository
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeWork(
    override val allWorkCheck: Flow<List<WorkInfoKmp>> = MutableStateFlow(emptyList()),
) : WorkRepository {
    val manual = MutableStateFlow<List<WorkInfoKmp>>(emptyList())
    override val manualCheck: Flow<List<WorkInfoKmp>> = manual
    var checkCalls = 0

    override fun pruneWork() = Unit
    override fun checkManually() {
        checkCalls++
    }

    override fun retryFailedUpdates() = true
}

private class FakeReports : UpdateCheckReportStore {
    val reportFlow = MutableStateFlow<UpdateCheckReport?>(null)
    override val report: Flow<UpdateCheckReport?> = reportFlow
    override suspend fun current() = reportFlow.value
    override suspend fun save(report: UpdateCheckReport) {
        reportFlow.value = report
    }
}

private class FakeHandler : BackgroundWorkHandler {
    val workers = MutableStateFlow<List<WorkerInfoModel>>(emptyList())
    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = workers
    override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = emptyFlow()
    override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = emptyFlow()
    override fun syncLocalToCloud() = Unit
    override fun syncCloudToLocal() = Unit
    override fun setupPeriodicCheckers() = Unit
    override fun sourceUpdate() = Unit
    override fun cancel(uuid: String) = Unit
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) = Unit
    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) = Unit
    override fun backupResultsFlow(): Flow<List<ItemResult>> = emptyFlow()
    override fun restoreResultsFlow(): Flow<List<ItemResult>> = emptyFlow()
}

/** Records the calls these tests care about; everything else is a no-op. */
private class RecordingNav : NavigationActions {
    val calls = mutableListOf<String>()
    override fun scanQrCode() {
        calls += "scanQrCode"
    }

    override fun workerInfo() {
        calls += "workerInfo"
    }

    override fun <T : Any> navigate(nav: T) {
        calls += "navigate:$nav"
    }

    override fun recent() = Unit
    override fun details(title: String, description: String, url: String, imageUrl: String, source: String) = Unit
    override fun details(model: KmpItemModel) = Unit
    override fun onboarding() = Unit
    override fun all() = Unit
    override fun webView(url: String) = Unit
    override fun incognito() = Unit
    override fun extensionList() = Unit
    override fun settings() = Unit
    override fun globalSearch(searchText: String?) = Unit
    override fun sourceSearch(serviceName: String, query: String) = Unit
    override fun customList() = Unit
    override fun customList(customList: CustomList) = Unit
    override fun history() = Unit
    override fun favorites() = Unit
    override fun notifications() = Unit
    override fun debug() = Unit
    override fun downloadInstall() = Unit
    override fun order() = Unit
    override fun general() = Unit
    override fun moreInfo() = Unit
    override fun moreSettings() = Unit
    override fun security() = Unit
    override fun accountInfo() = Unit
    override fun prerelease() = Unit
    override fun notificationsSettings() = Unit
    override fun otherSettings() = Unit
    override fun about() = Unit
    override fun importList(uri: String) = Unit
    override fun importFullList(uri: String) = Unit
    override fun popBackStack() = Unit
    override fun remove(route: NavKey) = Unit
    override fun popBackStack(route: Any, inclusive: Boolean) = Unit
    override fun <T : Any> clearBackStack(nav: T) = Unit
    override fun <T : Any> homeScreenNavigate(nav: T) = Unit
    override fun onboardingToRecent() = Unit
    override fun toOnboarding() = Unit
    override fun bookmarks() = Unit
    override fun notes() = Unit
    override fun dictionary() = Unit
    override fun dictionaryDetail(id: Long) = Unit
    override fun dictionaryForm(id: Long?) = Unit
    override fun library() = Unit
    override fun discover() = Unit
    override fun sources() = Unit
    override fun integrations() = Unit
    override fun appearance() = Unit
    override fun colors() = Unit
    override fun behaviorSettings() = Unit
    override fun layout() = Unit
    override fun contentReading() = Unit
    override fun privacySecurity() = Unit
    override fun dataManagement() = Unit
    override fun aboutSettings() = Unit
    override fun diagnostics() = Unit
    override fun developer() = Unit

    @Composable
    override fun currentDestination(screen: Screen): Boolean = false
}

private fun work(state: String, progress: Int? = null, max: Int? = null) = WorkInfoKmp(
    state = state,
    source = "check",
    progress = progress,
    max = max,
    nextScheduleTimeMillis = LocalDateTime(2026, 1, 1, 0, 0),
)

private fun running(progress: Int?, max: Int?) = work("RUNNING", progress, max)
private fun succeeded() = work("SUCCEEDED")

private const val OLD = 0L
private const val NEW = Long.MAX_VALUE

private fun report(updated: Int, startedAt: Long = NEW) = UpdateCheckReport(
    startedAt = startedAt,
    finishedAt = 0,
    favoriteCount = 10,
    checkedCount = 10,
    updatedCount = updated,
    sourceFailures = emptyList(),
    itemFailures = emptyList(),
)

private fun workerModel() = WorkerInfoModel(
    id = "1",
    progress = emptyMap(),
    status = "ENQUEUED",
    nextScheduleTimeMillis = LocalDateTime(2026, 1, 1, 0, 0),
    tags = emptySet(),
    isPeriodic = true,
    workerName = "w",
)

@OptIn(ExperimentalCoroutinesApi::class)
class QuickActionsViewModelTest {
    private lateinit var controller: QuickActionsController
    private lateinit var fakeWork: FakeWork
    private lateinit var fakeReports: FakeReports
    private lateinit var fakeHandler: FakeHandler
    private lateinit var nav: RecordingNav

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        controller = QuickActionsController()
        fakeReports = FakeReports()
        fakeHandler = FakeHandler()
        nav = RecordingNav()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(work: FakeWork = FakeWork().also { fakeWork = it }) =
        QuickActionsViewModel(controller, work, fakeReports, fakeHandler, nav)

    @Test
    fun runCheckStartsOnceAndCloses() = runTest {
        val vm = vm()
        controller.open(OpenOrigin.Bolt)
        runCurrent()
        vm.run(QuickAction.CheckForUpdates)
        fakeWork.manual.value = listOf(running(1, 42))
        runCurrent()
        vm.run(QuickAction.CheckForUpdates)
        assertEquals(1, fakeWork.checkCalls)
        assertNull(controller.open.value)
    }

    @Test
    fun runCheckSkippedWhilePeriodicRuns() = runTest {
        val allWork = MutableStateFlow(listOf(running(null, null)))
        val fake = FakeWork(allWork)
        val vm = vm(fake)
        runCurrent()
        vm.run(QuickAction.CheckForUpdates)
        assertEquals(0, fake.checkCalls)
        vm.uiState.test {
            runCurrent()
            assertIs<ManualCheckState.Running>(expectMostRecentItem().checkState)
        }
    }

    @Test
    fun scanQrNavigates() = runTest {
        vm().run(QuickAction.ScanQrCode)
        assertEquals(listOf("scanQrCode"), nav.calls)
    }

    @Test
    fun backgroundTasksNavigates() = runTest {
        vm().run(QuickAction.BackgroundTasks)
        assertEquals(listOf("workerInfo"), nav.calls)
    }

    @Test
    fun openErrorsNavigatesAndCloses() = runTest {
        val vm = vm()
        controller.open(OpenOrigin.Keyboard)
        vm.openErrors()
        assertEquals(listOf("navigate:${Screen.UpdateCheckErrors}"), nav.calls)
        assertNull(controller.open.value)
    }

    @Test
    fun finishedShowsResultAfterRun() = runTest {
        val vm = vm()
        vm.uiState.test {
            vm.run(QuickAction.CheckForUpdates)
            fakeWork.manual.value = listOf(running(1, 42))
            runCurrent()
            fakeWork.manual.value = listOf(succeeded())
            fakeReports.reportFlow.value = report(updated = 3)
            runCurrent()
            assertEquals(
                ManualCheckState.Finished(CheckOutcome.Updates(3)),
                expectMostRecentItem().checkState
            )
        }
    }

    @Test
    fun dismissResultReturnsToIdle() = runTest {
        val vm = vm()
        vm.uiState.test {
            vm.run(QuickAction.CheckForUpdates)
            fakeWork.manual.value = listOf(running(null, null))
            runCurrent()
            fakeWork.manual.value = listOf(succeeded())
            fakeReports.reportFlow.value = report(updated = 0)
            runCurrent()
            assertEquals(ManualCheckState.Finished(CheckOutcome.NoUpdates), expectMostRecentItem().checkState)
            vm.dismissResult()
            runCurrent()
            assertEquals(ManualCheckState.Idle, expectMostRecentItem().checkState)
        }
    }

    @Test
    fun noStaleResultRightAfterRun() = runTest {
        fakeReports.reportFlow.value = report(updated = 5, startedAt = OLD)
        val vm = vm()
        fakeWork.manual.value = listOf(succeeded())
        vm.uiState.test {
            runCurrent()
            vm.run(QuickAction.CheckForUpdates)
            runCurrent()
            val state = expectMostRecentItem().checkState
            assertTrue(state is ManualCheckState.Idle || state is ManualCheckState.Running, "was $state")
        }
    }

    @Test
    fun staleFailedEntryIgnored() = runTest {
        val vm = vm()
        fakeWork.manual.value = listOf(work("FAILED"))
        vm.uiState.test {
            runCurrent()
            vm.run(QuickAction.CheckForUpdates)
            fakeWork.manual.value = listOf(work("FAILED"), running(1, 10))
            runCurrent()
            fakeWork.manual.value = listOf(work("FAILED"), succeeded())
            fakeReports.reportFlow.value = report(updated = 3)
            runCurrent()
            assertEquals(ManualCheckState.Finished(CheckOutcome.Updates(3)), expectMostRecentItem().checkState)
        }
    }

    @Test
    fun runEndedWithoutNewReportIsSourcesFailed() = runTest {
        fakeReports.reportFlow.value = report(updated = 3, startedAt = OLD)
        val vm = vm()
        vm.uiState.test {
            runCurrent()
            vm.run(QuickAction.CheckForUpdates)
            fakeWork.manual.value = listOf(running(1, 10))
            runCurrent()
            fakeWork.manual.value = emptyList()
            runCurrent()
            assertEquals(ManualCheckState.Finished(CheckOutcome.SourcesFailed), expectMostRecentItem().checkState)
        }
    }

    @Test
    fun emptyWorkerInfoHidesCount() = runTest {
        val vm = vm()
        vm.uiState.test {
            runCurrent()
            assertNull(expectMostRecentItem().scheduledCount)
        }
    }

    @Test
    fun workerInfoShowsCount() = runTest {
        fakeHandler.workers.value = listOf(workerModel(), workerModel())
        val vm = vm()
        vm.uiState.test {
            runCurrent()
            assertEquals(2, expectMostRecentItem().scheduledCount)
        }
    }

    @Test
    fun uiStateEmitsWhenAllWorkNeverEmits() = runTest {
        // Desktop: allWorkCheck is emptyFlow(); combine would never emit without an initial value.
        // A report forces a real combine emission; the stateIn default would show Never.
        fakeReports.reportFlow.value = report(updated = 0)
        val vm = vm(FakeWork(emptyFlow()).also { fakeWork = it })
        vm.uiState.test {
            runCurrent()
            val state = expectMostRecentItem()
            assertEquals(ManualCheckState.Idle, state.checkState)
            assertIs<LastChecked.Ago>(state.lastChecked)
        }
    }
}
