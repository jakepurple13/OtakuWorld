package com.programmersbox.anime.shared.downloads

import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.File
import java.io.IOException
import java.util.Collections
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EpisodeDownloaderTest {

    private lateinit var server: MockWebServer
    private lateinit var folder: File
    private val routes = mutableMapOf<String, (RecordedRequest) -> MockResponse>()
    private val recorded = Collections.synchronizedList(mutableListOf<RecordedRequest>())
    private val downloader = EpisodeDownloader(OkHttpClient(), retryDelayMillis = 0)

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                recorded += request
                return routes[request.url.encodedPath]?.invoke(request)
                    ?: MockResponse.Builder().code(404).build()
            }
        }
        server.start()
        folder = createTempDirectory("episode-dl").toFile()
    }

    @AfterTest
    fun tearDown() {
        server.close()
        folder.deleteRecursively()
    }

    private fun route(path: String, contentType: String? = null, body: ByteArray) {
        routes[path] = {
            MockResponse.Builder()
                .body(Buffer().write(body))
                .apply { if (contentType != null) setHeader("Content-Type", contentType) }
                .build()
        }
    }

    private fun route(path: String, body: String, contentType: String? = null) =
        route(path, contentType, body.encodeToByteArray())

    private fun url(path: String) = server.url(path).toString()

    private fun request(
        path: String,
        mimeType: String? = null,
        subtitles: List<KmpSubtitle> = emptyList(),
        headers: Map<String, String> = emptyMap(),
    ) = EpisodeDownloadRequest(
        url = url(path),
        headers = headers,
        mimeType = mimeType,
        qualityHint = null,
        subtitles = subtitles,
        title = "Ep 1",
        folder = folder.absolutePath,
    )

    private fun fileNames() = folder.listFiles().orEmpty().map { it.name }.toSet()

    private val vtt = "WEBVTT\n\n00:00.000 --> 00:01.000\nHi\n"
    private val segs = listOf("seg-one".encodeToByteArray(), "seg-two!".encodeToByteArray(), "seg-three".encodeToByteArray())

    private fun mediaPlaylist(names: List<String>, extra: String = "") = buildString {
        appendLine("#EXTM3U")
        appendLine("#EXT-X-TARGETDURATION:10")
        append(extra)
        names.forEach { appendLine("#EXTINF:10,"); appendLine(it) }
        appendLine("#EXT-X-ENDLIST")
    }

    @Test
    fun progressiveDownloadWritesFileAndSidecars() = runTest {
        val video = ByteArray(5000) { it.toByte() }
        route("/video.mp4", "video/mp4", video)
        route("/subs/en.vtt", vtt)

        val progress = mutableListOf<DownloadProgress>()
        val file = downloader.download(
            request("/video.mp4", subtitles = listOf(KmpSubtitle(url("/subs/en.vtt"), "English"))),
        ) { progress += it }

        assertEquals("Ep 1.mp4", file.name)
        assertContentEquals(video, file.readBytes())
        assertEquals(setOf("Ep 1.mp4", "Ep 1.0.English.vtt", "Ep 1.subtitles.json"), fileNames())
        assertEquals(vtt, File(folder, "Ep 1.0.English.vtt").readText())
        val entries = Json.decodeFromString<List<SidecarEntry>>(File(folder, "Ep 1.subtitles.json").readText())
        assertEquals(listOf(SidecarEntry("Ep 1.0.English.vtt", "English", "English")), entries)
        assertEquals(1f, progress.last().fraction)
        assertEquals(video.size.toLong(), progress.last().bytes)
        assertEquals(video.size.toLong(), progress.last().totalBytes)
    }

    @Test
    fun staleRecordedPartIsDeletedAndBaseReused() = runTest {
        route("/video.mp4", "video/mp4", byteArrayOf(1, 2, 3))
        val stale = File(folder, "Ep 1.ts.part").apply { writeText("stale") }
        val record = File(folder.parentFile, folder.name + ".parts").apply { writeText(stale.absolutePath + "\n") }
        try {
            val file = downloader.download(request("/video.mp4"), partsRecord = record) {}

            assertEquals("Ep 1.mp4", file.name)
            assertEquals(setOf("Ep 1.mp4"), fileNames())
            assertTrue(!record.exists())
        } finally {
            record.delete()
        }
    }

    @Test
    fun recordListsWrittenPathsUntilRunEnds() = runTest {
        route("/video.mp4", "video/mp4", ByteArray(100))
        val record = File(folder.parentFile, folder.name + ".parts")
        var recorded = emptyList<String>()
        try {
            downloader.download(request("/video.mp4"), partsRecord = record) { recorded = record.readLines() }

            assertEquals(listOf(File(folder, "Ep 1.mp4.part").absolutePath), recorded)
            assertTrue(!record.exists())
        } finally {
            record.delete()
        }
    }

    @Test
    fun progressiveUnknownLengthReportsBytes() = runTest {
        val video = ByteArray(2_500_000) { it.toByte() }
        routes["/unknown.mp4"] = {
            MockResponse.Builder()
                .chunkedBody(Buffer().write(video), 64 * 1024)
                .setHeader("Content-Type", "video/mp4")
                .build()
        }

        val progress = mutableListOf<DownloadProgress>()
        val file = downloader.download(request("/unknown.mp4")) { progress += it }

        assertContentEquals(video, file.readBytes())
        assertTrue(progress.size >= 2)
        assertTrue(progress.all { it.fraction == null && it.totalBytes == null })
        assertTrue(progress.zipWithNext().all { (a, b) -> b.bytes - a.bytes >= 1_000_000 })
    }

    @Test
    fun progressiveExtensionFromContentType() = runTest {
        route("/stream", "video/x-matroska", byteArrayOf(1, 2, 3))
        assertEquals("Ep 1.mkv", downloader.download(request("/stream")) {}.name)
    }

    @Test
    fun hlsTsSegmentsConcatenated() = runTest {
        // No mime type and no .m3u8 path: detected from the #EXTM3U body.
        route("/stream", mediaPlaylist(listOf("s0.ts", "s1.ts", "s2.ts")))
        segs.forEachIndexed { i, b -> route("/s$i.ts", "video/mp2t", b) }

        val file = downloader.download(request("/stream")) {}

        assertEquals("Ep 1.ts", file.name)
        assertContentEquals(segs.reduce { a, b -> a + b }, file.readBytes())
        assertEquals(setOf("Ep 1.ts"), fileNames())
    }

    @Test
    fun hlsFmp4WritesInitThenSegments() = runTest {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=100000,RESOLUTION=640x360
            low/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=1920x1080
            high/index.m3u8
        """.trimIndent()
        route("/master", master)
        route("/high/index.m3u8", mediaPlaylist(listOf("s0.m4s", "s1.m4s", "s2.m4s"), "#EXT-X-MAP:URI=\"init.mp4\"\n"))
        val init = "init-box".encodeToByteArray()
        route("/high/init.mp4", body = init)
        segs.forEachIndexed { i, b -> route("/high/s$i.m4s", body = b) }

        val file = downloader.download(request("/master", mimeType = "application/x-mpegURL")) {}

        assertEquals("Ep 1.mp4", file.name)
        assertContentEquals(init + segs.reduce { a, b -> a + b }, file.readBytes())
    }

    private fun encrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding")
            .apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) }
            .doFinal(data)

    private val key = ByteArray(16) { (it * 7 + 3).toByte() }
    private val explicitIv = ByteArray(16) { (0xA0 + it).toByte() }
    private val explicitIvHex = "0x" + explicitIv.joinToString("") { "%02x".format(it) }

    /** Segment 0 uses an explicit IV; segments 1-2 use the sequence-number IV (media sequence 5). */
    private fun setUpAesStream() {
        val playlist = buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-TARGETDURATION:10")
            appendLine("#EXT-X-MEDIA-SEQUENCE:5")
            appendLine("#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\",IV=$explicitIvHex")
            appendLine("#EXTINF:10,"); appendLine("s0.ts")
            appendLine("#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"")
            appendLine("#EXTINF:10,"); appendLine("s1.ts")
            appendLine("#EXTINF:10,"); appendLine("s2.ts")
            appendLine("#EXT-X-ENDLIST")
        }
        route("/aes/index.m3u8", playlist)
        route("/aes/key.bin", body = key)
        segs.forEachIndexed { i, b ->
            val iv = if (i == 0) explicitIv else aesIv(HlsSegment("", HlsKey("AES-128", null, null), 5L + i))
            route("/aes/s$i.ts", body = encrypt(key, iv, b))
        }
    }

    @Test
    fun aes128SegmentsDecrypted() = runTest {
        setUpAesStream()

        val file = downloader.download(request("/aes/index.m3u8")) {}

        assertContentEquals(segs.reduce { a, b -> a + b }, file.readBytes())
        assertEquals(1, recorded.count { it.url.encodedPath == "/aes/key.bin" }, "key fetched once per URI")
    }

    @Test
    fun aes128EncryptedInitSectionDecrypted() = runTest {
        val init = "init-box".encodeToByteArray()
        val playlist = mediaPlaylist(
            listOf("s0.m4s"),
            "#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\",IV=$explicitIvHex\n#EXT-X-MAP:URI=\"init.mp4\"\n",
        )
        route("/em/index.m3u8", playlist)
        route("/em/key.bin", body = key)
        route("/em/init.mp4", body = encrypt(key, explicitIv, init))
        route("/em/s0.m4s", body = encrypt(key, explicitIv, segs[0]))

        val file = downloader.download(request("/em/index.m3u8")) {}

        assertContentEquals(init + segs[0], file.readBytes())
    }

    @Test
    fun encryptedInitWithoutIvFails() = runTest {
        route(
            "/ei/index.m3u8",
            mediaPlaylist(listOf("s0.m4s"), "#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXT-X-MAP:URI=\"init.mp4\"\n"),
        )

        assertFailsWith<UnsupportedHlsException> { downloader.download(request("/ei/index.m3u8")) {} }
        assertEquals(emptySet(), fileNames())
    }

    @Test
    fun writeAtomicallyDeletesPartOnFailure() {
        val target = File(folder, "sub.vtt").apply { mkdirs(); File(this, "child").writeText("x") }
        val written = mutableListOf<File>()

        assertFailsWith<IOException> { downloader.writeAtomically(target, byteArrayOf(1), written) }

        assertEquals(setOf("sub.vtt"), fileNames())
        assertEquals(emptyList(), written.filter { it.name == "sub.vtt" })
    }

    @Test
    fun requestFileRoundTripsLargeRequest() {
        val subtitles = List(60) { KmpSubtitle("https://cdn.example.com/" + "x".repeat(300) + "/$it.vtt", "lang$it", mapOf("Referer" to "r".repeat(200))) }
        val req = request("/video.mp4", subtitles = subtitles)

        val file = writeRequestFile(folder, req.folder + req.title, req)

        assertTrue(file.length() > 10_240)
        assertEquals(req, readRequestFile(file))
    }

    @Test
    fun sampleAesFails() = runTest {
        route(
            "/sa/index.m3u8",
            mediaPlaylist(listOf("s0.ts"), "#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"key.bin\"\n"),
        )
        route("/sa/s0.ts", body = segs[0])

        val error = assertFailsWith<IOException> { downloader.download(request("/sa/index.m3u8")) {} }
        assertEquals("Unsupported HLS encryption: SAMPLE-AES", error.message)
        assertEquals(emptySet(), fileNames())
    }

    @Test
    fun separateAudioRenditionFails() = runTest {
        val master = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="Japanese",URI="audio/index.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=1920x1080,AUDIO="aud"
            video/index.m3u8
        """.trimIndent()
        route("/sep/master.m3u8", master)

        assertFailsWith<UnsupportedHlsException> { downloader.download(request("/sep/master.m3u8")) {} }
        assertEquals(emptySet(), fileNames())
    }

    @Test
    fun livePlaylistFails() = runTest {
        route("/live/index.m3u8", mediaPlaylist(listOf("s0.ts")).replace("#EXT-X-ENDLIST\n", ""))

        assertFailsWith<UnsupportedHlsException> { downloader.download(request("/live/index.m3u8")) {} }
        assertEquals(emptySet(), fileNames())
    }

    @Test
    fun cancelLeavesNoFiles() = runTest {
        route("/c/index.m3u8", mediaPlaylist(listOf("s0.ts", "s1.ts", "s2.ts")))
        segs.forEachIndexed { i, b -> route("/c/s$i.ts", body = b) }

        val firstSegment = CompletableDeferred<Unit>()
        val result = withContext(Dispatchers.Default) {
            val job = async {
                downloader.download(request("/c/index.m3u8")) { firstSegment.complete(Unit) }
            }
            firstSegment.await()
            job.cancel()
            runCatching { job.await() }
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(emptySet(), fileNames())
    }

    @Test
    fun segmentFailureCleansUp() = runTest {
        route("/f/index.m3u8", mediaPlaylist(listOf("s0.ts", "s1.ts", "s2.ts")))
        route("/f/s0.ts", body = segs[0])
        route("/f/s2.ts", body = segs[2])
        routes["/f/s1.ts"] = { MockResponse.Builder().code(500).build() }

        assertFailsWith<IOException> { downloader.download(request("/f/index.m3u8")) {} }

        assertEquals(4, recorded.count { it.url.encodedPath == "/f/s1.ts" }, "1 attempt + 3 retries")
        assertEquals(0, recorded.count { it.url.encodedPath == "/f/s2.ts" })
        assertEquals(emptySet(), fileNames())
    }

    @Test
    fun subtitleFailureDoesNotFailVideo() = runTest {
        route("/video.mp4", "video/mp4", byteArrayOf(1, 2, 3))
        route("/subs/ja.srt", "1\n00:00:00,000 --> 00:00:01,000\nこんにちは\n")

        val file = downloader.download(
            request(
                "/video.mp4",
                subtitles = listOf(
                    KmpSubtitle(url("/subs/missing.vtt"), "English"),
                    KmpSubtitle(url("/subs/ja.srt"), "Japanese"),
                ),
            ),
        ) {}

        assertTrue(file.exists())
        assertEquals(setOf("Ep 1.mp4", "Ep 1.1.Japanese.srt", "Ep 1.subtitles.json"), fileNames())
        val entries = Json.decodeFromString<List<SidecarEntry>>(File(folder, "Ep 1.subtitles.json").readText())
        assertEquals(listOf(SidecarEntry("Ep 1.1.Japanese.srt", "Japanese", "Japanese")), entries)
    }

    @Test
    fun headersSentOnEveryRequest() = runTest {
        route(
            "/aes/master.m3u8",
            "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000\nindex.m3u8\n",
        )
        setUpAesStream()
        route("/subs/en.vtt", vtt)

        downloader.download(
            request(
                "/aes/master.m3u8",
                headers = mapOf("Referer" to "https://example.org/"),
                subtitles = listOf(KmpSubtitle(url("/subs/en.vtt"), "English", mapOf("X-Sub" to "1"))),
            ),
        ) {}

        val paths = recorded.map { it.url.encodedPath }.toSet()
        assertEquals(
            setOf("/aes/master.m3u8", "/aes/index.m3u8", "/aes/key.bin", "/aes/s0.ts", "/aes/s1.ts", "/aes/s2.ts", "/subs/en.vtt"),
            paths,
        )
        recorded.forEach { assertEquals("https://example.org/", it.headers["Referer"], it.url.encodedPath) }
        assertEquals("1", recorded.single { it.url.encodedPath == "/subs/en.vtt" }.headers["X-Sub"])
    }
}
