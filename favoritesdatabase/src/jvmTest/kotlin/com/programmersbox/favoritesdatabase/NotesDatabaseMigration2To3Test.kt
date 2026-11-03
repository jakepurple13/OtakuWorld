package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotesDatabaseMigration2To3Test {

    private val dbFile = File.createTempFile("notes-migration-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test fun `existing notes are searchable and new ones are indexed after migration`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.NotesDatabase",
            version = 2,
            seed = listOf(
                "INSERT INTO notes (itemUrl, itemTitle, content, timestamp) " +
                    "VALUES ('u1', 'Kagurabachi', 'Wait for the tournament arc', 0)"
            ),
        )

        val database = Room.databaseBuilder<NotesDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*NotesDatabase.MIGRATIONS)
            .build()
        val dao = database.notesDao()

        assertEquals("u1", dao.searchNoteHits(ftsMatch("tournament", "content")!!).first().single().itemUrl)

        dao.upsertNote(NoteItem("u2", "Frieren", "First-class mage exam"))
        assertEquals("u2", dao.searchNoteHits(ftsMatch("mage", "content")!!).first().single().itemUrl)

        database.close()
    }
}
