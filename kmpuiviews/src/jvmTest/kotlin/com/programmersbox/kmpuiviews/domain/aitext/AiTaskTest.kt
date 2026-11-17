package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.testing.FakeAiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiTaskTest {
    private val engine = FakeAiText.ENGINE

    private fun TestScope.task(ai: FakeAiText) = AiTask(ai, TestScope(StandardTestDispatcher(testScheduler)))

    private fun TestScope.collectStates(task: AiTask): MutableList<AiTextState> {
        val states = mutableListOf<AiTextState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { task.state.collect { states += it } }
        return states
    }

    @Test
    fun `deltas become streaming then done`() = runTest {
        val task = task(FakeAiText(output = { _, _ -> flow { emit("a"); emit("b") } }))
        val states = collectStates(task)
        assertTrue(task.start(AiFeature.Tldr, "x"))
        advanceUntilIdle()
        assertEquals(
            listOf(
                AiTextState.Idle,
                AiTextState.Loading,
                AiTextState.Streaming("a", engine),
                AiTextState.Streaming("ab", engine),
                AiTextState.Done("ab", engine),
            ),
            states,
        )
    }

    @Test
    fun `generation error becomes Error state`() = runTest {
        val task = task(FakeAiText(output = { _, _ -> flow { throw IllegalStateException("x") } }))
        task.start(AiFeature.Tldr, "x")
        advanceUntilIdle()
        assertIs<AiTextState.Error>(task.state.value)
    }

    @Test
    fun `new run cancels previous`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var first = true
        val ai = FakeAiText(output = { _, _ ->
            if (first) {
                first = false
                flow { gate.await(); emit("first") }
            } else {
                flowOf("second")
            }
        })
        val task = task(ai)
        val states = collectStates(task)
        task.start(AiFeature.Tldr, "x")
        advanceUntilIdle()
        task.start(AiFeature.Tldr, "y")
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(states.none { it is AiTextState.Streaming && it.text.contains("first") })
        assertEquals(AiTextState.Done("second", engine), task.state.value)
    }

    @Test
    fun `no engine returns false and stays idle`() = runTest {
        val ai = FakeAiText(engine = null)
        val task = task(ai)
        assertFalse(task.start(AiFeature.Tldr, "x"))
        advanceUntilIdle()
        assertEquals(AiTextState.Idle, task.state.value)
        assertTrue(ai.runs.isEmpty())
    }

    @Test
    fun `engine is picked for the prepared input length`() = runTest {
        val lengths = mutableListOf<Int>()
        val ai = FakeAiText(engineRule = { _, len -> lengths += len; FakeAiText.ENGINE })
        task(ai).start(AiFeature.Tldr, "a ".repeat(5_000))
        assertEquals(listOf(AiFeature.Tldr.prepare("a ".repeat(5_000)).length), lengths)
    }

    @Test
    fun `finish applies only to done`() = runTest {
        val task = task(FakeAiText(output = { _, _ -> flowOf("1. A\n", "2. a") }))
        val states = collectStates(task)
        task.start(AiFeature.ListNames, listOf("t"))
        advanceUntilIdle()
        assertEquals(AiTextState.Streaming("1. A\n", engine), states.first { it is AiTextState.Streaming })
        assertEquals(AiTextState.Done("A", engine), task.state.value)
    }

    @Test
    fun `cancel during engine resolution starts nothing`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val ai = FakeAiText(engineRule = { _, _ -> gate.await(); FakeAiText.ENGINE })
        val task = task(ai)
        val job = launch { task.start(AiFeature.Tldr, "x") }
        runCurrent()
        job.cancel()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AiTextState.Idle, task.state.value)
        assertTrue(ai.runs.isEmpty())
    }

    @Test
    fun `reset returns to idle`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val task = task(FakeAiText(output = { _, _ -> flow { gate.await(); emit("x") } }))
        task.start(AiFeature.Tldr, "x")
        advanceUntilIdle()
        task.reset()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AiTextState.Idle, task.state.value)
    }
}
