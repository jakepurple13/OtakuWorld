package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi

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

    /*fun getRecent(doc: Document): Single<List<ItemModel>> = Single.never()

    fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
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

    data class AnimeSimpleEpisode(val id: String?, val host: String?, val type: String?, val player: String?)

    data class AnimeSimpleFinalUrl(val success: Boolean, val file: String?)*/

}