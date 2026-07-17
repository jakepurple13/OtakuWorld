package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.kmpuiviews.presentation.Screen
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
import kotlin.test.assertNull

class DictionaryFormViewModelTest {

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

    private fun viewModel(id: Long?) = DictionaryFormViewModel(Screen.DictionaryScreen.Form(id), repository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("dictionary-form-vm-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `entry is null when id is null`() = runTest {
        val vm = viewModel(null)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        assertNull(vm.entry.value)
    }

    @Test fun `entry emits the stored value when id is set`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        assertEquals("Sensei", vm.entry.value?.term)
    }

    @Test fun `save with null id inserts a new entry with dateAdded set`() = runTest {
        val vm = viewModel(null)

        vm.save(
            term = "Sensei",
            definition = "Teacher",
            reading = "せんせい",
            category = "Honorifics",
            notes = null,
            language = "ja",
        )

        awaitCondition { repository.getAll(DictionarySort.Term).first().isNotEmpty() }
        val stored = repository.getAll(DictionarySort.Term).first().first()

        assertEquals("Sensei", stored.term)
        assertEquals("Teacher", stored.definition)
        assertEquals("せんせい", stored.reading)
        assertEquals("Honorifics", stored.category)
        assertEquals("ja", stored.language)
    }

    @Test fun `save with non-null id updates the existing entry without changing dateAdded`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei", dateAdded = 12345L))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        vm.save(
            term = "Sensei-updated",
            definition = null,
            reading = null,
            category = null,
            notes = null,
            language = null,
        )

        awaitCondition { vm.entry.value?.term == "Sensei-updated" }
        assertEquals(12345L, vm.entry.value?.dateAdded)
    }
}
