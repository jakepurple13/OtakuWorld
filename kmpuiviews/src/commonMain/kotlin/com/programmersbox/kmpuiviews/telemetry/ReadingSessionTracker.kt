package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.telemetry.Buckets
import com.programmersbox.telemetry.events.ReadingSession
import kotlin.time.Clock

/**
 * Counts what one reader session covered, for the `reading_session` event.
 * Readers call [pageShown] as pages come into view and send [event] when the reader closes.
 */
class ReadingSessionTracker(
    private val mediaType: String,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val startedAt = now()
    private val seen = mutableSetOf<Pair<Int, Int>>()
    private var reachedEnd = false

    fun pageShown(chapter: Int, page: Int, pagesInChapter: Int) {
        seen += chapter to page
        if (pagesInChapter > 0 && page >= pagesInChapter - 1) reachedEnd = true
    }

    /** For scrolling readers (novels): the user scrolled to the end of a chapter. */
    fun endReached() {
        reachedEnd = true
    }

    fun event(): ReadingSession = ReadingSession(
        mediaType = mediaType,
        pagesRead = Buckets.count(seen.size),
        duration = Buckets.durationMs(now() - startedAt),
        reachedEnd = reachedEnd,
    )
}
