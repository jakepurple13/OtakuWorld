package com.programmersbox.kmpuiviews.backup

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.RestoreWizardStep
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.RestoreWizardViewModel
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

private class RestoreFakeUiInfo(override val key: String) : BackupUiInfo {
    override val displayName = key
    override val description: String? = null
    override val icon = null
    override suspend fun currentSummary() = BackupDataSummary()
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(itemCount = 5)
}

private fun customList(name: String) = CustomList(
    item = CustomListItem(uuid = name, name = name),
    list = listOf(CustomListInfo(uuid = name, title = "T", description = "D", url = "https://example.com/$name", imageUrl = "https://example.com/$name.jpg", source = "Src")),
)

class RestoreWizardViewModelTest {

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
            startRestore = { _, _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }

        val state = vm.state.value
        assertEquals(RestoreWizardStep.SelectItems, state.step)
        assertEquals(listOf("a"), state.items.map { it.uiInfo.key })
        assertEquals(5, state.items.single().summary?.itemCount)
    }

    @Test
    fun `confirm calls startRestore with the picked file selected keys and null list filter`() = runTest {
        var called: Triple<String, Set<String>, Set<String>?>? = null
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys, listIds -> called = Triple(file, keys, listIds) },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }
        vm.goToReview()
        vm.confirm()

        assertEquals(Triple("file.zip", setOf("a"), null), called)
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = results,
            startRestore = { _, _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }
        vm.goToReview()
        vm.confirm()
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", timeTaken = "100ms", success = true))
        awaitCondition { vm.state.value.step == RestoreWizardStep.Complete }
        assertEquals(RestoreWizardStep.Complete, vm.state.value.step)
        assertEquals(1, vm.state.value.results.size)
    }

    @Test
    fun `pickFile loads subItems for the lists row from peekListContents not the local db`() = runTest {
        val listUiInfo = RestoreFakeUiInfo("lists.json")
        val zipLists = listOf(customList("zip-list-a"), customList("zip-list-b"))
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(listUiInfo),
            peekZip = { mapOf("lists.json" to BackupDataSummary(itemCount = 2)) },
            peekListContents = { zipLists },
            startRestore = { _, _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.items.singleOrNull()?.subItems != null }

        val subItems = vm.state.value.items.single().subItems!!
        assertEquals(setOf("zip-list-a", "zip-list-b"), subItems.map { it.name }.toSet())
    }

    @Test
    fun `toggleListSelected flips one sub-item confirm sends only the selected list ids`() = runTest {
        val listUiInfo = RestoreFakeUiInfo("lists.json")
        val zipLists = listOf(customList("zip-list-a"), customList("zip-list-b"))
        var called: Triple<String, Set<String>, Set<String>?>? = null
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(listUiInfo),
            peekZip = { mapOf("lists.json" to BackupDataSummary(itemCount = 2)) },
            peekListContents = { zipLists },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys, listIds -> called = Triple(file, keys, listIds) },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.items.singleOrNull()?.subItems != null }

        vm.toggleListSelected("zip-list-b")
        vm.goToReview()
        vm.confirm()

        assertEquals(setOf("zip-list-a"), called?.third)
    }

    @Test
    fun `confirm sends null list filter when peekListContents returns empty instead of a silent-skip empty set`() = runTest {
        val listUiInfo = RestoreFakeUiInfo("lists.json")
        var called: Triple<String, Set<String>, Set<String>?>? = null
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(listUiInfo),
            peekZip = { mapOf("lists.json" to BackupDataSummary(itemCount = 0)) },
            peekListContents = { emptyList() },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys, listIds -> called = Triple(file, keys, listIds) },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.items.singleOrNull()?.subItems != null }
        vm.goToReview()
        vm.confirm()

        assertEquals(null, called?.third)
    }
}
