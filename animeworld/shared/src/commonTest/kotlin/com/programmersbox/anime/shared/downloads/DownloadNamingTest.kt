package com.programmersbox.anime.shared.downloads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadNamingTest {
    @Test
    fun sanitizesAndDeduplicatesFileNames() {
        val s = sanitizeFileName("Episode 1: Conspiracy/Part?*" + "x".repeat(200))
        assertTrue(s.none { it in "/:?*" })
        assertTrue(s.length <= 180)
        assertEquals("x (2)", uniqueBaseName("x") { it == "x" })
        assertEquals("x (3)", uniqueBaseName("x") { it == "x" || it == "x (2)" })
        assertEquals("x", uniqueBaseName("x") { false })
    }

    @Test
    fun limitsUtf8BytesWithoutSplittingChars() {
        val jp = sanitizeFileName("進撃の巨人 第一話 ".repeat(30))
        assertTrue(jp.encodeToByteArray().size <= 180)
        assertEquals(jp, jp.encodeToByteArray().decodeToString())
        val emoji = sanitizeFileName("\uD83D\uDE00".repeat(100), 10)
        assertEquals("\uD83D\uDE00\uD83D\uDE00", emoji)
        assertEquals("a", sanitizeFileName("a. . ", 20))
        assertEquals("a b", sanitizeFileName("a\u007Fb"))
    }

    @Test
    fun sniffsSubtitleFormat() {
        assertEquals(SubtitleFormat.Vtt, subtitleFormat("https://h/sub?id=3", "WEBVTT\n"))
        assertEquals(SubtitleFormat.Ass, subtitleFormat("https://h/sub?id=3", "[Script Info]"))
        assertEquals(SubtitleFormat.Srt, subtitleFormat("https://h/sub?id=3", "1\n00:00"))
        assertEquals(SubtitleFormat.Ass, subtitleFormat("https://h/a.ass?x=1", ""))
        assertEquals(SubtitleFormat.Ass, subtitleFormat("https://h/a.ssa", ""))
    }

    @Test
    fun sidecarNameFormat() {
        assertEquals("Ep1.0.English.vtt", sidecarName("Ep1", 0, "English", SubtitleFormat.Vtt))
        assertFalse(sidecarName("Ep1", 2, "Br/Pt?", SubtitleFormat.Srt).contains("/"))
    }

    @Test
    fun containersShareNoBase() {
        assertEquals("x (2)", uniqueBaseName("x") { isBaseTaken(it) { f -> f == "x.mp4" } })
        assertEquals("x (2)", uniqueBaseName("x") { isBaseTaken(it) { f -> f == "x.ts.part" } })
        assertEquals("x", uniqueBaseName("x") { isBaseTaken(it) { false } })
    }
}
