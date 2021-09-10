package com.programmersbox.novel_sources.novels

import com.programmersbox.models.*
import com.programmersbox.novel_sources.Sources
import com.programmersbox.novel_sources.toJsoup
import io.reactivex.Single
import org.jsoup.Jsoup

object WuxiaWorld : ApiService {

    override val baseUrl: String get() = "https://wuxiaworld.online"

    override val serviceName: String get() = "WUXIAWORLD"

    override val canScroll: Boolean get() = true
    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        Single.create { emitter ->
            Jsoup.connect("$baseUrl/search.ajax?type=&query=$searchText").followRedirects(true).post()
                //.also { println(it) }
                .select("li.option").map {
                    ItemModel(
                        title = it.select("a").text(),
                        description = "",
                        url = it.select("a").attr("abs:href"),
                        imageUrl = it.select("img").attr("abs:src"),
                        source = this
                    )
                }
                .let { emitter.onSuccess(it) }
        }

    } catch (e: Exception) {
        super.searchList(searchText, page, list)
    }

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create {
        val pop = "/wuxia-list?view=list&page=$page"
        "$baseUrl$pop".toJsoup()
            .select("div.update_item")
            .map {
                ItemModel(
                    title = it
                        .select("h3")
                        .select("a.tooltip")
                        .attr("title"),
                    description = "",
                    imageUrl = it.select("img").attr("abs:src"),
                    url = it
                        .select("h3")
                        .select("a.tooltip")
                        .attr("abs:href"),
                    source = Sources.WUXIAWORLD
                )
            }
            .let(it::onSuccess)
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create {
        val pop = "/wuxia-list?view=list&sort=popularity&page=$page"
        "$baseUrl$pop".toJsoup()
            .select("div.update_item")
            .map {
                ItemModel(
                    title = it
                        .select("h3")
                        .select("a.tooltip")
                        .attr("title"),
                    description = "",
                    imageUrl = it.select("img").attr("abs:src"),
                    url = it
                        .select("h3")
                        .select("a.tooltip")
                        .attr("abs:href"),
                    source = Sources.WUXIAWORLD
                )
            }
            .let(it::onSuccess)
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create {

        val info = model.url.toJsoup()

        InfoModel(
            source = Sources.WUXIAWORLD,
            url = model.url,
            title = model.title,
            description = info.select("meta[name='description']").attr("content"),
            imageUrl = model.imageUrl,
            genres = emptyList(),
            chapters = info
                .select("div.chapter-list")
                .select("div.row")
                .select("span")
                .select("a")
                .map {
                    ChapterModel(
                        name = it.attr("title"),
                        url = it.attr("abs:href"),
                        uploaded = "",
                        sourceUrl = model.url,
                        source = Sources.WUXIAWORLD
                    )
                },
            alternativeNames = emptyList()
        )
            .let(it::onSuccess)

    }

    override fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {
        try {
            val doc = url.toJsoup()
            it.onSuccess(
                ItemModel(
                    title = doc.title(),
                    description = doc.select("meta[name='description']").attr("content"),
                    imageUrl = doc.select("link[rel='image_src']").attr("href"),
                    url = url,
                    source = Sources.WUXIAWORLD
                )
            )
        } catch (e: Exception) {
            it.onError(e)
        }
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        it.onSuccess(
            listOf(
                Storage(
                    link = chapterModel.url.toJsoup().select("div.content-area").html(),
                    source = chapterModel.url,
                    quality = "Good",
                    sub = "Yes"
                )
            )
        )
    }
}