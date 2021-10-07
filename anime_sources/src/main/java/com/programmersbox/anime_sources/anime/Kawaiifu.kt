package com.programmersbox.anime_sources.anime

import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.anime_sources.utilities.getQualityFromName
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Kawaiifu : ShowApi(
    baseUrl = "https://kawaiifu.com",
    recentPath = "page/", allPath = ""
) {
    override val serviceName: String get() = "KAWAIIFU"
    override val canScroll: Boolean get() = true
    override val canScrollAll: Boolean get() = false
    override val canPlay: Boolean get() = false
    override fun recentPage(page: Int): String = page.toString()

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc.select(".today-update .item").fastMap {
            ItemModel(
                title = it.selectFirst("img")?.attr("alt").orEmpty(),
                description = it.select("div.info").select("p").text(),
                imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                url = it.selectFirst("a")?.attr("href").orEmpty(),
                source = Sources.KAWAIIFU
            )
        }
            .let(emitter::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc.select(".section").flatMap {
            it.select(".list-film > .item").fastMap { ani ->
                ItemModel(
                    title = ani.selectFirst("img")?.attr("alt").orEmpty(),
                    description = it.selectFirst("p.txtstyle2")?.select("span.cot1")?.text().orEmpty(),
                    imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                    url = it.selectFirst("a")?.attr("abs:href").orEmpty(),
                    source = Sources.KAWAIIFU
                )
            }
        }
            .let(emitter::onSuccess)
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create<List<ItemModel>> { s ->
            Jsoup.connect("$baseUrl/search-movie?keyword=$searchText").get()
                .select(".item")
                .fastMap {
                    ItemModel(
                        title = it.selectFirst("img")?.attr("alt").orEmpty(),
                        description = it.text(),
                        imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                        url = it.selectFirst("a")?.attr("href").orEmpty(),
                        source = Sources.KAWAIIFU
                    )
                }
                .let(s::onSuccess)
        }
            .onErrorResumeNext(super.searchList(searchText, page, list))

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create { emitter ->
        InfoModel(
            source = Sources.KAWAIIFU,
            title = source.title,
            url = source.url,
            alternativeNames = emptyList(),
            description = doc.select(".sub-desc p")
                .filter { it.select("strong").isEmpty() && it.select("iframe").isEmpty() }
                .joinToString("\n") { it.text() },
            imageUrl = source.imageUrl,
            genres = doc.select(".table a[href*=\"/tag/\"]").fastMap { tag -> tag.text() },
            chapters = doc.selectFirst("a[href*=\".html-episode\"]")
                ?.attr("href")
                ?.toJsoup()
                ?.selectFirst(".list-ep")
                ?.select("li")
                ?.fastMap {
                    ChapterModel(
                        if (it.text().trim().toIntOrNull() != null) "Episode ${it.text().trim()}" else it.text().trim(),
                        it.selectFirst("a")?.attr("href").orEmpty(),
                        "",
                        source.url,
                        Sources.KAWAIIFU
                    )
                }
                .orEmpty()
        )
            .let(emitter::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->

        val data = chapterModel.url
        val doc = data.toJsoup()

        val episodeNum = if (data.contains("ep=")) data.split("ep=")[1].split("&")[0].toIntOrNull() else null

        val servers = doc.select(".list-server")
            .fastMap {
                val serverName = it.selectFirst(".server-name")?.text().orEmpty()
                val episodes = it.select(".list-ep > li > a").map { episode -> Pair(episode.attr("href"), episode.text()) }
                val episode = if (episodeNum == null) episodes[0] else episodes.mapNotNull { ep ->
                    if ((if (ep.first.contains("ep=")) ep.first.split("ep=")[1].split("&")[0].toIntOrNull() else null) == episodeNum) {
                        ep
                    } else null
                }[0]
                Pair(serverName, episode)
            }
            .fastMap {
                if (it.second.first == data) {
                    val sources = doc.select("video > source")
                        .fastMap { source -> Pair(source.attr("src"), source.attr("data-quality")) }
                    Triple(it.first, sources, it.second.second)
                } else {
                    val html = it.second.first.toJsoup()

                    val sources = html.select("video > source")
                        .fastMap { source -> Pair(source.attr("src"), source.attr("data-quality")) }
                    Triple(it.first, sources, it.second.second)
                }
            }
            .fastMap {
                it.second.fastMap { source ->
                    Storage(
                        link = source.first,
                        source = chapterModel.url,
                        filename = "${chapterModel.name}.mp4",
                        quality = it.first + "-" + source.second,
                        sub = getQualityFromName(source.second).value.toString()
                    )
                }
            }
            .flatten()

        emitter.onSuccess(servers)
    }

}