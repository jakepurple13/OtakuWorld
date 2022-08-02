package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.anime_sources.utilities.ApiResponse
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object AnimeSimpleSubbed : AnimeSimple(true) {
    override val serviceName: String get() = "ANIMESIMPLE_SUBBED"
}

object AnimeSimpleDubbed : AnimeSimple(false) {
    override val serviceName: String get() = "ANIMESIMPLE_DUBBED"
}

abstract class AnimeSimple(private val isSubbed: Boolean) : ShowApi(
    baseUrl = "https://animesimple.com",
    allPath = "series-list",
    recentPath = ""
) {

    override val canDownload: Boolean get() = false

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.never()

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("div#anime-list")
            .select("a.list-group-item")
            .map {
                ItemModel(
                    title = it.text(),
                    description = "",
                    imageUrl = it.select("img.card-img-top").attr("abs:src"),
                    url = "https:${it.select("a").attr("href")}",
                    source = this
                )
            }
            .let(it::onSuccess)

        //println(doc)
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create {
        val id2 = com.programmersbox.anime_sources.utilities.getApi("$baseUrl/request") {
            addEncodedQueryParameter("anime-id", doc.select("input#animeid").attr("value"))
            addEncodedQueryParameter("epi-page", "1")
            addEncodedQueryParameter("top", "10000")
            addEncodedQueryParameter("bottom", "0")
        }.let { (it as? ApiResponse.Success)?.body }

        InfoModel(
            title = source.title,
            description = doc.select("p.synopsis").text(),
            url = source.url,
            imageUrl = doc.select("img#series-image").attr("abs:src"),
            chapters = Jsoup.parse(id2).select("a.list-group-item").map {
                ChapterModel(
                    name = it.text(),
                    url = "https:${it.select("a").attr("href")}",
                    uploaded = "",
                    sourceUrl = source.url,
                    source = this
                )
            }.reversed(),
            genres = doc.select("a.badge, a.badge-secondary").select("a").eachText(),
            alternativeNames = emptyList(),
            source = this
        )
            .let(it::onSuccess)
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> {
        return Single.create<List<ItemModel>> {
            val f = com.programmersbox.anime_sources.utilities.getApi("$baseUrl/search") {
                addEncodedQueryParameter("q", searchText.toString())
            }

            when (f) {
                is ApiResponse.Success -> {
                    Jsoup.parse(f.body)
                        .select("div#explore-container")
                        .select("div.card-query")
                        .map {
                            ItemModel(
                                title = it.select("div.card[title]").attr("title"),
                                description = "",
                                imageUrl = it.select("img.card-img-top").attr("abs:src"),
                                url = "https:${it.select("h6.card-text a[href]").attr("href")}",
                                source = this
                            )
                        }
                        .let(it::onSuccess)
                }
                is ApiResponse.Failed -> it.onError(Throwable("Error ${f.code}"))
            }

        }
            .onErrorResumeNext(super.searchList(searchText, page, list))
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {

        val r = "var json = ([^;]*)".toRegex()
            .find(chapterModel.url.toJsoup().toString())
            ?.groups
            ?.get(1)
            ?.value
            ?.fromJson<List<AnimeSimpleEpisode>>()

        val streamUrls = r?.find { it.type?.lowercase() == if (isSubbed) "subbed" else "dubbed" }

        val url = "src=['|\\\"]([^\\'|^\\\"]*)".toRegex().find(streamUrls?.player!!)?.groups?.get(1)?.value.toString()

        val u = Jsoup.connect(url).header("Referer", "https://anistream.xyz").get()

        //println(u)

        val source = "<source src=\"(.*?)\"".toRegex()
        val token = "token\\s*=\\s*['\\\"|']([^\\\"']*)".toRegex()

        val finalUrl = when {
            source.containsMatchIn(u.toString()) -> source.find(u.toString())?.groups?.get(1)?.value
            token.containsMatchIn(u.toString()) -> {
                val id3 = url.split("/").last()
                Jsoup.connect("https://mp4.sh/v/$id3")
                    .header("Referer", url)
                    .data("token", token.find(u.toString())?.groups?.get(1)?.value.toString())
                    .ignoreContentType(true)
                    .post()
                    .text()
                    .fromJson<AnimeSimpleFinalUrl>()
                    ?.file
            }
            else -> ""
        }

        it.onSuccess(
            listOf(
                Storage(
                    link = finalUrl,
                    source = streamUrls.host,
                    quality = "Good",
                    sub = streamUrls.type.toString()
                ).apply { headers["referer"] = url }
            )
        )
    }

    data class AnimeSimpleEpisode(val id: String?, val host: String?, val type: String?, val player: String?)

    data class AnimeSimpleFinalUrl(val success: Boolean, val file: String?)

}