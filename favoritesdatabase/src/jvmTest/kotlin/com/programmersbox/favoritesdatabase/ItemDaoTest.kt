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

class ItemDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    private fun favorite(url: String, title: String = "Title", numChapters: Int = 0) = DbModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
        numChapters = numChapters,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("item-dao-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `insertFavorite then getAllFavoritesSync returns it`() = runTest {
        dao.insertFavorite(favorite("https://example.com/1"))

        val all = dao.getAllFavoritesSync()

        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].url)
    }

    @Test fun `getAllFavorites flow excludes soft-deleted rows`() = runTest {
        dao.insertFavorite(favorite("https://example.com/1"))
        dao.insertFavorite(favorite("https://example.com/2"))
        dao.softDeleteFavorite("https://example.com/1", timestamp = 1_000L)

        val visible = dao.getAllFavorites().first()

        assertEquals(1, visible.size)
        assertEquals("https://example.com/2", visible[0].url)
    }

    @Test fun `deleteFavorite hard-removes the row`() = runTest {
        val item = favorite("https://example.com/1")
        dao.insertFavorite(item)

        dao.deleteFavorite(item)

        assertNull(dao.getDbModelSync("https://example.com/1"))
    }

    @Test fun `containsItem reflects presence after insert and delete`() = runTest {
        val item = favorite("https://example.com/1")

        assertEquals(false, dao.containsItem("https://example.com/1").first())

        dao.insertFavorite(item)
        assertEquals(true, dao.containsItem("https://example.com/1").first())

        dao.deleteFavorite(item)
        assertEquals(false, dao.containsItem("https://example.com/1").first())
    }

    @Test fun `insertChapter and getAllChapters round-trip`() = runTest {
        val chapter = ChapterWatched(
            url = "https://example.com/1/ch1",
            name = "Chapter 1",
            favoriteUrl = "https://example.com/1",
        )

        dao.insertChapter(chapter)
        val chapters = dao.getAllChapters("https://example.com/1").first()

        assertEquals(1, chapters.size)
        assertEquals("Chapter 1", chapters[0].name)
    }

    @Test fun `insertIncognitoSource then getIncognitoSourceSync round-trips`() = runTest {
        dao.insertIncognitoSource(IncognitoSource(source = "ExampleService", name = "Example", isIncognito = true))

        val result = dao.getIncognitoSourceSync("ExampleService")

        assertTrue(result != null && result.isIncognito)
    }
}
