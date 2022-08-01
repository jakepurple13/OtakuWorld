package com.programmersbox.novel_sources.novels

import com.programmersbox.models.*
import com.programmersbox.novel_sources.Sources
import com.programmersbox.novel_sources.toJsoup
import io.reactivex.Single
import org.jsoup.Jsoup

object BestLightNovel : ApiService {
    override val baseUrl: String get() = "https://bestlightnovel.com"

    override val canDownload: Boolean get() = false

    override val serviceName: String get() = "BEST_LIGHT_NOVEL"

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create {
        Jsoup.connect("$baseUrl/novel_list?type=topview&category=all&state=all&page=$page")
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
            .let(it::onSuccess)
    }

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

    override fun getList(page: Int): Single<List<ItemModel>> = Single.never()
    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.never()

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

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.never()

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val doc = chapterModel.url.toJsoup()
        return listOf(
            Storage(
                link = doc.select("div#vung_doc").html()
            )
        )
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return super.search(searchText, page, list)
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        return super.sourceByUrl(url)
    }

}