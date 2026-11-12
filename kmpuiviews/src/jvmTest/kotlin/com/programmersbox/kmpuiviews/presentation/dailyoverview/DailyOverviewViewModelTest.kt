package com.programmersbox.kmpuiviews.presentation.dailyoverview

import com.programmersbox.kmpuiviews.testing.clearAndJoin
import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.HeatMapDatabase
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFactsBuilder
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewSettings
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailySummary
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import com.programmersbox.kmpuiviews.testing.FakeKmpGenericInfo
import com.programmersbox.kmpuiviews.testing.FakeUpdateCheckReportStore
import com.programmersbox.kmpuiviews.testing.FixedClock
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestHeatMapDatabase
import com.programmersbox.kmpuiviews.testing.createTestHistoryDatabase
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DailyOverviewViewModelTest {

    private class FakeEngine(
        override val id: SummaryEngineId = SummaryEngineId.Gemma,
        private val chunks: List<String> = listOf("Hello ", "world"),
        private val failAfterFirst: Boolean = false,
        private val gate: CompletableDeferred<Unit>? = null,
        private val sendsTo: String? = null,
        private val statusGate: CompletableDeferred<Unit>? = null,
    ) : SummaryEngine {
        @Volatile
        var calls = 0
        override suspend fun label() = "Fake ${id.name}"
        override val onDevice = sendsTo == null
        override suspend fun destination() = sendsTo
        override suspend fun status(): EngineStatus {
            statusGate?.await()
            return EngineStatus.Ready
        }
        override fun generate(system: String, user: String): Flow<String> = error("unused")
        override fun summarize(facts: DailyFacts): Flow<String> = flow {
            calls++
            gate?.await()
            chunks.forEachIndexed { i, c ->
                emit(c)
                if (failAfterFirst && i == 0) error("boom")
            }
        }
    }

    private val zone = TimeZone.of("America/Phoenix")
    private val yesterday = LocalDate(2026, 11, 10)
    private val clock = FixedClock(LocalDateTime(2026, 11, 11, 10, 0).toInstant(zone))

    private val viewModelStore = ViewModelStore()
    private lateinit var items: ItemDatabase
    private lateinit var heatMap: HeatMapDatabase
    private lateinit var history: HistoryDatabase
    private lateinit var settings: DailyOverviewSettings
    private val template = TemplateEngine()

    // Facts come from real Room DBs on their own dispatcher, so like the other ViewModel tests
    // here: Main is Default and the test waits in real time.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        items = createTestItemDatabase()
        heatMap = createTestHeatMapDatabase()
        history = createTestHistoryDatabase()
        settings = DailyOverviewSettings(createTestDataStoreHandling())
    }

    @AfterTest
    fun tearDown() {
        viewModelStore.clearAndJoin()
        Dispatchers.resetMain()
        items.close()
        heatMap.close()
        history.close()
    }

    private suspend fun await(condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) { while (!condition()) delay(10) }
        }
    }

    private suspend fun readSomething() {
        val at = LocalDateTime(2026, 11, 10, 9, 0).toInstant(zone).toEpochMilliseconds()
        heatMap.heatMapDao().insertHeatMap(HeatMapItem(yesterday, 1))
        items.itemDao().insertChapter(ChapterWatched("c1", "Ch 1", "fav", createdAt = at, updatedAt = at))
    }

    private var vmCount = 0

    private fun viewModel(vararg engines: SummaryEngine): DailyOverviewViewModel {
        val builder = DailyFactsBuilder(
            itemDao = items.itemDao(),
            heatMapDao = heatMap.heatMapDao(),
            historyDao = history.historyDao(),
            reportStore = FakeUpdateCheckReportStore(null),
            genericInfo = FakeKmpGenericInfo(),
            clock = clock,
            zone = { zone },
        )
        val resolver = EngineResolver(listOf(template) + engines, listOf(SummaryEngineId.Gemma))
        return DailyOverviewViewModel(builder, resolver, settings, template)
            .also { viewModelStore.put("vm${vmCount++}", it) }
    }

    private fun DailyOverviewViewModel.done() = uiState.value.summary as? SummaryState.Done

    @Test
    fun `facts render before the summary finishes`() = runTest {
        readSomething()
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(FakeEngine(gate = gate))
        await { vm.uiState.value.facts != null }
        assertTrue(vm.uiState.value.summary !is SummaryState.Done)
        gate.complete(Unit)
        await { vm.done() != null }
        assertEquals("Hello world", vm.done()?.text)
    }

    @Test
    fun `facts are published while a slow engine status is still suspended`() = runTest {
        readSomething()
        val statusGate = CompletableDeferred<Unit>()
        val vm = viewModel(FakeEngine(statusGate = statusGate))
        await { vm.uiState.value.facts != null }
        assertEquals(SummaryState.Loading, vm.uiState.value.summary)
        statusGate.complete(Unit)
        await { vm.done() != null }
        assertEquals("Hello world", vm.done()?.text)
    }

    @Test
    fun `summary stays Loading until the first non-empty chunk`() = runTest {
        readSomething()
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(FakeEngine(chunks = listOf("", "Hi"), gate = gate))
        await { vm.uiState.value.facts != null }
        // Give the engine time to start; nothing has been emitted yet.
        withContext(Dispatchers.Default) { delay(200) }
        assertEquals(SummaryState.Loading, vm.uiState.value.summary)
        gate.complete(Unit)
        await { vm.done() != null }
        assertEquals("Hi", vm.done()?.text)
    }

    @Test
    fun `cloud engine carries its destination to Done`() = runTest {
        readSomething()
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(FakeEngine(chunks = listOf("Hi"), gate = gate, sendsTo = "Google"))
        await { vm.uiState.value.facts != null }
        gate.complete(Unit)
        await { vm.done() != null }
        val done = vm.done()!!
        assertEquals("Google", done.destination)
        assertEquals(false, done.onDevice)
    }

    @Test
    fun `on-device engine has no destination`() = runTest {
        readSomething()
        val vm = viewModel(FakeEngine())
        await { vm.done() != null }
        assertNull(vm.done()?.destination)
    }

    @Test
    fun `cached summary for yesterday is shown without calling the engine`() = runTest {
        readSomething()
        settings.cache(DailySummary(yesterday, SummaryEngineId.Gemma, "From cache"))
        val engine = FakeEngine()
        val vm = viewModel(engine)
        await { vm.done() != null }
        assertEquals("From cache", vm.done()?.text)
        assertEquals("Fake Gemma", vm.done()?.label)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `cached summary for another date is ignored`() = runTest {
        readSomething()
        settings.cache(DailySummary(LocalDate(2026, 11, 9), SummaryEngineId.Gemma, "Stale"))
        val engine = FakeEngine()
        val vm = viewModel(engine)
        await { vm.done() != null }
        assertEquals("Hello world", vm.done()?.text)
        assertEquals(1, engine.calls)
        assertEquals(yesterday, settings.cached()?.date)
    }

    @Test
    fun `regenerate bypasses cache and re-caches`() = runTest {
        readSomething()
        settings.cache(DailySummary(yesterday, SummaryEngineId.Gemma, "Old"))
        val engine = FakeEngine()
        val vm = viewModel(engine)
        await { vm.done()?.text == "Old" }
        assertEquals(0, engine.calls)
        vm.regenerate()
        await { vm.done()?.text == "Hello world" }
        assertEquals(1, engine.calls)
        assertEquals("Hello world", settings.cached()?.text)
    }

    @Test
    fun `quiet day skips the engine and shows template text`() = runTest {
        val engine = FakeEngine()
        val vm = viewModel(engine)
        await { vm.done() != null }
        assertEquals(SummaryEngineId.Template, vm.done()?.engine)
        assertEquals(0, engine.calls)
        assertNull(settings.cached())
    }

    @Test
    fun `engine throwing mid-stream ends in Done with template text, nothing cached`() = runTest {
        readSomething()
        val vm = viewModel(FakeEngine(failAfterFirst = true))
        await { vm.done() != null }
        val done = vm.done()!!
        assertEquals(SummaryEngineId.Template, done.engine)
        assertTrue(vm.uiState.value.facts != null)
        assertTrue(done.text.isNotBlank() && !done.text.startsWith("Hello"))
        assertNull(settings.cached())
    }

    @Test
    fun `needsSetup true when the choice resolved to Template but the choice was not Template`() = runTest {
        readSomething()
        val vm = viewModel() // no Gemma registered, Automatic resolves to Template
        await { vm.done() != null }
        assertTrue(vm.uiState.value.needsSetup)

        settings.setEngine(SummaryEngineChoice.Template)
        val vm2 = viewModel()
        await { vm2.done() != null }
        assertEquals(false, vm2.uiState.value.needsSetup)
    }

    @Test
    fun `needsSetup false when an engine is ready`() = runTest {
        readSomething()
        val vm = viewModel(FakeEngine())
        await { vm.done() != null }
        assertEquals(false, vm.uiState.value.needsSetup)
    }
}
