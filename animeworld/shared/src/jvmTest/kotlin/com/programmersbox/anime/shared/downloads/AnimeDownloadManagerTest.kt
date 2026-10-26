package com.programmersbox.anime.shared.downloads

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.cast.toWebVtt
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.programmersbox.anime.shared.cast.SubtitleFormat as VttSource

class AnimeDownloadManagerTest {
    private lateinit var server: MockWebServer
    private val routes = ConcurrentHashMap<String, (RecordedRequest) -> MockResponse>()
    private val recorded = CopyOnWriteArrayList<RecordedRequest>()
    private lateinit var root: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var manager: AnimeDownloadManager

    private val showDir get() = File(root, "Show")

    @BeforeTest
    fun setUp() = runBlocking {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                recorded += request
                return routes[request.url.encodedPath]?.invoke(request) ?: MockResponse.Builder().code(404).build()
            }
        }
        server.start()
        root = createTempDirectory("anime-downloads").toFile()
        settings.downloadsDirectory.set(root.absolutePath)
        manager = AnimeDownloadManager(scope, settings, trayState = null, client = OkHttpClient())
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        server.close()
        root.deleteRecursively()
    }

    private fun route(path: String, body: ByteArray, contentType: String? = null, throttle: Boolean = false) {
        routes[path] = {
            MockResponse.Builder()
                .body(Buffer().write(body))
                .apply {
                    if (contentType != null) setHeader("Content-Type", contentType)
                    if (throttle) throttleBody(16 * 1024, 50, TimeUnit.MILLISECONDS)
                }
                .build()
        }
    }

    private fun route(path: String, body: String, contentType: String? = null) = route(path, body.encodeToByteArray(), contentType)

    private fun url(path: String) = server.url(path).toString()

    private fun storage(path: String, mimeType: String? = null, quality: String? = null, subtitles: List<KmpSubtitle> = emptyList()) =
        KmpStorage(link = url(path), mimeType = mimeType, quality = quality, subtitles = subtitles).apply {
            headers["X-Token"] = "abc"
        }

    private suspend fun awaitQueue(predicate: (List<QueuedDownload>) -> Boolean): List<QueuedDownload> =
        withTimeout(10_000) { manager.queue.first(predicate) }

    private suspend fun eventually(condition: () -> Boolean) = withTimeout(10_000) {
        while (!condition()) delay(20)
    }

    private fun encrypt(plain: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding")
            .apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) }
            .doFinal(plain)

    @Test
    fun progressiveDownloadSendsHeadersAndReportsProgress() = runBlocking {
        val video = ByteArray(200 * 1024) { it.toByte() }
        route("/ep.mp4", video, "video/mp4", throttle = true)

        val key = manager.download(storage("/ep.mp4"), referer = "http://ref.example/", animeTitle = "Show", episodeName = "Ep 1")

        val running = awaitQueue { q -> q.any { it.key == key && it.state == QueuedDownloadState.Running && (it.progress ?: 0f) > 0f } }
            .single()
        assertEquals(video.size.toLong(), running.totalBytes)
        awaitQueue { it.isEmpty() }

        assertContentEquals(video, File(showDir, "Ep 1.mp4").readBytes())
        assertFalse(File(showDir, "Ep 1.mp4.part").exists())
        val request = recorded.single { it.url.encodedPath == "/ep.mp4" }
        assertEquals("abc", request.headers["X-Token"])
        assertEquals("http://ref.example/", request.headers["referer"])
    }

    @Test
    fun hlsAes128DownloadDecryptsSegments() = runBlocking {
        val aesKey = ByteArray(16) { (it * 7).toByte() }
        val iv = ByteArray(16) { it.toByte() }
        val plain = listOf("segment-zero", "segment-one!", "segment-two!!").map { it.encodeToByteArray() }
        route(
            "/hls/master.m3u8",
            """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
            360/media.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720
            720/media.m3u8
            """.trimIndent(),
        )
        route(
            "/hls/720/media.m3u8",
            buildString {
                appendLine("#EXTM3U")
                appendLine("#EXT-X-TARGETDURATION:4")
                appendLine("#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\",IV=0x000102030405060708090a0b0c0d0e0f")
                plain.indices.forEach { appendLine("#EXTINF:4,"); appendLine("s$it.ts") }
                appendLine("#EXT-X-ENDLIST")
            },
        )
        routes["/hls/720/key.bin"] = { request ->
            if (request.headers["X-Token"] == "abc") MockResponse.Builder().body(Buffer().write(aesKey)).build()
            else MockResponse.Builder().code(403).build()
        }
        plain.forEachIndexed { i, bytes -> route("/hls/720/s$i.ts", encrypt(bytes, aesKey, iv)) }

        manager.download(storage("/hls/master.m3u8", "application/x-mpegURL", quality = "720p"), null, "Show", "Ep 1")
        awaitQueue { it.isEmpty() }

        assertContentEquals(plain.reduce(ByteArray::plus), File(showDir, "Ep 1.ts").readBytes())
        assertEquals(1, recorded.count { it.url.encodedPath == "/hls/720/key.bin" }, "key fetched once")
        assertTrue(recorded.none { it.url.encodedPath.startsWith("/hls/360/") })
    }

    @Test
    fun hlsFmp4WritesInitSegmentFirst() = runBlocking {
        route(
            "/fmp4/media.m3u8",
            """
            #EXTM3U
            #EXT-X-TARGETDURATION:4
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4,
            s0.m4s
            #EXTINF:4,
            s1.m4s
            #EXT-X-ENDLIST
            """.trimIndent(),
        )
        route("/fmp4/init.mp4", "INIT")
        route("/fmp4/s0.m4s", "-zero")
        route("/fmp4/s1.m4s", "-one")

        manager.download(storage("/fmp4/media.m3u8"), null, "Show", "Ep 1")
        awaitQueue { it.isEmpty() }

        assertEquals("INIT-zero-one", File(showDir, "Ep 1.mp4").readText())
    }

    @Test
    fun subtitleSidecarsWritten() = runBlocking {
        val vtt = "WEBVTT\n\n00:00.000 --> 00:01.000\nHi\n"
        val srt = "1\n00:00:00,000 --> 00:00:01,000\nHola\n"
        route("/ep.mp4", ByteArray(100), "video/mp4")
        route("/subs/en.vtt", vtt)
        route("/subs/es.srt", srt)
        val subtitles = listOf(KmpSubtitle(url("/subs/en.vtt"), "English"), KmpSubtitle(url("/subs/es.srt"), "Spanish"))

        manager.download(storage("/ep.mp4", subtitles = subtitles), null, "Show", "Ep 1")
        awaitQueue { it.isEmpty() }

        assertEquals(vtt, File(showDir, "Ep 1.0.English.vtt").readText())
        assertEquals(toWebVtt(srt, VttSource.Srt), File(showDir, "Ep 1.1.Spanish.vtt").readText())
        val entries = Json.decodeFromString<List<SidecarEntry>>(File(showDir, sidecarIndexName("Ep 1")).readText())
        assertEquals(
            listOf(
                SidecarEntry("Ep 1.0.English.vtt", "English", "English"),
                SidecarEntry("Ep 1.1.Spanish.vtt", "Spanish", "Spanish"),
            ),
            entries,
        )
        assertEquals(listOf("English", "Spanish"), OfflineSubtitles.find(File(showDir, "Ep 1.mp4")).map { it.lang })
        assertTrue(recorded.filter { it.url.encodedPath.startsWith("/subs/") }.all { it.headers["X-Token"] == "abc" })
    }

    @Test
    fun failureThenRetrySucceeds() = runBlocking {
        val calls = AtomicInteger()
        routes["/ep.mp4"] = {
            if (calls.getAndIncrement() == 0) MockResponse.Builder().code(500).build()
            else MockResponse.Builder().body(Buffer().write(ByteArray(10) { 1 })).build()
        }

        val key = manager.download(storage("/ep.mp4"), null, "Show", "Ep 1")
        val failed = awaitQueue { q -> q.any { it.state == QueuedDownloadState.Failed } }.single()
        assertEquals(key, failed.key)
        assertTrue(failed.error.orEmpty().contains("500"), failed.error)

        manager.retry(key)
        awaitQueue { it.isEmpty() }

        assertContentEquals(ByteArray(10) { 1 }, File(showDir, "Ep 1.mp4").readBytes())
    }

    @Test
    fun cancelRemovesPartFile() = runBlocking {
        route("/ep.mp4", ByteArray(4 * 1024 * 1024), "video/mp4", throttle = true)
        val part = File(showDir, "Ep 1.mp4.part")

        val key = manager.download(storage("/ep.mp4"), null, "Show", "Ep 1")
        awaitQueue { q -> q.any { (it.bytesDownloaded ?: 0) > 0 } }
        assertTrue(part.exists())

        manager.cancel(key)
        awaitQueue { it.isEmpty() }
        eventually { !part.exists() }
        assertFalse(File(showDir, "Ep 1.mp4").exists())
    }

    @Test
    fun queueShowsWaitingWhileOneRuns() = runBlocking {
        route("/slow.mp4", ByteArray(4 * 1024 * 1024), "video/mp4", throttle = true)
        route("/fast.mp4", ByteArray(10), "video/mp4")

        val first = manager.download(storage("/slow.mp4"), null, "Show", "Ep 1")
        val second = manager.download(storage("/fast.mp4"), null, "Show", "Ep 2")

        val queue = awaitQueue { q -> q.any { it.key == first && it.state == QueuedDownloadState.Running } }
        assertEquals(listOf(first to QueuedDownloadState.Running, second to QueuedDownloadState.Waiting), queue.map { it.key to it.state })
        assertEquals("Show - Ep 2", queue.last().title)

        manager.cancel(first)
        awaitQueue { it.isEmpty() }
        assertTrue(File(showDir, "Ep 2.mp4").exists())
    }

    @Test
    fun dashFailsUntilSupported() = runBlocking {
        val key = manager.download(storage("/ep.mpd", "application/dash+xml"), null, "Show", "Ep 1")

        val failed = awaitQueue { q -> q.any { it.state == QueuedDownloadState.Failed } }.single()
        assertEquals(key, failed.key)
        assertEquals("DASH downloads aren't supported yet", failed.error)
    }

    private companion object {
        /** DataStore is a process-wide singleton; point it at a temp file once. */
        val settings by lazy {
            val store = createTempDirectory("anime-datastore").toFile().apply { deleteOnExit() }
            DataStoreSettings { File(store, it).absolutePath }
            AnimeDesktopSettings(AppDirs { appName = "AnimeWorldTest"; appAuthor = "test" })
        }
    }
}
