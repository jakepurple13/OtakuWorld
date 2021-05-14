package com.programmersbox.novel_sources.novels

import com.programmersbox.models.*
import com.programmersbox.novel_sources.Sources
import com.programmersbox.novel_sources.toJsoup
import io.reactivex.Single

object WuxiaWorld : ApiService {

    override val baseUrl: String get() = "https://wuxiaworld.online"

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
                    source = this
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
                    source = this
                )
            }
            .let(it::onSuccess)
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create {

        val info = model.url.toJsoup()

        InfoModel(
            source = this,
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
                        source = Sources.WUXIAWORLD
                    )
                },
            alternativeNames = emptyList()
        )
            .let(it::onSuccess)

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