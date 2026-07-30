package com.programmersbox.kmpuiviews.backup

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardStep
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardViewModel
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
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

private fun fakeListDao(lists: List<CustomList>): ListDao = object : ListDao by UnimplementedListDao {
    override suspend fun getAllListsSync(): List<CustomList> = lists
}

/** Every method is unimplemented — these tests only ever call `getAllListsSync`, overridden per-fake above via delegation. */
private object UnimplementedListDao : ListDao {
    private fun unsupported(): Nothing = throw NotImplementedError("not used in these tests")
    override fun getAllLists() = throw NotImplementedError()
    override fun getAllListsCount() = throw NotImplementedError()
    override fun getAllListItemsCount() = throw NotImplementedError()
    override suspend fun getAllListsSync(): List<CustomList> = unsupported()
    override suspend fun getCustomListItem(uuid: String) = unsupported()
    override fun getCustomListItemFlow(uuid: String) = throw NotImplementedError()
    override suspend fun createList(listItem: CustomListItem) = unsupported()
    override suspend fun addItem(listItem: CustomListInfo) = unsupported()
    override suspend fun removeItem(listItem: CustomListInfo) = unsupported()
    override suspend fun removeItem(uuid: String) = unsupported()
    override suspend fun updateList(listItem: CustomListItem) = unsupported()
    override suspend fun removeList(item: CustomListItem) = unsupported()
    override suspend fun updateBiometric(uuid: String, useBiometric: Boolean) = unsupported()
    override suspend fun getDirtyCustomListItems() = unsupported()
    override fun observeDirtyCustomListItemCount() = throw NotImplementedError()
    override suspend fun getDirtyCustomListInfo() = unsupported()
    override fun observeDirtyCustomListInfoCount() = throw NotImplementedError()
    override suspend fun getCustomListItemByUuid(uuid: String) = unsupported()
    override suspend fun getCustomListInfoByUniqueId(uniqueId: String) = unsupported()
    override suspend fun updateCustomListItem(item: CustomListItem) = unsupported()
    override suspend fun updateCustomListInfo(info: CustomListInfo) = unsupported()
    override suspend fun softDeleteCustomListItem(uuid: String, timestamp: Long) = unsupported()
    override suspend fun softDeleteCustomListInfo(uniqueId: String, timestamp: Long) = unsupported()
    override suspend fun markCustomListItemSynced(uuid: String, timestamp: Long) = unsupported()
    override suspend fun markCustomListInfoSynced(uniqueId: String, timestamp: Long) = unsupported()
    override suspend fun getAllCustomListItemsSync() = unsupported()
    override suspend fun resetAllCustomListItemsIsDeleted() = unsupported()
    override suspend fun deleteAllDeletedCustomListItems() = unsupported()
    override suspend fun getAllCustomListInfoSync() = unsupported()
    override suspend fun resetAllCustomListInfoIsDeleted() = unsupported()
    override suspend fun deleteAllDeletedCustomListInfo() = unsupported()
}

private fun customList(name: String) = CustomList(
    item = CustomListItem(uuid = name, name = name),
    list = listOf(CustomListInfo(uuid = name, title = "T", description = "D", url = "https://example.com/$name", imageUrl = "https://example.com/$name.jpg", source = "Src")),
)

class BackupWizardViewModelTest {

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
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _, _ -> })
        val state = vm.state.value
        assertEquals(BackupWizardStep.SelectItems, state.step)
        assertTrue(state.items.all { it.selected })
    }

    @Test
    fun `deselectAll clears selection, selectAll restores it`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _, _ -> })
        vm.deselectAll()
        assertTrue(vm.state.value.items.none { it.selected })
        vm.selectAll()
        assertTrue(vm.state.value.items.all { it.selected })
    }

    @Test
    fun `toggleSelected flips a single item`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _, _ -> })
        vm.toggleSelected("a")
        assertEquals(false, vm.state.value.items.first { it.uiInfo.key == "a" }.selected)
        assertEquals(true, vm.state.value.items.first { it.uiInfo.key == "b" }.selected)
    }

    @Test
    fun `goToReview only carries selected items, confirm calls startBackup with the file, keys, and null list filter`() = runTest {
        var startedWith: Triple<String, Set<String>, Set<String>?>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys, listIds -> startedWith = Triple(file, keys, listIds) },
        )
        vm.toggleSelected("b")
        vm.goToReview()
        assertEquals(BackupWizardStep.Review, vm.state.value.step)
        assertEquals(listOf("a"), vm.state.value.items.map { it.uiInfo.key })

        vm.confirm("file.zip")
        assertEquals(Triple("file.zip", setOf("a"), null), startedWith)
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = results,
            startBackup = { _, _, _ -> },
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

    @Test
    fun `lists json row loads subItems from listDao`() = runTest {
        val lists = listOf(customList("list-a"), customList("list-b"))
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("lists.json")),
            listDao = fakeListDao(lists),
            startBackup = { _, _, _ -> },
        )
        awaitCondition { vm.state.value.items.single().subItems != null }
        val subItems = vm.state.value.items.single().subItems!!
        assertEquals(setOf("list-a", "list-b"), subItems.map { it.name }.toSet())
        assertTrue(subItems.all { it.selected })
    }

    @Test
    fun `toggleListSelected flips one sub-item, confirm sends only the selected list ids`() = runTest {
        val lists = listOf(customList("list-a"), customList("list-b"))
        var startedWith: Triple<String, Set<String>, Set<String>?>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("lists.json")),
            listDao = fakeListDao(lists),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys, listIds -> startedWith = Triple(file, keys, listIds) },
        )
        awaitCondition { vm.state.value.items.single().subItems != null }

        vm.toggleListSelected("list-b")
        assertEquals(
            setOf("list-a"),
            vm.state.value.items.single().subItems!!.filter { it.selected }.map { it.id }.toSet(),
        )

        vm.goToReview()
        vm.confirm("file.zip")
        assertEquals("file.zip", startedWith?.first)
        assertEquals(setOf("list-a"), startedWith?.third)
    }

    @Test
    fun `confirm sends null list filter when listDao returns no lists`() = runTest {
        var startedWith: Triple<String, Set<String>, Set<String>?>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("lists.json")),
            listDao = fakeListDao(emptyList()),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys, listIds -> startedWith = Triple(file, keys, listIds) },
        )
        awaitCondition { vm.state.value.items.single().subItems != null }

        vm.goToReview()
        vm.confirm("file.zip")

        assertEquals(null, startedWith?.third)
    }
}
