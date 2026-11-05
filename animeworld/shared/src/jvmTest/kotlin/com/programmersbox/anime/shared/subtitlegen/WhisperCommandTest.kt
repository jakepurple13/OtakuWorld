package com.programmersbox.anime.shared.subtitlegen

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhisperCommandTest {
    private val ja = WhisperLanguage("ja", "Japanese")
    private val en = WhisperLanguage("en", "English")
    private val model = File("/m/ggml-small.bin")
    private val wav = File("/t/a.wav")
    private val out = File("/t/out")

    @Test
    fun extractArgsKeepPathsWhole() {
        val video = File("/v/[SubsPlease] Frieren - 07 (1080p).mkv")
        val args = extractAudioArgs("ffmpeg", video, wav)
        assertEquals(video.path, args[args.indexOf("-i") + 1])
        assertEquals(wav.path, args.last())
    }

    @Test
    fun whisperArgsTranscribe() {
        val args = whisperArgs("w", model, wav, out, GenerationRequest(ja, ja))
        assertTrue(args.containsAll(listOf("-ovtt", "-pp")))
        assertEquals("ja", args[args.indexOf("-l") + 1])
        assertFalse("-tr" in args)
    }

    @Test
    fun whisperArgsTranslate() {
        assertEquals("-tr", whisperArgs("w", model, wav, out, GenerationRequest(ja, en)).last())
    }

    @Test
    fun whisperArgsAuto() {
        val args = whisperArgs("w", model, wav, out, GenerationRequest(WhisperAuto, en))
        assertEquals("auto", args[args.indexOf("-l") + 1])
        assertTrue("-tr" in args)
    }

    @Test
    fun parsesProgressLines() {
        assertEquals(45, parseWhisperProgress("whisper_print_progress_callback: progress =  45%"))
        assertEquals(100, parseWhisperProgress("progress = 100%"))
        assertNull(parseWhisperProgress("[00:00.000 --> 00:02.000] hi"))
    }

    @Test
    fun remainingTime() {
        assertEquals(60_000L, remainingMs(60_000, 0.5f))
        assertNull(remainingMs(1_000, 0.01f))
    }

    @Test
    fun findsConfiguredWhisper() = runBlocking {
        val f = File.createTempFile("whisper-cli", "").apply { setExecutable(true); deleteOnExit() }
        assertEquals(f, findWhisperCli(f.path))
    }
}
