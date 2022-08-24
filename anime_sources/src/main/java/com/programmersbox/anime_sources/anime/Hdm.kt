package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi

object Hdm : ShowApi(
    baseUrl = "https://hdm.to",
    allPath = "movies/page",
    recentPath = ""
) {

    override val canDownload: Boolean get() = false
    override val canScrollAll: Boolean get() = true
    override val serviceName: String get() = "HDM"
    override fun allPage(page: Int): String = "/$page"

    /*fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
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

    fun getList(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
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
    }*/

}