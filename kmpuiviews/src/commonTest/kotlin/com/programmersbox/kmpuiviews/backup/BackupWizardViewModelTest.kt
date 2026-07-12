package com.programmersbox.kmpuiviews.backup

import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardStep
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardViewModel
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

private class FakeUiInfo(override val key: String) : BackupUiInfo {
    override val displayName = key
    override val description: String? = null
    override val icon = null
    override suspend fun currentSummary() = BackupDataSummary(itemCount = 1)
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary()
}

class BackupWizardViewModelTest {

    // confirm() collects resultsFlow inside viewModelScope, which runs on the real Main
    // dispatcher (set to Dispatchers.Default below) rather than runTest's virtual scheduler.
    // A test-dispatcher advance doesn't drive that collection, so poll for it with real time.
    private suspend fun awaitCondition(condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) { while (!condition()) delay(10) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on SelectItems with all items selected`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _ -> })
        val state = vm.state.value
        assertEquals(BackupWizardStep.SelectItems, state.step)
        assertTrue(state.items.all { it.selected })
    }

    @Test
    fun `deselectAll clears selection, selectAll restores it`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _ -> })
        vm.deselectAll()
        assertTrue(vm.state.value.items.none { it.selected })
        vm.selectAll()
        assertTrue(vm.state.value.items.all { it.selected })
    }

    @Test
    fun `toggleSelected flips a single item`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _ -> })
        vm.toggleSelected("a")
        assertEquals(false, vm.state.value.items.first { it.uiInfo.key == "a" }.selected)
        assertEquals(true, vm.state.value.items.first { it.uiInfo.key == "b" }.selected)
    }

    @Test
    fun `goToReview only carries selected items, confirm calls startBackup with the file and their keys`() = runTest {
        var startedWith: Pair<String, Set<String>>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys -> startedWith = file to keys },
        )
        vm.toggleSelected("b")
        vm.goToReview()
        assertEquals(BackupWizardStep.Review, vm.state.value.step)
        assertEquals(listOf("a"), vm.state.value.items.map { it.uiInfo.key })

        vm.confirm("file.zip")
        assertEquals("file.zip" to setOf("a"), startedWith)
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = results,
            startBackup = { _, _ -> },
        )
        vm.goToReview()
        vm.confirm("file.zip")
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", timeTaken = "100ms", success = true))
        awaitCondition { vm.state.value.results.size == 1 }
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", timeTaken = "100ms", success = true), ItemResult("b", timeTaken = "100ms", success = true))
        awaitCondition { vm.state.value.step == BackupWizardStep.Complete }
        assertEquals(BackupWizardStep.Complete, vm.state.value.step)
        assertEquals(2, vm.state.value.results.size)
    }
}
