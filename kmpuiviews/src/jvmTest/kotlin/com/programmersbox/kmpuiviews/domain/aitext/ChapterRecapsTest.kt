package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.testing.FakeAiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterRecapsTest {
    private val engine = AiEngineInfo("Fake", onDevice = true, destination = null)

    private fun fakeAi(engine: AiEngineInfo?, recapFlow: (String) -> Flow<String>) =
        FakeAiText(engine = engine, output = { _, input -> recapFlow(input as String) })

    private val FakeAiText.recapCalls get() = inputsFor<AiFeature.Recap>().size

    private fun TestScope.recaps(ai: AiText) = ChapterRecaps(ai, TestScope(StandardTestDispatcher(testScheduler)))

    @Test
    fun `starts idle and does not generate`() = runTest {
        val ai = fakeAi(engine) { flow { emit("x") } }
        val recaps = recaps(ai)
        advanceUntilIdle()
        assertEquals(AiTextState.Idle, recaps.stateFor("ch13").value)
        assertEquals(0, ai.recapCalls)
        assertTrue(recaps.available.value)
    }

    @Test
    fun `request generates once and caches`() = runTest {
        val ai = fakeAi(engine) { flow { emit("sum") } }
        val recaps = recaps(ai)
        recaps.request("ch13") { "text" }
        advanceUntilIdle()
        assertEquals(AiTextState.Done("sum", engine), recaps.stateFor("ch13").value)
        recaps.request("ch13") { "text" }
        advanceUntilIdle()
        assertEquals(1, ai.recapCalls)
    }

    @Test
    fun `second request while streaming is a no-op`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val ai = fakeAi(engine) { flow { emit("a"); gate.await(); emit("b") } }
        val recaps = recaps(ai)
        recaps.request("ch13") { "text" }
        advanceUntilIdle()
        assertIs<AiTextState.Streaming>(recaps.stateFor("ch13").value)
        recaps.request("ch13") { "text" }
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, ai.recapCalls)
        assertEquals(AiTextState.Done("ab", engine), recaps.stateFor("ch13").value)
    }

    @Test
    fun `changing chapter cancels and ignores stale recap`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val ai = fakeAi(engine) { flow { emit("a"); gate.await(); emit("b") } }
        val recaps = recaps(ai)
        recaps.request("ch13") { "text" }
        advanceUntilIdle()
        recaps.onChapterChanged("ch14")
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AiTextState.Idle, recaps.stateFor("ch13").value)
        assertEquals(AiTextState.Idle, recaps.stateFor("ch14").value)
    }

    @Test
    fun `recap error then retry`() = runTest {
        var fail = true
        val ai = fakeAi(engine) {
            flow {
                if (fail) throw IllegalStateException("boom")
                emit("ok")
            }
        }
        val recaps = recaps(ai)
        recaps.request("ch13") { "text" }
        advanceUntilIdle()
        assertIs<AiTextState.Error>(recaps.stateFor("ch13").value)
        fail = false
        recaps.request("ch13") { "text" }
        advanceUntilIdle()
        assertEquals(AiTextState.Done("ok", engine), recaps.stateFor("ch13").value)
    }

    @Test
    fun `no previous chapter gives Error`() = runTest {
        val ai = fakeAi(engine) { flow { emit("x") } }
        val recaps = recaps(ai)
        recaps.request("ch13") { null }
        advanceUntilIdle()
        assertIs<AiTextState.Error>(recaps.stateFor("ch13").value)
        assertEquals(0, ai.recapCalls)
    }

    @Test
    fun `no engine gives Error without loading the previous chapter`() = runTest {
        val ai = fakeAi(null) { flow { emit("x") } }
        val recaps = recaps(ai)
        var loads = 0
        recaps.request("ch13") { loads++; "text" }
        advanceUntilIdle()
        assertIs<AiTextState.Error>(recaps.stateFor("ch13").value)
        assertEquals(0, loads)
        assertEquals(0, ai.recapCalls)
    }
}
