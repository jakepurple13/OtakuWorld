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
import kotlin.test.assertTrue

class HistoryDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: HistoryDatabase
    private lateinit var dao: HistoryDao

    private fun history(searchText: String, time: Long = 0L) = HistoryItem(
        time = time,
        searchText = searchText,
    )

    private fun recent(url: String, title: String = "Title", timestamp: Long = 0L) = RecentModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
        timestamp = timestamp,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("history-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<HistoryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.historyDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertHistory then getAllHistory returns it`() = runTest {
        dao.insertHistory(history("one"))

        val all = dao.getAllHistory().first()

        assertEquals(1, all.size)
        assertEquals("one", all[0].searchText)
    }

    @Test fun `getAllHistory excludes soft-deleted rows`() = runTest {
        dao.insertHistory(history("one"))
        dao.insertHistory(history("two"))
        dao.softDeleteHistory("one", timestamp = 1_000L)

        val visible = dao.getAllHistory().first()

        assertEquals(1, visible.size)
        assertEquals("two", visible[0].searchText)
    }

    @Test fun `searchHistory matches with LIKE`() = runTest {
        dao.insertHistory(history("one piece"))
        dao.insertHistory(history("naruto"))

        val results = dao.searchHistory("%piece%").first()

        assertEquals(1, results.size)
        assertEquals("one piece", results[0].searchText)
    }

    @Test fun `insertRecentlyViewed then getRecentlyViewed returns it`() = runTest {
        dao.insertRecentlyViewed(recent("https://example.com/1"))

        val all = dao.getRecentlyViewed().first()

        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].url)
    }

    @Test fun `deleteRecent removes a row`() = runTest {
        val item = recent("https://example.com/1")
        dao.insertRecentlyViewed(item)

        dao.deleteRecent(item)

        assertTrue(dao.getRecentlyViewed().first().isEmpty())
    }

    @Test fun `deleteAllRecentHistory clears the table`() = runTest {
        dao.insertRecentlyViewed(recent("https://example.com/1"))
        dao.insertRecentlyViewed(recent("https://example.com/2"))

        dao.deleteAllRecentHistory()

        assertTrue(dao.getRecentlyViewed().first().isEmpty())
    }
}
