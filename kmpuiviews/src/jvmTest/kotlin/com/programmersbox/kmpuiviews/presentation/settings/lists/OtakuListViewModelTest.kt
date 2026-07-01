package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
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
import kotlin.test.assertTrue

class OtakuListViewModelTest {

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

    private fun viewModel() = OtakuListViewModel(
        listDao = dao(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("otaku-list-viewmodel-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `starts with no custom lists`() = runTest {
        val vm = viewModel()

        assertTrue(vm.customLists.isEmpty())
    }

    @Test fun `created list shows up in customLists after collection`() = runTest {
        dao().create("My List")

        val vm = viewModel()
        awaitCondition { vm.customLists.isNotEmpty() }

        assertEquals(1, vm.customLists.size)
        assertEquals("My List", vm.customLists[0].item.name)
    }

    @Test fun `removed list disappears from customLists`() = runTest {
        dao().create("My List")
        val vm = viewModel()
        awaitCondition { vm.customLists.isNotEmpty() }

        dao().removeList(vm.customLists[0].item)
        awaitCondition { vm.customLists.isEmpty() }

        assertTrue(vm.customLists.isEmpty())
    }
}
