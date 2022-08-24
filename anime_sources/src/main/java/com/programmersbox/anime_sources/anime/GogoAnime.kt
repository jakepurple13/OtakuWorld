package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi

object GogoAnimeApi : ShowApi(
    baseUrl = "https://www.gogoanime1.com",
    allPath = "home/anime-list",
    recentPath = "home/latest-episodes"
) {

    override val serviceName: String get() = "GOGOANIME"

    /*fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
        try {
            it(doc.allElements.select("div.dl-item").map {
                val tempUrl = it.select("div.name").select("a[href^=http]").attr("abs:href")
                ItemModel(
                    title = it.select("div.name").text(),
                    description = "",
                    imageUrl = "",
                    url = tempUrl.substring(0, tempUrl.indexOf("/episode")),
                    source = this
                )
            })
        } catch (e: Exception) {
            it(e)
        }
    }

    fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        try {
            it(doc.allElements.select("ul.arrow-list").select("li")
                .map {
                    ItemModel(
                        title = it.text(),
                        description = "",
                        imageUrl = "",
                        url = it.select("a[href^=http]").attr("abs:href"),
                        source = this
                    )
                })
        } catch (e: Exception) {
            it(e)
        }
    }

    *//*override fun getVideoLink(info: EpisodeInfo): Single<List<Storage>> = Single.create {
        try {
            val storage = Storage(
                link = info.url.toJsoup().select("a[download^=http]").attr("abs:download"),
                source = info.url,
                quality = "Good",
                sub = "Yes"
            )
            val regex = "^[^\\[]+(.*mp4)".toRegex().toPattern().matcher(storage.link!!)
            storage.filename = if (regex.find()) regex.group(1)!! else "${URI(info.url).path.split("/")[2]} ${info.name}.mp4"
            it(listOf(storage))
        } catch (e: Exception) {
            it(e)
        }
    }*//*

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> {
        return Single.create { emitter ->
            try {
                if (searchText.isNotEmpty()) {
                    emitter.onSuccess(
                        getJsonApi<Base>("https://www.gogoanime1.com/search/topSearch?q=$searchText")
                            ?.data
                            .orEmpty()
                            .map {
                                ItemModel(
                                    title = it.name.orEmpty(),
                                    description = "",
                                    imageUrl = "",
                                    url = "https://www.gogoanime1.com/watch/${it.seo_name}",
                                    source = this
                                )
                            }
                    )
                } else {
                    emitter.onSuccess(searchListNonSingle(searchText, page, list))
                }
            } catch (e: Exception) {
                emitter.onSuccess(searchListNonSingle(searchText, page, list))
            }
        }
    }*/

    private data class Base(val status: Number?, val data: List<DataShowData>?)

    private data class DataShowData(
        val rel: Number?,
        val anime_id: Number?,
        val name: String?,
        val has_image: Number?,
        val seo_name: String?,
        val score_count: Number?,
        val score: Number?,
        val aired: Number?,
        val episodes: List<Episodes>?
    )

    private data class Episodes(val episode_id: Number?, val episode_seo_name: String?, val episode_name: String?)

}