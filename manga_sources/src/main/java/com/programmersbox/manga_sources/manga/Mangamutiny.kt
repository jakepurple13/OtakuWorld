package com.programmersbox.manga_sources.manga

import androidx.compose.ui.util.fastMap
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.gsonutils.header
import com.programmersbox.models.*
import io.reactivex.Single
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

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = try {
        if (searchText.isBlank()) {
            super.searchList(searchText, page, list)
        } else {
            Single.create { emitter ->
                emitter.onSuccess(
                    getJsonApi<Munity>(
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
                )
            }
        }
    } catch (e: Exception) {
        super.searchList(searchText, page, list)
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        emitter.onSuccess(
            getJsonApi<Munity>(
                "$baseUrl$mangaApiPath?sort=-lastReleasedAt&limit=20${if (page != 1) "&skip=${page * 20}" else ""}",
                header
            )
                ?.items
                ?.fastMap {
                    ItemModel(
                        title = it.title.orEmpty(),
                        description = "",
                        url = "$baseUrl$mangaApiPath/${it.slug}",
                        imageUrl = it.thumbnail.orEmpty(),
                        source = this
                    )
                }.orEmpty()
        )
    }

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        emitter.onSuccess(
            getJsonApi<Munity>(
                "$baseUrl$mangaApiPath?sort=-lastReleasedAt&limit=20${if (page != 1) "&skip=${page * 20}" else ""}",
                header
            )
                ?.items
                ?.fastMap {
                    ItemModel(
                        title = it.title.orEmpty(),
                        description = "",
                        url = "$baseUrl$mangaApiPath/${it.slug}",
                        imageUrl = it.thumbnail.orEmpty(),
                        source = this
                    )
                }.orEmpty()
        )
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create { emitter ->
        getJsonApi<MangaInfoMunity>(model.url, header)?.let {
            InfoModel(
                title = model.title,
                description = it.summary.orEmpty(),
                url = model.url,
                imageUrl = model.imageUrl,
                chapters = it.chapters?.fastMap { c ->
                    ChapterModel(
                        name = chapterTitleBuilder(c),
                        url = "$baseUrl$chapterApiPath/${c.slug}",
                        uploaded = c.releasedAt.orEmpty(),
                        sourceUrl = model.url,
                        source = this
                    )
                }.orEmpty(),
                genres = it.genres.orEmpty(),
                alternativeNames = listOf(it.alias.orEmpty()),
                source = this
            )
                .let { emitter.onSuccess(it) }
        } ?: emitter.onError(Throwable("Failed to Get ${model.title}"))
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

    override fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {
        getJsonApi<MangaInfoMunity>(url, header).let {
            ItemModel(
                title = it?.title.orEmpty(),
                description = it?.summary.orEmpty(),
                url = url,
                imageUrl = it?.thumbnail.orEmpty(),
                source = this
            )
        }.let(it::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->
        getJsonApi<MunityPage>(chapterModel.url, header)?.let {
            val chapterUrl = "${it.storage}/${it.manga}/${it.id}/"
            it.images?.fastMap { i -> "$chapterUrl$i" }
        }
            ?.fastMap { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }
            .orEmpty()
            .let { emitter.onSuccess(it) }
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