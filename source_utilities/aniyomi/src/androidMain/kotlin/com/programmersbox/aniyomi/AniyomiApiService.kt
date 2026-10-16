package com.programmersbox.aniyomi

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Hoster.Companion.toHosterList
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.ResolvableAnimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

fun aniyomiApiService(source: AnimeCatalogueSource, lib: Double): AniyomiApiService =
    if (source is ConfigurableAnimeSource) ConfigurableAniyomiApiService(source, lib) else AniyomiApiService(source, lib)

open class AniyomiApiService(val source: AnimeCatalogueSource, val lib: Double) : KmpApiService {
    override val baseUrl: String get() = (source as? AnimeHttpSource)?.baseUrl.orEmpty()
    override val serviceName: String = "aniyomi:${source.id}"
    override val canScroll: Boolean = true
    override val canDownload: Boolean = false
    override val canCast: Boolean = false
    override val displayName: String = "${source.name} (${source.lang})"
    override val language: String = source.lang

    /** Lazy hosters handed out as placeholders, keyed by [HOSTER] in the storage extras. */
    private val lazyHosters = ConcurrentHashMap<String, Hoster>()

    /** Videos that still need [AnimeHttpSource.resolveVideo] or a local http server, keyed by [VIDEO]. */
    private val pendingVideos = ConcurrentHashMap<String, Video>()
    private val nextVideoKey = AtomicLong()

    override suspend fun recent(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        val result = if (source.supportsLatest) source.getLatestUpdates(page) else source.getPopularAnime(page)
        AniyomiSourceMapper.toItems(result, this@AniyomiApiService)
    }

    override suspend fun allList(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        AniyomiSourceMapper.toItems(source.getPopularAnime(page), this@AniyomiApiService)
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> =
        withContext(Dispatchers.IO) {
            AniyomiSourceMapper.toItems(
                source.getSearchAnime(page, searchText.toString(), source.getFilterList()),
                this@AniyomiApiService,
            )
        }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel = withContext(Dispatchers.IO) {
        val anime = AniyomiSourceMapper.toSAnime(model, this@AniyomiApiService)
        // Fetches details and episodes concurrently (or the lib 17 combined call).
        val update = source.getAnimeEpisodeUpdate(anime, emptyList(), fetchDetails = true, fetchEpisodes = true)
        AniyomiSourceMapper.toInfo(update, anime, model, this@AniyomiApiService)
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> = withContext(Dispatchers.IO) {
        val episode = AniyomiSourceMapper.toSEpisode(chapterModel, this@AniyomiApiService)
        // Only the latest episode's picks can be resolved; drop the previous ones.
        lazyHosters.clear()
        pendingVideos.clear()
        hostersFor(episode).flatMap { hoster ->
            val server = hoster.hosterName.takeUnless { it == Hoster.NO_HOSTER_LIST }
            if (hoster.lazy) {
                listOf(placeholder(hoster, server))
            } else {
                try {
                    videosFor(hoster).map { toStorage(it, server) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "${source.name}: hoster ${hoster.hosterName} failed" }
                    emptyList()
                }
            }
        }
    }

    override suspend fun resolve(storage: KmpStorage): KmpStorage = withContext(Dispatchers.IO) {
        val hoster = storage.extras[HOSTER]?.let { lazyHosters[it] }
        val video = when {
            hoster != null -> videosFor(hoster).firstOrNull() ?: error("${hoster.hosterName} returned no videos")
            else -> storage.extras[VIDEO]?.let { pendingVideos[it] } ?: return@withContext storage
        }
        AniyomiSourceMapper.toStorage(playable(video), source, storage.server)
    }

    override suspend fun sourceByUrl(url: String): KmpItemModel = withContext(Dispatchers.IO) {
        val resolvable = source as? ResolvableAnimeSource ?: error("${source.name} cannot open URLs")
        val anime = resolvable.getAnime(url) ?: error("${source.name} could not resolve $url")
        AniyomiSourceMapper.toItem(anime, this@AniyomiApiService)
            ?: error("${source.name} returned an anime without a url for $url")
    }

    /**
     * Mirrors Aniyomi's `EpisodeLoader.getHostersOnHttp`: extensions that do not implement hosters
     * (lib 14, or lib 16+ without overrides) get their episode videos wrapped in a single hoster.
     */
    @Suppress("DEPRECATION")
    private suspend fun hostersFor(episode: SEpisode): List<Hoster> {
        val http = source as? AnimeHttpSource
        return when {
            // AnimeCatalogueSource's own getHosterList default recurses forever.
            http == null -> source.getVideoList(episode).toHosterList()
            lib >= 16 && hasHosters(http) -> http.run { getHosterList(episode).sortHosters() }
            else -> source.getVideoList(episode).sorted().toHosterList()
        }
    }

    /** Upstream `EpisodeLoader.checkHasHosters`: a hoster method declared below [AnimeHttpSource]. */
    private fun hasHosters(source: AnimeHttpSource): Boolean {
        var current: Class<*>? = source.javaClass
        while (current != null &&
            current != ParsedAnimeHttpSource::class.java &&
            current != AnimeHttpSource::class.java
        ) {
            if (current.declaredMethods.any { it.name in HOSTER_METHODS }) return true
            current = current.superclass
        }
        return false
    }

    private suspend fun videosFor(hoster: Hoster): List<Video> =
        (hoster.videoList ?: source.getVideoList(hoster)).sorted()

    /** Keeps the extension's preferred order (quality/server preferences). */
    private fun List<Video>.sorted(): List<Video> =
        (source as? AnimeHttpSource)?.run { this@sorted.sortVideos() } ?: this

    private fun placeholder(hoster: Hoster, server: String?): KmpStorage {
        val key = hoster.hosterUrl.ifEmpty { hoster.hosterName }
        lazyHosters[key] = hoster
        return KmpStorage(quality = hoster.hosterName, server = server).apply { extras[HOSTER] = key }
    }

    private fun toStorage(video: Video, server: String?): KmpStorage =
        AniyomiSourceMapper.toStorage(video, source, server).apply {
            if (needsResolve(video) || video.usesHttpServer()) {
                val key = nextVideoKey.incrementAndGet().toString()
                pendingVideos[key] = video
                extras[VIDEO] = key
            }
        }

    private fun needsResolve(video: Video) = !video.initialized || video.videoUrl.isEmpty() || hasNullUrl(video)

    /** Lib 14 videos built with a null url carry the literal "null" until `getVideoUrl` fills it. */
    private fun hasNullUrl(video: Video) = video.videoUrl == "null"

    private suspend fun playable(video: Video): Video {
        val http = source as? AnimeHttpSource ?: return video
        var result = video
        // Upstream `EpisodeLoader.parseVideoUrls`.
        if (hasNullUrl(result)) result = result.copy(videoUrl = http.getVideoUrl(result))
        if (!result.initialized || result.videoUrl.isEmpty()) {
            result = http.resolveVideo(result) ?: error("${source.name} could not resolve ${video.videoTitle}")
        }
        if (result.usesHttpServer()) result = result.copyHttpServer(AniyomiHttpServers.portFor(http))
        return result
    }

    companion object {
        const val HOSTER = "aniyomiHoster"
        const val VIDEO = "aniyomiVideo"
        private val HOSTER_METHODS = setOf("getHosterList", "hosterListRequest", "hosterListParse")
    }
}

class ConfigurableAniyomiApiService(source: AnimeCatalogueSource, lib: Double) :
    AniyomiApiService(source, lib), KmpConfigurableApiService
