package com.programmersbox.kmpuiviews.tabs

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.clearAndJoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NoteDraftTest {

    private val store = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: NotesDatabase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("note-draft-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<NotesDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        store.clearAndJoin()
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test
    fun savesAfterTheDebounceNotBefore() = runTest {
        val written = mutableListOf<String>()
        val saver = NoteDraftSaver(backgroundScope, PendingWrites(this), { written += it }, debounceMs = 500)

        saver.edit("a")
        advanceTimeBy(499)
        runCurrent()
        assertTrue(written.isEmpty())

        // Another keystroke restarts the wait
        saver.edit("ab")
        advanceTimeBy(499)
        runCurrent()
        assertTrue(written.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("ab"), written)
    }

    @Test
    fun flushWritesAtOnceAndOnlyOnce() = runTest {
        val written = mutableListOf<String>()
        val saver = NoteDraftSaver(backgroundScope, PendingWrites(this), { written += it }, debounceMs = 500)

        saver.edit("typed")
        saver.flush()
        runCurrent()
        assertEquals(listOf("typed"), written)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf("typed"), written)
    }

    // Closing a tab disposes the panel and clears the tab's ViewModel store in the same pass
    @Test
    fun textTypedBeforeTheTabClosesStillPersists() = runTest {
        val url = "https://example.com/1"
        val vm = DetailsNotesViewModel(url, "Title", database.notesDao(), FakeAiText(engine = null), FakeAuthManager())
        store.put("notes-panel:$url", vm)
        val writes = PendingWrites(CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val saver = NoteDraftSaver(backgroundScope, writes, vm::persist)

        saver.edit("typed just before closing")
        saver.flush() // the panel's onDispose
        store.clearAndJoin() // the tab's store, cleared right after
        writes.awaitAll()

        assertEquals("typed just before closing", database.notesDao().getNote(url).first()?.content)
    }

    @Test
    fun switchingTitlesNeverWritesOneTitlesTextIntoTheOther() = runTest {
        val written = mutableMapOf<String, MutableList<String>>()
        val writes = PendingWrites(this)
        fun saverFor(url: String) =
            NoteDraftSaver(backgroundScope, writes, { written.getOrPut(url) { mutableListOf() } += it })

        val a = saverFor("A")
        a.edit("text for a")
        a.flush() // panel for A leaves composition
        val b = saverFor("B")
        b.edit("text for b")
        b.flush()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(mapOf("A" to listOf("text for a"), "B" to listOf("text for b")), written)
    }

    @Test
    fun newerDatabaseContentIsNotOverwrittenByAnOldDraft() {
        // The draft was saved and matched the database, so it is no longer "edited"
        val synced = NoteDraft().typed("old").onNote("old")
        assertFalse(synced.edited)
        // A sync pull or the Notes screen then changed the note: the field follows it
        assertEquals(NoteDraft("newer"), synced.onNote("newer"))
    }

    @Test
    fun unsavedTypingIsKeptWhileTheDatabaseCatchesUp() {
        val typing = NoteDraft().typed("typing")
        assertEquals(typing, typing.onNote("something older"))
        assertEquals(NoteDraft("typing", edited = false), typing.onNote("typing"))
    }

    @Test
    fun aFreshDraftTakesTheStoredNote() {
        assertEquals(NoteDraft("stored"), NoteDraft().onNote("stored"))
        assertEquals(NoteDraft(""), NoteDraft().onNote(null))
    }
}
