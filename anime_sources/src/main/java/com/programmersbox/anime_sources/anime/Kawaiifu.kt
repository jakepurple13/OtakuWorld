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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object Kawaiifu : ShowApi(
    baseUrl = "https://kawaiifu.com",
    recentPath = "page/", allPath = ""
) {
    override val serviceName: String get() = "KAWAIIFU"
    override val canScroll: Boolean get() = true
    override val canScrollAll: Boolean get() = false
    override val canDownload: Boolean get() = false
    override fun recentPage(page: Int): String = page.toString()

    override suspend fun recent(page: Int): List<ItemModel> {
        return recentPath(page)
            .select(".today-update .item")
            .fastMap {
                ItemModel(
                    title = it.selectFirst("img")?.attr("alt").orEmpty(),
                    description = it.select("div.info").select("p").text(),
                    imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                    url = it.selectFirst("a")?.attr("href").orEmpty(),
                    source = Sources.KAWAIIFU
                )
            }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return all(page)
            .select(".section")
            .select(".list-film > .item")
            .fastMap {
                ItemModel(
                    title = it.select("img").attr("alt"),
                    description = it.selectFirst("p.txtstyle2")?.select("span.cot1")?.text().orEmpty(),
                    imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                    url = it.selectFirst("a")?.attr("abs:href").orEmpty(),
                    source = Sources.KAWAIIFU
                )
            }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return Jsoup.connect("$baseUrl/search-movie?keyword=$searchText").get()
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
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = model.url.toJsoup()
        return InfoModel(
            source = Sources.KAWAIIFU,
            title = model.title,
            url = model.url,
            alternativeNames = emptyList(),
            description = doc.select(".sub-desc p")
                .filter { it: Element -> it.select("strong").isEmpty() && it.select("iframe").isEmpty() }
                .joinToString("\n") { it.text() },
            imageUrl = model.imageUrl,
            genres = doc.select(".table a[href*=\"/tag/\"]").fastMap { tag -> tag.text() },
            chapters = try {
                doc.selectFirst("a[href*=\".html-episode\"]")
                    ?.attr("href")
                    ?.toJsoup()
                    ?.selectFirst(".list-ep")
                    ?.select("li")
                    ?.fastMap {
                        ChapterModel(
                            if (it.text().trim().toIntOrNull() != null) "Episode ${it.text().trim()}" else it.text().trim(),
                            it.selectFirst("a")?.attr("href").orEmpty(),
                            "",
                            model.url,
                            Sources.KAWAIIFU
                        )
                    }
                    ?.reversed()
                    .orEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val data = chapterModel.url
        val doc = data.toJsoup()

        val episodeNum = if (data.contains("ep=")) data.split("ep=")[1].split("&")[0].toIntOrNull() else null

        return doc.select(".list-server")
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
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = url.toJsoup()
        return ItemModel(
            title = doc.selectFirst(".title")?.text().orEmpty(),
            description = doc.select(".sub-desc p")
                .filter { it: Element -> it.select("strong").isEmpty() && it.select("iframe").isEmpty() }
                .joinToString("\n") { it.text() },
            imageUrl = doc.selectFirst("a.thumb > img")?.attr("src").orEmpty(),
            url = url,
            source = this
        )
    }

}