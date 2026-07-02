package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertTrue

class AllNotesViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: NotesDatabase

    // The ViewModel observes NotesDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun note(
        itemUrl: String,
        itemTitle: String = "Title",
        content: String = "Content",
        timestamp: Long = 0L,
    ) = NoteItem(
        itemUrl = itemUrl,
        itemTitle = itemTitle,
        content = content,
        timestamp = timestamp,
    )

    private fun viewModel() = AllNotesViewModel(
        notesDao = database.notesDao(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("all-notes-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<NotesDatabase>(name = dbFile.absolutePath)
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

    @Test fun `starts with no notes`() = runTest {
        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.notes.collect {} }
        assertTrue(vm.notes.value.isEmpty())
    }

    @Test fun `existing notes show up after collection`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(note("https://example.com/1"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.notes.collect {} }
        awaitCondition { vm.notes.value.isNotEmpty() }

        assertEquals(1, vm.notes.value.size)
        assertEquals("https://example.com/1", vm.notes.value[0].itemUrl)
    }

    @Test fun `updateQuery filters notes by content`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(note("https://example.com/1", content = "A story about dragons"))
        dao.upsertNote(note("https://example.com/2", content = "A story about robots"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.notes.collect {} }
        awaitCondition { vm.notes.value.size == 2 }

        vm.updateQuery("dragons")
        awaitCondition { vm.notes.value.size == 1 }

        assertEquals("https://example.com/1", vm.notes.value[0].itemUrl)
    }

    @Test fun `updateQuery filters notes by itemTitle`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(note("https://example.com/1", itemTitle = "Dragon Tales", content = "Nothing relevant"))
        dao.upsertNote(note("https://example.com/2", itemTitle = "Robot Tales", content = "Nothing relevant"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.notes.collect {} }
        awaitCondition { vm.notes.value.size == 2 }

        vm.updateQuery("Dragon")
        awaitCondition { vm.notes.value.size == 1 }

        assertEquals("https://example.com/1", vm.notes.value[0].itemUrl)
    }

    @Test fun `blank query resets to all notes`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(note("https://example.com/1", content = "Alpha"))
        dao.upsertNote(note("https://example.com/2", content = "Beta"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.notes.collect {} }
        awaitCondition { vm.notes.value.size == 2 }

        vm.updateQuery("Alpha")
        awaitCondition { vm.notes.value.size == 1 }

        vm.updateQuery("")
        awaitCondition { vm.notes.value.size == 2 }
    }

    @Test fun `saveNote with blank content deletes the note`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(note("https://example.com/1"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.notes.collect {} }
        awaitCondition { vm.notes.value.isNotEmpty() }

        vm.saveNote(vm.notes.value[0], "")

        awaitCondition { vm.notes.value.isEmpty() }
        assertNull(dao.getNote("https://example.com/1").first())
    }

    @Test fun `saveNote with non-blank content upserts the note`() = runTest {
        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.notes.collect {} }
        val target = note("https://example.com/1", content = "Original")

        vm.saveNote(target, "Updated content")

        awaitCondition { vm.notes.value.isNotEmpty() }
        assertEquals("Updated content", vm.notes.value[0].content)
    }

    @Test fun `deleteNote removes the note`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(note("https://example.com/1"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.notes.collect {} }
        awaitCondition { vm.notes.value.isNotEmpty() }

        vm.deleteNote("https://example.com/1")

        awaitCondition { vm.notes.value.isEmpty() }
        assertNull(dao.getNote("https://example.com/1").first())
    }
}
