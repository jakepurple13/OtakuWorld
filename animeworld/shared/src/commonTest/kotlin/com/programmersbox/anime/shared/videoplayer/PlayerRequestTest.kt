package com.programmersbox.anime.shared.videoplayer

import com.programmersbox.kmpmodels.KmpSubtitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PlayerRequestTest {

    @Test
    fun embeddedTrackLabel_names() {
        assertEquals("Embedded", embeddedTrackLabel(null, null))
        assertEquals("Embedded", embeddedTrackLabel("  ", ""))
        assertEquals("Embedded (ja)", embeddedTrackLabel("Unknown", "ja"))
        assertEquals("Eng", embeddedTrackLabel("Eng", "en"))
    }

    @Test
    fun playerHeaders_isCaseInsensitive() {
        val result = playerHeaders(mapOf("Referer" to "https://a/"), "https://b/")
        val referers = result.filterKeys { it.equals("referer", ignoreCase = true) }
        assertEquals(1, referers.size)
        assertEquals("https://a/", referers.values.single())
        assertEquals("Referer", referers.keys.single())
    }

    @Test
    fun playerHeaders_keepsDefaults() {
        val result = playerHeaders(mapOf("X-Custom" to "1"), null)
        DEFAULT_PLAYER_HEADERS.forEach { (k, v) -> assertEquals(v, result[k]) }
        assertEquals("1", result["X-Custom"])
    }

    @Test
    fun playerHeaders_blankRefererOmitted() {
        val blank = playerHeaders(emptyMap(), "  ")
        assertFalse(blank.keys.any { it.equals("referer", ignoreCase = true) })
        val none = playerHeaders(emptyMap(), null)
        assertFalse(none.keys.any { it.equals("referer", ignoreCase = true) })
    }

    @Test
    fun playerHeaders_refererArgAdded() {
        assertEquals("https://b/", playerHeaders(emptyMap(), "https://b/")["referer"])
    }

    @Test
    fun subtitleMime_byExtension() {
        assertEquals("text/vtt", subtitleMime("https://x/en.vtt"))
        assertEquals("application/x-subrip", subtitleMime("https://x/en.srt"))
        assertEquals("text/x-ssa", subtitleMime("https://x/en.ass"))
        assertEquals("text/x-ssa", subtitleMime("https://x/en.ssa"))
        assertEquals("text/vtt", subtitleMime("https://x/en.unknown"))
    }

    @Test
    fun subtitleMime_ignoresQuery() {
        assertEquals("text/vtt", subtitleMime("https://x/en.vtt?token=1"))
        assertEquals("application/x-subrip", subtitleMime("https://x/a.srt#t"))
    }

    private fun subs(vararg langs: String) = langs.map { KmpSubtitle("https://x/$it.vtt", it) }

    @Test
    fun defaultSubtitleIndex_emptyIsNull() {
        assertNull(defaultSubtitleIndex(emptyList(), "en"))
    }

    @Test
    fun defaultSubtitleIndex_localeMatch() {
        assertEquals(2, defaultSubtitleIndex(subs("English", "Spanish", "es-MX"), "ES"))
        assertEquals(1, defaultSubtitleIndex(subs("en", "fr", "de"), "fr"))
    }

    @Test
    fun defaultSubtitleIndex_nameMatch() {
        assertEquals(1, defaultSubtitleIndex(subs("Spanish", "English"), "en"))
        assertEquals(1, defaultSubtitleIndex(subs("Spanish", "English"), "English"))
    }

    @Test
    fun defaultSubtitleIndex_englishFallback() {
        assertEquals(2, defaultSubtitleIndex(subs("Spanish", "Portuguese", "English [CC]"), "ja"))
        assertEquals(1, defaultSubtitleIndex(subs("fr", "en-US"), "ja"))
    }

    private val aniZoneTracks = listOf(
        KmpSubtitle("https://x/0_en.ass", "[BestBob] English Subtitles"),
        KmpSubtitle("https://x/1_en.ass", "English - [BestBob] Signs/Songs"),
        KmpSubtitle("https://x/2_en.ass", "English - [BestBob] Dubtitles"),
        KmpSubtitle("https://x/3_ja.ass", "[Netflix] Japanese Subtitles"),
    )

    @Test
    fun defaultSubtitleIndex_prefersFullDialogueOverSignsSongs() {
        assertEquals(0, defaultSubtitleIndex(aniZoneTracks, "en", "English"))
        assertEquals(0, defaultSubtitleIndex(aniZoneTracks, "en"))
        assertEquals(0, defaultSubtitleIndex(aniZoneTracks, "en-US", "English"))
        assertEquals(3, defaultSubtitleIndex(aniZoneTracks, "ja", "Japanese"))
    }

    @Test
    fun defaultSubtitleIndex_partialOnlyMatchStillPicked() {
        assertEquals(1, defaultSubtitleIndex(subs("Japanese", "English Signs & Songs"), "en", "English"))
        assertEquals(0, defaultSubtitleIndex(subs("Signs & Songs only"), "en", "English"))
    }

    @Test
    fun defaultSubtitleIndex_wholeTokenOnly() {
        assertEquals(1, defaultSubtitleIndex(subs("Estonian", "es"), "es"))
        assertEquals(1, defaultSubtitleIndex(subs("Estonian", "Spanish"), "es", "Spanish"))
        // No Spanish match: falls back to English, not to "Estonian".
        assertEquals(1, defaultSubtitleIndex(subs("Estonian", "English"), "es", "Spanish"))
    }

    @Test
    fun defaultSubtitleIndex_firstFallback() {
        assertEquals(1, defaultSubtitleIndex(subs("Forced", "French"), "ja"))
        assertEquals(0, defaultSubtitleIndex(subs("Spanish", "French"), "ja"))
        assertEquals(0, defaultSubtitleIndex(subs("Spanish", "French"), ""))
    }
}
