package com.programmersbox.anime.shared.cast

import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CastMediaServerTest {

    private val client = OkHttpClient()
    private lateinit var upstream: MockWebServer
    private lateinit var server: CastMediaServer
    private lateinit var folder: File

    @BeforeTest
    fun setUp() {
        upstream = MockWebServer().also { it.start() }
        server = CastMediaServer(client, { "127.0.0.1" }, port = 0, userAgent = "AnimeWorld/test")
        assertEquals(CastResult.Started, server.start())
        folder = createTempDirectory("cast-server").toFile()
    }

    @AfterTest
    fun tearDown() {
        server.stop()
        upstream.close()
        folder.deleteRecursively()
    }

    private fun get(url: String, headers: Map<String, String> = emptyMap()) =
        client.newCall(Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()).execute()

    @Test
    fun subtitleServedAsVttWithCors() = runTest {
        upstream.enqueue(MockResponse.Builder().body("1\n00:00:01,000 --> 00:00:02,000\nHello\n").build())

        val reg = assertNotNull(server.registerSubtitle(KmpSubtitle(upstream.url("/a.srt").toString(), "en")))

        assertEquals(listOf("Hello"), reg.cues.map { it.text })
        get(reg.url).use {
            assertEquals(200, it.code)
            assertEquals("*", it.header("Access-Control-Allow-Origin"))
            assertTrue(it.header("Content-Type").orEmpty().startsWith("text/vtt"))
            assertTrue(it.body.string().startsWith("WEBVTT"))
        }
    }

    @Test
    fun subtitleFetchUsesSubtitleHeaders() = runTest {
        upstream.enqueue(MockResponse.Builder().body("WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nHi\n").build())

        server.registerSubtitle(KmpSubtitle(upstream.url("/a.vtt").toString(), "en", mapOf("Referer" to "https://r.example")))

        assertEquals("https://r.example", upstream.takeRequest().headers["Referer"])
    }

    @Test
    fun failedSubtitleReturnsNull() = runTest {
        upstream.enqueue(MockResponse.Builder().code(404).build())

        assertNull(server.registerSubtitle(KmpSubtitle(upstream.url("/a.vtt").toString(), "en")))
    }

    @Test
    fun proxyForwardsHeadersUpstream() {
        upstream.enqueue(MockResponse.Builder().body("video-bytes").build())

        val proxied = server.registerStream(upstream.url("/v.mp4").toString(), mapOf("Referer" to "https://r.example"))

        assertFalse(proxied.contains("r.example"))
        get(proxied).use {
            assertEquals("video-bytes", it.body.string())
            assertEquals("*", it.header("Access-Control-Allow-Origin"))
        }
        assertEquals("https://r.example", upstream.takeRequest().headers["Referer"])
    }

    @Test
    fun proxyRewritesHlsChildrenThroughProxy() {
        upstream.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                .body("#EXTM3U\n#EXTINF:4,\nseg1.ts\n")
                .build()
        )
        upstream.enqueue(MockResponse.Builder().body("segment").build())

        val proxied = server.registerStream(upstream.url("/dir/index.m3u8").toString(), mapOf("Referer" to "https://r.example"))
        val playlist = get(proxied).use { it.body.string() }

        val child = playlist.lines().first { it.startsWith("http") }
        assertTrue(child.startsWith(server.baseUrl!! + "/p/"))
        assertEquals("segment", get(child).use { it.body.string() })
        upstream.takeRequest()
        val childRequest = upstream.takeRequest()
        assertEquals("/dir/seg1.ts", childRequest.url.encodedPath)
        assertEquals("https://r.example", childRequest.headers["Referer"])
    }

    @Test
    fun fileServedWithRange() {
        val file = File(folder, "ep.mp4").also { it.writeText("0123456789") }
        val url = server.registerFile(file)

        get(url, mapOf("Range" to "bytes=2-5")).use {
            assertEquals(206, it.code)
            assertEquals("bytes 2-5/10", it.header("Content-Range"))
            assertEquals("2345", it.body.string())
        }
        get(url).use { assertEquals("0123456789", it.body.string()) }
    }

    @Test
    fun startFailsWithoutWifiHost() {
        val noWifi = CastMediaServer(client, { null }, port = 0)

        assertEquals(CastResult.NoWifi, noWifi.start())
        assertNull(noWifi.baseUrl)
    }

    @Test
    fun hlsWithCharsetContentTypeIsRewritten() {
        upstream.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/vnd.apple.mpegurl; charset=UTF-8")
                .body("#EXTM3U\n#EXTINF:4,\nseg1.ts\n")
                .build()
        )

        val playlist = get(server.registerStream(upstream.url("/playlist").toString(), emptyMap())).use { it.body.string() }

        assertTrue(playlist.lines().any { it.startsWith(server.baseUrl!! + "/p/") })
    }

    @Test
    fun startFailsWhenPortBusy() {
        val busyPort = server.baseUrl!!.substringAfterLast(':').toInt()
        val second = CastMediaServer(client, { "127.0.0.1" }, port = busyPort)

        assertEquals(CastResult.Failed("Couldn't start local server"), second.start())
        assertNull(second.baseUrl)
    }

    @Test
    fun downloadedFileSubtitleIsServed() = runTest {
        val srt = File(folder, "Ep 1.en.srt").also { it.writeText("1\n00:00:01,000 --> 00:00:02,000\nOffline\n") }

        val reg = assertNotNull(server.registerSubtitle(KmpSubtitle(srt.toURI().toString(), "English")))

        assertEquals(listOf("Offline"), reg.cues.map { it.text })
        get(reg.url).use { assertTrue(it.body.string().startsWith("WEBVTT")) }
    }

    @Test
    fun optionsPreflightAnswered() {
        val request = Request.Builder().url(server.baseUrl!! + "/p/anything").method("OPTIONS", null).build()

        client.newCall(request).execute().use {
            assertEquals(204, it.code)
            assertEquals("*", it.header("Access-Control-Allow-Origin"))
            assertEquals("GET, HEAD, OPTIONS", it.header("Access-Control-Allow-Methods"))
            assertEquals("Range, Content-Type", it.header("Access-Control-Allow-Headers"))
        }
    }

    @Test
    fun proxySendsPlayerUserAgentUnlessSourceSetsOne() {
        upstream.enqueue(MockResponse.Builder().body("a").build())
        upstream.enqueue(MockResponse.Builder().body("b").build())

        get(server.registerStream(upstream.url("/a.mp4").toString(), mapOf("Referer" to "r"))).use { it.body.string() }
        get(server.registerStream(upstream.url("/b.mp4").toString(), mapOf("user-agent" to "Source/1"))).use { it.body.string() }

        assertEquals("AnimeWorld/test", upstream.takeRequest().headers["User-Agent"])
        assertEquals("Source/1", upstream.takeRequest().headers["User-Agent"])
    }

    @Test
    fun webmFileServedAsWebm() {
        val url = server.registerFile(File(folder, "ep.webm").also { it.writeText("webm") })

        get(url).use { assertEquals("video/webm", it.header("Content-Type")) }
    }

    @Test
    fun refetchingPlaylistReusesChildUrls() {
        repeat(2) {
            upstream.enqueue(
                MockResponse.Builder()
                    .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                    .body("#EXTM3U\n#EXTINF:4,\nseg1.ts\n")
                    .build()
            )
        }
        val proxied = server.registerStream(upstream.url("/index.m3u8").toString(), mapOf("Referer" to "r"))

        val first = get(proxied).use { it.body.string() }
        val second = get(proxied).use { it.body.string() }

        assertEquals(first, second)
    }
}
