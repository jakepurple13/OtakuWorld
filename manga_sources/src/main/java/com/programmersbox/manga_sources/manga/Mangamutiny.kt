package com.programmersbox.manga_sources.manga

import androidx.compose.ui.util.fastMap
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.gsonutils.header
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import okhttp3.Request

object Mangamutiny : ApiService {

    override val baseUrl = "https://api.mangamutiny.org"
    private const val mangaApiPath = "/v1/public/manga"
    private const val chapterApiPath = "/v1/public/chapter"

    override val serviceName: String get() = "MANGAMUTINY"

    override val websiteUrl: String = "https://mangamutiny.org"

    private val headers: List<Pair<String, String>>
        get() = listOf(
            "Accept" to "application/json",
            "Origin" to "https://mangamutiny.org"
        )

    private val header: Request.Builder.() -> Unit = { header(*headers.toTypedArray()) }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return getJsonApi<Munity>(
            "$baseUrl$mangaApiPath?sort=-titles&limit=20&text=$searchText${if (page != 1) "&skip=${page * 20}" else ""}",
            header
        )
            ?.items?.fastMap {
                ItemModel(
                    title = it.title.orEmpty(),
                    description = "",
                    url = "$baseUrl$mangaApiPath/${it.slug}",
                    imageUrl = it.thumbnail.orEmpty(),
                    source = this
                )
            }
            .orEmpty()
    }

    private fun chapterTitleBuilder(rootNode: MunityChapters): String {
        val volume = rootNode.volume//volumegetNullable("volume")?.asInt
        val chapter = rootNode.chapter?.toInt()//getNullable("chapter")?.asInt
        val textTitle = rootNode.title//getNullable("title")?.asString
        val chapterTitle = StringBuilder()
        if (volume != null) chapterTitle.append("Vol. $volume")
        if (chapter != null) {
            if (volume != null) chapterTitle.append(" ")
            chapterTitle.append("Chapter $chapter")
        }
        if (textTitle != null && textTitle != "") {
            if (volume != null || chapter != null) chapterTitle.append(" ")
            chapterTitle.append(textTitle)
        }
        return chapterTitle.toString()
    }

    override val canScroll: Boolean get() = true

    private data class Munity(val items: List<MangaMunity>?, val total: Number?)
    private data class MangaMunity(val title: String?, val slug: String?, val thumbnail: String?, val id: String?)
    private data class MangaInfoMunity(
        val status: String?,
        val genres: List<String>?,
        val chapterCount: Number?,
        val viewCount: Number?,
        val rating: Number?,
        val ratingCount: Number?,
        val title: String?,
        val summary: String?,
        val authors: String?,
        val artists: String?,
        val slug: String?,
        val updatedAt: String?,
        val thumbnail: String?,
        val lastReleasedAt: String?,
        val category: String?,
        val alias: String?,
        val subCount: Number?,
        val commentCount: Number?,
        val chapters: List<MunityChapters>?,
        val id: String?
    )

    private data class MunityChapters(
        val viewCount: Number?,
        val title: String?,
        val volume: Int?,
        val chapter: Number?,
        val slug: String?,
        val releasedAt: String?,
        val id: String?
    )

    private data class MunityPage(
        val images: List<String>?,
        val storage: String?,
        val viewCount: Number?,
        val title: String?,
        val volume: Number?,
        val chapter: Number?,
        val manga: String?,
        val slug: String?,
        val releasedAt: String?,
        val order: Number?,
        val id: String?
    )
}