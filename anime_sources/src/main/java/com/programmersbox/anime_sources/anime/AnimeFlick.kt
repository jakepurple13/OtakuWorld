package com.programmersbox.anime_sources.anime

import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.ItemModel
import org.jsoup.Jsoup

object AnimeFlick : ShowApi(
    baseUrl = "https://animeflick.net",
    recentPath = "updates", allPath = "Anime-List"
) {
    override val serviceName: String get() = "ANIMEFLICK"
    override val canScroll: Boolean get() = true
    override val canScrollAll: Boolean get() = true
    override fun recentPage(page: Int): String = "-$page"
    override fun allPage(page: Int): String = "/All/$page"

    override suspend fun recent(page: Int): List<ItemModel> {
        return recentPath(page)
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
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return all(page)
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
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return Jsoup.connect("$baseUrl/search.php?search=$searchText").get()
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
    }

}

