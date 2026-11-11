package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.SourceRepository
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

    private fun repository(
        loggedIn: Boolean = false,
        sourceRepository: SourceRepository = SourceRepository(),
    ) = FavoritesRepository(
        dao = database.itemDao(),
        systemAlerter = SystemAlerter(),
        authManager = FakeAuthManager(loggedIn = loggedIn),
        sourceRepository = sourceRepository,
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

    // The Incognito settings screen keys source rows by package name, while callers pass the service name
    @Test fun `isIncognito is true for a service whose package is incognito`() = runTest {
        val info = ExampleService.getSourceInformation()
        val sourceRepository = SourceRepository().apply { setSources(listOf(info)) }
        database.itemDao().insertIncognitoSource(
            IncognitoSource(source = info.packageName, name = info.apiService.serviceName, isIncognito = true)
        )

        assertEquals(true, repository(sourceRepository = sourceRepository).isIncognito(info.apiService.serviceName))
    }

    @Test fun `isIncognito is false for a service whose package is toggled off`() = runTest {
        val info = ExampleService.getSourceInformation()
        val sourceRepository = SourceRepository().apply { setSources(listOf(info)) }
        database.itemDao().insertIncognitoSource(
            IncognitoSource(source = info.packageName, name = info.apiService.serviceName, isIncognito = false)
        )

        assertEquals(false, repository(sourceRepository = sourceRepository).isIncognito(info.apiService.serviceName))
    }

    @Test fun `isIncognito is true for an incognito item url`() = runTest {
        database.itemDao().insertIncognitoSource(
            IncognitoSource(source = "https://example.com/1", name = "Title", isIncognito = true)
        )

        assertEquals(true, repository().isIncognito("https://example.com/1"))
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
