package com.programmersbox.anime.shared.videoplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PlayerRequestTest {

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
}
