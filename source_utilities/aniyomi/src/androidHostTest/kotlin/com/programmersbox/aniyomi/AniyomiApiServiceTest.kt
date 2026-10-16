package com.programmersbox.aniyomi

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.PreferenceScreen
import eu.kanade.tachiyomi.animesource.model.Hoster
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class AniyomiApiServiceTest {

    private suspend fun AniyomiApiService.firstEpisode(): KmpChapterModel =
        itemInfo(allList(1).single()).chapters.first()

    @Test
    fun serviceIdentity() {
        val service = aniyomiApiService(FakeAnimeCatalogueSource(), 16.0)
        assertEquals("aniyomi:42", service.serviceName)
        assertEquals("Fake (en)", service.displayName)
        assertEquals("en", service.language)
        assertFalse(service.canDownload)
        assertFalse(service.canCast)
        assertFalse(service is KmpConfigurableApiService)
    }

    @Test
    fun configurableSourceGetsMarker() {
        val configurable = object : AnimeCatalogueSource by FakeAnimeCatalogueSource(), ConfigurableAnimeSource {
            override fun setupPreferenceScreen(screen: PreferenceScreen) = Unit
        }
        assertIs<KmpConfigurableApiService>(aniyomiApiService(configurable, 16.0))
    }

    @Test
    fun lib14_usesEpisodeVideoList() = runTest {
        val source = FakeAnimeHttpSource(episodeVideos = listOf(video("480p"), video("720p")))
        val service = aniyomiApiService(source, 14.0)

        val storages = service.chapterInfo(service.firstEpisode())

        // sortVideos() of the fake sorts by title descending
        assertEquals(listOf("720p", "480p"), storages.map { it.quality })
        assertEquals(listOf(null, null), storages.map { it.server })
        assertEquals(emptyList(), source.hosterVideoCalls)
    }

    @Test
    fun lib16_usesHosters_serverNamesSet() = runTest {
        val source = FakeAnimeHttpSource(
            hosters = listOf(
                Hoster(hosterUrl = "https://h/1", hosterName = "Alpha"),
                Hoster(hosterUrl = "https://h/2", hosterName = "Beta", videoList = listOf(video("b1"))),
            ),
            hosterVideos = mapOf("Alpha" to listOf(video("a1"), video("a2"))),
        )
        val service = aniyomiApiService(source, 16.0)

        val storages = service.chapterInfo(service.firstEpisode())

        assertEquals(listOf("a2", "a1", "b1"), storages.map { it.quality })
        assertEquals(listOf("Alpha", "Alpha", "Beta"), storages.map { it.server })
        // Beta came with its videos, so only Alpha was fetched
        assertEquals(listOf("Alpha"), source.hosterVideoCalls)
    }

    @Test
    fun chapterInfo_skipsFailingHoster() = runTest {
        val source = FakeAnimeHttpSource(
            hosters = listOf(
                Hoster(hosterUrl = "https://h/broken", hosterName = "Broken"),
                Hoster(hosterUrl = "https://h/ok", hosterName = "Ok"),
            ),
            hosterVideos = mapOf("Ok" to listOf(video("ok"))),
        )
        val service = aniyomiApiService(source, 16.0)

        val storages = service.chapterInfo(service.firstEpisode())

        assertEquals(listOf("ok"), storages.map { it.quality })
        assertEquals(listOf("Ok"), storages.map { it.server })
    }

    @Test
    fun lazyHoster_resolvesOnResolve() = runTest {
        val source = FakeAnimeHttpSource(
            hosters = listOf(Hoster(hosterUrl = "https://h/lazy", hosterName = "Lazy", lazy = true)),
            hosterVideos = mapOf("Lazy" to listOf(video("360p"), video("720p"))),
        )
        val service = aniyomiApiService(source, 17.0)

        val placeholder = service.chapterInfo(service.firstEpisode()).single()
        assertEquals("Lazy", placeholder.quality)
        assertEquals("Lazy", placeholder.server)
        assertNull(placeholder.link)
        assertEquals(emptyList(), source.hosterVideoCalls)

        val resolved = service.resolve(placeholder)

        assertEquals("https://cdn.example.org/720p.mp4", resolved.link)
        assertEquals("720p", resolved.quality)
        assertEquals("Lazy", resolved.server)
        assertEquals(listOf("Lazy"), source.hosterVideoCalls)
    }

    @Test
    fun uninitializedVideo_resolvesWithResolveVideo() = runTest {
        val source = FakeAnimeHttpSource(episodeVideos = listOf(video("auto", initialized = false)))
        val service = aniyomiApiService(source, 14.0)

        val storage = service.chapterInfo(service.firstEpisode()).single()
        val resolved = service.resolve(storage)

        assertEquals(listOf("auto"), source.resolvedVideos)
        assertEquals("https://cdn.example.org/resolved.m3u8", resolved.link)
        assertEquals("application/x-mpegURL", resolved.mimeType)
        assertEquals("https://example.org/", resolved.headers["Referer"])
    }

    @Test
    fun initializedVideo_resolveIsNoOp() = runTest {
        val source = FakeAnimeHttpSource(episodeVideos = listOf(video("720p")))
        val service = aniyomiApiService(source, 14.0)

        val storage = service.chapterInfo(service.firstEpisode()).single()
        val resolved = service.resolve(storage)

        assertEquals(emptyList(), source.resolvedVideos)
        assertEquals(storage.link, resolved.link)
    }
}
