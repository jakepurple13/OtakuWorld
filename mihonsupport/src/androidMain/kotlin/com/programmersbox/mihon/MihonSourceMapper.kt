package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Headers
import okhttp3.Request
import java.text.DateFormat
import java.util.Date

object MihonSourceMapper {
    const val URL = "mihonUrl"
    const val MEMO = "mihonMemo"
    const val HAS_NEXT = "hasNextPage"

    private val json = Json { ignoreUnknownKeys = true }

    fun absolute(baseUrl: String, path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path else baseUrl + path

    fun relative(baseUrl: String, url: String): String =
        if (baseUrl.isNotEmpty() && url.startsWith(baseUrl)) url.removePrefix(baseUrl) else url

    fun toItems(page: MangasPage, service: MihonApiService): List<KmpItemModel> =
        page.mangas.map { toItem(it, service).apply { extras[HAS_NEXT] = page.hasNextPage } }

    fun toItem(manga: SManga, service: MihonApiService): KmpItemModel = KmpItemModel(
        title = manga.title,
        description = manga.description.orEmpty(),
        url = absolute(service.baseUrl, manga.url),
        imageUrl = manga.thumbnail_url.orEmpty(),
        source = service,
    ).apply {
        extras[URL] = manga.url
        if (manga.memo.isNotEmpty()) extras[MEMO] = manga.memo.toString()
    }

    fun toSManga(model: KmpItemModel, service: MihonApiService): SManga = SManga.create().apply {
        url = model.extras[URL] as? String ?: relative(service.baseUrl, model.url)
        title = model.title
        thumbnail_url = model.imageUrl.ifEmpty { null }
        (model.extras[MEMO] as? String)?.let { memo = json.decodeFromString<JsonObject>(it) }
    }

    fun toInfo(update: SMangaUpdate, requested: SManga, model: KmpItemModel, service: MihonApiService): KmpInfoModel {
        val manga = update.manga.also { if (it.url.isBlank()) it.url = requested.url }
        val infoUrl = absolute(service.baseUrl, manga.url)
        return KmpInfoModel(
            title = manga.title.ifBlank { model.title },
            description = manga.description.orEmpty(),
            url = infoUrl,
            imageUrl = manga.thumbnail_url ?: model.imageUrl,
            chapters = update.chapters.map { toChapter(it, infoUrl, service) },
            genres = manga.getGenres().orEmpty(),
            alternativeNames = emptyList(),
            source = service,
        ).apply {
            extras[URL] = manga.url
            manga.author?.let { extras["author"] = it }
            manga.artist?.let { extras["artist"] = it }
            extras["status"] = statusName(manga.status)
        }
    }

    fun toChapter(chapter: SChapter, infoUrl: String, service: MihonApiService): KmpChapterModel {
        val time = chapter.date_upload.takeIf { it > 0 }
        return KmpChapterModel(
            name = chapter.name,
            url = absolute(service.baseUrl, chapter.url),
            uploaded = time?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }.orEmpty(),
            sourceUrl = infoUrl,
            source = service,
        ).apply {
            uploadedTime = time
            extras[URL] = chapter.url
            extras["chapterNumber"] = chapter.chapter_number
            if (chapter.memo.isNotEmpty()) extras[MEMO] = chapter.memo.toString()
        }
    }

    fun toSChapter(model: KmpChapterModel, service: MihonApiService): SChapter = SChapter.create().apply {
        url = model.extras[URL] as? String ?: relative(service.baseUrl, model.url)
        name = model.name
        (model.extras[MEMO] as? String)?.let { memo = json.decodeFromString<JsonObject>(it) }
    }

    fun toStorage(page: Page, imageUrl: String, source: Any): KmpStorage =
        KmpStorage(link = imageUrl).apply {
            if (source is HttpSource) headers.putAll(imageHeaders(source, page).toMap())
        }

    private val imageRequestMethod by lazy {
        HttpSource::class.java.getDeclaredMethod("imageRequest", Page::class.java).apply { isAccessible = true }
    }

    /** `imageRequest` is protected; extensions override it to add per-image headers. */
    private fun imageHeaders(source: HttpSource, page: Page): Headers =
        runCatching { (imageRequestMethod.invoke(source, page) as Request).headers }.getOrElse { source.headers }

    private fun Headers.toMap(): Map<String, String> = names().associateWith { get(it).orEmpty() }

    private fun statusName(status: Int) = when (status) {
        SManga.ONGOING -> "Ongoing"
        SManga.COMPLETED -> "Completed"
        SManga.LICENSED -> "Licensed"
        SManga.PUBLISHING_FINISHED -> "Publishing finished"
        SManga.CANCELLED -> "Cancelled"
        SManga.ON_HIATUS -> "On hiatus"
        else -> "Unknown"
    }
}
