package com.programmersbox.anime_sources.anime

import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.anime_sources.utilities.extractors
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object AnimeFlick : ShowApi(
    baseUrl = "https://animeflick.net",
    recentPath = "updates", allPath = "Anime-List"
) {
    override val serviceName: String get() = "ANIMEFLICK"
    override val canScroll: Boolean get() = true
    override val canScrollAll: Boolean get() = true
    override fun recentPage(page: Int): String = "-$page"
    override fun allPage(page: Int): String = "/All/$page"

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { s ->
        doc
            .select("div.mb-4")
            .select("li.slide-item")
            .fastMap {
                ItemModel(
                    title = it.select("img.img-fluid").attr("title").orEmpty(),
                    description = "",
                    imageUrl = baseUrl + it.select("img.img-fluid").attr("src"),
                    url = baseUrl + it.selectFirst("a")?.attr("href").orEmpty(),
                    source = Sources.ANIMEFLICK
                )
            }
            .let(s::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create { s ->
        doc
            .select("table.table")
            .select("tr")
            .fastMap {
                ItemModel(
                    title = it.select("h4.title").text().orEmpty(),
                    description = "",
                    imageUrl = baseUrl + it.select("img.d-block").attr("src"),
                    url = baseUrl + it.selectFirst("a")?.attr("href").orEmpty(),
                    source = Sources.ANIMEFLICK
                )
            }
            .let(s::onSuccess)
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create<List<ItemModel>> { s ->
            Jsoup.connect("$baseUrl/search.php?search=$searchText").get()
                .select(".row.mt-2")
                .fastMap {
                    ItemModel(
                        title = it.selectFirst("h5 > a")?.text().orEmpty(),
                        description = "",
                        imageUrl = baseUrl + it.selectFirst("img")?.attr("src")?.replace("70x110", "225x320").orEmpty(),
                        url = baseUrl + it.selectFirst("a")?.attr("href").orEmpty(),
                        source = Sources.ANIMEFLICK
                    )
                }
                .let(s::onSuccess)
        }
            .onErrorResumeNext(super.searchList(searchText, page, list))

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create { emitter ->
        val poster = baseUrl + doc.select("img.rounded").attr("src")
        val title = doc.select("h2.title").text()
        val description = doc.select("p").text()
        val genres = doc.select("a[href*=\"genre-\"]").map { it.text() }
        val episodes = doc.select("#collapseOne .block-space > .row > div:nth-child(2)").map {
            val name = it.select("a").text()
            val link = baseUrl + it.select("a").attr("href")
            ChapterModel(
                name,
                link,
                "",
                source.url,
                Sources.ANIMEFLICK
            )
        }

        InfoModel(
            source = Sources.ANIMEFLICK,
            title = title,
            url = source.url,
            alternativeNames = emptyList(),
            description = description,
            imageUrl = poster,
            genres = genres,
            chapters = episodes
        )
            .let(emitter::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->
        val data = chapterModel.url
        val doc = data.toJsoup().select("iframe.embed-responsive-item").attr("src").toJsoup()
        val links = doc.select("ul.list-server-items").select("li").eachAttr("data-video")
        val d = links.flatMap { link ->
            extractors
                .filter { link.startsWith(it.mainUrl) }
                .flatMap { it.getUrl(link) }
        }
            .distinctBy { it.link }
        emitter.onSuccess(d)
    }

    override fun getSourceByUrl(url: String): Single<ItemModel> = Single.create { emitter ->
        val doc = url.toJsoup()
        ItemModel(
            title = doc.select("h2.title").text(),
            description = doc.select("p").text(),
            imageUrl = baseUrl + doc.select("img.rounded").attr("src"),
            url = url,
            source = this
        ).let(emitter::onSuccess)
    }

}

