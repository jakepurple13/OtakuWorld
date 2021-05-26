package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.nodes.Document

object AnimeKisaSubbed : AnimeKisa("anime")
object AnimeKisaDubbed : AnimeKisa("alldubbed")
object AnimeKisaMovies : AnimeKisa("movies")

abstract class AnimeKisa(allPath: String) : ShowApi(
    baseUrl = "https://www.animekisa.tv",
    allPath = allPath,
    recentPath = ""
) {
    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("div.listAnimes")
            .select("div.episode-box-2")
            .map {
                ItemModel(
                    title = it.select("div.title-box").text(),
                    description = "",
                    imageUrl = it.select("img").attr("abs:src"),
                    url = it.select("a.an").next().select("a.an").attr("abs:href"),
                    source = this
                )
            }
            .let(it::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("a.an")
            .map {
                ItemModel(
                    title = it.text(),
                    description = "",
                    imageUrl = "",
                    url = it.attr("abs:href"),
                    source = this
                )
            }
            .let(it::onSuccess)
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create {
        InfoModel(
            source = this,
            url = source.url,
            title = source.title,
            description = "",
            imageUrl = doc.select("div.infopicbox").select("img").attr("abs:src"),
            genres = emptyList(),
            chapters = doc.select("a.infovan").map {
                ChapterModel(
                    name = it.text(),
                    url = it.attr("abs:href"),
                    uploaded = it.select("div.timeS").attr("time"),
                    source = this
                )
            },
            alternativeNames = emptyList()
        )
            .let(it::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        val doc = chapterModel.url.toJsoup()
        val downloadUrl = "var VidStreaming = \"(.*?)\";".toRegex()
            .find(doc.toString())?.groups?.get(1)?.value?.replace("load.php", "download")
            ?.toJsoup()
            ?.select("div.dowload")
            ?.select("a")
            ?.eachAttr("abs:href")
            ?.filter { it.contains(".mp4") }
            ?.randomOrNull()

        it.onSuccess(
            listOf(
                Storage(
                    link = downloadUrl,
                    source = chapterModel.url,
                    quality = "Good",
                    sub = "Yes"
                )
            )
        )
    }

}