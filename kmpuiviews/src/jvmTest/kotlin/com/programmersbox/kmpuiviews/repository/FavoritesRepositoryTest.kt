package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoritesRepositoryTest {

    private lateinit var database: ItemDatabase

    private fun repository(loggedIn: Boolean = false) = FavoritesRepository(
        dao = database.itemDao(),
        systemAlerter = SystemAlerter(),
        authManager = FakeAuthManager(loggedIn = loggedIn),
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
}
