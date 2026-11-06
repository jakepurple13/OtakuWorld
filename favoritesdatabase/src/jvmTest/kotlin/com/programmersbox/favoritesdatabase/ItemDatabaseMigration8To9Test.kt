package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemDatabaseMigration8To9Test {

    private val dbFile = File.createTempFile("item-migration-8-9-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test fun `favorites survive and sources can be skipped after migration`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ItemDatabase",
            version = 8,
            seed = listOf(
                "INSERT INTO FavoriteItem (title, description, url, imageUrl, sources) " +
                    "VALUES ('Blue Lock', '', 'u1', '', 'Source')"
            ),
        )

        val database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ItemDatabase.MIGRATIONS)
            .build()
        val dao = database.itemDao()

        assertEquals(listOf("u1"), dao.getFavoritesToCheckSync(checkAll = false).map { it.url })

        dao.insertSkippedUpdateSources(listOf(SkippedUpdateSource("Source")))
        assertEquals(listOf("Source"), dao.getSkippedUpdateSources().first())
        assertEquals(emptyList(), dao.getFavoritesToCheckSync(checkAll = false))

        database.close()
    }
}
