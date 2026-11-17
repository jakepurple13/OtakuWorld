package com.programmersbox.kmpuiviews.domain.updatecheck

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnownErrorTest {
    @Test
    fun `timeout is known`() {
        assertEquals(KnownError.TimedOut, knownError(FailureKind.TimedOut, null, ""))
    }

    @Test
    fun `http codes map`() {
        val expected = mapOf(
            429 to KnownError.RateLimited,
            403 to KnownError.Blocked,
            503 to KnownError.Blocked,
            404 to KnownError.NotFound,
            410 to KnownError.NotFound,
            500 to KnownError.ServerError,
            502 to KnownError.ServerError,
            401 to null,
        )
        expected.forEach { (code, known) -> assertEquals(known, knownError(FailureKind.Http, code, ""), "HTTP $code") }
        assertNull(knownError(FailureKind.Http, null, ""))
    }

    @Test
    fun `offline signatures map`() {
        listOf(
            "UnknownHostException: Unable to resolve host \"mangadex.org\"",
            "IOException: Unable to resolve host",
            "ConnectException: Failed to connect",
            "NoRouteToHostException: No route to host",
            "UnresolvedAddressException: null",
            "SocketException: Network is unreachable",
        ).forEach { assertEquals(KnownError.Offline, knownError(FailureKind.CouldNotRead, null, "$it\nat a.b.C"), it) }
    }

    @Test
    fun `other unreadable errors are unknown`() {
        assertNull(knownError(FailureKind.CouldNotRead, null, "SerializationException: Unexpected JSON token"))
        assertNull(knownError(FailureKind.CouldNotRead, null, "IllegalStateException: x\nUnknownHostException: later line"))
        assertNull(knownError(FailureKind.CouldNotRead, null, ""))
    }

    @Test
    fun `every known error has text naming the source`() = runTest {
        KnownError.entries.forEach {
            val text = it.explanation("MangaDex")
            assertTrue("MangaDex" in text, "$it: $text")
            assertTrue('\\' !in text, "$it has a stray escape: $text")
        }
    }
}
