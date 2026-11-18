package com.programmersbox.kmpuiviews.presentation.details

import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.domain.moodtags.MoodRepository
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MoodControllerTest {
    private lateinit var database: ItemDatabase
    private val favorite = MutableStateFlow(true)

    private fun kotlinx.coroutines.CoroutineScope.controller(ai: FakeAiText = FakeAiText(output = { _, _ -> flowOf("cozy") })) =
        MoodController(MoodRepository(database.itemDao(), ai), ai, this, "u1", favorite)

    private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T =
        withContext(Dispatchers.Default) { withTimeout(5_000) { first(predicate) } }

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `suggest chip shows for favorites without moods`() = runTest {
        val c = backgroundScope.controller()
        c.canSuggest.await { it }
        favorite.value = false
        c.canSuggest.await { !it }
    }

    @Test
    fun `suggesting adds moods and hides the chip`() = runTest {
        val c = backgroundScope.controller()
        c.canSuggest.await { it }
        c.suggest("Frieren", "An elf mage")
        c.moods.await { it == listOf("cozy") }
        c.canSuggest.await { !it }
    }

    @Test
    fun `suggesting stays on until the moods arrive`() = runTest {
        // Real time: the wait for the moods has a timeout, which virtual time would skip straight past
        val c = (backgroundScope + Dispatchers.Default).controller()
        c.canSuggest.await { it }
        c.suggest("Frieren", "An elf mage")
        c.suggesting.await { !it }
        assertEquals(listOf("cozy"), c.moods.value)
    }

    @Test
    fun `chip is hidden while suggesting`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val c = backgroundScope.controller(FakeAiText(output = { _, _ -> flow { gate.await(); emit("cozy") } }))
        c.canSuggest.await { it }
        c.suggest("Frieren", "An elf mage")
        c.suggesting.await { it }
        c.canSuggest.await { !it }
        gate.complete(Unit)
        c.suggesting.await { !it }
    }

    @Test
    fun `removing the last mood brings the chip back`() = runTest {
        val c = backgroundScope.controller()
        c.toggle("dark")
        c.canSuggest.await { !it }
        c.remove("dark")
        c.canSuggest.await { it }
    }

    @Test
    fun `chip hidden when ai is off but moods still work`() = runTest {
        database.itemDao().insertFavorite(com.programmersbox.favoritesdatabase.DbModel("T", "D", "u1", "", "S"))
        val ai = FakeAiText(engine = null)
        val c = backgroundScope.controller(ai)
        c.create("comfort read")
        c.moods.await { it == listOf("comfort read") }
        assertEquals(false, c.canSuggest.value)
        c.customMoods.await { it == listOf("comfort read") }
    }

    @Test
    fun `failed suggestion is reported`() = runTest {
        val c = backgroundScope.controller(FakeAiText(output = { _, _ -> flowOf("nothing usable") }))
        c.canSuggest.await { it }
        val failed = kotlinx.coroutines.CompletableDeferred<Unit>()
        backgroundScope.launch(Dispatchers.Default) { c.suggestFailed.first(); failed.complete(Unit) }
        c.suggest("Frieren", "An elf mage")
        withContext(Dispatchers.Default) { withTimeout(5_000) { failed.await() } }
    }

    @Test
    fun `add sheet state survives in the controller`() = runTest {
        val c = backgroundScope.controller()
        assertEquals(false, c.sheetOpen.value)
        c.openSheet()
        assertEquals(true, c.sheetOpen.value)
        c.closeSheet()
        assertEquals(false, c.sheetOpen.value)
    }
}
