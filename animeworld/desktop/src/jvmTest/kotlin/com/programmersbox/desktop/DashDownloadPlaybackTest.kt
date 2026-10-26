package com.programmersbox.desktop

import com.programmersbox.anime.shared.downloads.DashDownloader
import com.programmersbox.anime.shared.videoplayer.DesktopPlayer
import com.programmersbox.anime.shared.videoplayer.LocalStreamProxy
import com.programmersbox.anime.shared.videoplayer.PlayerSource
import com.programmersbox.anime.shared.videoplayer.VlcRuntime
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.fail

/** A DASH download (fixture from `:animeworld:shared`'s test resources) plays in libVLC with video and audio. */
class DashDownloadPlaybackTest {
    private val factory = VlcRuntime.factory().getOrThrow()
    private val proxy = LocalStreamProxy()
    private val dir = createTempDirectory("dash-playback").toFile()
    private var player: DesktopPlayer? = null

    @AfterTest
    fun tearDown() {
        player?.close()
        proxy.close()
        dir.deleteRecursively()
    }

    @Test
    fun downloadedDashPlaysWithVideoAndAudio() {
        val out = File(dir, "Ep 1.mp4")
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val bytes = DashDownloadPlaybackTest::class.java
                        .getResource("/fixtures/dash/${request.url.encodedPath.removePrefix("/dash/")}")
                        ?.readBytes() ?: return MockResponse.Builder().code(404).build()
                    return MockResponse.Builder().body(Buffer().write(bytes)).build()
                }
            }
            server.start()
            runBlocking { DashDownloader(OkHttpClient()).download(server.url("/dash/manifest.mpd").toString(), emptyMap(), out) {} }
        }

        val desktop = DesktopPlayer(factory, proxy).also { player = it }
        desktop.open(PlayerSource.LocalFile(out.absolutePath))
        awaitTrue("duration within 10% of 3 s") { desktop.state.value.durationMs in 2_700..3_300 }
        awaitTrue("a video frame") { desktop.frame.value != null }

        // DesktopPlayer does not list audio tracks; ask libVLC directly with a headless player.
        val probe = factory.mediaPlayers().newMediaPlayer()
        try {
            probe.media().play(out.absolutePath, ":vout=dummy", ":aout=dummy")
            awaitTrue("libVLC reports an audio track") { probe.media().info()?.audioTracks()?.isNotEmpty() == true }
        } finally {
            probe.release()
        }
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
