package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenerateDailySummaryTest {
    private class FakeEngine(
        override val id: SummaryEngineId = SummaryEngineId.Gemma,
        private val fail: Boolean = false,
    ) : SummaryEngine {
        var calls = 0
        override suspend fun label() = id.name
        override val onDevice = true
        override suspend fun status(): EngineStatus = EngineStatus.Ready
        override fun summarize(facts: DailyFacts): Flow<String> = flow {
            calls++
            emit("Hello ")
            if (fail) error("boom")
            emit("world")
        }
    }

    private val day = LocalDate(2026, 11, 10)
    private val settings = DailyOverviewSettings(createTestDataStoreHandling())
    private val template = TemplateEngine()

    private fun facts(date: LocalDate = day, reads: Int = 3) = DailyFacts(
        date = date,
        unit = UpdateUnit.Chapters,
        readCount = reads,
        titles = emptyList(),
        readsByHour = List(24) { 0 },
        updates = emptyList(),
        failedSources = emptyList(),
    )

    private fun resolver(engine: SummaryEngine) =
        EngineResolver(listOf(template, engine), listOf(SummaryEngineId.Gemma))

    private suspend fun run(engine: FakeEngine, facts: DailyFacts = facts(), bypass: Boolean = false) =
        generateDailySummary(facts, resolver(engine), settings, template, bypass)

    @Test
    fun `cache hit for the same date skips the engine`() = runTest {
        settings.cache(DailySummary(day, SummaryEngineId.Gemma, "Cached"))
        val engine = FakeEngine()
        assertEquals("Cached", run(engine).text)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `cache for another date is ignored`() = runTest {
        settings.cache(DailySummary(LocalDate(2026, 11, 9), SummaryEngineId.Gemma, "Stale"))
        val engine = FakeEngine()
        assertEquals("Hello world", run(engine).text)
        assertEquals(1, engine.calls)
        assertEquals(day, settings.cached()?.date)
    }

    @Test
    fun `bypassCache reruns the engine and re-caches`() = runTest {
        settings.cache(DailySummary(day, SummaryEngineId.Gemma, "Old"))
        val engine = FakeEngine()
        assertEquals("Hello world", run(engine, bypass = true).text)
        assertEquals("Hello world", settings.cached()?.text)
    }

    @Test
    fun `quiet day returns template text without the engine or cache`() = runTest {
        val engine = FakeEngine()
        val result = run(engine, facts(reads = 0))
        assertEquals(SummaryEngineId.Template, result.engine)
        assertEquals(0, engine.calls)
        assertNull(settings.cached())
    }

    @Test
    fun `engine failure returns template text and caches nothing`() = runTest {
        val result = run(FakeEngine(fail = true))
        assertEquals(SummaryEngineId.Template, result.engine)
        assertNull(settings.cached())
    }

    @Test
    fun `Template resolution is not cached`() = runTest {
        settings.setEngine(SummaryEngineChoice.Template)
        val engine = FakeEngine()
        assertEquals(SummaryEngineId.Template, run(engine).engine)
        assertEquals(0, engine.calls)
        assertNull(settings.cached())
    }
}
