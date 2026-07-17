package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.kmpuiviews.repository.DictionaryRepository
import com.programmersbox.kmpuiviews.repository.DictionarySort
import com.programmersbox.kmpuiviews.repository.StubTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

class DictionaryListViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var repository: DictionaryRepository

    // Room's Flow emits on its own dispatcher, not the test dispatcher's virtual clock,
    // so wait for state changes with real time instead of advancing test time.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel() = DictionaryListViewModel(repository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("dictionary-list-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = DictionaryRepository(database.dictionaryDao(), StubTranslationService())
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

    @Test fun `starts with no entries`() = runTest {
        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        assertTrue(vm.entries.value.isEmpty())
    }

    @Test fun `existing entries show up after collection sorted by term`() = runTest {
        repository.save(DictionaryEntry(term = "Zulu"))
        repository.save(DictionaryEntry(term = "Alpha"))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        assertEquals(listOf("Alpha", "Zulu"), vm.entries.value.map { it.term })
    }

    @Test fun `updateQuery filters entries by term`() = runTest {
        repository.save(DictionaryEntry(term = "Sensei"))
        repository.save(DictionaryEntry(term = "Gakusei"))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        vm.updateQuery("Sen")
        awaitCondition { vm.entries.value.size == 1 }

        assertEquals("Sensei", vm.entries.value[0].term)
    }

    @Test fun `blank query resets to all entries`() = runTest {
        repository.save(DictionaryEntry(term = "Sensei"))
        repository.save(DictionaryEntry(term = "Gakusei"))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        vm.updateQuery("Sen")
        awaitCondition { vm.entries.value.size == 1 }

        vm.updateQuery("")
        awaitCondition { vm.entries.value.size == 2 }
    }

    @Test fun `updateSort switches ordering to date added`() = runTest {
        repository.save(DictionaryEntry(term = "First", dateAdded = 100L))
        repository.save(DictionaryEntry(term = "Second", dateAdded = 200L))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        vm.updateSort(DictionarySort.DateAdded)
        awaitCondition { vm.entries.value.map { it.term } == listOf("Second", "First") }
    }

    @Test fun `delete removes the entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))
        val stored = repository.getById(id).first()

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.isNotEmpty() }

        vm.delete(stored!!)

        awaitCondition { vm.entries.value.isEmpty() }
    }
}
