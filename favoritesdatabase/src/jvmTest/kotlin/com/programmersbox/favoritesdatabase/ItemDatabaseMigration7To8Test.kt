package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemDatabaseMigration7To8Test {

    private val dbFile = File.createTempFile("item-migration-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test fun `existing favorites are searchable and new ones are indexed after migration`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ItemDatabase",
            version = 7,
            seed = listOf(
                "INSERT INTO FavoriteItem (title, description, url, imageUrl, sources) " +
                    "VALUES ('Blue Lock', 'Strikers compete', 'u1', '', 'Source')"
            ),
        )

        val database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ItemDatabase.MIGRATIONS)
            .build()
        val dao = database.itemDao()

        assertEquals("u1", dao.searchFavorites(ftsMatch("lock")!!).first().single().url)

        dao.insertFavorite(DbModel("Kaiju No. 8", "", "u2", "", "Source"))
        assertEquals("u2", dao.searchFavorites(ftsMatch("kaiju")!!).first().single().url)

        database.close()
    }
}
