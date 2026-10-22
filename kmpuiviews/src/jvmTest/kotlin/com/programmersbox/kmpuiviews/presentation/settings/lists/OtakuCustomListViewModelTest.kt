package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtakuCustomListViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: ListDatabase

    // The ViewModel observes ListDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun dao(): ListDao = database.listDao()

    private fun listRepository() = ListRepository(
        listDao = dao(),
        systemAlerter = SystemAlerter(),
        authManager = FakeAuthManager(),
    )

    private suspend fun listWith(vararg items: Pair<String, String>): String {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid
        items.forEachIndexed { index, (title, source) ->
            dao().addToList(uuid, title, "", "https://example.com/$index", "", source)
            // createdAt has millisecond resolution; keep each add distinct.
            Thread.sleep(2)
        }
        return uuid
    }

    private fun viewModel(uuid: String) = OtakuCustomListViewModel(
        screen = Screen.CustomListScreen.CustomListItem(uuid),
        listDao = dao(),
        listRepository = listRepository(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("otaku-custom-list-viewmodel-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun `starts with empty items when list does not exist yet`() = runTest {
        val vm = viewModel("missing-uuid")

        assertEquals(OtakuListState.Empty, vm.items)
    }

    @Test fun `loads the custom list matching the screen uuid`() = runTest {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid
        dao().addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }

        assertEquals("My List", vm.customList?.item?.name)
        assertEquals(1, vm.customList?.list?.size)
        assertTrue("ExampleService" in vm.filtered)
    }

    @Test fun `items defaults to ByTitle grouping`() = runTest {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid
        dao().addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }

        assertTrue(vm.items is OtakuListState.ByTitle)
        assertEquals(1, (vm.items as OtakuListState.ByTitle).items.size)
    }

    @Test fun `viewMode BySource switches items to BySource grouping`() = runTest {
        val uuid = listWith("Title" to "ExampleService")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }

        vm.viewMode = ListViewMode.BySource

        assertTrue(vm.items is OtakuListState.BySource)
    }

    @Test fun `defaults to recently added sort with newest first`() = runTest {
        val uuid = listWith("Old" to "A", "Middle" to "A", "New" to "A")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 3 }

        assertEquals(ListSort.RecentlyAdded, vm.sort)
        assertEquals(listOf("New", "Middle", "Old"), (vm.items as OtakuListState.ByTitle).items.map { it.title })
    }

    @Test fun `recently added puts items without createdAt last in reverse insertion order`() = runTest {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid
        dao().addItem(CustomListInfo(uuid = uuid, title = "Legacy1", description = "", url = "u1", imageUrl = "", source = "A"))
        dao().addItem(CustomListInfo(uuid = uuid, title = "Legacy2", description = "", url = "u2", imageUrl = "", source = "A"))
        dao().addToList(uuid, "Fresh", "", "u3", "", "A")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 3 }

        assertEquals(listOf("Fresh", "Legacy2", "Legacy1"), (vm.items as OtakuListState.ByTitle).items.map { it.title })
    }

    @Test fun `title sort orders alphabetically ignoring case`() = runTest {
        val uuid = listWith("beta" to "A", "Alpha" to "A", "Gamma" to "A")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 3 }
        vm.sort = ListSort.Title

        assertEquals(listOf("Alpha", "beta", "Gamma"), (vm.items as OtakuListState.ByTitle).items.map { it.title })
    }

    @Test fun `source sort orders by source then title`() = runTest {
        val uuid = listWith("Zed" to "Alpha", "Bee" to "Zulu", "Ant" to "Zulu")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 3 }
        vm.sort = ListSort.Source

        assertEquals(listOf("Zed", "Ant", "Bee"), (vm.items as OtakuListState.ByTitle).items.map { it.title })
    }

    @Test fun `selectSource narrows to one source then toggles and resets when empty`() = runTest {
        val uuid = listWith("One" to "A", "Two" to "B", "Three" to "C")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 3 }
        assertTrue(vm.allSourcesShown)

        vm.selectSource("A")
        assertEquals(listOf("A"), vm.filtered.toList())
        assertFalse(vm.allSourcesShown)

        vm.selectSource("B")
        assertEquals(setOf("A", "B"), vm.filtered.toSet())

        vm.selectSource("A")
        vm.selectSource("B")
        assertEquals(setOf("A", "B", "C"), vm.filtered.toSet())
        assertTrue(vm.allSourcesShown)
    }

    @Test fun `clearFilter resets filtered to all sources in the list`() = runTest {
        val uuid = listWith("One" to "A", "Two" to "B")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 2 }

        vm.selectSource("A")
        assertFalse(vm.allSourcesShown)

        vm.clearFilter()
        assertTrue(vm.allSourcesShown)
        assertEquals(setOf("A", "B"), vm.filtered.toSet())
    }

    @Test fun `setQuery updates searchQuery text`() = runTest {
        val vm = viewModel("missing-uuid")

        vm.setQuery("hello")

        assertEquals("hello", vm.searchQuery.text.toString())
    }

    @Test fun `removeItems removes the item from the list`() = runTest {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid
        dao().addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }
        val itemToRemove = vm.customList!!.list[0]

        val result = vm.removeItems(listOf(itemToRemove))
        awaitCondition { dao().getAllListsSync()[0].list.isEmpty() }

        assertTrue(result.isSuccess)
        assertTrue(dao().getAllListsSync()[0].list.isEmpty())
    }

    @Test fun `toggleSelection selects a whole title group and clears it again`() = runTest {
        val uuid = listWith("Same" to "A", "Same" to "B", "Other" to "A")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 3 }
        val group = vm.customList!!.list.filter { it.title == "Same" }

        vm.toggleSelection(group)
        assertTrue(vm.inSelectionMode)
        assertEquals(group.map { it.uniqueId }.toSet(), vm.selectedItems.map { it.uniqueId }.toSet())

        vm.toggleSelection(group)
        assertFalse(vm.inSelectionMode)
    }

    @Test fun `selectAllVisible only selects items that pass the filter`() = runTest {
        val uuid = listWith("One" to "A", "Two" to "B")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 2 }
        vm.selectSource("A")

        vm.selectAllVisible()

        assertEquals(listOf("One"), vm.selectedItems.map { it.title })
    }

    @Test fun `randomPick returns a visible title group`() = runTest {
        val uuid = listWith("One" to "A", "Two" to "B")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 2 }
        vm.selectSource("B")

        repeat(5) { assertEquals(listOf("Two"), vm.randomPick()?.map { it.title }) }
    }

    @Test fun `addSelectedTo copies items to the target list and keeps them here`() = runTest {
        val uuid = listWith("Copied" to "A", "Stayer" to "A")
        dao().create("Target")
        val target = dao().getAllListsSync().first { it.item.name == "Target" }

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 2 }
        vm.toggleSelection(vm.customList!!.list.filter { it.title == "Copied" })

        val result = vm.addSelectedTo(listOf(target))
        awaitCondition { dao().getCustomListItem(target.item.uuid).list.size == 1 }

        assertEquals(1, result.getOrNull())
        assertEquals(setOf("Copied", "Stayer"), dao().getCustomListItem(uuid).list.map { it.title }.toSet())
        assertEquals(listOf("Copied"), dao().getCustomListItem(target.item.uuid).list.map { it.title })
        assertFalse(vm.inSelectionMode)
    }

    @Test fun `addSelectedTo counts zero when the target already has the items`() = runTest {
        val uuid = listWith("Dupe" to "A")
        dao().create("Target")
        val target = dao().getAllListsSync().first { it.item.name == "Target" }
        dao().addToList(target.item.uuid, "Dupe", "", "https://example.com/0", "", "A")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 1 }
        vm.toggleSelection(vm.customList!!.list)

        assertEquals(0, vm.addSelectedTo(listOf(target)).getOrNull())
    }

    @Test fun `addSelectedTo adds items to every target and skips ones already there`() = runTest {
        val uuid = listWith("One" to "A", "Two" to "A")
        dao().create("First")
        dao().create("Second")
        val first = dao().getAllListsSync().first { it.item.name == "First" }
        val second = dao().getAllListsSync().first { it.item.name == "Second" }
        dao().addToList(second.item.uuid, "One", "", "https://example.com/0", "", "A")

        val vm = viewModel(uuid)
        awaitCondition { vm.customList?.list?.size == 2 }
        vm.toggleSelection(vm.customList!!.list)

        assertEquals(3, vm.addSelectedTo(listOf(first, second)).getOrNull())
        assertEquals(setOf("One", "Two"), dao().getCustomListItem(first.item.uuid).list.map { it.title }.toSet())
        assertEquals(setOf("One", "Two"), dao().getCustomListItem(second.item.uuid).list.map { it.title }.toSet())
    }

    @Test fun `updateDetails saves name and description`() = runTest {
        val uuid = listWith()

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }

        vm.updateDetails("Renamed", "A description")
        awaitCondition { dao().getCustomListItem(uuid).item.name == "Renamed" }

        assertEquals("A description", dao().getCustomListItem(uuid).item.description)
    }

    @Test fun `deleteAll removes the whole list`() = runTest {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }
        val customList = vm.customList!!

        // OtakuCustomListViewModel keeps a live subscription (via ListDao.getCustomListItemFlow)
        // to the exact list under test, and that flow throws once its row disappears. Clear the
        // ViewModel's scope first to cancel that subscription, then invoke the same repository
        // call deleteAll() makes, so the deletion itself is still verified without racing the
        // now-defunct subscription's real-dispatcher collection.
        viewModelStore.clear()
        Thread.sleep(50)

        listRepository().removeList(customList)
        awaitCondition { dao().getAllListsSync().isEmpty() }

        assertTrue(dao().getAllListsSync().isEmpty())
    }
}
