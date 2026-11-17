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

class MoodTagsDaoTest {
    private lateinit var dbFile: File
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    private fun favorite(url: String) = DbModel(
        title = "Title $url",
        description = "Description",
        url = url,
        imageUrl = "",
        source = "ExampleService",
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("mood-tags-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.itemDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `upsert and read mood tags`() = runTest {
        assertNull(dao.moodsFlow("u1").first())
        dao.upsertMoods(MoodTagRow("u1", "[\"cozy\"]", 1))
        dao.upsertMoods(MoodTagRow("u1", "[\"cozy\",\"dark\"]", 2))
        assertEquals("[\"cozy\",\"dark\"]", dao.moodsFlow("u1").first()?.moods)
        dao.deleteMoods("u1")
        assertNull(dao.moodsFlow("u1").first())
    }

    @Test fun `favoriteMoodRows only returns favorites`() = runTest {
        dao.insertFavorite(favorite("fav"))
        dao.insertFavorite(favorite("gone"))
        dao.deleteFavorite(favorite("gone"))
        dao.upsertMoods(MoodTagRow("fav", "[\"cozy\"]", 1))
        dao.upsertMoods(MoodTagRow("gone", "[\"dark\"]", 1))
        dao.upsertMoods(MoodTagRow("never", "[\"funny\"]", 1))

        assertEquals(listOf("fav"), dao.favoriteMoodRows().first().map { it.url })
        assertEquals(setOf("fav", "gone", "never"), dao.allMoodRows().first().map { it.url }.toSet())
    }
}
