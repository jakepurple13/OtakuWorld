package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi

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
    /*fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("ul.list")
            .select("div.item")
            .fastMap {
                val des = it.select("a.thumb").attr("onmouseover").orEmpty()

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
        return if (searchText.isEmpty()) super.searchList(searchText, page, list)
        else Single.create<List<ItemModel>> {
            "$baseUrl/search-movies/${searchText.split(" ").joinToString("+") { it.trim() }}.html".toJsoup()
                .select("ul.list")
                .select("div.item")
                .fastMap {
                    val des = it.select("a.thumb").attr("onmouseover").orEmpty()

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

    fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        doc
            .select("ul.list")
            .select("div.item")
            .fastMap {
                val des = it.select("a.thumb").attr("onmouseover").orEmpty()

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
    }*/

}