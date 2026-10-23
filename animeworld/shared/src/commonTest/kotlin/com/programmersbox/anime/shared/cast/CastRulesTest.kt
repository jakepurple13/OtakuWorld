package com.programmersbox.anime.shared.cast

import com.programmersbox.anime.shared.subtitles.SubtitleFont
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CastRulesTest {

    @Test
    fun mkvByExtensionBlocked() {
        assertEquals(CastCompat.Blocked, castCompatibility("https://a/v.mkv?x=1", null))
    }

    @Test
    fun mkvByMimeBlocked() {
        assertEquals(CastCompat.Blocked, castCompatibility("https://a/v", "video/x-matroska"))
        assertEquals(CastCompat.Blocked, castCompatibility("https://a/v", "video/mkv"))
    }

    @Test
    fun mp4Ok() {
        assertEquals(CastCompat.Ok, castCompatibility("https://a/v.mp4", "video/mp4"))
    }

    @Test
    fun contentTypeFromMimeWins() {
        assertEquals("video/mp4", castContentType("https://a/v.m3u8", "video/mp4"))
    }

    @Test
    fun contentTypeFromM3u8() {
        assertEquals("application/x-mpegURL", castContentType("https://a/v.m3u8?t=1", null))
        assertEquals("application/dash+xml", castContentType("https://a/v.mpd", null))
        assertEquals("video/webm", castContentType("https://a/v.webm", null))
        assertEquals("video/mp4", castContentType("https://a/v", null))
    }

    @Test
    fun hlsAlwaysProxied() {
        assertTrue(needsProxy("https://a/v.m3u8", null, emptyMap()))
        assertTrue(needsProxy("https://a/v", "application/x-mpegURL", emptyMap()))
    }

    @Test
    fun mp4WithoutHeadersNotProxied() {
        assertFalse(needsProxy("https://a/v.mp4", null, emptyMap()))
    }

    @Test
    fun mp4WithRefererProxied() {
        assertTrue(needsProxy("https://a/v.mp4", null, mapOf("referer" to "https://a/")))
    }

    @Test
    fun progressiveTsBlockedByExtensionAndMime() {
        assertEquals(CastCompat.Blocked, castCompatibility("https://a/v.ts?x=1", null))
        assertEquals(CastCompat.Blocked, castCompatibility("https://a/v", "video/MP2T"))
    }

    @Test
    fun hlsStillAllowed() {
        assertEquals(CastCompat.Ok, castCompatibility("https://a/index.m3u8", null))
        assertEquals(CastCompat.Ok, castCompatibility("https://a/v", "application/x-mpegURL"))
    }

    @Test
    fun sourceHeadersExcludePlayerDefaults() {
        assertEquals(emptyMap<String, String>(), castSourceHeaders(emptyMap(), null))
        // A source name in the referer slot is not a referer.
        assertEquals(emptyMap<String, String>(), castSourceHeaders(emptyMap(), "Vidstream"))
        assertFalse(needsProxy("https://a/v.mp4", null, castSourceHeaders(emptyMap(), "")))
    }

    @Test
    fun sourceHeadersAddUrlReferer() {
        assertEquals(mapOf("referer" to "https://r/"), castSourceHeaders(emptyMap(), "https://r/"))
        assertEquals(mapOf("Referer" to "https://a/"), castSourceHeaders(mapOf("Referer" to "https://a/"), "https://r/"))
        assertTrue(needsProxy("https://a/v.mp4", null, castSourceHeaders(emptyMap(), "https://r/")))
    }

    @Test
    fun ownTrackByServerUrlElseIdRange() {
        assertTrue(isOwnTrack(1, "http://192.168.1.2:4000/sub/1.vtt", "http://192.168.1.2:4000"))
        assertFalse(isOwnTrack(OWN_TRACK_BASE, "https://cdn/a.vtt", "http://192.168.1.2:4000"))
        assertTrue(isOwnTrack(OWN_TRACK_BASE + 2, null, "http://192.168.1.2:4000"))
        assertFalse(isOwnTrack(3, "http://192.168.1.2:4000/sub/1.vtt", null))
    }

    @Test
    fun closestMatchesIsoPrefix() {
        val subs = listOf("Español", "English (Signs & Songs)", "English").map { KmpSubtitle("https://a/$it", it) }
        assertEquals(2, closestSubtitleIndex("en", null, subs, languageName = "English"))
    }

    @Test
    fun closestMatchesIsoCodeWithRegion() {
        val subs = listOf("ja", "en-US").map { KmpSubtitle("https://a/$it", it) }
        assertEquals(1, closestSubtitleIndex("en", null, subs))
    }

    @Test
    fun closestNullWhenNoLanguage() {
        val subs = listOf("English").map { KmpSubtitle("https://a/x", it) }
        assertNull(closestSubtitleIndex(null, null, subs))
        assertNull(closestSubtitleIndex("fr", "French", subs))
    }

    @Test
    fun styleMapsSizeAndBackground() {
        val style = SubtitleStyle(sizePercent = 150, backgroundPercent = 50).toCastTextStyle()
        assertEquals(1.5f, style.fontScale)
        assertEquals(0x80000000L, style.backgroundArgb)
    }

    @Test
    fun styleMapsFonts() {
        fun font(f: SubtitleFont) = SubtitleStyle(font = f).toCastTextStyle().font
        assertEquals(CastFont.SansSerif, font(SubtitleFont.Default))
        assertEquals(CastFont.SansSerif, font(SubtitleFont.Bold))
        assertEquals(CastFont.Serif, font(SubtitleFont.Serif))
        assertEquals(CastFont.Monospace, font(SubtitleFont.Mono))
    }
}
