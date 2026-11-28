package com.programmersbox.kmpuiviews.domain.exceptions

import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.asura403
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.backupFailure
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.comickJson
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.mangaDexTimeout
import com.programmersbox.kmpuiviews.domain.updatecheck.KnownError
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExceptionReportTest {
    private val ny = TimeZone.of("America/New_York")
    private val t = LocalDateTime(2026, 11, 27, 16, 52).toInstant(ny).toEpochMilliseconds()
    private val ctx = ReportContext("MangaWorld", "33.0.0", "Android 16 · Pixel 8", ny)

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }

    @Test
    fun reportForSourceError() {
        val r = buildExceptionReport(comickJson, parseException(comickJson), t, "Comick", "1.4.63", 1, ctx)
        assertTrue(
            r.startsWith(
                "### MangaWorld error report\n- App: MangaWorld 33.0.0\n- Device: Android 16 · Pixel 8\n" +
                    "- When: 2026-11-27 16:52 (America/New_York)\n- Source: Comick (all.comickfun v1.4.63)\n" +
                    "- Seen: 1× in the last 7 days\n\n```\n"
            ),
            r,
        )
        assertTrue(r.contains(comickJson))
        assertTrue(r.endsWith("\n```"))
    }

    @Test
    fun reportForAppErrorHasNoSourceLine() {
        assertFalse(buildExceptionReport(backupFailure, parseException(backupFailure), t, null, null, 2, ctx).contains("- Source:"))
    }

    @Test
    fun reportOmitsVersionWhenUnknown() {
        val r = buildExceptionReport(mangaDexTimeout, parseException(mangaDexTimeout), t, "en.mangadex", null, 1, ctx)
        assertTrue(r.contains("- Source: en.mangadex (en.mangadex)\n"), r)
    }

    @Test
    fun subjectAndFileName() {
        assertEquals("MangaWorld error: Asura Scans HttpStatusException", reportSubject("MangaWorld", "Asura Scans", parseException(asura403)))
        assertEquals("MangaWorld error: App FileNotFoundException", reportSubject("MangaWorld", null, parseException(backupFailure)))
        assertEquals("MangaWorld-error-2026-11-27-AsuraScans.md", reportFileName("MangaWorld", t, "Asura Scans", ny))
        assertEquals("MangaWorld-error-2026-11-27-App.md", reportFileName("MangaWorld", t, null, ny))
    }

    @Test
    fun knownErrorMapping() {
        assertEquals(KnownError.TimedOut, parseException(mangaDexTimeout).knownError())
        assertEquals(KnownError.Blocked, parseException(asura403).knownError())
        assertNull(parseException(comickJson).knownError())
    }

    @Test
    fun hourlyCountsBuckets() {
        val now = 10 * HOUR
        val c = hourlyCounts(listOf(now - 1, now - 1, now - HOUR - 1, now - 25 * HOUR), now)
        assertEquals(24, c.size)
        assertEquals(2, c[23])
        assertEquals(1, c[22])
        assertEquals(3, c.sum())
    }

    @Test
    fun hourlyCountsEmpty() {
        assertEquals(List(24) { 0 }, hourlyCounts(emptyList(), 1000L))
    }

    @Test
    fun seenCountsWeekWindow() {
        val now = 30 * DAY
        assertEquals(mapOf("k" to 2), seenCounts(listOf(now - DAY to "k", now - 2 * DAY to "k", now - 8 * DAY to "k"), now))
    }
}
