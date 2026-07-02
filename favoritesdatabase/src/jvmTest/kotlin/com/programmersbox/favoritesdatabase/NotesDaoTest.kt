package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotesDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: NotesDatabase
    private lateinit var dao: NotesDao

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

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("notes-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<NotesDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.notesDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `upsertNote then getNote and getAllNotes return it`() = runTest {
        dao.upsertNote(note("https://example.com/1"))

        val single = dao.getNote("https://example.com/1").first()
        val all = dao.getAllNotes().first()

        assertEquals("https://example.com/1", single?.itemUrl)
        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].itemUrl)
    }

    @Test fun `getAllNotesCount reflects inserts`() = runTest {
        assertEquals(0, dao.getAllNotesCount().first())

        dao.upsertNote(note("https://example.com/1"))
        dao.upsertNote(note("https://example.com/2"))

        assertEquals(2, dao.getAllNotesCount().first())
    }

    @Test fun `upsertNote with same itemUrl replaces rather than duplicates`() = runTest {
        dao.upsertNote(note("https://example.com/1", content = "First"))
        dao.upsertNote(note("https://example.com/1", content = "Second"))

        val all = dao.getAllNotesSync()

        assertEquals(1, all.size)
        assertEquals("Second", all[0].content)
    }

    @Test fun `deleteNote removes the row`() = runTest {
        dao.upsertNote(note("https://example.com/1"))

        dao.deleteNote("https://example.com/1")

        assertNull(dao.getNote("https://example.com/1").first())
        assertTrue(dao.getAllNotesSync().isEmpty())
    }

    @Test fun `searchNotes finds a note by a word in its content`() = runTest {
        dao.upsertNote(note("https://example.com/1", content = "A story about dragons"))
        dao.upsertNote(note("https://example.com/2", content = "A story about robots"))

        val results = dao.searchNotes("dragons").first()

        assertEquals(1, results.size)
        assertEquals("https://example.com/1", results[0].itemUrl)
    }

    @Test fun `searchNotes finds a note by a word in its itemTitle`() = runTest {
        dao.upsertNote(note("https://example.com/1", itemTitle = "Dragon Tales", content = "Nothing relevant"))
        dao.upsertNote(note("https://example.com/2", itemTitle = "Robot Tales", content = "Nothing relevant"))

        val results = dao.searchNotes("Dragon").first()

        assertEquals(1, results.size)
        assertEquals("https://example.com/1", results[0].itemUrl)
    }

    @Test fun `softDeleteNote sets is_deleted but leaves the row queryable`() = runTest {
        dao.upsertNote(note("https://example.com/1"))

        dao.softDeleteNote("https://example.com/1", timestamp = 1_000L)

        val synced = dao.getAllNotesSync()
        assertEquals(1, synced.size)
        assertTrue(synced[0].isDeleted)

        val flowed = dao.getAllNotes().first()
        assertEquals(1, flowed.size)
        assertTrue(flowed[0].isDeleted)
    }
}
