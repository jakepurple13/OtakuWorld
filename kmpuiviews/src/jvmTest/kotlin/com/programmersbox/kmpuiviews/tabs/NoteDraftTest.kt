package com.programmersbox.kmpuiviews.tabs

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.clearAndJoin
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNull
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
        val saver = NoteDraftSaver(backgroundScope, PendingWrites(backgroundScope), { t, _ -> written += t }, debounceMs = 500)

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
        val saver = NoteDraftSaver(backgroundScope, PendingWrites(backgroundScope), { t, _ -> written += t }, debounceMs = 500)

        saver.edit("typed")
        saver.flush()
        runCurrent()
        assertEquals(listOf("typed"), written)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf("typed"), written)
    }

    // Closing a tab disposes the panel and clears the tab's ViewModel store in the same pass. The DAO gate holds
    // the write until after the store is cleared, so a write tied to the ViewModel's scope can never finish.
    @Test
    fun textTypedBeforeTheTabClosesStillPersists() = runTest {
        val url = "https://example.com/1"
        val gate = CompletableDeferred<Unit>()
        val real = database.notesDao()
        val gated = object : NotesDao by real {
            override fun getNote(itemUrl: String): Flow<NoteItem?> = flow {
                gate.await()
                emitAll(real.getNote(itemUrl))
            }
        }
        val vm = DetailsNotesViewModel(url, "Title", gated, FakeAiText(engine = null), FakeAuthManager())
        store.put("notes-panel:$url", vm)
        val writes = PendingWrites(CoroutineScope(Dispatchers.IO))
        val saver = NoteDraftSaver(backgroundScope, writes, vm::persist)

        saver.edit("typed just before closing")
        saver.flush() // the panel's onDispose
        store.clear() // the tab's store, cleared right after, with no join
        gate.complete(Unit)
        writes.awaitAll()

        assertEquals("typed just before closing", real.getNote(url).first()?.content)
    }

    @Test
    fun blankForLongerThanTheDebounceNeverDeletesAndKeepsSyncIds() = runTest {
        val url = "https://example.com/1"
        val dao = database.notesDao()
        dao.upsertNote(
            NoteItem(url, "Title", "original", timestamp = 1L, supabaseId = "remote-1", createdAt = 42L, isDirty = false)
        )
        // Logged in: a delete here would be a soft delete, which hides the row from getNote
        val vm = DetailsNotesViewModel(url, "Title", dao, FakeAiText(engine = null), FakeAuthManager(loggedIn = true))
        store.put("notes-panel:$url", vm)
        val writes = PendingWrites(CoroutineScope(Dispatchers.IO))
        val saver = NoteDraftSaver(backgroundScope, writes, vm::persist)

        saver.edit("") // select all, delete
        advanceTimeBy(600)
        runCurrent()
        writes.awaitAll()
        assertEquals("original", dao.getNote(url).first()?.content)

        saver.edit("retyped")
        saver.flush()
        writes.awaitAll()

        val note = dao.getNote(url).first()
        assertEquals("retyped", note?.content)
        assertEquals("remote-1", note?.supabaseId)
        assertEquals(42L, note?.createdAt)
    }

    @Test
    fun theFinalFlushOfBlankTextDoesDelete() = runTest {
        val url = "https://example.com/1"
        val dao = database.notesDao()
        dao.upsertNote(NoteItem(url, "Title", "original", timestamp = 1L))
        val vm = DetailsNotesViewModel(url, "Title", dao, FakeAiText(engine = null), FakeAuthManager())
        store.put("notes-panel:$url", vm)
        val writes = PendingWrites(CoroutineScope(Dispatchers.IO))
        val saver = NoteDraftSaver(backgroundScope, writes, vm::persist)

        saver.edit("")
        saver.flush()
        writes.awaitAll()

        assertNull(dao.getNote(url).first())
    }

    @Test
    fun anOlderWriteThatFinishesLaterDoesNotOverwriteANewerOne() = runTest {
        val events = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val writes = PendingWrites(backgroundScope)

        writes.launch {
            events += "start older"
            gate.await() // would finish last if writes overlapped
            events += "end older"
        }
        writes.launch {
            events += "start newer"
            events += "end newer"
        }
        runCurrent()
        assertEquals(listOf("start older"), events)

        gate.complete(Unit)
        writes.awaitAll()
        assertEquals(listOf("start older", "end older", "start newer", "end newer"), events)
    }

    @Test
    fun aFailedWriteIsSkippedAndLaterWritesStillRun() = runTest {
        val ran = mutableListOf<String>()
        val writes = PendingWrites(backgroundScope)
        writes.launch { error("boom") }
        writes.launch { ran += "after" }
        writes.awaitAll()
        assertEquals(listOf("after"), ran)
    }

    @Test
    fun switchingTitlesNeverWritesOneTitlesTextIntoTheOther() = runTest {
        val written = mutableMapOf<String, MutableList<String>>()
        val writes = PendingWrites(backgroundScope)
        fun saverFor(url: String) =
            NoteDraftSaver(backgroundScope, writes, { t, _ -> written.getOrPut(url) { mutableListOf() } += t })

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
