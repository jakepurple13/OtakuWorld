package com.programmersbox.manga.shared.downloads

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DownloadCoreTest {

    private fun makeRequest(urls: List<String>) = DownloadRequest(
        chapterUrl = "https://example.com/chapter/1",
        chapterName = "Chapter 1",
        mangaTitle = "Test Manga",
        imageUrls = urls,
        headers = emptyMap(),
    )

    @Ignore
    @Test
    fun `happy path - writes bytes in index order and reports progress`() = runTest {
        val imageData = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
        val written = mutableListOf<Pair<Int, ByteArray>>()
        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        var callCount = 0

        val client = HttpClient(MockEngine {
            when (it.url.toString()) {
                "https://mock.local/img0" -> respond(imageData[callCount++], HttpStatusCode.OK)
                "https://mock.local/img1" -> respond(imageData[callCount++], HttpStatusCode.OK)
                else -> respond(imageData[callCount++], HttpStatusCode.OK)
            }
        })

        executeDownload(
            client = client,
            request = makeRequest(listOf("https://mock.local/img0", "https://mock.local/img1")),
            onProgress = { done, total -> progressUpdates.add(done to total) },
            writeBytes = { index, bytes -> written.add(index to bytes) },
        )

        assertEquals(2, written.size)
        assertEquals(0, written[0].first)
        assertEquals(1, written[1].first)
        assert(imageData[0].contentEquals(written[0].second))
        assert(imageData[1].contentEquals(written[1].second))
        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), progressUpdates)
    }

    @Ignore
    @Test
    fun `skips 404 images without writing bytes`() = runTest {
        val written = mutableListOf<Int>()
        var callCount = 0

        val client = HttpClient(MockEngine {
            val status = if (callCount++ == 1) HttpStatusCode.NotFound else HttpStatusCode.OK
            respond(byteArrayOf(callCount.toByte()), status)
        })

        executeDownload(
            client = client,
            request = makeRequest(listOf("https://mock.local/img0", "https://mock.local/img1", "https://mock.local/img2")),
            onProgress = { _, _ -> },
            writeBytes = { index, _ -> written.add(index) },
        )

        assertEquals(listOf(0, 2), written) // index 1 skipped (404)
    }

    @Ignore
    @Test
    fun `retries on 5xx and succeeds on third attempt`() = runTest {
        var attempt = 0
        val client = HttpClient(MockEngine {
            attempt++
            if (attempt < 3) respond(byteArrayOf(), HttpStatusCode.InternalServerError)
            else respond(byteArrayOf(42), HttpStatusCode.OK)
        })

        val written = mutableListOf<ByteArray>()

        executeDownload(
            client = client,
            request = makeRequest(listOf("https://mock.local/img0")),
            maxRetries = 3,
            onProgress = { _, _ -> },
            writeBytes = { _, bytes -> written.add(bytes) },
        )

        assertEquals(3, attempt)
        assertEquals(1, written.size)
        assert(byteArrayOf(42).contentEquals(written[0]))
    }

    @Ignore
    @Test
    fun `throws after exhausting retries`() = runTest {
        var callCount = 0
        val client = HttpClient(MockEngine {
            callCount++
            respond(byteArrayOf(), HttpStatusCode.InternalServerError)
        })

        assertFailsWith<Exception> {
            executeDownload(
                client = client,
                request = makeRequest(listOf("https://mock.local/img0")),
                maxRetries = 2,
                onProgress = { _, _ -> },
                writeBytes = { _, _ -> },
            )
        }

        assertEquals(3, callCount) // 1 initial attempt + 2 retries
    }

    @Test
    fun `sanitize replaces illegal filename characters`() {
        assertEquals("manga_title", "manga/title".sanitize())
        assertEquals("ch_1_2", "ch:1\\2".sanitize())
        assertEquals("no change", "no change".sanitize())
    }
}
