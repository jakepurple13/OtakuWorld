package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemDatabaseMigration11To12Test {

    private val dbFile = File.createTempFile("item-migration-11-12-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    // Room validates the new table against the v12 schema when it opens the migrated database.
    @Test fun `migration 11 to 12 adds mood_tags and keeps favorites`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ItemDatabase",
            version = 11,
            seed = listOf(
                "INSERT INTO FavoriteItem (title, description, url, imageUrl, sources) " +
                    "VALUES ('Frieren', 'An elf mage', 'fav', '', 'Source')",
            ),
        )

        val database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ItemDatabase.MIGRATIONS)
            .build()
        val dao = database.itemDao()

        assertEquals(listOf("fav"), dao.getAllFavoritesSync().map { it.url })
        dao.upsertMoods(MoodTagRow("fav", "[\"cozy\"]", 1))
        assertEquals(listOf("fav"), dao.favoriteMoodRows().first().map { it.url })

        database.close()
    }
}
