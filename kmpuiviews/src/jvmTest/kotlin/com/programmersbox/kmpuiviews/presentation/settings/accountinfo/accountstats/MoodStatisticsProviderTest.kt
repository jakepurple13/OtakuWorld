package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstats

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.domain.moodtags.MoodRepository
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoodStatisticsProviderTest {
    private lateinit var database: ItemDatabase
    private lateinit var moods: MoodRepository

    private suspend fun favorite(url: String) =
        database.itemDao().insertFavorite(DbModel("Title $url", "Description", url, "", "Source"))

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
        moods = MoodRepository(database.itemDao(), FakeAiText(engine = null))
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `hidden when nothing is tagged`() = runTest {
        favorite("u1")
        assertTrue(MoodStatisticsProvider(database.itemDao(), moods).observeStats().first().isEmpty())
    }

    @Test
    fun `shows tagged count and top three moods`() = runTest {
        listOf("u1", "u2", "u3", "u4", "u5").forEach { favorite(it) }
        moods.toggle("u1", "cozy"); moods.toggle("u2", "cozy"); moods.toggle("u3", "cozy")
        moods.toggle("u1", "dark"); moods.toggle("u2", "dark")
        moods.create("u3", "comfort read")
        moods.toggle("u4", "funny")

        val stats = MoodStatisticsProvider(database.itemDao(), moods).observeStats().first()

        assertEquals("4 of 5", stats.first { it.id == "tagged" }.value)
        assertEquals(
            listOf("cozy · 3", "dark · 2"),
            stats.filter { it.id.startsWith("top") }.take(2).map { it.value },
        )
        assertEquals(4, stats.size)
    }
}
