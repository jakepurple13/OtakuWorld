package com.programmersbox.anime.shared.cast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubtitleVttTest {

    @Test
    fun srtToVtt_convertsCommaTimestamps() {
        val srt = "1\n00:00:01,500 --> 00:00:03,000\nHello\n"
        val vtt = toWebVtt(srt, SubtitleFormat.Srt)
        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue("00:00:01.500 --> 00:00:03.000" in vtt)
        assertTrue("," !in vtt)
    }

    @Test
    fun srtToVtt_stripsBomAndCrlf() {
        val srt = "﻿1\r\n00:00:01,000 --> 00:00:02,000\r\nHi\r\n"
        val vtt = toWebVtt(srt, SubtitleFormat.Srt)
        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue('\r' !in vtt)
        assertTrue('﻿' !in vtt)
    }

    @Test
    fun assToVtt_usesDialogueTextOnly() {
        val ass = """
            [Script Info]
            Title: x

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,ignore me
            Dialogue: 0,0:00:01.50,0:00:03.25,Default,,0,0,0,,{\i1}Hello, {\pos(10,20)}world\NSecond
        """.trimIndent()
        val vtt = toWebVtt(ass, SubtitleFormat.Ass)
        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue("ignore me" !in vtt)
        assertTrue("00:00:01.500 --> 00:00:03.250" in vtt)
        assertTrue("Hello, world\nSecond" in vtt)
        assertTrue("{" !in vtt)
    }

    @Test
    fun assToVtt_dropsDrawingsAndEmptyCues() {
        val ass = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:02.00,Sign,,0,0,0,,{\pos(5,5)\p1}m 0 0 l 100 0 100 100 0 100{\p0}
            Dialogue: 0,0:00:03.00,0:00:04.00,Default,,0,0,0,,Before {\p2}m 0 0 l 1 1{\p0}after
        """.trimIndent()
        val vtt = toWebVtt(ass, SubtitleFormat.Ass)
        assertTrue("m 0 0" !in vtt)
        assertTrue("00:00:01.000" !in vtt)
        assertEquals(listOf("Before after"), parseVttCues(vtt).map { it.text })
    }

    @Test
    fun vttPassthrough_unchanged() {
        val vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nHi\n"
        assertEquals(vtt, toWebVtt(vtt, SubtitleFormat.Vtt))
    }

    @Test
    fun formatFromUrl() {
        assertEquals(SubtitleFormat.Srt, SubtitleFormat.fromUrl("https://a/b.srt?x=1"))
        assertEquals(SubtitleFormat.Ass, SubtitleFormat.fromUrl("https://a/b.ass"))
        assertEquals(SubtitleFormat.Vtt, SubtitleFormat.fromUrl("https://a/b"))
    }

    @Test
    fun parseVttCues_readsTimesAndText() {
        val vtt = "WEBVTT\n\n1\n00:00:01.500 --> 00:00:03.000\nLine one\nLine two\n\n01:00.000 --> 01:02.000\nLater\n"
        assertEquals(
            listOf(
                VttCue(1_500, 3_000, "Line one\nLine two"),
                VttCue(60_000, 62_000, "Later"),
            ),
            parseVttCues(vtt),
        )
    }

    @Test
    fun cueAt_returnsNullBetweenCues() {
        val cues = listOf(VttCue(1_000, 2_000, "A"), VttCue(3_000, 4_000, "B"))
        assertEquals("A", cues.cueAt(1_000))
        assertNull(cues.cueAt(2_000))
        assertNull(cues.cueAt(2_500))
        assertEquals("B", cues.cueAt(3_500))
    }
}
