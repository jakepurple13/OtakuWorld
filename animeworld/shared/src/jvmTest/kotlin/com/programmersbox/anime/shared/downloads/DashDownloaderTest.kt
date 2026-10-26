package com.programmersbox.anime.shared.downloads

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashDownloaderTest {
    private lateinit var server: MockWebServer
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private lateinit var dir: File

    /** Requests for this path wait on [release] (cancellation test). */
    @Volatile
    private var blockedPath: String? = null
    private val blockedRequested = CountDownLatch(1)
    private val release = CountDownLatch(1)

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                if (request.headers["X-Test"] != "1") return MockResponse.Builder().code(403).build()
                val path = request.url.encodedPath
                if (path == blockedPath) {
                    blockedRequested.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
                val bytes = DashDownloaderTest::class.java.getResource("/fixtures/dash/${path.removePrefix("/dash/")}")
                    ?.readBytes() ?: return MockResponse.Builder().code(404).build()
                return MockResponse.Builder().body(Buffer().write(bytes)).build()
            }
        }
        server.start()
        dir = createTempDirectory("dash-download").toFile()
    }

    @AfterTest
    fun tearDown() {
        release.countDown()
        server.close()
        dir.deleteRecursively()
    }

    @Test
    fun downloadsWithHeadersAndProgress() = runBlocking {
        val out = File(dir, "Ep 1.mp4.part")
        val progress = mutableListOf<Float>()

        DashDownloader(OkHttpClient()).download(server.url("/dash/manifest.mpd").toString(), mapOf("X-Test" to "1"), out) {
            progress += it
        }

        assertEquals(1f, progress.last())
        assertEquals(progress.sorted(), progress, "progress went backwards: $progress")
        assertTrue(progress.any { it > 0f && it < 0.95f }, "no intermediate progress: $progress")
        // Manifest, two init segments and 3 s / 1 s = three media segments per track.
        assertEquals(9, requests.size)
        requests.forEach { assertEquals("1", it.headers["X-Test"], "request ${it.url} lacked X-Test") }

        val top = topLevelBoxes(out.readBytes())
        assertEquals("ftyp", top.first().first)
        assertEquals(6, top.count { it.first == "moof" })
        assertEquals(6, top.count { it.first == "mdat" })
        assertFalse(top.any { it.first == "sidx" || it.first == "styp" }, "segment index boxes copied: ${top.map { it.first }}")
        val moov = top.single { it.first == "moov" }.second
        assertEquals(2, childBoxes(moov).count { it.first == "trak" })
        assertEquals(listOf(out), dir.listFiles()!!.toList(), "intermediates left behind")
    }

    @Test
    fun cancelCleansIntermediates() = runBlocking {
        blockedPath = "/dash/chunk-1-00002.m4s"
        val out = File(dir, "Ep 1.mp4.part")

        val job = async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            DashDownloader(OkHttpClient()).download(server.url("/dash/manifest.mpd").toString(), mapOf("X-Test" to "1"), out) {}
        }
        assertTrue(blockedRequested.await(10, TimeUnit.SECONDS), "blocked segment never requested")
        assertTrue(dir.walk().any { it.isFile && it.length() > 0 }, "nothing was written before cancelling")

        withTimeout(10_000) { job.cancelAndJoin() }

        assertEquals(emptyList(), dir.listFiles()!!.toList())
    }

    /** (type, payload) for each box in [bytes]; 32-bit sizes only (the fixture is tiny). */
    private fun topLevelBoxes(bytes: ByteArray): List<Pair<String, ByteArray>> = childBoxes(bytes)

    private fun childBoxes(bytes: ByteArray): List<Pair<String, ByteArray>> {
        val buffer = ByteBuffer.wrap(bytes)
        val boxes = mutableListOf<Pair<String, ByteArray>>()
        while (buffer.remaining() >= 8) {
            val size = buffer.int
            val type = ByteArray(4).also(buffer::get).decodeToString()
            boxes += type to ByteArray(size - 8).also(buffer::get)
        }
        return boxes
    }
}
