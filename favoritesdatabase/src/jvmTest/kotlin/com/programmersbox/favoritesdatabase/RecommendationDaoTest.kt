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

class RecommendationDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: RecommendationDatabase
    private lateinit var dao: RecommendationDao

    private fun recommendation(
        title: String,
        description: String = "Description",
        reason: String = "Reason",
        genre: List<String> = listOf("Action", "Comedy"),
    ) = Recommendation(
        title = title,
        description = description,
        reason = reason,
        genre = genre,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("recommendation-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<RecommendationDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.recommendationDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertRecommendation then getAllRecommendations returns it with genre round-tripped`() = runTest {
        dao.insertRecommendation(recommendation(title = "Title 1", genre = listOf("Action", "Drama")))

        val all = dao.getAllRecommendations().first()

        assertEquals(1, all.size)
        assertEquals("Title 1", all[0].title)
        assertEquals(listOf("Action", "Drama"), all[0].genre)
    }

    @Test fun `getRecommendationCount reflects inserts`() = runTest {
        assertEquals(0, dao.getRecommendationCount().first())

        dao.insertRecommendation(recommendation(title = "Title 1"))
        assertEquals(1, dao.getRecommendationCount().first())

        dao.insertRecommendation(recommendation(title = "Title 2"))
        assertEquals(2, dao.getRecommendationCount().first())
    }

    @Test fun `insertRecommendation with same title replaces existing row`() = runTest {
        dao.insertRecommendation(recommendation(title = "Title 1", description = "Old"))
        dao.insertRecommendation(recommendation(title = "Title 1", description = "New"))

        val all = dao.getAllRecommendations().first()

        assertEquals(1, all.size)
        assertEquals("New", all[0].description)
    }

    @Test fun `deleteRecommendation by title removes the row`() = runTest {
        dao.insertRecommendation(recommendation(title = "Title 1"))

        dao.deleteRecommendation("Title 1")

        assertNull(dao.getAllRecommendations().first().find { it.title == "Title 1" })
    }

    @Test fun `deleteRecommendation by entity removes the row`() = runTest {
        val item = recommendation(title = "Title 1")
        dao.insertRecommendation(item)

        dao.deleteRecommendation(item)

        assertEquals(0, dao.getAllRecommendations().first().size)
    }
}
