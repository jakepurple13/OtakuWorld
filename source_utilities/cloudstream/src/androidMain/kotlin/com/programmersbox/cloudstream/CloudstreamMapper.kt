package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.DrmExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpmodels.KmpSubtitle
import java.text.DateFormat
import java.util.Date

object CloudstreamMapper {
    /** The episode's `data`, handed back to `loadLinks`. */
    const val CS_DATA = "csData"
    const val HAS_NEXT = "hasNextPage"

    private const val HLS = "application/x-mpegURL"
    private const val DASH = "application/dash+xml"

    fun toItems(results: List<SearchResponse>, hasNext: Boolean, service: CloudstreamApiService): List<KmpItemModel> =
        results.map { toItem(it, service).apply { extras[HAS_NEXT] = hasNext } }

    fun toItem(result: SearchResponse, service: CloudstreamApiService): KmpItemModel = KmpItemModel(
        title = result.name,
        description = "",
        url = result.url,
        imageUrl = result.posterUrl.orEmpty(),
        source = service,
    )

    fun toItem(response: LoadResponse, service: CloudstreamApiService): KmpItemModel = KmpItemModel(
        title = response.name,
        description = response.plot.orEmpty(),
        url = response.url,
        imageUrl = response.posterUrl.orEmpty(),
        source = service,
    )

    fun toInfo(response: LoadResponse, model: KmpItemModel, service: CloudstreamApiService): KmpInfoModel = KmpInfoModel(
        title = response.name.ifBlank { model.title },
        description = response.plot.orEmpty(),
        url = response.url,
        imageUrl = response.posterUrl ?: model.imageUrl,
        chapters = toChapters(response, service),
        genres = response.tags.orEmpty(),
        alternativeNames = (response as? AnimeLoadResponse)?.synonyms.orEmpty(),
        source = service,
    )

    private fun toChapters(response: LoadResponse, service: CloudstreamApiService): List<KmpChapterModel> =
        when (response) {
            is AnimeLoadResponse -> {
                val sub = response.episodes[DubStatus.Subbed].orEmpty()
                val dub = response.episodes[DubStatus.Dubbed].orEmpty()
                val both = sub.isNotEmpty() && dub.isNotEmpty()
                sub.mapIndexed { i, e -> toChapter(e, i, if (both) " (Sub)" else "", response.url, service) } +
                    dub.mapIndexed { i, e -> toChapter(e, i, if (both) " (Dub)" else "", response.url, service) }
            }

            is TvSeriesLoadResponse -> response.episodes.mapIndexed { i, e -> toChapter(e, i, "", response.url, service) }

            is MovieLoadResponse -> listOf(
                KmpChapterModel(
                    name = response.name,
                    url = response.dataUrl,
                    uploaded = "",
                    sourceUrl = response.url,
                    source = service,
                ).apply { extras[CS_DATA] = response.dataUrl }
            )

            else -> emptyList()
        }

    private fun toChapter(
        episode: Episode,
        index: Int,
        suffix: String,
        infoUrl: String,
        service: CloudstreamApiService,
    ): KmpChapterModel = KmpChapterModel(
        name = (episode.name ?: "Episode ${episode.episode ?: (index + 1)}") + suffix,
        url = episode.data,
        uploaded = episode.date?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }.orEmpty(),
        sourceUrl = infoUrl,
        source = service,
    ).apply {
        uploadedTime = episode.date
        extras[CS_DATA] = episode.data
    }

    /** Null for links the player cannot use: DRM, torrent and magnet. */
    fun toStorage(link: ExtractorLink, subtitles: List<SubtitleFile>): KmpStorage? {
        if (link is DrmExtractorLink) return null
        val mimeType = when (link.type) {
            ExtractorLinkType.M3U8 -> HLS
            ExtractorLinkType.DASH -> DASH
            ExtractorLinkType.VIDEO -> null
            ExtractorLinkType.TORRENT, ExtractorLinkType.MAGNET -> return null
        }
        return KmpStorage(
            link = link.url,
            quality = Qualities.getStringByInt(link.quality),
            subtitles = subtitles.map { KmpSubtitle(it.url, it.lang, it.headers.orEmpty()) },
            server = link.source,
            mimeType = mimeType,
        ).apply { headers.putAll(link.getAllHeaders()) }
    }
}
