package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultAiTextTest {
    private class FakeEngine(
        override val id: SummaryEngineId,
        var status: EngineStatus = EngineStatus.Ready,
        override val onDevice: Boolean = true,
        private val deltas: List<String> = listOf("generated"),
    ) : SummaryEngine {
        var generateCalls = 0
        var collectThread: String? = null
        var statusThread: String? = null
        override suspend fun label() = id.name
        override suspend fun status(): EngineStatus {
            statusThread = Thread.currentThread().name
            return status
        }
        override fun summarize(facts: DailyFacts): Flow<String> = flowOf()
        override fun generate(system: String, user: String): Flow<String> {
            generateCalls++
            return flow {
                collectThread = Thread.currentThread().name
                deltas.forEach { emit(it) }
            }
        }
    }

    private class FakeNanoTools(var ready: Boolean = true) : NanoTools {
        val features = mutableListOf<AiFeature>()
        override suspend fun isReady(feature: AiFeature, inputLength: Int) = ready
        override fun run(feature: AiFeature, input: String, style: RewriteStyle?): Flow<String> {
            features += feature
            return flowOf("nano-out")
        }
    }

    private val order = listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma)

    private fun aiText(vararg engines: SummaryEngine, tools: NanoTools? = null) =
        DefaultAiText(listOf<SummaryEngine>(TemplateEngine()) + engines, tools, order)

    @Test
    fun `nano ready uses nano`() = runTest {
        val ai = aiText(FakeEngine(SummaryEngineId.Nano), FakeEngine(SummaryEngineId.Gemma))
        assertEquals("Nano", ai.engineFor(AiFeature.Tldr)?.label)
    }

    @Test
    fun `nano unavailable falls back to gemma`() = runTest {
        val ai = aiText(FakeEngine(SummaryEngineId.Nano, EngineStatus.Unavailable), FakeEngine(SummaryEngineId.Gemma))
        assertEquals("Gemma", ai.engineFor(AiFeature.Tldr)?.label)
    }

    @Test
    fun `nothing ready returns null`() = runTest {
        val ai = aiText(
            FakeEngine(SummaryEngineId.Nano, EngineStatus.Unavailable),
            FakeEngine(SummaryEngineId.Gemma, EngineStatus.NeedsSetup("x")),
        )
        assertNull(ai.engineFor(AiFeature.Tldr))
    }

    @Test
    fun `provider and template are never chosen`() = runTest {
        val ai = aiText(
            FakeEngine(SummaryEngineId.Nano, EngineStatus.Unavailable),
            FakeEngine(SummaryEngineId.Gemma, EngineStatus.Unavailable),
            FakeEngine(SummaryEngineId.Provider, onDevice = false),
        )
        assertNull(ai.engineFor(AiFeature.Recap))
    }

    @Test
    fun `availability is re-checked per call`() = runTest {
        val nano = FakeEngine(SummaryEngineId.Nano, EngineStatus.NeedsSetup("download"))
        val ai = aiText(nano)
        assertNull(ai.engineFor(AiFeature.Tldr))
        nano.status = EngineStatus.Ready
        assertEquals("Nano", ai.engineFor(AiFeature.Tldr)?.label)
    }

    @Test
    fun `long proofread skips nano`() = runTest {
        val ai = aiText(FakeEngine(SummaryEngineId.Nano), FakeEngine(SummaryEngineId.Gemma))
        assertEquals("Gemma", ai.engineFor(AiFeature.Proofread, AiTextPrompts.NANO_TEXT_LIMIT + 1)?.label)
    }

    @Test
    fun `long proofread with only nano returns null`() = runTest {
        val ai = aiText(FakeEngine(SummaryEngineId.Nano))
        assertNull(ai.engineFor(AiFeature.Proofread, AiTextPrompts.NANO_TEXT_LIMIT + 1))
    }

    @Test
    fun `nano tools used when ready`() = runTest {
        val nano = FakeEngine(SummaryEngineId.Nano)
        val tools = FakeNanoTools()
        val ai = aiText(nano, tools = tools)
        assertEquals(listOf("nano-out"), ai.tldr("x").toList())
        assertEquals(listOf(AiFeature.Tldr), tools.features)
        assertEquals(0, nano.generateCalls)
    }

    @Test
    fun `nano without ready tools uses generate`() = runTest {
        val nano = FakeEngine(SummaryEngineId.Nano)
        val ai = aiText(nano, tools = FakeNanoTools(ready = false))
        assertEquals(listOf("generated"), ai.tldr("x").toList())
        assertEquals(1, nano.generateCalls)
    }

    @Test
    fun `no engine ready makes the flow throw on collect`() = runTest {
        val ai = aiText(FakeEngine(SummaryEngineId.Nano, EngineStatus.Unavailable))
        val flow = ai.tldr("x")
        assertFailsWith<IllegalStateException> { flow.toList() }
    }

    @Test
    fun `generation runs off the collector thread`() = runTest {
        val gemma = FakeEngine(SummaryEngineId.Gemma)
        val ai = aiText(gemma)
        val testThread = Thread.currentThread().name
        ai.tldr("x").toList()
        assertNotEquals(testThread, gemma.collectThread)
        assertTrue(gemma.collectThread!!.startsWith("DefaultDispatcher"))
    }

    @Test
    fun `engineFor status check runs off the caller thread`() = runTest {
        val gemma = FakeEngine(SummaryEngineId.Gemma)
        val ai = aiText(gemma)
        val testThread = Thread.currentThread().name
        ai.engineFor(AiFeature.Tldr)
        assertNotEquals(testThread, gemma.statusThread)
        assertTrue(gemma.statusThread!!.startsWith("DefaultDispatcher"))
    }

    /** Holds the first generation until the other one has arrived, then records the peak overlap. */
    private class GatedEngine(private val otherArrived: CompletableDeferred<Unit>) : SummaryEngine {
        override val id = SummaryEngineId.Gemma
        override val onDevice = true
        val active = AtomicInteger()
        val maxActive = AtomicInteger()
        val starts = AtomicInteger()
        override suspend fun label() = id.name
        override suspend fun status() = EngineStatus.Ready
        override fun summarize(facts: DailyFacts): Flow<String> = flowOf()
        override fun generate(system: String, user: String): Flow<String> = flow {
            maxActive.accumulateAndGet(active.incrementAndGet(), ::maxOf)
            if (starts.incrementAndGet() == 1) otherArrived.await() else otherArrived.complete(Unit)
            emit(user)
            active.decrementAndGet()
        }
    }

    /** Signals when a second caller asks for the lock, i.e. it is waiting behind the first. */
    private class LockWatcher(private val onSecondLock: () -> Unit, private val inner: Mutex = Mutex()) : Mutex by inner {
        private val locks = AtomicInteger()
        override suspend fun lock(owner: Any?) {
            if (locks.incrementAndGet() == 2) onSecondLock()
            inner.lock(owner)
        }
    }

    @Test
    fun `concurrent generations never overlap`() = runTest {
        val otherArrived = CompletableDeferred<Unit>()
        val engine = GatedEngine(otherArrived)
        val ai = DefaultAiText(
            listOf(TemplateEngine(), engine),
            nanoTools = null,
            order = order,
            generationLock = LockWatcher({ otherArrived.complete(Unit) }),
        )
        val tldr = async { ai.tldr("a").toList() }
        val names = async { ai.listNames(listOf("b")).toList() }
        tldr.await()
        names.await()
        assertEquals(2, engine.starts.get())
        assertEquals(1, engine.maxActive.get())
    }
}
