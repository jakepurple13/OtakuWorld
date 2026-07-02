package com.programmersbox.kmpuiviews.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BookmarkRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: BookmarkDatabase

    private fun repository(loggedIn: Boolean = false) = BookmarkRepository(
        dao = database.bookmarkDao(),
        authManager = FakeAuthManager(loggedIn = loggedIn),
    )

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
        dbFile = File.createTempFile("bookmark-repository-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<BookmarkDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertBookmark then getAllBookmarks returns it`() = runTest {
        val repo = repository()

        repo.insertBookmark(bookmark("https://example.com/series/ch1"))

        val all = repo.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("https://example.com/series/ch1", all[0].chapterUrl)
    }

    @Test fun `insertBookmark then getBookmark returns it`() = runTest {
        val repo = repository()
        val chapterUrl = "https://example.com/series/ch1"

        repo.insertBookmark(bookmark(chapterUrl))

        val result = repo.getBookmark(chapterUrl).first()
        assertNotNull(result)
        assertEquals(chapterUrl, result.chapterUrl)
    }

    @Test fun `getBookmarksForDetail filters by parentUrl`() = runTest {
        val repo = repository()
        repo.insertBookmark(bookmark("https://example.com/series-a/ch1", parentUrl = "https://example.com/series-a"))
        repo.insertBookmark(bookmark("https://example.com/series-b/ch1", parentUrl = "https://example.com/series-b"))

        val forA = repo.getBookmarksForDetail("https://example.com/series-a").first()

        assertEquals(1, forA.size)
        assertEquals("https://example.com/series-a/ch1", forA[0].chapterUrl)
    }

    @Test fun `hasBookmark reflects presence after insert and delete`() = runTest {
        val repo = repository(loggedIn = false)
        val chapterUrl = "https://example.com/series/ch1"

        assertFalse(repo.hasBookmark(chapterUrl))

        repo.insertBookmark(bookmark(chapterUrl))
        assertTrue(repo.hasBookmark(chapterUrl))

        repo.deleteBookmark(chapterUrl)
        assertFalse(repo.hasBookmark(chapterUrl))
    }

    @Test fun `searchBookmarks matches by chapterName`() = runTest {
        val repo = repository()
        repo.insertBookmark(bookmark("https://example.com/series/ch1", chapterName = "Dragon Fight"))
        repo.insertBookmark(bookmark("https://example.com/series/ch2", chapterName = "Calm Morning"))

        val results = repo.searchBookmarks("Dragon").first()

        assertEquals(1, results.size)
        assertEquals("https://example.com/series/ch1", results[0].chapterUrl)
    }

    @Test fun `deleteBookmark hard-deletes when logged out`() = runTest {
        val repo = repository(loggedIn = false)
        val chapterUrl = "https://example.com/series/ch1"
        repo.insertBookmark(bookmark(chapterUrl))

        repo.deleteBookmark(chapterUrl)

        assertEquals(0, repo.getAllBookmarksSync().size)
    }

    @Test fun `deleteBookmark soft-deletes when logged in`() = runTest {
        val repo = repository(loggedIn = true)
        val chapterUrl = "https://example.com/series/ch1"
        repo.insertBookmark(bookmark(chapterUrl))

        repo.deleteBookmark(chapterUrl)

        val all = repo.getAllBookmarksSync()
        assertEquals(1, all.size) // soft-deleted row still exists
        assertTrue(all[0].isDeleted)
    }
}
