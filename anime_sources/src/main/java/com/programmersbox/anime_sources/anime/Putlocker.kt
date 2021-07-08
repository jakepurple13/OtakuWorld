package com.programmersbox.anime_sources.anime

import android.util.Base64
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object PutlockerTV : Putlocker("tv-series.html") {
    override val serviceName: String get() = "PUTLOCKERTV"
}

object PutlockerAnime : Putlocker("anime-series.html") {
    override val serviceName: String get() = "PUTLOCKERANIME"
}

object PutlockerCartoons : Putlocker("cartoon.html") {
    override val serviceName: String get() = "PUTLOCKERCARTOONS"
}

object PutlockerMovies : Putlocker("cinema-movies.html") {
    override val serviceName: String get() = "PUTLOCKERMOVIES"
}

abstract class Putlocker(allPath: String) : ShowApi(
    baseUrl = "https://putlockers.fm",
    allPath = allPath,
    recentPath = "recently-added.html"
) {
    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("ul.list")
            .select("div.item")
            .map {

                val des = it.select("a.thumb").attr("onmouseover")
                    .orEmpty()

                val regex = "Tip\\('(.*)'\\)".toRegex()
                    .find(des)
                    ?.groups?.get(1)?.value
                    .orEmpty()

                ItemModel(
                    title = it.text(),
                    description = Jsoup.parse(regex).text(),
                    imageUrl = it.select("img[alt]").attr("abs:src"),
                    url = it.select("a.thumb").attr("abs:href"),
                    source = this
                )
            }
            .let(it::onSuccess)
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> {
        return Single.create<List<ItemModel>> {

            "$baseUrl/search-movies/${searchText.split(" ").joinToString("+") { it.trim() }}.html".toJsoup()
                .select("ul.list")
                .select("div.item")
                .map {

                    val des = it.select("a.thumb").attr("onmouseover")
                        .orEmpty()

                    val regex = "Tip\\('(.*)'\\)".toRegex()
                        .find(des)
                        ?.groups?.get(1)?.value
                        .orEmpty()

                    ItemModel(
                        title = it.text(),
                        description = Jsoup.parse(regex).text(),
                        imageUrl = it.select("img[alt]").attr("abs:src"),
                        url = it.select("a.thumb").attr("abs:href"),
                        source = this
                    )
                }
                .let(it::onSuccess)

        }
            .onErrorResumeNext(super.searchList(searchText, page, list))
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("ul.list")
            .select("div.item")
            .map {

                val des = it.select("a.thumb").attr("onmouseover")
                    .orEmpty()

                val regex = "Tip\\('(.*)'\\)".toRegex()
                    .find(des)
                    ?.groups?.get(1)?.value
                    .orEmpty()

                ItemModel(
                    title = it.text(),
                    description = Jsoup.parse(regex).text(),
                    imageUrl = it.select("img[alt]").attr("abs:src"),
                    url = it.select("a.thumb").attr("abs:href"),
                    source = this
                )
            }
            .let(it::onSuccess)
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create {
        InfoModel(
            source = this,
            title = source.title,
            url = source.url,
            alternativeNames = emptyList(),
            description = doc.select("p").select("strong:contains(Synopsis:)").text(),
            imageUrl = source.imageUrl,
            genres = doc.select("p").select("strong:contains(Genres:)").text().split(","),
            chapters = doc.select("a.episode")
                .map {
                    ChapterModel(
                        it.text(),
                        it.select("a").attr("abs:href"),
                        "",
                        source.url,
                        this
                    )
                }.reversed()
                .ifEmpty {
                    listOf(
                        ChapterModel(
                            source.title,
                            source.url,
                            "",
                            source.url,
                            this
                        )
                    )
                }
        )
            .let(it::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> {
        return Single.create {

            val d = chapterModel.url.toJsoup()

            val regex = "Base64.decode\\(\"(.*)\"\\)".toRegex().find(d?.toString().orEmpty())?.groups?.get(1)?.value
            val b = Jsoup.parse(String(Base64.decode(regex, Base64.DEFAULT))).select("iframe").attr("abs:src")
            val links = b.toJsoup().select("source").attr("src")

            it.onSuccess(
                listOf(
                    Storage(
                        link = links,
                        source = chapterModel.url,
                        quality = "Good",
                        sub = "Yes"
                    ).apply {
                        headers["referer"] = "http://eplayvid.com/"
                    }
                )
            )
        }
    }

}