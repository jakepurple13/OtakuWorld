package com.programmersbox.anime.shared.videoplayer

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalStreamProxyTest {
    private val client = OkHttpClient()
    private val routes = ConcurrentHashMap<String, (RecordedRequest) -> MockResponse>()
    private val seen = CopyOnWriteArrayList<RecordedRequest>()
    private lateinit var upstream: MockWebServer
    private lateinit var proxy: LocalStreamProxy
    private val headers = mapOf("X-Test" to "1", "Referer" to "https://r.example")

    @BeforeTest
    fun setUp() {
        upstream = MockWebServer().also {
            it.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    seen += request
                    return routes[request.url.encodedPath]?.invoke(request) ?: MockResponse.Builder().code(404).build()
                }
            }
            it.start()
        }
        proxy = LocalStreamProxy(client).also { it.start() }
    }

    @AfterTest
    fun tearDown() {
        proxy.close()
        upstream.close()
    }

    private fun route(path: String, type: String? = null, code: Int = 200, extra: Map<String, String> = emptyMap(), body: String) {
        routes[path] = {
            MockResponse.Builder().code(code).body(body).apply {
                type?.let { t -> addHeader("Content-Type", t) }
                extra.forEach { (k, v) -> addHeader(k, v) }
            }.build()
        }
    }

    private fun get(url: String, hdrs: Map<String, String> = emptyMap()) =
        client.newCall(Request.Builder().url(url).apply { hdrs.forEach { (k, v) -> header(k, v) } }.build()).execute()

    private fun proxyBase(url: String) = "http://127.0.0.1:" + url.substringAfter("127.0.0.1:").substringBefore('/') + "/"

    @Test
    fun injectsHeadersOnEveryHop() {
        route("/hls/master.m3u8", "application/vnd.apple.mpegurl", body = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nv/index.m3u8\n")
        route("/hls/v/index.m3u8", body = "#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"k.key\"\n#EXTINF:1,\nseg0.ts\n")
        route("/hls/v/k.key", body = "key")
        route("/hls/v/seg0.ts", body = "seg")

        val master = proxy.register(upstream.url("/hls/master.m3u8").toString(), headers, "application/x-mpegURL")
        val masterBody = get(master).use { it.body.string() }
        val variant = masterBody.lines().first { it.startsWith("http") }
        val variantBody = get(variant).use { it.body.string() }
        val key = Regex("URI=\"([^\"]+)\"").find(variantBody)!!.groupValues[1]
        val seg = variantBody.lines().first { it.startsWith("http") }
        assertEquals("key", get(key).use { it.body.string() })
        assertEquals("seg", get(seg).use { it.body.string() })

        assertEquals(4, seen.size)
        seen.forEach {
            assertEquals("1", it.headers["X-Test"], it.url.toString())
            assertEquals("https://r.example", it.headers["Referer"], it.url.toString())
        }
    }

    @Test
    fun rewritesKeyMapAndMediaUris() {
        route(
            "/p/list.m3u8", "application/vnd.apple.mpegurl",
            body = """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="k.key"
                #EXT-X-MAP:URI="init.mp4"
                #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="s",NAME="en",URI="sub.m3u8"
                #EXTINF:1,
                a.ts
                #EXTINF:1,
                ${upstream.url("/other/b.ts")}
            """.trimIndent(),
        )
        val url = proxy.register(upstream.url("/p/list.m3u8").toString(), headers, null)
        val out = get(url).use { it.body.string() }

        val uris = Regex("URI=\"([^\"]+)\"").findAll(out).map { it.groupValues[1] }.toList() +
            out.lines().filter { !it.startsWith("#") && it.isNotBlank() }
        assertEquals(5, uris.size)
        uris.forEach { assertTrue(it.startsWith(proxyBase(url)), it) }
        assertFalse(out.contains("${upstream.port}/"), out)
    }

    @Test
    fun manifestRangeRequestGetsFullRewrittenBody() {
        route("/m/x.m3u8", "application/vnd.apple.mpegurl", code = 206, body = "#EXTM3U\ns.ts\n")
        val url = proxy.register(upstream.url("/m/x.m3u8").toString(), headers, null)

        get(url, mapOf("Range" to "bytes=0-")).use {
            assertEquals(200, it.code)
            assertTrue(it.body.string().contains("127.0.0.1"))
        }
        assertNull(seen.single().headers["Range"])
    }

    @Test
    fun segmentRangePassesThrough() {
        route("/s/seg.ts", code = 206, extra = mapOf("Content-Range" to "bytes 10-19/100"), body = "0123456789")
        val url = proxy.register(upstream.url("/s/seg.ts").toString(), headers, null)

        get(url, mapOf("Range" to "bytes=10-19")).use {
            assertEquals(206, it.code)
            assertEquals("bytes 10-19/100", it.header("Content-Range"))
            assertEquals("0123456789", it.body.string())
        }
        assertEquals("bytes=10-19", seen.single().headers["Range"])
    }

    @Test
    fun dashRelativeSegmentsResolveThroughProxy() {
        route(
            "/dir/manifest.mpd", "application/dash+xml",
            body = """<MPD xmlns="urn:mpeg:dash:schema:mpd:2011"><Period><AdaptationSet><SegmentTemplate media="seg-${'$'}Number${'$'}.m4s" initialization="init.mp4"/></AdaptationSet></Period></MPD>""",
        )
        route("/dir/seg-1.m4s", body = "dash-seg")
        val url = proxy.register(upstream.url("/dir/manifest.mpd").toString(), headers, "application/dash+xml")

        val manifest = get(url).use { it.body.string() }
        assertTrue(manifest.contains("127.0.0.1"), manifest)
        assertEquals("dash-seg", get(url.substringBeforeLast('/') + "/seg-1.m4s").use { it.body.string() })

        val segReq = seen.last()
        assertEquals("/dir/seg-1.m4s", segReq.url.encodedPath)
        assertEquals("1", segReq.headers["X-Test"])
    }

    @Test
    fun rejectsWrongToken() {
        route("/v.mp4", body = "x")
        val url = proxy.register(upstream.url("/v.mp4").toString(), headers, null)
        val bad = url.replace(Regex("(127\\.0\\.0\\.1:\\d+/)[^/]+"), "$1wrong")

        get(bad).use { assertEquals(404, it.code) }
        assertTrue(seen.isEmpty())
    }

    @Test
    fun bindsLoopbackOnly() {
        route("/v.mp4", body = "x")
        val url = proxy.register(upstream.url("/v.mp4").toString(), headers, null)
        assertTrue(url.startsWith("http://127.0.0.1:"))
        val port = url.substringAfter("127.0.0.1:").substringBefore('/').toInt()
        val external = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
        if (external != null) {
            val reached = runCatching { Socket().use { it.connect(InetSocketAddress(external, port), 500) } }.isSuccess
            assertFalse(reached, "proxy reachable on ${external.hostAddress}")
        }
    }

    @Test
    fun upstreamErrorStatusPropagates() {
        route("/denied.mp4", code = 403, body = "no")
        val url = proxy.register(upstream.url("/denied.mp4").toString(), headers, null)
        get(url).use { assertEquals(403, it.code) }
    }
}
