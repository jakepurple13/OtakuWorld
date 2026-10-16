package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newDrmExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudstreamApiServiceTest {

    private fun service(api: FakeMainApi) = CloudstreamApiService(api, "fakeplugin", "cloudstream:${api.name}")

    private suspend fun CloudstreamApiService.info(): KmpInfoModel = itemInfo(recent(1).first())

    private suspend fun CloudstreamApiService.firstEpisode(): KmpChapterModel = info().chapters.first()

    private fun withLinks(
        links: suspend (String, (SubtitleFile) -> Unit, (ExtractorLink) -> Unit) -> Unit,
    ) = service(
        FakeMainApi(
            loadResponse = { url -> newMovieLoadResponse("Movie", url, TvType.AnimeMovie, "https://fake.test/play") },
            links = links,
        )
    )

    private suspend fun link(url: String, type: ExtractorLinkType, quality: Int = Qualities.P720.value) =
        newExtractorLink("Server", "Vidstream", url, type) {
            this.quality = quality
            referer = "https://fake.test/"
        }

    @Test
    fun serviceIdentity() {
        val service = service(FakeMainApi(name = "Fake", lang = "ja"))
        assertEquals("cloudstream:Fake", service.serviceName)
        assertEquals("Fake (ja)", service.displayName)
        assertEquals("ja", service.language)
        assertEquals("https://fake.test", service.baseUrl)
        assertTrue(service.canScroll)
        assertFalse(service.canDownload)
        assertFalse(service.canCast)
        assertFalse(service is KmpConfigurableApiService)
        assertIs<KmpConfigurableApiService>(
            ConfigurableCloudstreamApiService(FakeMainApi(), "fakeplugin", "cloudstream:Fake") {}
        )
    }

    @Test
    fun nameCollision_getsDistinctServiceNames() {
        val names = serviceNamesFor(
            listOf(
                "pluginA" to FakeMainApi(name = "Same"),
                "pluginB" to FakeMainApi(name = "Same"),
                "pluginC" to FakeMainApi(name = "Other"),
            )
        )

        assertEquals(listOf("cloudstream:pluginA:Same", "cloudstream:pluginB:Same", "cloudstream:Other"), names)
    }

    @Test
    fun recent_usesFirstMainPageEntry() = runTest {
        val api = FakeMainApi(
            mainPage = listOf(
                MainPageData("Latest", "https://fake.test/latest"),
                MainPageData("Popular", "https://fake.test/popular"),
            )
        )

        val items = service(api).recent(2)

        assertEquals(listOf("Latest-2"), items.map { it.title })
        assertEquals("https://fake.test/Latest-2.png", items.single().imageUrl)
        assertEquals(listOf("https://fake.test/latest"), api.mainPageRequests.map { it.data })
        assertFalse(api.mainPageRequests.single().horizontalImages)
    }

    @Test
    fun recent_emptyMainPage_returnsEmptyWithoutSearching() = runTest {
        val api = FakeMainApi(mainPage = emptyList())

        assertEquals(emptyList(), service(api).recent(1))
        assertEquals(emptyList(), api.searchQueries)
    }

    @Test
    fun subAndDub_bothListed() = runTest {
        val service = service(
            FakeMainApi(loadResponse = { url ->
                newAnimeLoadResponse("Show", url, TvType.Anime) {
                    // Dub added first: sub must still come first.
                    addEpisodes(DubStatus.Dubbed, listOf(newEpisode("https://fake.test/d1") { name = "Ep 1" }))
                    addEpisodes(
                        DubStatus.Subbed,
                        listOf(
                            newEpisode("https://fake.test/s1") { name = "Ep 1" },
                            newEpisode("https://fake.test/s2") { episode = 2 },
                        )
                    )
                }
            })
        )

        val chapters = service.info().chapters

        // Newest first within each block; sub block before dub block.
        assertEquals(listOf("Episode 2 (Sub)", "Ep 1 (Sub)", "Ep 1 (Dub)"), chapters.map { it.name })
        assertEquals(
            listOf("https://fake.test/s2", "https://fake.test/s1", "https://fake.test/d1"),
            chapters.map { it.extras[CloudstreamMapper.CS_DATA] },
        )
    }

    @Test
    fun subOnly_noSuffix() = runTest {
        val service = service(
            FakeMainApi(loadResponse = { url ->
                newAnimeLoadResponse("Show", url, TvType.Anime) {
                    addEpisodes(DubStatus.Subbed, listOf(newEpisode("https://fake.test/s1") { name = "Ep 1" }))
                }
            })
        )

        assertEquals(listOf("Ep 1"), service.info().chapters.map { it.name })
    }

    @Test
    fun movieBecomesOneEpisode() = runTest {
        val service = service(
            FakeMainApi(loadResponse = { url ->
                newMovieLoadResponse("The Movie", url, TvType.AnimeMovie, "{\"id\":7}") {
                    plot = "Plot"
                    tags = listOf("Action")
                }
            })
        )

        val info = service.info()

        assertEquals("The Movie", info.title)
        assertEquals("Plot", info.description)
        assertEquals(listOf("Action"), info.genres)
        val chapter = info.chapters.single()
        assertEquals("The Movie", chapter.name)
        assertEquals("{\"id\":7}", chapter.extras[CloudstreamMapper.CS_DATA])
    }

    @Test
    fun concurrentCallbacks_allCollected() = runTest {
        val service = withLinks { _, _, callback ->
            coroutineScope {
                repeat(50) { i ->
                    launch(Dispatchers.Default) { callback(link("https://fake.test/$i.mp4", ExtractorLinkType.VIDEO)) }
                }
            }
        }

        val storages = service.chapterInfo(service.firstEpisode())

        assertEquals(50, storages.size)
        assertEquals(50, storages.map { it.link }.toSet().size)
    }

    @Test
    fun drmAndTorrentDropped() = runTest {
        val service = withLinks { _, _, callback ->
            callback(link("https://fake.test/ok.mp4", ExtractorLinkType.VIDEO))
            callback(link("https://fake.test/a.torrent", ExtractorLinkType.TORRENT))
            callback(link("magnet:?xt=urn:btih:abc", ExtractorLinkType.MAGNET))
            callback(
                newDrmExtractorLink("Server", "Drm", "https://fake.test/drm.mpd", ExtractorLinkType.DASH, uuid = UUID.randomUUID())
            )
        }

        val storages = service.chapterInfo(service.firstEpisode())

        assertEquals(listOf("https://fake.test/ok.mp4"), storages.map { it.link })
    }

    @Test
    fun subtitlesAttachedToEveryLink() = runTest {
        val service = withLinks { _, subtitle, callback ->
            subtitle(newSubtitleFile("English", "https://fake.test/en.vtt") { headers = mapOf("X" to "1") })
            callback(link("https://fake.test/1.mp4", ExtractorLinkType.VIDEO))
            callback(link("https://fake.test/2.mp4", ExtractorLinkType.VIDEO))
            // Subtitles found after a link still belong to it.
            subtitle(newSubtitleFile("Spanish", "https://fake.test/es.vtt"))
        }

        val storages = service.chapterInfo(service.firstEpisode())

        assertEquals(2, storages.size)
        storages.forEach { storage ->
            assertEquals(listOf("English", "Spanish"), storage.subtitles.map { it.lang })
            assertEquals(mapOf("X" to "1"), storage.subtitles.first().headers)
        }
    }

    @Test
    fun m3u8MimeSet() = runTest {
        val service = withLinks { data, _, callback ->
            assertEquals("https://fake.test/play", data)
            callback(link("https://fake.test/a.m3u8", ExtractorLinkType.M3U8, Qualities.P1080.value))
            callback(link("https://fake.test/a.mpd", ExtractorLinkType.DASH))
            callback(link("https://fake.test/a.mp4", ExtractorLinkType.VIDEO, Qualities.Unknown.value))
        }

        val storages = service.chapterInfo(service.firstEpisode())

        assertEquals(listOf("application/x-mpegURL", "application/dash+xml", null), storages.map { it.mimeType })
        assertEquals(listOf("Vidstream - 1080p", "Vidstream - 720p", "Vidstream"), storages.map { it.quality })
        assertEquals("Server", storages.first().server)
        assertEquals("https://fake.test/", storages.first().headers["referer"])
    }

    @Test
    fun chapterInfo_loadLinksFailure_keepsCollectedLinks() = runTest {
        val service = withLinks { _, _, callback ->
            callback(link("https://fake.test/ok.mp4", ExtractorLinkType.VIDEO))
            throw NotImplementedError()
        }

        assertEquals(listOf("https://fake.test/ok.mp4"), service.chapterInfo(service.firstEpisode()).map { it.link })
    }

    @Test
    fun itemInfo_nullLoad_fails() = runTest {
        val service = service(FakeMainApi(loadResponse = { null }))
        val item = service.recent(1).first()

        val result = runCatching { service.itemInfo(item) }

        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertNull(result.getOrNull())
    }
}
