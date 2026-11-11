package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineResolverTest {
    private class Fake(
        override val id: SummaryEngineId,
        private val status: EngineStatus,
    ) : SummaryEngine {
        override suspend fun label() = id.name
        override val onDevice = true
        override suspend fun status() = status
        override fun summarize(facts: DailyFacts): Flow<String> = flowOf(id.name)
    }

    private val order = listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma)

    private fun resolver(vararg engines: SummaryEngine) =
        EngineResolver(listOf(TemplateEngine()) + engines, order)

    @Test
    fun `automatic picks first Ready in order`() = runTest {
        val r = resolver(
            Fake(SummaryEngineId.Nano, EngineStatus.Unavailable),
            Fake(SummaryEngineId.Gemma, EngineStatus.Ready),
        )
        assertEquals(SummaryEngineId.Gemma, r.resolve(SummaryEngineChoice.Automatic).id)
    }

    @Test
    fun `automatic falls back to Template when nothing is Ready`() = runTest {
        val r = resolver(Fake(SummaryEngineId.Nano, EngineStatus.NeedsSetup("download")))
        assertEquals(SummaryEngineId.Template, r.resolve(SummaryEngineChoice.Automatic).id)
    }

    @Test
    fun `automatic never picks Provider even when it is the only Ready engine`() = runTest {
        val r = resolver(Fake(SummaryEngineId.Provider, EngineStatus.Ready))
        assertEquals(SummaryEngineId.Template, r.resolve(SummaryEngineChoice.Automatic).id)
    }

    @Test
    fun `explicit choice that is not Ready falls back to Template`() = runTest {
        val r = resolver(Fake(SummaryEngineId.Gemma, EngineStatus.NeedsSetup("download")))
        assertEquals(SummaryEngineId.Template, r.resolve(SummaryEngineChoice.Gemma).id)
    }

    @Test
    fun `explicit Provider that is Ready is used`() = runTest {
        val r = resolver(Fake(SummaryEngineId.Provider, EngineStatus.Ready))
        assertEquals(SummaryEngineId.Provider, r.resolve(SummaryEngineChoice.Provider).id)
    }

    @Test
    fun `choice for an engine not registered on this platform falls back to Template`() = runTest {
        val r = resolver()
        assertEquals(SummaryEngineId.Template, r.resolve(SummaryEngineChoice.Ollama).id)
    }

    @Test
    fun `available reports status per registered engine`() = runTest {
        val r = resolver(Fake(SummaryEngineId.Nano, EngineStatus.Unavailable))
        assertEquals(
            mapOf(
                SummaryEngineId.Template to EngineStatus.Ready,
                SummaryEngineId.Nano to EngineStatus.Unavailable,
            ),
            r.available(),
        )
    }
}
