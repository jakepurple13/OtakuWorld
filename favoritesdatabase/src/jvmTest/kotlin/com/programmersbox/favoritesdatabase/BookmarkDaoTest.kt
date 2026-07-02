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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookmarkDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: BookmarkDatabase
    private lateinit var dao: BookmarkDao

    private fun bookmark(
        chapterUrl: String,
        chapterName: String = "Chapter 1",
        parentUrl: String = "https://example.com/series",
        parentTitle: String = "Series Title",
    ) = BookmarkedChapter(
        chapterUrl = chapterUrl,
        chapterName = chapterName,
        parentUrl = parentUrl,
        parentTitle = parentTitle,
        parentImageUrl = "https://example.com/series.jpg",
        source = "ExampleService",
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("bookmark-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<BookmarkDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.bookmarkDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertBookmark then getAllBookmarks returns it`() = runTest {
        dao.insertBookmark(bookmark("https://example.com/series/ch1"))

        val all = dao.getAllBookmarks().first()

        assertEquals(1, all.size)
        assertEquals("https://example.com/series/ch1", all[0].chapterUrl)
    }

    @Test fun `deleteBookmarkByUrl removes it`() = runTest {
        dao.insertBookmark(bookmark("https://example.com/series/ch1"))

        dao.deleteBookmarkByUrl("https://example.com/series/ch1")

        assertEquals(0, dao.getAllBookmarks().first().size)
    }

    @Test fun `getBookmarksForDetail filters by parentUrl`() = runTest {
        dao.insertBookmark(bookmark("https://example.com/series-a/ch1", parentUrl = "https://example.com/series-a"))
        dao.insertBookmark(bookmark("https://example.com/series-b/ch1", parentUrl = "https://example.com/series-b"))

        val forA = dao.getBookmarksForDetail("https://example.com/series-a").first()

        assertEquals(1, forA.size)
        assertEquals("https://example.com/series-a/ch1", forA[0].chapterUrl)
    }

    @Test fun `hasBookmark reflects presence after insert and delete`() = runTest {
        val chapterUrl = "https://example.com/series/ch1"

        assertFalse(dao.hasBookmark(chapterUrl))

        dao.insertBookmark(bookmark(chapterUrl))
        assertTrue(dao.hasBookmark(chapterUrl))

        dao.deleteBookmarkByUrl(chapterUrl)
        assertFalse(dao.hasBookmark(chapterUrl))
    }

    @Test fun `searchBookmarks matches by chapterName`() = runTest {
        dao.insertBookmark(bookmark("https://example.com/series/ch1", chapterName = "Dragon Fight"))
        dao.insertBookmark(bookmark("https://example.com/series/ch2", chapterName = "Calm Morning"))

        val results = dao.searchBookmarks("Dragon").first()

        assertEquals(1, results.size)
        assertEquals("https://example.com/series/ch1", results[0].chapterUrl)
    }

    @Test fun `searchBookmarks matches by parentTitle`() = runTest {
        dao.insertBookmark(bookmark("https://example.com/series-a/ch1", parentTitle = "Amazing Series"))
        dao.insertBookmark(bookmark("https://example.com/series-b/ch1", parentTitle = "Other Story"))

        val results = dao.searchBookmarks("Amazing").first()

        assertEquals(1, results.size)
        assertEquals("https://example.com/series-a/ch1", results[0].chapterUrl)
    }
}
