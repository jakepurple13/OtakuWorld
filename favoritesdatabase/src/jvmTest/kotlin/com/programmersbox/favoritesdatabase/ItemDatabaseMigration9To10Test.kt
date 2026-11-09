package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemDatabaseMigration9To10Test {

    private val dbFile = File.createTempFile("item-migration-9-10-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test fun `old per-language skips are cleared and favorites survive`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ItemDatabase",
            version = 9,
            seed = listOf(
                "INSERT INTO FavoriteItem (title, description, url, imageUrl, sources) " +
                    "VALUES ('Blue Lock', '', 'u1', '', 'Source')",
                "INSERT INTO SkippedUpdateSource (source) VALUES ('Source')",
            ),
        )

        val database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ItemDatabase.MIGRATIONS)
            .build()
        val dao = database.itemDao()

        assertEquals(emptyList(), dao.getSkippedUpdateSources().first())
        assertEquals(listOf("u1"), dao.getFavoritesToCheckSync(checkAll = false, emptyList()).map { it.url })

        database.close()
    }
}
