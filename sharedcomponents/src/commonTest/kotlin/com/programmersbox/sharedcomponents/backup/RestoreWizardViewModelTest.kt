package com.programmersbox.sharedcomponents.backup

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

private class RestoreFakeUiInfo(override val key: String) : BackupUiInfo {
    override val displayName = key
    override val description: String? = null
    override val icon = null
    override suspend fun currentSummary() = BackupDataSummary()
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(itemCount = 5)
}

class RestoreWizardViewModelTest {

    // pickFile() and confirm() launch work inside viewModelScope, which runs on the real Main
    // dispatcher (set to Dispatchers.Default below) rather than runTest's virtual scheduler.
    // A test-dispatcher advance doesn't drive that work, so poll for it with real time.
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
    fun `pickFile runs the peek pass and moves to SelectItems with only matched entries`() = runTest {
        val a = RestoreFakeUiInfo("a")
        val b = RestoreFakeUiInfo("b")
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(a, b),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 5)) },
            startRestore = { _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }

        val state = vm.state.value
        assertEquals(RestoreWizardStep.SelectItems, state.step)
        assertEquals(listOf("a"), state.items.map { it.uiInfo.key })
        assertEquals(5, state.items.single().summary?.itemCount)
    }

    @Test
    fun `confirm calls startRestore with the picked file and selected keys`() = runTest {
        var called: Pair<String, Set<String>>? = null
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys -> called = file to keys },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }
        vm.goToReview()
        vm.confirm()

        assertEquals("file.zip" to setOf("a"), called)
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = results,
            startRestore = { _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }
        vm.goToReview()
        vm.confirm()
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", success = true))
        awaitCondition { vm.state.value.step == RestoreWizardStep.Complete }
        assertEquals(RestoreWizardStep.Complete, vm.state.value.step)
        assertEquals(1, vm.state.value.results.size)
    }
}
