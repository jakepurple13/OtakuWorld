package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.StubTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import kotlin.test.assertNull

class DictionaryDetailViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var repository: DictionaryRepository

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(id: Long) = DictionaryDetailViewModel(id, repository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("dictionary-detail-vm-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `entry emits the stored value for the given id`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        assertEquals("Sensei", vm.entry.value?.term)
    }

    @Test fun `delete removes the entry and entry becomes null`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        vm.delete()

        awaitCondition { vm.entry.value == null }
        assertNull(vm.entry.value)
    }
}
