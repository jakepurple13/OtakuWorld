package com.programmersbox.aniyomi

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Hoster.Companion.toHosterList
import eu.kanade.tachiyomi.animesource.model.Video
import kotlin.test.Test
import kotlin.test.assertEquals

class VendoredRuntimeTest {

    @Suppress("DEPRECATION")
    @Test
    fun lib14VideoConstructorExists() {
        val video = Video("u", "720p", "https://v")

        assertEquals("720p", video.videoTitle)
        assertEquals("https://v", video.videoUrl)
        assertEquals("u", video.url)
    }

    @Suppress("DEPRECATION")
    @Test
    fun hosterListFromVideos() {
        val video = Video("u", "720p", "https://v")

        val hosters = listOf(video).toHosterList()

        assertEquals(1, hosters.size)
        assertEquals(Hoster.NO_HOSTER_LIST, hosters.single().hosterName)
        assertEquals(listOf(video), hosters.single().videoList)
    }
}
