package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VendoredLibraryTest {

    // Older plugins still call these constructors; compiling this call is the real assertion.
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    @Test
    fun deprecatedConstructorsCallable() {
        val link = ExtractorLink(
            "s", "n", "https://x/a.mp4", "r", Qualities.P720.value, mapOf(), null, ExtractorLinkType.VIDEO, emptyList()
        )
        val oldLink = ExtractorLink("s", "n", "https://x/a.m3u8", "r", Qualities.P720.value, true)
        val typedLink = ExtractorLink("s", "n", "https://x/a.mp4", "r", Qualities.P720.value, ExtractorLinkType.VIDEO)
        val episode = Episode("data", "name", 1, 2)
        val subtitle = SubtitleFile("en", "https://x/s.vtt")

        assertEquals("https://x/a.mp4", link.url)
        assertEquals(ExtractorLinkType.M3U8, oldLink.type)
        assertEquals("https://x/a.mp4", typedLink.url)
        assertEquals(2, episode.episode)
        assertEquals("en", subtitle.lang)
    }

    @Test
    fun newExtractorLinkBuildsM3u8() = runTest {
        val link = newExtractorLink("s", "n", "https://x/a.m3u8", ExtractorLinkType.M3U8) {
            referer = "r"
        }

        assertEquals(ExtractorLinkType.M3U8, link.type)
        assertEquals("r", link.getAllHeaders()["referer"])
    }
}
