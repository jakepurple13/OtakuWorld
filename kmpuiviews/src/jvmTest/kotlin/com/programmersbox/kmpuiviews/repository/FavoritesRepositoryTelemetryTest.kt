package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.telemetry.TelemetrySourceNamer
import com.programmersbox.kmpuiviews.testing.FakeAnalytics
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.telemetry.AnalyticsEvent
import com.programmersbox.telemetry.events.FavoriteAdded
import com.programmersbox.telemetry.events.FavoriteRemoved
import com.programmersbox.telemetry.events.OpenedFrom
import com.programmersbox.telemetry.events.UpdateCheckToggled
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoritesRepositoryTelemetryTest {
    private lateinit var database: ItemDatabase
    private val analytics = FakeAnalytics()

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun repository(): FavoritesRepository {
        val sources = SourceRepository()
        return FavoritesRepository(
            dao = database.itemDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(loggedIn = false),
            sourceRepository = sources,
            analytics = analytics,
            sourceNamer = TelemetrySourceNamer(sources, database.itemDao()),
        )
    }

    private fun favorite(n: Int, source: String = "Src") =
        DbModel(title = "Secret $n", description = "", url = "https://x/$n", imageUrl = "", source = source)

    @Test
    fun `user add fires with from`() = runTest {
        repository().addFavorite(favorite(1), from = OpenedFrom.DETAILS)
        assertEquals<List<AnalyticsEvent>>(listOf(FavoriteAdded("Src", "details")), analytics.events)
    }

    @Test
    fun `restore add fires nothing`() = runTest {
        val repo = repository()
        repeat(50) { repo.addFavorite(favorite(it)) }
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `incognito add fires nothing`() = runTest {
        database.itemDao().insertIncognitoSource(IncognitoSource("Src", "Src", isIncognito = true))
        repository().addFavorite(favorite(1), from = OpenedFrom.DETAILS)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `user remove fires, silent remove does not`() = runTest {
        val repo = repository()
        repo.removeFavorite(favorite(1))
        repo.removeFavorite(favorite(2), userAction = true)
        assertEquals<List<AnalyticsEvent>>(listOf(FavoriteRemoved("Src")), analytics.events)
    }

    @Test
    fun `toggle notify fires the new state`() = runTest {
        repository().toggleNotify(favorite(1).copy(shouldCheckForUpdate = false))
        assertEquals<List<AnalyticsEvent>>(listOf(UpdateCheckToggled(enabled = false)), analytics.events)
    }
}
