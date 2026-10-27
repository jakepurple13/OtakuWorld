package com.programmersbox.anime.shared.videoexport

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.datastore.DataStoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** Runs the real ffmpeg on a generated clip; skipped when ffmpeg isn't installed. */
class VideoExportManagerTest {
    private val root = createTempDirectory("export-test").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val manager by lazy {
        DataStoreSettings { File(root, it).absolutePath }
        VideoExportManager(scope, AnimeDesktopSettings(AppDirs { appName = "ExportTest"; appAuthor = "test" }), trayState = null)
    }

    @AfterTest
    fun cleanup() {
        scope.cancel()
        root.deleteRecursively()
    }

    private fun ffmpegOrSkip(): File {
        val ffmpeg = runBlocking { manager.ffmpeg() }
        org.junit.Assume.assumeTrue("ffmpeg not installed", ffmpeg != null)
        return ffmpeg!!
    }

    private fun clip(ffmpeg: File, seconds: Int = 3): File {
        val video = File(root, "Episode 1.ts")
        val p = ProcessBuilder(
            ffmpeg.path, "-hide_banner", "-y",
            "-f", "lavfi", "-i", "testsrc=size=320x240:rate=24:duration=$seconds",
            "-f", "lavfi", "-i", "sine=frequency=440:duration=$seconds",
            "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", video.path,
        ).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        assertEquals(0, p.waitFor(), out)
        val vtt = "WEBVTT\n\n00:00.000 --> 00:02.000\nHello there\n"
        File(root, "Episode 1.0.English.vtt").writeText(vtt)
        File(root, "Episode 1.1.Spanish.vtt").writeText(vtt.replace("Hello there", "Hola"))
        return video
    }

    private fun export(ffmpeg: File, source: File, options: ExportOptions): ExportState.Done {
        val output = manager.outputFor(source, options.format, root)
        val info = runBlocking { manager.probe(ffmpeg, source) }
        manager.start(ffmpeg, source, options, output, info?.durationMs)
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            when (val s = manager.exports.value[source.absolutePath]?.state) {
                is ExportState.Done -> return s.also { manager.clear(source.absolutePath) }
                is ExportState.Failed -> fail("export failed: ${s.message}\n${s.log}")
                else -> Thread.sleep(100)
            }
        }
        fail("export timed out")
    }

    private fun streams(ffmpeg: File, file: File): String {
        val p = ProcessBuilder(ffmpeg.path, "-hide_banner", "-i", file.path).redirectErrorStream(true).start()
        return p.inputStream.bufferedReader().readText().also { p.waitFor() }
    }

    @Test
    fun probeReadsGeneratedClip() {
        val ffmpeg = ffmpegOrSkip()
        val info = assertNotNull(runBlocking { manager.probe(ffmpeg, clip(ffmpeg)) })
        assertEquals("MPEGTS", info.container)
        assertEquals("H264 320×240", info.video)
        assertTrue(info.durationMs!! in 2_500..3_600, "duration ${info.durationMs}")
    }

    @Test
    fun mp4WithTracksKeepsSourceAndAddsSubtitleStreams() {
        val ffmpeg = ffmpegOrSkip()
        val source = clip(ffmpeg)
        val done = export(ffmpeg, source, ExportOptions(ExportFormat.Mp4, SubtitleMode.AsTracks))

        assertEquals("Episode 1 - MP4.mp4", done.output.name)
        assertTrue(source.isFile, "source kept")
        assertTrue(File(root, "Episode 1.0.English.vtt").isFile, "sidecars kept")
        val info = streams(ffmpeg, done.output)
        assertEquals(2, Regex("Subtitle: mov_text").findAll(info).count(), info)
        assertEquals(emptyList(), root.listFiles()!!.filter { it.name.endsWith(".part") })
    }

    @Test
    fun mkvWithTracksCopiesSubtitles() {
        val ffmpeg = ffmpegOrSkip()
        val done = export(ffmpeg, clip(ffmpeg), ExportOptions(ExportFormat.Mkv, SubtitleMode.AsTracks))

        assertEquals("Episode 1 - MKV.mkv", done.output.name)
        val info = streams(ffmpeg, done.output)
        assertEquals(2, Regex("Subtitle: webvtt").findAll(info).count(), info)
    }

    @Test
    fun burnInReencodesWithoutSubtitleStreams() {
        val ffmpeg = ffmpegOrSkip()
        org.junit.Assume.assumeTrue("ffmpeg built without libass", runBlocking { manager.canBurnSubtitles(ffmpeg) })
        val done = export(ffmpeg, clip(ffmpeg), ExportOptions(ExportFormat.Mp4, SubtitleMode.BurnIn(1)))

        val info = streams(ffmpeg, done.output)
        assertTrue("Video: h264" in info, info)
        assertTrue("Subtitle" !in info, info)
    }

    @Test
    fun secondExportGetsNumberedName() {
        val ffmpeg = ffmpegOrSkip()
        val source = clip(ffmpeg)
        export(ffmpeg, source, ExportOptions(ExportFormat.Mp4, SubtitleMode.None))
        val second = export(ffmpeg, source, ExportOptions(ExportFormat.Mp4, SubtitleMode.None))
        assertEquals("Episode 1 - MP4 (2).mp4", second.output.name)
    }

    @Test
    fun cancelRemovesPartialOutput() {
        val ffmpeg = ffmpegOrSkip()
        val source = clip(ffmpeg, seconds = 120)
        val output = manager.outputFor(source, ExportFormat.Mkv, root)
        manager.start(ffmpeg, source, ExportOptions(ExportFormat.Mkv, SubtitleMode.None), output, 120_000)
        // Cancel straight away: a stream copy can finish in well under a second.
        manager.cancel(source.absolutePath)
        Thread.sleep(1_000)

        assertEquals(null, manager.exports.value[source.absolutePath])
        assertTrue(root.listFiles()!!.none { it.name.startsWith("Episode 1 - MKV") }, root.list()!!.joinToString())
        assertIs<File>(source)
    }
}
