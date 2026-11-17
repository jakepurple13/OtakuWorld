package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** The default [AiText.availability] polling, on a bare [AiText]. */
class AiTextAvailabilityTest {
    private class Bare(var engine: AiEngineInfo? = null) : AiText {
        val lengths = mutableListOf<Int>()
        override suspend fun engineFor(feature: AiFeature<*>, inputLength: Int): AiEngineInfo? {
            lengths += inputLength
            return engine
        }

        override fun <I> run(feature: AiFeature<I>, input: I): Flow<String> = emptyFlow()
    }

    private val engine = AiEngineInfo("Fake", onDevice = true, destination = null)

    @Test
    fun `availability retries until ready`() = runTest {
        val ai = Bare()
        val seen = mutableListOf<AiEngineInfo?>()
        val job = launch { ai.availability(AiFeature.Tldr, retry = 2.seconds).toList(seen) }
        runCurrent()
        assertEquals(listOf<AiEngineInfo?>(null), seen)

        ai.engine = engine
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(1, seen.size)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(listOf(null, engine), seen)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `availability passes the input length through`() = runTest {
        val ai = Bare(engine)
        ai.availability(AiFeature.Proofread, inputLength = 42).toList()
        assertEquals(listOf(42), ai.lengths)
    }
}
