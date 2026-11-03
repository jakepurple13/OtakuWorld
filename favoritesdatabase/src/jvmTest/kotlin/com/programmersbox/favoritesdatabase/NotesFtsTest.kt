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
import kotlin.test.assertTrue

class NotesFtsTest {

    private lateinit var dbFile: File
    private lateinit var database: NotesDatabase
    private lateinit var dao: NotesDao

    private suspend fun search(raw: String) = dao.searchNoteHits(ftsMatch(raw, column = "content")!!).first()

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("notes-fts-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `content match returns snippet`() = runTest {
        dao.upsertNote(NoteItem("u1", "Kagurabachi", "Wait for the tournament arc to finish, then binge it."))

        val hit = search("tour arc").single()

        assertEquals("u1", hit.itemUrl)
        assertTrue("\u0002tournament\u0003" in hit.contentSnippet)
    }

    @Test fun `itemTitle not searched`() = runTest {
        dao.upsertNote(NoteItem("u1", "Kagurabachi", "Great fights"))

        assertTrue(search("kagura").isEmpty())
    }

    @Test fun `soft deleted excluded`() = runTest {
        dao.upsertNote(NoteItem("u1", "Title", "tournament arc"))
        dao.softDeleteNote("u1", 1)

        assertTrue(search("tournament").isEmpty())
    }
}
