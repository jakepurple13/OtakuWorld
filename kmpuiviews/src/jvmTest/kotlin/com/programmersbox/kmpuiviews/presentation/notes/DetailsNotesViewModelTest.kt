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

class DetailsNotesViewModelTest {

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

    private fun viewModel(itemUrl: String = "https://example.com/1", itemTitle: String = "Title") =
        DetailsNotesViewModel(
            itemUrl = itemUrl,
            itemTitle = itemTitle,
            notesDao = database.notesDao(),
        ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("details-notes-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<NotesDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModelStore.clear()
        database.close()
        dbFile.delete()
    }

    @Test fun `starts with no note when none exists`() = runTest {
        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.note.collect {} }
        assertNull(vm.note.value)
    }

    @Test fun `existing note is loaded into state after collection`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(
            NoteItem(
                itemUrl = "https://example.com/1",
                itemTitle = "Title",
                content = "Existing content",
                timestamp = 0L,
            )
        )

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        assertEquals("Existing content", vm.note.value?.content)
    }

    @Test fun `saveNote with content inserts a note visible in state`() = runTest {
        val vm = viewModel(itemUrl = "https://example.com/1", itemTitle = "My Title")
        val __sub = backgroundScope.launch { vm.note.collect {} }
        vm.saveNote("Some new content")
        awaitCondition { vm.note.value != null }

        assertEquals("Some new content", vm.note.value?.content)
        assertEquals("My Title", vm.note.value?.itemTitle)
        assertEquals("https://example.com/1", vm.note.value?.itemUrl)
    }

    @Test fun `saveNote updates existing note content`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(
            NoteItem(
                itemUrl = "https://example.com/1",
                itemTitle = "Title",
                content = "First",
                timestamp = 0L,
            )
        )

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value?.content == "First" }

        vm.saveNote("Second")
        awaitCondition { vm.note.value?.content == "Second" }
    }

    @Test fun `saveNote with blank content deletes the note`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(
            NoteItem(
                itemUrl = "https://example.com/1",
                itemTitle = "Title",
                content = "Existing content",
                timestamp = 0L,
            )
        )

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        vm.saveNote("   ")
        awaitCondition { vm.note.value == null }

        assertNull(vm.note.value)
    }

    @Test fun `deleteNote removes an existing note from state`() = runTest {
        val dao = database.notesDao()
        dao.upsertNote(
            NoteItem(
                itemUrl = "https://example.com/1",
                itemTitle = "Title",
                content = "Existing content",
                timestamp = 0L,
            )
        )

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        vm.deleteNote()
        awaitCondition { vm.note.value == null }

        assertNull(vm.note.value)
    }
}
