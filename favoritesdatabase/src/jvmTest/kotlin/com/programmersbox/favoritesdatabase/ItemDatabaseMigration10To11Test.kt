package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemDatabaseMigration10To11Test {

    private val dbFile = File.createTempFile("item-migration-10-11-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    // Room validates the new indices when it opens the migrated database.
    @Test fun `indices are added and rows survive`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ItemDatabase",
            version = 10,
            seed = listOf(
                "INSERT INTO ChapterWatched (url, name, favoriteUrl, created_at) VALUES ('c1', 'Ch 1', 'fav', 5)",
                "INSERT INTO Notifications (id, url, summaryText, notiTitle, source, contentTitle, created_at) " +
                    "VALUES (7, 'n1', '', '', 'Source', '', 5)",
            ),
        )

        val database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ItemDatabase.MIGRATIONS)
            .build()
        val dao = database.itemDao()

        assertEquals(listOf("c1"), dao.getAllChapters("fav").first().map { it.url })
        assertEquals("n1", dao.getNotificationItemById(7)?.url)

        database.close()
    }
}
