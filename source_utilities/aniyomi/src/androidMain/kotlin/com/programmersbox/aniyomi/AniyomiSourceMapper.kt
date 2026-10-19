package com.programmersbox.aniyomi

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.kmpmodels.cleanSubtitleLabel
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Headers
import java.text.DateFormat
import java.util.Date

object AniyomiSourceMapper {
    const val URL = "aniyomiUrl"
    const val MEMO = "aniyomiMemo"
    const val HAS_NEXT = "hasNextPage"
    const val EPISODE_NUMBER = "episodeNumber"
    const val FILLER = "filler"

    private const val HLS = "application/x-mpegURL"

    private val json = Json { ignoreUnknownKeys = true }

    fun absolute(baseUrl: String, path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path else baseUrl + path

    fun relative(baseUrl: String, url: String): String =
        if (baseUrl.isNotEmpty() && url.startsWith(baseUrl)) url.removePrefix(baseUrl) else url

    /** `lateinit` properties on SAnime/SEpisode throw when an extension never set them. */
    private inline fun <T> safe(block: () -> T): T? =
        try {
            block()
        } catch (e: UninitializedPropertyAccessException) {
            null
        }

    fun toItems(page: AnimesPage, service: AniyomiApiService): List<KmpItemModel> =
        page.animes.mapNotNull { toItem(it, service)?.apply { extras[HAS_NEXT] = page.hasNextPage } }

    fun toItem(anime: SAnime, service: AniyomiApiService): KmpItemModel? {
        val url = safe { anime.url } ?: return null
        return KmpItemModel(
            title = safe { anime.title }.orEmpty(),
            description = anime.description.orEmpty(),
            url = absolute(service.baseUrl, url),
            imageUrl = anime.thumbnail_url.orEmpty(),
            source = service,
        ).apply {
            extras[URL] = url
            if (anime.memo.isNotEmpty()) extras[MEMO] = anime.memo.toString()
        }
    }

    fun toSAnime(model: KmpItemModel, service: AniyomiApiService): SAnime = SAnime.create().apply {
        url = model.extras[URL] as? String ?: relative(service.baseUrl, model.url)
        title = model.title
        thumbnail_url = model.imageUrl.ifEmpty { null }
        (model.extras[MEMO] as? String)?.let { memo = json.decodeFromString<JsonObject>(it) }
    }

    fun toInfo(update: SAnimeEpisodeUpdate, requested: SAnime, model: KmpItemModel, service: AniyomiApiService): KmpInfoModel {
        val anime = requested.copy().apply {
            val updated = update.anime
            safe { updated.url }?.takeIf { it.isNotBlank() }?.let { url = it }
            safe { updated.title }?.takeIf { it.isNotBlank() }?.let { title = it }
            updated.description?.let { description = it }
            updated.genre?.let { genre = it }
            updated.author?.let { author = it }
            updated.artist?.let { artist = it }
            updated.thumbnail_url?.let { thumbnail_url = it }
            status = updated.status
            if (updated.memo.isNotEmpty()) memo = updated.memo
        }
        val infoUrl = absolute(service.baseUrl, anime.url)
        return KmpInfoModel(
            title = safe { anime.title }?.takeIf { it.isNotBlank() } ?: model.title,
            description = anime.description.orEmpty(),
            url = infoUrl,
            imageUrl = anime.thumbnail_url ?: model.imageUrl,
            chapters = update.episodes.mapNotNull { toEpisode(it, infoUrl, service) },
            genres = anime.getGenres().orEmpty(),
            alternativeNames = emptyList(),
            source = service,
        ).apply {
            extras[URL] = anime.url
            anime.author?.let { extras["author"] = it }
            anime.artist?.let { extras["artist"] = it }
            extras["status"] = statusName(anime.status)
        }
    }

    fun toEpisode(episode: SEpisode, infoUrl: String, service: AniyomiApiService): KmpChapterModel? {
        val url = safe { episode.url } ?: return null
        val time = episode.date_upload.takeIf { it > 0 }
        return KmpChapterModel(
            name = safe { episode.name }.orEmpty(),
            url = absolute(service.baseUrl, url),
            uploaded = time?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }.orEmpty(),
            sourceUrl = infoUrl,
            source = service,
        ).apply {
            uploadedTime = time
            extras[URL] = url
            extras[EPISODE_NUMBER] = episode.episode_number
            extras[FILLER] = episode.fillermark
            if (episode.memo.isNotEmpty()) extras[MEMO] = episode.memo.toString()
        }
    }

    fun toSEpisode(model: KmpChapterModel, service: AniyomiApiService): SEpisode = SEpisode.create().apply {
        url = model.extras[URL] as? String ?: relative(service.baseUrl, model.url)
        name = model.name
        (model.extras[EPISODE_NUMBER] as? Float)?.let { episode_number = it }
        (model.extras[FILLER] as? Boolean)?.let { fillermark = it }
        (model.extras[MEMO] as? String)?.let { memo = json.decodeFromString<JsonObject>(it) }
    }

    fun toStorage(video: Video, source: AnimeCatalogueSource, server: String?): KmpStorage =
        KmpStorage(
            // Lib 14 videos with a deferred url hold the literal "null" until resolved.
            link = video.videoUrl.takeUnless { it == "null" },
            quality = video.videoTitle,
            subtitles = video.subtitleTracks.map { KmpSubtitle(it.url, cleanSubtitleLabel(it.lang)) },
            server = server,
            mimeType = HLS.takeIf { isHls(video.videoUrl) },
        ).apply {
            (video.headers ?: (source as? AnimeHttpSource)?.headers)?.let { headers.putAll(it.toMap()) }
        }

    private fun isHls(url: String): Boolean =
        url.substringBefore('#').substringBefore('?').endsWith(".m3u8", ignoreCase = true) ||
                url.contains("m3u8", ignoreCase = true)

    private fun Headers.toMap(): Map<String, String> = names().associateWith { get(it).orEmpty() }

    private fun statusName(status: Int) = when (status) {
        SAnime.ONGOING -> "Ongoing"
        SAnime.COMPLETED -> "Completed"
        SAnime.LICENSED -> "Licensed"
        SAnime.PUBLISHING_FINISHED -> "Publishing finished"
        SAnime.CANCELLED -> "Cancelled"
        SAnime.ON_HIATUS -> "On hiatus"
        SAnime.UPCOMING -> "Upcoming"
        else -> "Unknown"
    }
}
