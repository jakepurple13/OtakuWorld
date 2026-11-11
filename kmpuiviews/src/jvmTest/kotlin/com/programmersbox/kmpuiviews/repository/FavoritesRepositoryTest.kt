package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FixedClock
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class FavoritesRepositoryTest {

    private lateinit var database: ItemDatabase

    private fun repository(
        loggedIn: Boolean = false,
        clock: Clock = Clock.System,
    ) = FavoritesRepository(
        dao = database.itemDao(),
        systemAlerter = SystemAlerter(),
        authManager = FakeAuthManager(loggedIn = loggedIn),
        clock = clock,
    )

    private fun favorite(url: String) = DbModel(
        title = "Title",
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
    )

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test fun `addFavorite persists the item`() = runTest {
        repository().addFavorite(favorite("https://example.com/1"))

        val all = repository().getAllFavorites()

        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].url)
    }

    @Test fun `addFavorite stamps createdAt when missing`() = runTest {
        repository().addFavorite(favorite("https://example.com/1"))

        assertTrue(repository().getAllFavorites().single().createdAt > 0)
    }

    @Test fun `addFavorite keeps an existing createdAt`() = runTest {
        repository().addFavorite(favorite("https://example.com/1").copy(createdAt = 123))

        assertEquals(123, repository().getAllFavorites().single().createdAt)
    }

    @Test fun `removeFavorite hard-deletes when logged out`() = runTest {
        val repo = repository(loggedIn = false)
        val item = favorite("https://example.com/1")
        repo.addFavorite(item)

        repo.removeFavorite(item)

        assertEquals(0, repo.getAllFavorites().size)
    }

    @Test fun `removeFavorite soft-deletes when logged in`() = runTest {
        val repo = repository(loggedIn = true)
        val item = favorite("https://example.com/1")
        repo.addFavorite(item)

        repo.removeFavorite(item)

        assertTrue(database.itemDao().getDbModelSync("https://example.com/1")!!.isDeleted)
        assertEquals(1, database.itemDao().getAllFavoritesSync().size) // soft-deleted row still exists
    }

    @Test fun `isIncognito is false for a source with no incognito entry`() = runTest {
        assertEquals(false, repository().isIncognito("ExampleService"))
    }

    @Test fun `addWatched persists a chapter`() = runTest {
        val repo = repository()
        repo.addFavorite(favorite("https://example.com/1"))

        repo.addWatched(
            ChapterWatched(
                url = "https://example.com/1/ch1",
                name = "Chapter 1",
                favoriteUrl = "https://example.com/1",
            )
        )

        assertEquals(1, database.itemDao().getAllChaptersSync().size)
    }

    @Test fun `addWatched stamps createdAt and updatedAt with now`() = runTest {
        val fixed = Instant.fromEpochMilliseconds(1_762_800_000_000)
        val repo = repository(clock = FixedClock(fixed))
        repo.addWatched(ChapterWatched(url = "c1", name = "Ch 1", favoriteUrl = "f1"))
        val row = database.itemDao().getChapterByUrl("c1")!!
        assertEquals(fixed.toEpochMilliseconds(), row.createdAt)
        assertEquals(fixed.toEpochMilliseconds(), row.updatedAt)
    }

    @Test fun `addWatched without stampReadTime leaves createdAt at 0`() = runTest {
        val repo = repository(clock = FixedClock(Instant.fromEpochMilliseconds(1_762_800_000_000)))
        repo.addWatched(ChapterWatched(url = "c1", name = "Ch 1", favoriteUrl = "f1"), stampReadTime = false)
        assertEquals(0L, database.itemDao().getChapterByUrl("c1")!!.createdAt)
    }

    @Test fun `re-reading a chapter moves createdAt to the latest read`() = runTest {
        val t1 = Instant.fromEpochMilliseconds(1_762_800_000_000)
        val t2 = Instant.fromEpochMilliseconds(1_762_900_000_000)
        val clock = FixedClock(t1)
        val repo = repository(clock = clock)
        val chapter = ChapterWatched(url = "c1", name = "Ch 1", favoriteUrl = "f1")
        repo.addWatched(chapter)
        clock.instant = t2
        repo.addWatched(chapter)
        assertEquals(t2.toEpochMilliseconds(), database.itemDao().getChapterByUrl("c1")!!.createdAt)
    }
}
