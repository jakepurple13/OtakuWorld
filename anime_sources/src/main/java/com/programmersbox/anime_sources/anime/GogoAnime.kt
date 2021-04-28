package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import com.programmersbox.rxutils.invoke
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

object GogoAnimeApi : ShowApi(
    baseUrl = "https://www.gogoanime1.com",
    allPath = "home/anime-list",
    recentPath = "home/latest-episodes"
) {

    override val serviceName: String get() = "GOGOANIME"

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
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

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
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

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        try {
            val storage = Storage(
                link = chapterModel.url.toJsoup().select("a[download^=http]").attr("abs:download"),
                source = chapterModel.url,
                quality = "Good",
                sub = "Yes"
            )
            val regex = "^[^\\[]+(.*mp4)".toRegex().toPattern().matcher(storage.link!!)
            storage.filename = if (regex.find()) regex.group(1)!! else "${URI(chapterModel.url).path.split("/")[2]} ${chapterModel.name}.mp4"
            it(listOf(storage))
        } catch (e: Exception) {
            it(e)
        }
    }

    /*override fun getVideoLink(info: EpisodeInfo): Single<List<Storage>> = Single.create {
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
    }*/

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create {
        try {
            val name = doc.select("div.anime-title").text()
            it(
                InfoModel(
                    source = this,
                    title = name,
                    url = source.url,
                    alternativeNames = emptyList(),
                    description = doc.select("p.anime-details").text(),
                    imageUrl = doc.select("div.animeDetail-image").select("img[src^=http]")?.attr("abs:src").orEmpty(),
                    genres = doc.select("div.animeDetail-item:contains(Genres)").select("a[href^=http]").eachText(),
                    chapters = doc.select("ul.check-list").select("li").map {
                        val urlInfo = it.select("a[href^=http]")
                        val epName = urlInfo.text().let { info -> if (info.contains(name)) info.substring(name.length) else info }.trim()
                        ChapterModel(epName, urlInfo.attr("abs:href"), "", this)
                    }.distinctBy(ChapterModel::url)
                )
            )
        } catch (e: Exception) {
            it(e)
        }
    }

    override suspend fun getSourceByUrl(url: String): ItemModel? = try {
        val doc = Jsoup.connect(url).get()
        ItemModel(
            source = this,
            title = doc.select("div.anime-title").text(),
            url = url,
            description = doc.select("p.anime-details").text(),
            imageUrl = doc.select("div.animeDetail-image").select("img[src^=http]")?.attr("abs:src").orEmpty(),
        )
    } catch (e: Exception) {
        null
    }

    override fun searchList(text: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> {
        return Single.create { emitter ->
            try {
                if (text.isNotEmpty()) {
                    emitter.onSuccess(
                        getJsonApi<Base>("https://www.gogoanime1.com/search/topSearch?q=$text")
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
                    emitter.onSuccess(searchListNonSingle(text, page, list))
                }
            } catch (e: Exception) {
                emitter.onSuccess(searchListNonSingle(text, page, list))
            }
        }
    }

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