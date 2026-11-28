package com.programmersbox.kmpuiviews.domain.exceptions

import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.asura403
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.backupFailure
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.comickJson
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.mangaDexTimeout
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.unknownHost
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures.weebCentral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParsedExceptionTest {

    @Test
    fun timeoutIsNetwork() {
        val p = parseException(mangaDexTimeout)
        assertEquals(ExceptionHeadline.TimedOut, p.headline)
        assertEquals(ExceptionKind.Network, p.kind)
        assertEquals("en.mangadex", p.extension?.id)
        assertEquals("SocketTimeoutException", p.shortType)
        assertEquals("java.net.SocketTimeoutException", p.type)
        assertEquals("timeout", p.message)
    }

    @Test
    fun cloudflare403IsBlocked() {
        val p = parseException(asura403)
        assertEquals(403, p.httpCode)
        assertEquals(ExceptionHeadline.Blocked, p.headline)
        assertEquals(ExceptionKind.Blocked, p.kind)
    }

    @Test
    fun htmlInsteadOfJsonIsParsing() {
        val p = parseException(comickJson)
        assertEquals(ExceptionKind.Parsing, p.kind)
        assertEquals("eu.kanade.tachiyomi.extension.all.comickfun", p.extension?.packageName)
        assertEquals("all.comickfun", p.extension?.id)
    }

    @Test
    fun messageKeepsContinuationLines() {
        val p = parseException(comickJson)
        assertTrue(p.message.contains("JSON input: <!DOCTYPE html>"))
        assertTrue(p.firstLine.startsWith("Unexpected JSON token"))
        assertFalse(p.firstLine.contains("JSON input"))
    }

    @Test
    fun emptyChapterListIsSource() {
        val p = parseException(weebCentral)
        assertEquals(ExceptionKind.Source, p.kind)
        assertEquals(ExceptionHeadline.Source, p.headline)
        assertEquals(listOf("Caused by: java.util.NoSuchElementException: Collection is empty."), p.causes)
    }

    @Test
    fun unknownHostIsOffline() {
        val p = parseException(unknownHost)
        assertEquals(ExceptionHeadline.Offline, p.headline)
        assertEquals(ExceptionKind.Network, p.kind)
    }

    @Test
    fun noExtensionFrameIsApp() {
        val p = parseException(backupFailure)
        assertEquals(ExceptionKind.App, p.kind)
        assertEquals(ExceptionHeadline.App, p.headline)
        assertNull(p.extension)
    }

    @Test
    fun wrappedTimeoutUsesCause() {
        val p = parseException("java.lang.Exception: load failed\n\tat a.b(C.kt:1)\nCaused by: java.net.SocketTimeoutException: timeout")
        assertEquals(ExceptionHeadline.TimedOut, p.headline)
    }

    @Test
    fun libraryFramesFlagged() {
        val f = parseException(mangaDexTimeout).frames
        assertEquals(5, f.size)
        assertFalse(f.first().isLibrary)
        assertTrue(f.first().text.startsWith("at eu.kanade"))
        assertTrue(f.last().isLibrary)
    }

    @Test
    fun ellipsisLinesAreNotFrames() {
        assertTrue(parseException(weebCentral).frames.none { it.text.startsWith("...") })
    }

    @Test
    fun groupKeyStable() {
        assertEquals(parseException(mangaDexTimeout).groupKey, parseException(mangaDexTimeout).groupKey)
        assertNotEquals(parseException(mangaDexTimeout).groupKey, parseException(unknownHost).groupKey)
        assertEquals("java.net.SocketTimeoutException|timeout|en.mangadex", parseException(mangaDexTimeout).groupKey)
    }

    @Test
    fun singleLineNoFrames() {
        val p = parseException("boom")
        assertEquals("boom", p.type)
        assertEquals("", p.message)
        assertEquals(ExceptionKind.App, p.kind)
    }

    @Test
    fun otherHttpCodeIsNetwork() {
        val p = parseException("java.io.IOException: HTTP 418\n\tat eu.kanade.tachiyomi.extension.en.foo.Foo.a(Foo.kt:1)")
        assertEquals(418, p.httpCode)
        assertEquals(ExceptionHeadline.Network, p.headline)
    }

    @Test
    fun notFoundAndRateLimited() {
        assertEquals(ExceptionHeadline.NotFound, parseException("java.io.IOException: HTTP error 404").headline)
        assertEquals(ExceptionHeadline.RateLimited, parseException("java.io.IOException: HTTP 429").headline)
        assertEquals(ExceptionHeadline.ServerError, parseException("java.io.IOException: HTTP 502").headline)
    }

    @Test
    fun animeExtensionDetected() {
        val p = parseException("java.lang.IllegalStateException: x\n\tat eu.kanade.tachiyomi.animeextension.en.allanime.AllAnime.a(AllAnime.kt:1)")
        assertEquals("eu.kanade.tachiyomi.animeextension.en.allanime", p.extension?.packageName)
    }
}
