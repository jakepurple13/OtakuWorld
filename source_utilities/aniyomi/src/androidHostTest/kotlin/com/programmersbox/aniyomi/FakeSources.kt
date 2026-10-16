package com.programmersbox.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import rx.Observable

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class FakeAnimeCatalogueSource(
    override val supportsLatest: Boolean = true,
    private val episodeVideos: List<Video> = emptyList(),
) : AnimeCatalogueSource {
    override val id = 42L
    override val name = "Fake"
    override val lang = "en"

    override suspend fun getPopularAnime(page: Int) = AnimesPage(emptyList(), hasNextPage = false)
    override suspend fun getLatestUpdates(page: Int) = AnimesPage(emptyList(), hasNextPage = false)
    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList) =
        AnimesPage(emptyList(), hasNextPage = false)

    override suspend fun getVideoList(episode: SEpisode): List<Video> = episodeVideos

    override fun fetchPopularAnime(page: Int): Observable<AnimesPage> = throw UnsupportedOperationException()
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage> =
        throw UnsupportedOperationException()
    override fun fetchLatestUpdates(page: Int): Observable<AnimesPage> = throw UnsupportedOperationException()
}

/**
 * AnimeHttpSource that never touches Injekt/network: overrides headersBuilder and the suspend API.
 * It declares no hoster methods, so it behaves like a lib 14 extension (or a lib 16+ one
 * without hoster support) and answers with [episodeVideos].
 */
@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class FakeAnimeHttpSource(
    private val episodeVideos: List<Video> = emptyList(),
) : AnimeHttpSource() {
    override val name = "FakeHttp"
    override val lang = "en"
    override val supportsLatest = false
    override val baseUrl = "https://example.org"
    var lastDetailsUrl: String? = null
    val resolvedVideos = mutableListOf<String>()
    val videoUrlCalls = mutableListOf<String>()

    override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Referer", "https://example.org/")

    override suspend fun getPopularAnime(page: Int) =
        AnimesPage(listOf(SAnime.create().apply { url = "/a"; title = "A" }), hasNextPage = false)

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        lastDetailsUrl = anime.url
        return SAnime.create().apply { url = anime.url; title = anime.title; description = "Desc" }
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = listOf(
        SEpisode.create().apply { url = "/e/1"; name = "Ep 1"; episode_number = 1f; fillermark = true },
    )

    override suspend fun getVideoList(episode: SEpisode): List<Video> = episodeVideos

    override suspend fun getVideoUrl(video: Video): String {
        videoUrlCalls += video.videoTitle
        return "https://cdn.example.org/${video.videoTitle}-page.mp4"
    }

    override suspend fun resolveVideo(video: Video): Video {
        resolvedVideos += video.videoTitle
        return video.copy(videoUrl = "https://cdn.example.org/resolved.m3u8", initialized = true)
    }

    override fun List<Video>.sortVideos(): List<Video> = sortedByDescending { it.videoTitle }

    override fun popularAnimeRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun popularAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        throw UnsupportedOperationException()
    override fun searchAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()
}

/** A lib 16+ extension with hosters. A hoster missing from [hosterVideos] throws. */
class FakeHosterAnimeHttpSource(
    private val hosters: List<Hoster>,
    private val hosterVideos: Map<String, List<Video>> = emptyMap(),
) : FakeAnimeHttpSource() {
    val hosterVideoCalls = mutableListOf<String>()

    override suspend fun getHosterList(episode: SEpisode): List<Hoster> = hosters

    override suspend fun getVideoList(hoster: Hoster): List<Video> {
        hosterVideoCalls += hoster.hosterName
        return hosterVideos[hoster.hosterName] ?: error("${hoster.hosterName} is down")
    }

    /** Puts hosters named "Preferred" first, to prove sortHosters() is applied. */
    override fun List<Hoster>.sortHosters(): List<Hoster> = sortedByDescending { it.hosterName == "Preferred" }
}

fun video(title: String, url: String = "https://cdn.example.org/$title.mp4", initialized: Boolean = true) =
    Video(videoUrl = url, videoTitle = title, initialized = initialized)
