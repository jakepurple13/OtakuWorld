package com.programmersbox.kmpuiviews.presentation.notes

import com.programmersbox.kmpuiviews.testing.clearAndJoin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.kmpuiviews.testing.NoopAiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

class NoteDetailViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: NotesDatabase

    private val url = "https://example.com/1"

    // Room's Flow emits on its own dispatcher, so wait in real time (see DetailsNotesViewModelTest).
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel() = NoteDetailViewModel(
        itemUrl = url,
        notesDao = database.notesDao(),
        aiText = NoopAiText(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun seed(content: String = "a", supabaseId: String? = null) =
        database.notesDao().upsertNote(
            NoteItem(itemUrl = url, itemTitle = "Title", content = content, timestamp = 0L, supabaseId = supabaseId)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("note-detail-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<NotesDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clearAndJoin()
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun saveWritesContent() = runTest {
        seed("a")
        val vm = viewModel()
        backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        vm.save("b")

        awaitCondition { database.notesDao().getNoteByUrl(url)?.content == "b" }
    }

    @Test fun saveKeepsSyncFields() = runTest {
        seed("a", supabaseId = "remote-7")
        val vm = viewModel()
        backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        vm.save("b")

        awaitCondition { database.notesDao().getNoteByUrl(url)?.content == "b" }
        assertEquals("remote-7", database.notesDao().getNoteByUrl(url)?.supabaseId)
    }

    @Test fun saveBlankDeletes() = runTest {
        seed("a")
        val vm = viewModel()
        backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        vm.save("  ")

        awaitCondition { database.notesDao().getNoteByUrl(url) == null }
    }

    @Test fun vmMissingNoteEmitsNull() = runTest {
        seed("a")
        val vm = viewModel()
        backgroundScope.launch { vm.note.collect {} }
        awaitCondition { vm.note.value != null }

        database.notesDao().deleteNote(url)

        awaitCondition { vm.note.value == null }
        assertNull(vm.note.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun saveOnLeave() = runComposeUiTest {
        runBlocking { seed("a") }
        val vm = viewModel()
        var shown by mutableStateOf(true)
        setContent { if (shown) NoteDetailScreen(onBackPress = {}, vm = vm) }
        // waitUntil keeps pumping frames; blocking the test thread would stall the note collection
        waitUntil(timeoutMillis = 5_000) { vm.note.value != null }
        waitForIdle()

        onNode(hasSetTextAction()).performTextReplacement("written then left")
        shown = false
        waitForIdle()

        waitUntil(timeoutMillis = 5_000) { runBlocking { database.notesDao().getNoteByUrl(url)?.content } == "written then left" }
    }
}
