package com.programmersbox.manga_sources.manga

import androidx.compose.ui.util.fastMap
import com.programmersbox.manga_sources.Sources
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.manga_sources.utilities.asJsoup
import com.programmersbox.manga_sources.utilities.cloudflare
import com.programmersbox.models.*
import io.reactivex.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AsuraScans : ApiService, KoinComponent {

    override val baseUrl: String = "https://www.asurascans.com/"
    override val serviceName: String = "ASURA_SCANS"
    private val helper: NetworkHelper by inject()
    //override val canScroll: Boolean = true

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create {
        val models = cloudflare(
            helper,
            "$baseUrl/manga/?page=$page&order=update",
            "referer" to baseUrl,
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
        ).execute().asJsoup()
            .select("div.bs")
            .fastMap {
                val s = it.select("div.bsx > a")

                ItemModel(
                    title = s.attr("title"),
                    description = "",
                    url = s.attr("abs:href"),
                    imageUrl = it.select("div.limit img").let { if (it.hasAttr("data-src")) it.attr("abs:data-src") else it.attr("abs:src") },
                    source = Sources.ASURA_SCANS
                )
            }

        it.onSuccess(models)
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create {
        val models = cloudflare(
            helper,
            "$baseUrl/manga/?page=$page&order=popular",
            "referer" to baseUrl,
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
        ).execute().asJsoup()
            .select("div.bs")
            .fastMap {

                val s = it.select("div.bsx > a")

                ItemModel(
                    title = s.attr("title"),
                    description = "",
                    url = s.attr("abs:href"),
                    imageUrl = it.select("div.limit img").let { if (it.hasAttr("data-src")) it.attr("abs:data-src") else it.attr("abs:src") },
                    source = Sources.ASURA_SCANS
                )
            }

        it.onSuccess(models)
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = try {
        if (searchText.isBlank()) {
            super.searchList(searchText, page, list)
        } else {
            super.searchList(searchText, page, list)
        }
    } catch (e: Exception) {
        super.searchList(searchText, page, list)
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create {
        val request = cloudflare(
            helper,
            model.url,
            "referer" to baseUrl,
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
        ).execute().asJsoup()

        val i = request.select("div.bigcontent, div.animefull, div.main-info")

        val c = request.select("div.bxcl ul li, div.cl ul li, ul li:has(div.chbox):has(div.eph-num)")
            .fastMap {
                val urlElement = it.select(".lchx > a, span.leftoff a, div.eph-num > a").first()!!
                ChapterModel(
                    url = urlElement.attr("abs:href"),
                    name = if (urlElement.select("span.chapternum").isNotEmpty()) urlElement.select("span.chapternum").text() else urlElement.text(),
                    sourceUrl = model.url,
                    source = Sources.ASURA_SCANS,
                    uploaded = it.select("span.rightoff, time, span.chapterdate").firstOrNull()?.text().orEmpty()
                )
            }

        val info = InfoModel(
            title = model.title,
            description = i.select("div.desc p, div.entry-content p").joinToString("\n") { it.text() },
            url = model.url,
            imageUrl = model.imageUrl,
            chapters = c,
            genres = i.select("span:contains(Genre) a, .mgen a").fastMap { element -> element.text() }.distinct(),
            alternativeNames = emptyList(),
            source = Sources.ASURA_SCANS
        )

        it.onSuccess(info)

    }

    override fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {

        val request = cloudflare(
            helper,
            url,
            "referer" to baseUrl,
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
        ).execute().asJsoup()

        val i = request.select("div.bigcontent, div.animefull, div.main-info")

        val info = ItemModel(
            title = i.select("h1.entry-title").text(),
            description = i.select("div.desc p, div.entry-content p").joinToString("\n") { it.text() },
            url = url,
            imageUrl = i.select("div.thumb img").let { if (it.hasAttr("data-src")) it.attr("abs:data-src") else it.attr("abs:src") },
            source = Sources.ASURA_SCANS
        )

        it.onSuccess(info)

    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {

        val request = cloudflare(
            helper,
            chapterModel.url,
            "referer" to chapterModel.url,
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
        ).execute().asJsoup()
            .select("div.rdminimal img[loading*=lazy]")
            .filterNot { it.attr("abs:src").isNullOrEmpty() }
            .fastMap { it.attr("abs:src") }
            .fastMap { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }

        it.onSuccess(request)

    }

}