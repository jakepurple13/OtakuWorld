package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.testing.FakeAiText
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AiTextAvailabilityTest {
    @Test
    fun `availability retries until ready`() = runTest {
        val ai = FakeAiText(engine = null)
        val seen = mutableListOf<AiEngineInfo?>()
        val job = launch { ai.availability(AiFeature.Tldr, retry = 2.seconds).toList(seen) }
        runCurrent()
        assertEquals(listOf<AiEngineInfo?>(null), seen)

        ai.engine = FakeAiText.ENGINE
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(1, seen.size)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(listOf(null, FakeAiText.ENGINE), seen)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `availability passes the input length through`() = runTest {
        val lengths = mutableListOf<Int>()
        val ai = FakeAiText(engineRule = { _, len -> lengths += len; FakeAiText.ENGINE })
        ai.availability(AiFeature.Proofread, inputLength = 42).toList()
        assertEquals(listOf(42), lengths)
    }
}
