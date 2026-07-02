package com.programmersbox.kmpuiviews.presentation.settings.lists.imports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportFullListViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: ListDatabase

    // The ViewModel's init{} launches a coroutine on viewModelScope to read the file and
    // set importStatus asynchronously. A test-dispatcher virtual-clock advance doesn't drive
    // real file IO, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun customList(uuid: String, name: String, items: List<CustomListInfo> = emptyList()) = CustomList(
        item = CustomListItem(uuid = uuid, name = name),
        list = items,
    )

    private fun customListInfo(uuid: String, url: String) = CustomListInfo(
        uuid = uuid,
        title = "Title $url",
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
    )

    private fun writeJsonFile(content: String): File =
        File.createTempFile("import-full-list-test", ".json").also {
            it.deleteOnExit()
            it.writeText(content)
        }

    private fun viewModel(uri: String) = ImportFullListViewModel(
        listRepository = ListRepository(
            listDao = database.listDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
        savedStateHandle = SavedStateHandle(mapOf("uri" to uri, "route" to "import_full_list")),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("list-repo-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `valid file decodes into Success with the imported list`() = runTest {
        val list = listOf(
            customList("uuid-1", "My List", listOf(customListInfo("uuid-1", "https://example.com/1"))),
        )
        val file = writeJsonFile(Json.encodeToString(list))

        val vm = viewModel(file.absolutePath)
        awaitCondition { vm.importStatus !is ImportFullListStatus.Loading }

        // CustomListItem.time defaults to Clock.System.now() and Json (encodeDefaults = false)
        // omits values equal to their default, so a fresh timestamp is assigned on decode.
        // Compare everything except that field instead of full object equality.
        val status = assertIs<ImportFullListStatus.Success>(vm.importStatus)
        assertEquals(1, status.customList.size)
        assertEquals(list[0].item.copy(time = 0), status.customList[0].item.copy(time = 0))
        assertEquals(list[0].list, status.customList[0].list)
        assertEquals(1, vm.importingList.size)
        assertEquals(list[0].item.copy(time = 0), vm.importingList[0].item.copy(time = 0))
        assertEquals(list[0].list, vm.importingList[0].list)
    }

    @Test fun `missing file decodes into Error`() = runTest {
        val vm = viewModel("/tmp/does-not-exist-${System.nanoTime()}.json")
        awaitCondition { vm.importStatus !is ImportFullListStatus.Loading }

        assertIs<ImportFullListStatus.Error>(vm.importStatus)
    }

    @Test fun `malformed json decodes into Error`() = runTest {
        val file = writeJsonFile("not valid json")

        val vm = viewModel(file.absolutePath)
        awaitCondition { vm.importStatus !is ImportFullListStatus.Loading }

        assertIs<ImportFullListStatus.Error>(vm.importStatus)
    }

    @Test fun `importList persists the imported lists and items into the database`() = runTest {
        val list = listOf(
            customList(
                uuid = "uuid-1",
                name = "My List",
                items = listOf(
                    customListInfo("uuid-1", "https://example.com/1"),
                    customListInfo("uuid-1", "https://example.com/2"),
                ),
            ),
        )
        val file = writeJsonFile(Json.encodeToString(list))

        val vm = viewModel(file.absolutePath)
        awaitCondition { vm.importStatus !is ImportFullListStatus.Loading }

        vm.importList()

        val persisted = database.listDao().getAllLists().first()
        assertEquals(1, persisted.size)
        assertEquals("My List", persisted[0].item.name)
        assertEquals(2, persisted[0].list.size)
        assertTrue(persisted[0].list.any { it.url == "https://example.com/1" })
        assertTrue(persisted[0].list.any { it.url == "https://example.com/2" })
    }
}
