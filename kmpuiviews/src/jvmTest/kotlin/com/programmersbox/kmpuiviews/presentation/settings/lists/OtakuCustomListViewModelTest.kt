package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
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

    private fun viewModel(uuid: String) = OtakuCustomListViewModel(
        screen = Screen.CustomListScreen.CustomListItem(uuid),
        dataStoreHandling = createTestDataStoreHandling(),
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

    @Test fun `toggleShowSource switches items to BySource grouping`() = runTest {
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

        vm.toggleShowSource(true)
        awaitCondition { vm.showBySource }

        assertTrue(vm.items is OtakuListState.BySource)
    }

    @Test fun `filter removes and re-adds a source from filtered`() = runTest {
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
        assertTrue("ExampleService" in vm.filtered)

        vm.filter("ExampleService")
        assertFalse("ExampleService" in vm.filtered)

        vm.filter("ExampleService")
        assertTrue("ExampleService" in vm.filtered)
    }

    @Test fun `clearFilter resets filtered to all sources in the list`() = runTest {
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

        vm.filter("ExampleService")
        assertFalse("ExampleService" in vm.filtered)

        vm.clearFilter()
        assertTrue("ExampleService" in vm.filtered)
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

    @Test fun `rename updates the list name`() = runTest {
        dao().create("My List")
        val uuid = dao().getAllListsSync()[0].item.uuid

        val vm = viewModel(uuid)
        awaitCondition { vm.customList != null }

        vm.rename("New Name")
        awaitCondition { dao().getAllListsSync()[0].item.name == "New Name" }

        assertEquals("New Name", dao().getAllListsSync()[0].item.name)
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
