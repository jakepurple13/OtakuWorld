package com.programmersbox.anime_sources.anime

import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.gsonutils.getApi
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.nodes.Document

object Hdm : ShowApi(
    baseUrl = "https://hdm.to",
    allPath = "movies/page",
    recentPath = ""
) {

    override val canDownload: Boolean get() = false
    override val canScrollAll: Boolean get() = true
    override val serviceName: String get() = "HDM"
    override fun allPage(page: Int): String = "/$page"

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc.select("div.container")
            .select("div.col-md-2")
            .fastMap {
                val img = it.select("img").attr("src").orEmpty()
                val name = it.select("div.movie-details").text().orEmpty()
                ItemModel(
                    title = name,
                    description = "",
                    imageUrl = img,
                    url = it.select("a").attr("href"),
                    source = Sources.HDM
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc.select("div.container")
            .select("div.col-md-2")
            .fastMap {
                val img = it.select("img").attr("src").orEmpty()
                val name = it.select("div.movie-details").text().orEmpty()
                ItemModel(
                    title = name,
                    description = "",
                    imageUrl = img,
                    url = it.select("a").attr("href"),
                    source = Sources.HDM
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create { emitter ->
        val title = doc.selectFirst("h2.movieTitle")?.text()
        val poster = doc.selectFirst("div.post-thumbnail > img")?.attr("src")
        val description = doc.selectFirst("div.synopsis > p")?.text().orEmpty()
        val data = "src/player/\\?v=(.*?)\"".toRegex().find(doc.toString())?.groupValues?.get(1)

        InfoModel(
            source = Sources.HDM,
            title = title ?: source.title,
            url = source.url,
            alternativeNames = emptyList(),
            description = description,
            imageUrl = poster ?: source.imageUrl,
            genres = emptyList(),
            chapters = listOf(
                ChapterModel(
                    title ?: source.title,
                    "$baseUrl/src/player/?v=$data",
                    "",
                    source.url,
                    Sources.HDM
                )
            )
        )
            .let(emitter::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        val slug = ".*/(.*?)\\.mp4".toRegex().find(chapterModel.url)?.groupValues?.get(1)
        val response = getApi(chapterModel.url).orEmpty()
        val key = "playlist\\.m3u8(.*?)\"".toRegex().find(response)?.groupValues?.get(1)
        it.onSuccess(
            listOf(
                Storage(
                    link = "https://hls.1o.to/vod/$slug/playlist.m3u8$key",
                    source = chapterModel.url,
                    quality = "720",
                    sub = "Yes"
                )
            )
        )
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = Single.create { emitter ->
        val url = "$baseUrl/search/$searchText"
        url.toJsoup()
            .select("div.col-md-2 > article > a")
            .fastMap {
                val data = it.selectFirst("> div.item")
                val img = data?.selectFirst("> img")?.attr("src").orEmpty()
                val name = data?.selectFirst("> div.movie-details")?.text().orEmpty()
                ItemModel(
                    title = name,
                    description = "",
                    imageUrl = img,
                    url = it.attr("href"),
                    source = Sources.HDM
                )
            }
            .let(emitter::onSuccess)
    }

}