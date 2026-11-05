package com.programmersbox.kmpuiviews.domain.updatecheck

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCheckReportTest {

    private fun fail(url: String, source: String = "S") = ItemFailure(
        url = url,
        title = url,
        imageUrl = "",
        source = source,
        kind = FailureKind.CouldNotRead,
        httpCode = null,
        detail = "",
        sourceFailed = false,
    )

    private fun sourceFail(source: String, kind: FailureKind = FailureKind.TimedOut) =
        SourceFailure(source, kind, null, "")

    private fun report(
        items: List<ItemFailure> = emptyList(),
        sources: List<SourceFailure> = emptyList(),
        updated: Int = 0,
        finishedAt: Long = 10L,
        stoppedEarly: String? = null,
    ) = UpdateCheckReport(
        startedAt = 1L,
        finishedAt = finishedAt,
        favoriteCount = 10,
        checkedCount = 8,
        updatedCount = updated,
        sourceFailures = sources,
        itemFailures = items,
        stoppedEarly = stoppedEarly,
    )

    @Test
    fun `timeout classifies as TimedOut`() = runTest {
        val e = runCatching { withTimeout(1) { delay(50) } }.exceptionOrNull()!!
        assertEquals(FailureKind.TimedOut, classifyFailure(e).kind)
    }

    @Test
    fun `http code is read from message chain`() {
        val info = classifyFailure(RuntimeException("wrapper", IOException("HTTP error 403")))
        assertEquals(FailureKind.Http, info.kind)
        assertEquals(403, info.httpCode)
    }

    @Test
    fun `other exceptions classify as CouldNotRead`() {
        val info = classifyFailure(IllegalStateException("no element"))
        assertEquals(FailureKind.CouldNotRead, info.kind)
        assertNull(info.httpCode)
        assertTrue(info.detail.startsWith("IllegalStateException: no element"))
    }

    @Test
    fun `hasProblems reflects failures and early stop`() {
        assertFalse(report().hasProblems)
        assertTrue(report(items = listOf(fail("a"))).hasProblems)
        assertTrue(report(sources = listOf(sourceFail("S"))).hasProblems)
        assertTrue(report(stoppedEarly = "x").hasProblems)
    }

    @Test
    fun `merge drops retried items that succeeded and keeps ones still failing`() {
        val prev = report(items = listOf(fail("a"), fail("b"), fail("c")), updated = 3)
        val retry = report(items = listOf(fail("b")), updated = 1, finishedAt = 99L)
        val merged = prev.mergeRetry(retry, retriedUrls = setOf("a", "b"), retriedSources = setOf("S"))
        assertEquals(setOf("b", "c"), merged.itemFailures.map { it.url }.toSet())
        assertEquals(2, merged.itemFailures.size)
        assertEquals(4, merged.updatedCount)
        assertEquals(prev.checkedCount, merged.checkedCount)
        assertEquals(prev.startedAt, merged.startedAt)
        assertEquals(retry.finishedAt, merged.retriedAt)
    }

    @Test
    fun `merge replaces source failures only for retried sources`() {
        val prev = report(sources = listOf(sourceFail("S"), sourceFail("T")))
        val retry = report(sources = listOf(sourceFail("S", FailureKind.Http)), stoppedEarly = "stop")
        val merged = prev.mergeRetry(retry, retriedUrls = emptySet(), retriedSources = setOf("S"))
        assertEquals(setOf("S", "T"), merged.sourceFailures.map { it.source }.toSet())
        assertEquals(FailureKind.Http, merged.sourceFailures.first { it.source == "S" }.kind)
        assertEquals(FailureKind.TimedOut, merged.sourceFailures.first { it.source == "T" }.kind)
        assertEquals("stop", merged.stoppedEarly)

        val cleared = prev.mergeRetry(report(), emptySet(), setOf("S"))
        assertEquals(listOf("T"), cleared.sourceFailures.map { it.source })
        assertNull(cleared.stoppedEarly)
    }
}
