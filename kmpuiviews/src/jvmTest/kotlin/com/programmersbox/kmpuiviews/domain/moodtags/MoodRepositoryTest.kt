package com.programmersbox.kmpuiviews.domain.moodtags

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.MoodInput
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoodRepositoryTest {
    private lateinit var database: ItemDatabase

    private fun repo(ai: FakeAiText = FakeAiText()) = MoodRepository(database.itemDao(), ai)

    private suspend fun favorite(url: String) =
        database.itemDao().insertFavorite(DbModel("Title $url", "Description", url, "", "Source"))

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `toggle adds and removes`() = runTest {
        val repo = repo()
        repo.toggle("u1", "cozy")
        repo.toggle("u1", "dark")
        assertEquals(listOf("cozy", "dark"), repo.moods("u1").first())
        repo.toggle("u1", "Cozy")
        assertEquals(listOf("dark"), repo.moods("u1").first())
    }

    @Test
    fun `empty moods delete the row`() = runTest {
        val repo = repo()
        repo.toggle("u1", "cozy")
        repo.remove("u1", "cozy")
        assertEquals(emptyList(), repo.moods("u1").first())
        assertNull(database.itemDao().moodsFlow("u1").first())
    }

    @Test
    fun `custom names are cleaned and deduped`() = runTest {
        val repo = repo()
        assertTrue(repo.create("u1", "  Comfort   Read "))
        assertTrue(repo.create("u2", "comfort read"))
        assertTrue(repo.create("u3", "COZY"))
        assertFalse(repo.create("u4", "   "))
        assertEquals(listOf("Comfort Read"), repo.moods("u2").first())
        assertEquals(listOf("cozy"), repo.moods("u3").first())
        assertEquals(listOf("Comfort Read"), repo.customMoods.first())
    }

    @Test
    fun `custom moods disappear when unused`() = runTest {
        val repo = repo()
        repo.create("u1", "rainy day read")
        repo.remove("u1", "rainy day read")
        assertEquals(emptyList(), repo.customMoods.first())
    }

    @Test
    fun `suggest keeps only allowed moods`() = runTest {
        val ai = FakeAiText(output = { _, _ -> flowOf("Cozy, Space Opera\nfound family\ndark\nfunny") })
        val repo = repo(ai)
        repo.create("other", "space opera")
        assertTrue(repo.suggest("u1", "Frieren", "An elf mage"))
        assertEquals(listOf("cozy", "space opera", "found family"), repo.moods("u1").first())
        val input = ai.inputsFor<AiFeature.MoodTags>().single() as MoodInput
        assertTrue("space opera" in input.choices)
    }

    @Test
    fun `suggest without usable moods or engine returns false`() = runTest {
        assertFalse(repo(FakeAiText(output = { _, _ -> flowOf("nonsense words") })).suggest("u1", "T", "D"))
        assertFalse(repo(FakeAiText(output = { _, _ -> flow { error("no engine") } })).suggest("u1", "T", "D"))
        assertEquals(emptyList(), repo().moods("u1").first())
    }

    @Test
    fun `favoriteMoods counts favorites only`() = runTest {
        favorite("fav")
        val repo = repo()
        repo.toggle("fav", "cozy")
        repo.toggle("notfav", "dark")
        assertEquals(mapOf("fav" to listOf("cozy")), repo.favoriteMoods.first())
    }
}
