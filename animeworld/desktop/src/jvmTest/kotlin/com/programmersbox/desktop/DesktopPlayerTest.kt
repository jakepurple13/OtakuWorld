package com.programmersbox.desktop

import com.programmersbox.anime.shared.videoplayer.DesktopPlayer
import com.programmersbox.anime.shared.videoplayer.LocalStreamProxy
import com.programmersbox.anime.shared.videoplayer.PlayerSource
import com.programmersbox.anime.shared.videoplayer.VlcRuntime
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class DesktopPlayerTest {
    private val factory = VlcRuntime.factory().getOrThrow()
    private val proxy = LocalStreamProxy()
    private val players = mutableListOf<DesktopPlayer>()
    private val fixture = File(DesktopPlayerTest::class.java.getResource("/fixtures/tiny clip ü.mp4")!!.toURI())

    private fun newPlayer() = DesktopPlayer(factory, proxy).also(players::add)

    @AfterTest
    fun tearDown() {
        players.forEach { it.close() }
        proxy.close()
    }

    @Test
    fun playsLocalFileWithSpacesAndUnicode() {
        val player = newPlayer()
        player.open(PlayerSource.LocalFile(fixture.absolutePath))

        awaitTrue("duration ~3 s") { player.state.value.durationMs in 2900..3100 }
        awaitTrue("first frame") { player.frame.value != null }
        assertEquals(320, player.frame.value!!.width)
        // The decoder pads 180 lines to 192; frames are cropped once libVLC reports the video size.
        awaitTrue("frame cropped to 320x180") { player.frame.value.let { it != null && it.width == 320 && it.height == 180 } }

        val start = player.state.value.positionMs
        awaitTrue("position advances past $start") { player.state.value.positionMs > start }
        assertEquals(null, player.state.value.error)
    }

    @Test
    fun seekByMovesPosition() {
        val player = newPlayer()
        player.open(PlayerSource.LocalFile(fixture.absolutePath))
        awaitTrue("first frame") { player.frame.value != null && player.state.value.playing }
        player.playPause()
        awaitTrue("paused") { !player.state.value.playing }
        val before = player.state.value.positionMs

        player.seekBy(2_000)
        player.playPause()

        // Plain playback from `before` cannot get 2.1 s further within 900 ms; only the seek can.
        awaitTrue("position past ${before + 2_100}", timeoutMs = 900) { player.state.value.positionMs >= before + 2_100 }
    }

    @Test
    fun playPauseToggles() {
        val player = newPlayer()
        player.open(PlayerSource.LocalFile(fixture.absolutePath))
        awaitTrue("playing") { player.state.value.playing && player.frame.value != null }

        player.playPause()
        awaitTrue("paused") { !player.state.value.playing }
        val pausedAt = player.state.value.positionMs
        Thread.sleep(400)
        assertTrue(player.state.value.positionMs - pausedAt < 100, "position moved while paused")

        player.playPause()
        awaitTrue("playing again") { player.state.value.playing }
    }

    @Test
    fun releaseWhilePlayingDoesNotCrash() {
        val player = newPlayer()
        player.open(PlayerSource.LocalFile(fixture.absolutePath))
        awaitTrue("first frame") { player.frame.value != null && player.state.value.playing }

        thread(name = "test-closer") { player.close() }.join()
        player.close()
        player.playPause() // no-op after close, must not touch the released native player

        val next = newPlayer()
        next.open(PlayerSource.LocalFile(fixture.absolutePath))
        awaitTrue("next player frame") { next.frame.value != null }
    }

    @Test
    fun streamGoesThroughProxy() {
        val bytes = fixture.readBytes()
        val requests = CopyOnWriteArrayList<RecordedRequest>()
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request
                    return rangeResponse(bytes, request.headers["Range"])
                }
            }
            server.start()
            proxy.start()

            val player = newPlayer()
            player.open(PlayerSource.Stream(server.url("/clip.mp4").toString(), mapOf("X-Test" to "1"), "video/mp4"))

            awaitTrue("frame through proxy") { player.frame.value != null }
            assertTrue(requests.isNotEmpty())
            requests.forEach { assertEquals("1", it.headers["X-Test"], "request ${it.url} lacked X-Test") }
        }
    }

    @Test
    fun httpErrorIsShown() {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse.Builder().code(403).body("no").build()
            }
            server.start()

            val player = newPlayer()
            player.open(PlayerSource.Stream(server.url("/clip.mp4").toString(), emptyMap(), "video/mp4"))

            awaitTrue("error shown") { player.state.value.error != null }
            val error = assertNotNull(player.state.value.error)
            assertTrue("403" in error, "error was: $error")
        }
    }

    private fun rangeResponse(bytes: ByteArray, range: String?): MockResponse {
        val match = range?.let { Regex("bytes=(\\d+)-(\\d*)").matchEntire(it.trim()) }
            ?: return MockResponse.Builder()
                .code(200)
                .setHeader("Content-Type", "video/mp4")
                .setHeader("Accept-Ranges", "bytes")
                .body(Buffer().write(bytes))
                .build()
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toIntOrNull()?.coerceAtMost(bytes.size - 1) ?: (bytes.size - 1)
        if (start >= bytes.size) {
            return MockResponse.Builder().code(416).setHeader("Content-Range", "bytes */${bytes.size}").build()
        }
        return MockResponse.Builder()
            .code(206)
            .setHeader("Content-Type", "video/mp4")
            .setHeader("Accept-Ranges", "bytes")
            .setHeader("Content-Range", "bytes $start-$end/${bytes.size}")
            .body(Buffer().write(bytes, start, end - start + 1))
            .build()
    }

    private fun awaitTrue(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        if (!condition()) fail("timed out after $timeoutMs ms waiting for: $what")
    }
}
