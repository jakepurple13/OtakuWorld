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
        assertTrue(s.length <= 120)
        assertEquals("x (2)", uniqueBaseName("x") { it == "x" })
        assertEquals("x (3)", uniqueBaseName("x") { it == "x" || it == "x (2)" })
        assertEquals("x", uniqueBaseName("x") { false })
    }

    @Test
    fun sniffsSubtitleFormat() {
        assertEquals(SubtitleFormat.Vtt, subtitleFormat("https://h/sub?id=3", "WEBVTT\n"))
        assertEquals(SubtitleFormat.Ass, subtitleFormat("https://h/sub?id=3", "[Script Info]"))
        assertEquals(SubtitleFormat.Srt, subtitleFormat("https://h/sub?id=3", "1\n00:00"))
        assertEquals(SubtitleFormat.Ass, subtitleFormat("https://h/a.ass?x=1", ""))
    }

    @Test
    fun sidecarNameFormat() {
        assertEquals("Ep1.0.English.vtt", sidecarName("Ep1", 0, "English", SubtitleFormat.Vtt))
        assertFalse(sidecarName("Ep1", 2, "Br/Pt?", SubtitleFormat.Srt).contains("/"))
    }
}
