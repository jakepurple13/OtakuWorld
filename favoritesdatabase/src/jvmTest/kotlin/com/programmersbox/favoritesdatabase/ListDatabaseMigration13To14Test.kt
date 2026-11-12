package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListDatabaseMigration13To14Test {

    private val dbFile = File.createTempFile("list-migration-13-14-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    // Room validates the new index when it opens the migrated database.
    @Test fun `uuid index is added and lists survive`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ListDatabase",
            version = 13,
            seed = listOf(
                "INSERT INTO CustomListItem (uuid, name, time) VALUES ('list', 'Reading', 1)",
                "INSERT INTO CustomListInfo (uniqueId, uuid, title, description, url, imageUrl, sources) " +
                    "VALUES ('i1', 'list', 'Blue Lock', '', 'u1', '', 'Source')",
            ),
        )

        val database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ListDatabase.MIGRATIONS)
            .build()

        assertEquals(listOf("Blue Lock"), database.listDao().getCustomListItem("list")?.list?.map { it.title })

        database.close()
    }
}
