package com.programmersbox.anime.shared.videoexport

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FfmpegCommandTest {

    private val input = File("/videos/Show/Episode 1.ts")
    private val output = File("/videos/Show/Episode 1 - MP4.mp4.part")
    private val subs = listOf(
        ExportSubtitle(File("/videos/Show/Episode 1.0.English.vtt"), "English"),
        ExportSubtitle(File("/videos/Show/Episode 1.1.Spanish.vtt"), "Spanish"),
    )

    private fun args(format: ExportFormat, mode: SubtitleMode, burn: File? = null) =
        ffmpegArgs("ffmpeg", input, subs, ExportOptions(format, mode), output, burn)

    private fun List<String>.after(flag: String) = this[indexOf(flag) + 1]

    @Test
    fun mp4WithoutSubtitlesCopiesStreams() {
        val a = args(ExportFormat.Mp4, SubtitleMode.None)
        assertEquals(listOf("-i", input.path), a.subList(a.indexOf("-i"), a.indexOf("-i") + 2))
        assertEquals(1, a.count { it == "-i" })
        assertEquals("copy", a.after("-c:v"))
        assertEquals("copy", a.after("-c:a"))
        assertFalse("-c:s" in a)
        assertEquals("mp4", a.after("-f"))
        assertEquals("+faststart", a.after("-movflags"))
        assertEquals("pipe:1", a.after("-progress"))
        assertEquals(output.path, a.last())
    }

    @Test
    fun tracksAddEveryInputMappedAndTitled() {
        val a = args(ExportFormat.Mp4, SubtitleMode.AsTracks)
        assertEquals(3, a.count { it == "-i" })
        assertTrue(listOf("-map", "1:0").let { a.windowed(2).contains(it) })
        assertTrue(listOf("-map", "2:0").let { a.windowed(2).contains(it) })
        assertEquals("mov_text", a.after("-c:s"))
        assertEquals("title=English", a.after("-metadata:s:s:0"))
        assertEquals("title=Spanish", a.after("-metadata:s:s:1"))
        assertEquals("copy", a.after("-c:v"))
    }

    @Test
    fun mkvTracksAreCopied() {
        val a = args(ExportFormat.Mkv, SubtitleMode.AsTracks)
        assertEquals("copy", a.after("-c:s"))
        assertEquals("matroska", a.after("-f"))
        assertFalse("-movflags" in a)
    }

    @Test
    fun burnInReencodesWithSubtitlesFilter() {
        val a = args(ExportFormat.Mkv, SubtitleMode.BurnIn(1), burn = File("/tmp/x/sub.vtt"))
        assertEquals(1, a.count { it == "-i" })
        assertEquals("subtitles=filename=/tmp/x/sub.vtt", a.after("-vf"))
        assertEquals("libx264", a.after("-c:v"))
        assertEquals("20", a.after("-crf"))
        assertEquals("copy", a.after("-c:a"))
        assertFalse("-c:s" in a)
    }

    @Test
    fun filterPathEscapesBothLevels() {
        assertEquals("C\\\\:/a\\\\\\'b\\,c.vtt", escapeFilterPath("C:\\a'b,c.vtt"))
    }

    @Test
    fun progressBlocksAreParsed() {
        val parser = FfmpegProgressParser()
        assertNull(parser.line("frame=10"))
        parser.line("total_size=1048576")
        parser.line("out_time_us=5000000")
        parser.line("speed=3.1x")
        val p = parser.line("progress=continue")!!
        assertEquals(5_000L, p.positionMs)
        assertEquals(1_048_576L, p.bytes)
        assertEquals(3.1f, p.speed)
        assertFalse(p.finished)
        parser.line("out_time_us=N/A")
        parser.line("speed=N/A")
        val end = parser.line("progress=end")!!
        assertTrue(end.finished)
        assertEquals(5_000L, end.positionMs)
        assertNull(end.speed)
    }

    @Test
    fun probeOutputIsParsed() {
        val stderr = """
            Input #0, mpegts, from 'Episode 1.ts':
              Duration: 00:23:40.12, start: 1.400000, bitrate: 3612 kb/s
                Stream #0:0[0x100]: Video: h264 (High) ([27][0][0][0] / 0x001B), yuv420p(progressive), 1920x1080 [SAR 1:1 DAR 16:9], 23.98 fps
                Stream #0:1[0x101]: Audio: aac (LC) ([15][0][0][0] / 0x000F), 48000 Hz, stereo, fltp, 130 kb/s
            At least one output file must be specified
        """.trimIndent()
        val info = parseProbe(stderr)!!
        assertEquals("MPEGTS", info.container)
        assertEquals(1_420_120L, info.durationMs)
        assertEquals("H264 1920×1080", info.video)
        assertEquals("AAC stereo", info.audio)
    }

    @Test
    fun subtitlesFilterDetected() {
        assertTrue(hasSubtitlesFilter(" .. abench            A->A       Benchmark.\n .. subtitles         V->V       Render text subtitles onto input video using the libass library.\n"))
        assertFalse(hasSubtitlesFilter(" .. abench            A->A       Benchmark.\n .. subtitles_x       V->V       Other.\n"))
    }

    @Test
    fun probeWithoutInputIsNull() {
        assertNull(parseProbe("Episode 1.ts: Invalid data found when processing input"))
    }
}
