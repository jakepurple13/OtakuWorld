package com.programmersbox.kmpuiviews.domain.updatecheck

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.testing.FakeApiService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCheckTextTest {
    private val service = FakeApiService()

    private fun chapter(name: String, time: Long? = null) = KmpChapterModel(
        name = name, url = name, uploaded = "", sourceUrl = "", source = service,
    ).also { it.uploadedTime = time }

    private fun info(chapters: List<KmpChapterModel>) = KmpInfoModel(
        title = "t", description = "", url = "u", imageUrl = "", chapters = chapters,
        genres = emptyList(), alternativeNames = emptyList(), source = service,
    )

    private fun source(
        name: String,
        state: SourceState,
        favorites: Int = 1,
        toCheck: Int = favorites,
        checked: Int = 0,
        updates: Int = 0,
        failure: FailureInfo? = null,
    ) = SourceProgress(name, state, favorites, toCheck, checked, updates, failure)

    private fun progress(
        sources: List<SourceProgress> = emptyList(),
        updatesFound: Int = 0,
        itemFailures: Int = 0,
    ) = UpdateCheckProgress(
        favoriteCount = 42, loadingRecents = false, total = 10, checked = 3, current = "X",
        sources = sources, updatesFound = updatesFound, itemFailures = itemFailures,
    )

    private fun report(updated: Int = 0, stopped: String? = null, items: Int = 0, sources: Int = 0) = UpdateCheckReport(
        startedAt = 0, finishedAt = 0, favoriteCount = 42, checkedCount = 42, updatedCount = updated,
        sourceFailures = List(sources) { SourceFailure("S$it", FailureKind.TimedOut, null, "") },
        itemFailures = List(items) {
            ItemFailure("u$it", "t", "", "S", FailureKind.CouldNotRead, null, "", false)
        },
        stoppedEarly = stopped,
    )

    @Test
    fun `newChapters picks newest by uploadedTime when list is oldest first`() {
        val list = (1..5).map { chapter("C$it", it.toLong()) }
        assertEquals(listOf("C5", "C4"), newChapters(info(list), 3).map { it.name })
    }

    @Test
    fun `newChapters takes head of list when uploadedTime missing`() {
        val list = (5 downTo 1).map { chapter("C$it") }
        assertEquals(listOf("C5", "C4"), newChapters(info(list), 3).map { it.name })
    }

    @Test
    fun `newChapters is empty when count did not grow`() {
        val list = (1..3).map { chapter("C$it") }
        assertEquals(emptyList(), newChapters(info(list), 3))
        assertEquals(emptyList(), newChapters(info(list), 5))
    }

    @Test
    fun `inboxLines caps at 5 and adds more line`() {
        val lines = inboxLines((1..300).map { chapter("C$it") })
        assertEquals(6, lines.size)
        assertEquals("+295 more", lines.last())
    }

    @Test
    fun `chapterRange names one or oldest to newest`() {
        assertEquals("C1", chapterRange(listOf(chapter("C1"))))
        assertEquals("C1 – C3", chapterRange(listOf(chapter("C3"), chapter("C2"), chapter("C1"))))
    }

    @Test
    fun `updateTitle uses singular and episodes`() = runTest {
        assertEquals("Frieren · 3 new chapters", updateTitle("Frieren", 3, UpdateUnit.Chapters))
        assertEquals("Frieren · 1 new chapter", updateTitle("Frieren", 1, UpdateUnit.Chapters))
        assertEquals("Show · 2 new episodes", updateTitle("Show", 2, UpdateUnit.Episodes))
    }

    @Test
    fun `headline and fallbackTitle`() = runTest {
        val p = progress(listOf(source("A", SourceState.Done), source("B", SourceState.Done)))
        assertEquals("Checking 2 sources · 42 favorites", p.headline())
        assertEquals("Checking X · 3 of 10", p.fallbackTitle())
        assertEquals("Checking 2 sources · 42 favorites", p.copy(loadingRecents = true).fallbackTitle())
    }

    @Test
    fun `summary joins parts`() = runTest {
        val p = progress(
            sources = listOf(source("Comick", SourceState.Failed, failure = FailureInfo(FailureKind.TimedOut, null, ""))),
            updatesFound = 3,
        )
        assertEquals("3 updates · Comick failed", p.summary())
        assertEquals("", progress().summary())
        assertEquals("1 update · 1 failed", progress(updatesFound = 1, itemFailures = 1).summary())
    }

    @Test
    fun `sourceLines`() = runTest {
        val p = progress(
            listOf(
                source("MangaDex", SourceState.Done, updates = 1),
                source("Quiet", SourceState.Done),
                source("Comick", SourceState.Failed, failure = FailureInfo(FailureKind.Http, 403, "")),
                source("MangaPark", SourceState.Pending),
            )
        )
        assertEquals(
            listOf("✓ MangaDex · 1 update", "✓ Quiet · no updates", "✗ Comick · HTTP 403 · likely Cloudflare", "… MangaPark"),
            p.sourceLines(),
        )
    }

    @Test
    fun `failureMessage variants`() = runTest {
        assertEquals("No response after 15s", failureMessage(FailureKind.TimedOut, null))
        assertEquals("HTTP 404", failureMessage(FailureKind.Http, 404))
        assertEquals("HTTP 503 · likely Cloudflare", failureMessage(FailureKind.Http, 503))
        assertEquals("Couldn't read the page", failureMessage(FailureKind.CouldNotRead, null))
    }

    @Test
    fun `finishedTitle and finishedText`() = runTest {
        assertEquals("3 updates from 42 favorites", report(updated = 3).finishedTitle())
        assertEquals("1 update from 42 favorites", report(updated = 1).finishedTitle())
        assertEquals("No updates from 42 favorites", report().finishedTitle())
        assertEquals("3 couldn't be checked. Tap to see why.", report(items = 3).finishedText())
        assertEquals("1 couldn't be checked. Tap to see why.", report(items = 1).finishedText())
        assertEquals("Check stopped early. Tap to see why.", report(stopped = "x").finishedText())
        assertNull(report().finishedText())
    }

    @Test
    fun `segmentLayout`() {
        val p = progress(
            listOf(
                source("A", SourceState.Done, favorites = 10, updates = 1),
                source("B", SourceState.Failed, favorites = 4),
                source("C", SourceState.Running, favorites = 6, toCheck = 6, checked = 3),
                source("D", SourceState.Pending, favorites = 2),
            )
        )
        val layout = p.segmentLayout()
        assertEquals(listOf(10 to false, 4 to true, 6 to false, 2 to false), layout.segments)
        assertEquals(17, layout.progress)
        assertEquals(listOf(10), layout.points)
    }
}
