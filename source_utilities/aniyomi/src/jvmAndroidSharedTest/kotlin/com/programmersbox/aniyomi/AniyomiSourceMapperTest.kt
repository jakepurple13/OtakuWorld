package com.programmersbox.aniyomi

import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpSubtitle
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AniyomiSourceMapperTest {

    @Test
    fun relativeUrlRoundTrip() = runTest {
        val source = FakeAnimeHttpSource()
        val service = aniyomiApiService(source, 16.0)

        val item = service.allList(1).single()
        assertEquals("https://example.org/a", item.url)
        assertEquals("/a", item.extras[AniyomiSourceMapper.URL])
        assertEquals(false, item.extras[AniyomiSourceMapper.HAS_NEXT])

        val info = service.itemInfo(item)
        assertEquals("/a", source.lastDetailsUrl)
        assertEquals("Desc", info.description)
        val episode = info.chapters.single()
        assertEquals("https://example.org/e/1", episode.url)
        assertEquals("/e/1", episode.extras[AniyomiSourceMapper.URL])
        assertEquals(1f, episode.extras["episodeNumber"])
        assertEquals(true, episode.extras["filler"])
        assertEquals(info.url, episode.sourceUrl)
        assertEquals("/e/1", AniyomiSourceMapper.toSEpisode(episode, service).url)

        // Item reloaded from the DB has no extras: the base url is stripped instead.
        service.itemInfo(KmpItemModel("A", "", "https://example.org/a", "", service))
        assertEquals("/a", source.lastDetailsUrl)
    }

    @Test
    fun videoWithoutHeaders_usesSourceHeaders() {
        val source = FakeAnimeHttpSource()
        val storage = AniyomiSourceMapper.toStorage(video("720p"), source, server = "S")
        assertEquals(mapOf("Referer" to "https://example.org/"), storage.headers.toMap())

        val own = AniyomiSourceMapper.toStorage(
            Video(videoUrl = "https://v/x.mp4", videoTitle = "x", headers = headersOf("Origin", "https://o")),
            source,
            server = null,
        )
        assertEquals(mapOf("Origin" to "https://o"), own.headers.toMap())
    }

    @Test
    fun subtitleLabelsUnescaped() {
        val storage = AniyomiSourceMapper.toStorage(
            Video(
                videoUrl = "https://v/a.mp4",
                videoTitle = "720p",
                subtitleTracks = listOf(Track("https://s/en.vtt", "English - [BestBob] Signs\\/Songs")),
            ),
            FakeAnimeHttpSource(),
            server = null,
        )
        assertEquals("English - [BestBob] Signs/Songs", storage.subtitles.single().lang)
    }

    @Test
    fun subtitlesMapped() {
        val storage = AniyomiSourceMapper.toStorage(
            Video(
                videoUrl = "https://v/master.m3u8?token=1",
                videoTitle = "1080p",
                subtitleTracks = listOf(Track("https://s/en.vtt", "English"), Track("https://s/es.vtt", "Spanish")),
            ),
            FakeAnimeHttpSource(),
            server = "Main",
        )

        assertEquals(
            listOf(KmpSubtitle("https://s/en.vtt", "English"), KmpSubtitle("https://s/es.vtt", "Spanish")),
            storage.subtitles,
        )
        assertEquals("https://v/master.m3u8?token=1", storage.link)
        assertEquals("1080p", storage.quality)
        assertEquals("Main", storage.server)
        assertEquals("application/x-mpegURL", storage.mimeType)
        assertNull(AniyomiSourceMapper.toStorage(video("mp4"), FakeAnimeHttpSource(), null).mimeType)
    }

    @Test
    fun hlsDetectedFromQueryOrMixedCase() {
        fun mime(url: String) = AniyomiSourceMapper.toStorage(
            Video(videoUrl = url, videoTitle = "q"), FakeAnimeHttpSource(), null,
        ).mimeType
        assertEquals("application/x-mpegURL", mime("https://v/play?file=/a/master.M3U8&t=1"))
        assertEquals("application/x-mpegURL", mime("https://v/hls/index.m3u8/seg"))
        assertNull(mime("https://v/video.mp4"))
    }
}
