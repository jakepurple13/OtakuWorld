package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiTextRunnerTest {
    private val engine = AiEngineInfo("Fake", onDevice = true, destination = null)

    private fun TestScope.collectStates(runner: AiTextRunner): MutableList<AiTextState> {
        val states = mutableListOf<AiTextState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { runner.state.collect { states += it } }
        return states
    }

    @Test
    fun `deltas become streaming then done`() = runTest {
        val runner = AiTextRunner(TestScope(StandardTestDispatcher(testScheduler)))
        val states = collectStates(runner)
        runner.run(engine) { flow { emit("a"); emit("b") } }
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
        val runner = AiTextRunner(TestScope(StandardTestDispatcher(testScheduler)))
        runner.run(engine) { flow { throw IllegalStateException("x") } }
        advanceUntilIdle()
        assertIs<AiTextState.Error>(runner.state.value)
    }

    @Test
    fun `new run cancels previous`() = runTest {
        val runner = AiTextRunner(TestScope(StandardTestDispatcher(testScheduler)))
        val states = collectStates(runner)
        val gate = CompletableDeferred<Unit>()
        runner.run(engine) { flow { gate.await(); emit("first") } }
        advanceUntilIdle()
        runner.run(engine) { flowOf("second") }
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(states.none { it is AiTextState.Streaming && it.text.contains("first") })
        assertEquals(AiTextState.Done("second", engine), runner.state.value)
    }
}
