package com.programmersbox.novel_sources.novels

import com.programmersbox.models.*
import com.programmersbox.novel_sources.Sources
import com.programmersbox.novel_sources.toJsoup
import org.jsoup.Jsoup

object BestLightNovel : ApiService {
    override val baseUrl: String get() = "https://bestlightnovel.com"

    override val canDownload: Boolean get() = false
    override val canScroll: Boolean get() = true

    override val serviceName: String get() = "BEST_LIGHT_NOVEL"

    override suspend fun recent(page: Int): List<ItemModel> {
        return Jsoup.connect("$baseUrl/novel_list?type=topview&category=all&state=all&page=$page")
            .followRedirects(true)
            .get()
            .select("div.update_item.list_category")
            .map {
                ItemModel(
                    title = it.select("h3 > a").text(),
                    description = "",
                    url = it.select("h3 > a").attr("abs:href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = Sources.BEST_LIGHT_NOVEL
                )
            }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return super.allList(page)
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = model.url.toJsoup()

        return InfoModel(
            source = Sources.BEST_LIGHT_NOVEL,
            url = model.url,
            title = doc.select(".truyen_info_right h1").text().trim(),
            description = doc.select("div#noidungm").text(),
            imageUrl = doc.select(".info_image img").attr("abs:src"),
            genres = emptyList(),
            chapters = doc
                .select("div.chapter-list div.row")
                .map {
                    ChapterModel(
                        name = it.select("a").text(),
                        url = it.select("a").attr("abs:href"),
                        uploaded = it.select("span:nth-child(2)").text(),
                        sourceUrl = model.url,
                        source = Sources.BEST_LIGHT_NOVEL
                    )
                },
            alternativeNames = emptyList()
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val doc = chapterModel.url.toJsoup()
        return listOf(
            Storage(
                link = doc.select("div#vung_doc").html()
            )
        )
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        val doc = Jsoup.connect("$baseUrl/search_novels/$searchText").get()
            .select("div.update_item.list_category")
            .map {
                ItemModel(
                    title = it.select("h3 > a").text(),
                    description = "",
                    url = it.select("h3 > a").attr("abs:href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = Sources.BEST_LIGHT_NOVEL
                )
            }
        return doc
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = url.toJsoup()

        return ItemModel(
            source = Sources.BEST_LIGHT_NOVEL,
            url = url,
            title = doc.select(".truyen_info_right h1").text().trim(),
            description = doc.select("div#noidungm").text(),
            imageUrl = doc.select(".info_image img").attr("abs:src")
        )
    }
}