package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.telemetry.events.ReadingSession
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingSessionTrackerTest {
    private var now = 0L
    private fun tracker() = ReadingSessionTracker(mediaType = "manga", now = { now })

    @Test
    fun `counts distinct pages, time and reaching the end`() {
        val t = tracker()
        t.pageShown(chapter = 0, page = 0, pagesInChapter = 3)
        t.pageShown(chapter = 0, page = 1, pagesInChapter = 3)
        t.pageShown(chapter = 0, page = 1, pagesInChapter = 3)
        t.pageShown(chapter = 0, page = 2, pagesInChapter = 3)
        now = 45_000

        assertEquals(ReadingSession("manga", pagesRead = "1-5", duration = "30s-2m", reachedEnd = true), t.event())
    }

    @Test
    fun `closing at once is well formed`() {
        assertEquals(ReadingSession("manga", pagesRead = "0", duration = "<1s", reachedEnd = false), tracker().event())
    }

    @Test
    fun `pages from different chapters count separately`() {
        val t = tracker()
        repeat(4) { t.pageShown(chapter = 0, page = it, pagesInChapter = 10) }
        repeat(4) { t.pageShown(chapter = 1, page = it, pagesInChapter = 10) }

        assertEquals("6-20", t.event().pagesRead)
        assertEquals(false, t.event().reachedEnd)
    }
}
