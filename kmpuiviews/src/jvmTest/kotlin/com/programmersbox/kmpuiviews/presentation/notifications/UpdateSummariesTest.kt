package com.programmersbox.kmpuiviews.presentation.notifications

import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.FakeUpdateCheckReportStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.onCompletion

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateSummariesTest {
    private fun report(startedAt: Long, finishedAt: Long = startedAt + 1_000) = UpdateCheckReport(
        startedAt = startedAt,
        finishedAt = finishedAt,
        favoriteCount = 10,
        checkedCount = 10,
        updatedCount = 2,
        sourceFailures = emptyList(),
        itemFailures = emptyList(),
    )

    private fun noti(title: String, createdAt: Long) = NotificationItem(
        id = title.hashCode(),
        url = "https://example.com/$title",
        summaryText = "$title · 1 new chapter: Ch 1",
        notiTitle = title,
        imageUrl = null,
        source = "src",
        contentTitle = title,
        createdAt = createdAt,
    )

    private val items = MutableStateFlow(listOf(noti("A", 2_000), noti("B", 2_100), noti("Old", 500)))

    private fun TestScope.summaries(
        ai: FakeAiText = FakeAiText(),
        store: FakeUpdateCheckReportStore = FakeUpdateCheckReportStore(report(startedAt = 1_000)),
        cache: UpdateSummaryCache = UpdateSummaryCache(),
    ) = UpdateSummaries(ai, store, cache, items, TestScope(StandardTestDispatcher(testScheduler)))

    @Test
    fun `only notifications from the latest check`() = runTest {
        val ai = FakeAiText()
        val s = summaries(ai)
        advanceUntilIdle()
        assertEquals(AiTextState.Done("out", FakeAiText.ENGINE), s.state.value)
        @Suppress("UNCHECKED_CAST")
        val lines = ai.inputsFor<AiFeature.UpdateSummary>().single() as List<String>
        assertEquals(listOf("A · 1 new chapter: Ch 1", "B · 1 new chapter: Ch 1"), lines.sorted())
    }

    @Test
    fun `one update shows nothing`() = runTest {
        items.value = listOf(noti("A", 2_000), noti("Old", 500))
        val ai = FakeAiText()
        val s = summaries(ai)
        advanceUntilIdle()
        assertNull(s.state.value)
        assertEquals(0, ai.runs.size)
    }

    @Test
    fun `no report shows nothing`() = runTest {
        val s = summaries(store = FakeUpdateCheckReportStore(null))
        advanceUntilIdle()
        assertNull(s.state.value)
    }

    @Test
    fun `same check reuses the cached summary`() = runTest {
        val cache = UpdateSummaryCache()
        val store = FakeUpdateCheckReportStore(report(startedAt = 1_000))
        summaries(store = store, cache = cache)
        advanceUntilIdle()
        val ai = FakeAiText()
        val again = summaries(ai = ai, store = store, cache = cache)
        advanceUntilIdle()
        assertEquals(AiTextState.Done("out", FakeAiText.ENGINE), again.state.value)
        assertEquals(0, ai.runs.size)
    }

    @Test
    fun `new check summarizes again`() = runTest {
        val ai = FakeAiText()
        val store = FakeUpdateCheckReportStore(report(startedAt = 1_000))
        summaries(ai = ai, store = store)
        advanceUntilIdle()
        items.value = items.value + noti("C", 9_000) + noti("D", 9_100)
        store.save(report(startedAt = 8_000))
        advanceUntilIdle()
        assertEquals(2, ai.runs.size)
        @Suppress("UNCHECKED_CAST")
        val latest = ai.inputsFor<AiFeature.UpdateSummary>().last() as List<String>
        assertEquals(listOf("C · 1 new chapter: Ch 1", "D · 1 new chapter: Ch 1"), latest.sorted())
    }

    @Test
    fun `hides when ai becomes unavailable`() = runTest {
        val ai = FakeAiText()
        val s = summaries(ai)
        advanceUntilIdle()
        ai.engine = null
        advanceUntilIdle()
        assertNull(s.state.value)
    }

    @Test
    fun `engine gone at start shows a retryable error`() = runTest {
        var calls = 0
        // Ready for the availability check, gone by the time the summary starts
        val ai = FakeAiText(engineRule = { _, _ -> if (calls++ == 0) FakeAiText.ENGINE else null })
        val s = summaries(ai)
        advanceUntilIdle()
        kotlin.test.assertIs<AiTextState.Error>(s.state.value)
        ai.engineRule = null
        s.retry()
        advanceUntilIdle()
        assertEquals(AiTextState.Done("out", FakeAiText.ENGINE), s.state.value)
    }

    @Test
    fun `failed summary can retry`() = runTest {
        var fail = true
        val ai = FakeAiText(output = { _, _ ->
            kotlinx.coroutines.flow.flow {
                if (fail) error("boom")
                emit("ok")
            }
        })
        val s = summaries(ai)
        advanceUntilIdle()
        kotlin.test.assertIs<AiTextState.Error>(s.state.value)
        fail = false
        s.retry()
        advanceUntilIdle()
        assertEquals(AiTextState.Done("ok", FakeAiText.ENGINE), s.state.value)
    }

    @Test
    fun `rows arriving after the report restart the summary`() = runTest {
        val ai = FakeAiText()
        items.value = listOf(noti("Old", 500))
        val store = FakeUpdateCheckReportStore(report(startedAt = 1_000))
        val cache = UpdateSummaryCache()
        val s = summaries(ai = ai, store = store, cache = cache)
        advanceUntilIdle()
        // The worker writes one row at a time after saving the report
        items.value = items.value + noti("A", 2_000)
        advanceUntilIdle()
        items.value = items.value + noti("B", 2_100)
        advanceUntilIdle()
        items.value = items.value + noti("C", 2_200)
        advanceUntilIdle()
        @Suppress("UNCHECKED_CAST")
        val last = ai.inputsFor<AiFeature.UpdateSummary>().last() as List<String>
        assertEquals(3, last.size)
        assertEquals(AiTextState.Done("out", FakeAiText.ENGINE), s.state.value)
        // A new screen with the same cache shows it without another run
        val again = FakeAiText()
        summaries(ai = again, store = store, cache = cache)
        advanceUntilIdle()
        assertEquals(0, again.runs.size)
    }

    @Test
    fun `a retry run summarizes again`() = runTest {
        val ai = FakeAiText()
        val store = FakeUpdateCheckReportStore(report(startedAt = 1_000, finishedAt = 3_000))
        val cache = UpdateSummaryCache()
        summaries(ai = ai, store = store, cache = cache)
        advanceUntilIdle()
        items.value = items.value + noti("C", 5_000)
        store.save(report(startedAt = 1_000, finishedAt = 3_000).copy(retriedAt = 6_000))
        advanceUntilIdle()
        @Suppress("UNCHECKED_CAST")
        val last = ai.inputsFor<AiFeature.UpdateSummary>().last() as List<String>
        assertEquals(3, last.size)
    }

    @Test
    fun `turning ai off stops the running summary`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        var stopped = false
        val ai = FakeAiText(output = { _, _ ->
            kotlinx.coroutines.flow.flow {
                gate.await()
                emit("late")
            }.onCompletion { stopped = it != null }
        })
        val s = summaries(ai)
        advanceUntilIdle()
        ai.engine = null
        advanceUntilIdle()
        assertNull(s.state.value)
        assertTrue(stopped)
    }

    @Test
    fun `an update during engine resolution still finishes`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        var calls = 0
        // The first call is the availability check; the second (the run's own) waits on the gate
        val ai = FakeAiText(engineRule = { _, _ -> if (++calls == 2) gate.await(); FakeAiText.ENGINE })
        val s = summaries(ai)
        advanceUntilIdle()
        items.value = items.value + noti("Older", 100) // same lines from the check, another emission
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AiTextState.Done("out", FakeAiText.ENGINE), s.state.value)
    }
}
