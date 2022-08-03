package com.programmersbox.novel_sources.novels

import androidx.compose.ui.util.fastMap
import com.programmersbox.models.*
import com.programmersbox.novel_sources.Sources
import com.programmersbox.novel_sources.toJsoup
import org.jsoup.Jsoup

object WuxiaWorld : ApiService {

    override val baseUrl: String get() = "https://wuxiaworld.online"

    override val canDownload: Boolean get() = false

    override val serviceName: String get() = "WUXIAWORLD"

    override val canScroll: Boolean get() = true

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return Jsoup.connect("$baseUrl/search.ajax?type=&query=$searchText").followRedirects(true).post()
            //.also { println(it) }
            .select("li.option").fastMap {
                ItemModel(
                    title = it.select("a").text(),
                    description = "",
                    url = it.select("a").attr("abs:href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = this
                )
            }
    }

    override suspend fun recent(page: Int): List<ItemModel> {
        val pop = "/wuxia-list?view=list&page=$page"
        return "$baseUrl$pop".toJsoup()
            .select("div.update_item")
            .fastMap {
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
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        val pop = "/wuxia-list?view=list&sort=popularity&page=$page"
        return "$baseUrl$pop".toJsoup()
            .select("div.update_item")
            .fastMap {
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
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val info = model.url.toJsoup()

        return InfoModel(
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
                .fastMap {
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
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = url.toJsoup()
        return ItemModel(
            title = doc.title(),
            description = doc.select("meta[name='description']").attr("content"),
            imageUrl = doc.select("link[rel='image_src']").attr("href"),
            url = url,
            source = Sources.WUXIAWORLD
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        return listOf(
            Storage(
                link = chapterModel.url.toJsoup().select("div.content-area").html(),
                source = chapterModel.url,
                quality = "Good",
                sub = "Yes"
            )
        )
    }
}