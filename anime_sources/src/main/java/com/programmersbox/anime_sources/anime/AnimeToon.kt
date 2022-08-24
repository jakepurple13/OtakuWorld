package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.models.ItemModel
import org.jsoup.nodes.Element

abstract class AnimeToon(allPath: String, recentPath: String) : ShowApi(
    baseUrl = "http://www.animetoon.org",
    allPath = allPath,
    recentPath = recentPath
) {
    private fun toShowInfo(element: Element) = ItemModel(
        title = element.text(),
        description = "",
        imageUrl = "",
        url = element.attr("abs:href"),
        source = this
    )

    /*fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        try {
            it.onSuccess(doc.allElements.select("td").select("a[href^=http]").map(this::toShowInfo))
        } catch (e: Exception) {
            it.onError(e)
        }
    }

    fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
        try {
            var listOfStuff = doc.allElements.select("div.left_col").select("table#updates").select("a[href^=http]")
            if (listOfStuff.size == 0) listOfStuff = doc.allElements.select("div.s_left_col").select("table#updates").select("a[href^=http]")
            it(listOfStuff.map(this::toShowInfo).filter { !it.title.contains("Episode") })
        } catch (e: Exception) {
            it(e)
        }
    }

    @Throws(IOException::class)
    private fun getHtml(url: String): String? = try {
        // Build and set timeout values for the request.
        val connection = URL(url).openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0")
        connection.addRequestProperty("Accept-Language", "en-US,en;q=0.5")
        connection.addRequestProperty("Referer", "http://thewebsite.com")
        connection.connect()
        // Read and store the result line by line then return the entire string.
        val in1 = connection.getInputStream()
        val reader = BufferedReader(InputStreamReader(in1))
        val html = reader.readText()
        in1.close()
        html
    } catch (e: Exception) {
        null
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = Single.create {
        try {
            if (searchText.isNotEmpty()) {
                it.onSuccess(
                    Jsoup.connect("http://www.animetoon.org/toon/search?key=$searchText").get()
                        .select("div.right_col").select("h3").select("a[href^=http]").map(this::toShowInfo)
                )
            } else {
                it.onSuccess(searchListNonSingle(searchText, page, list))
            }
        } catch (e: Exception) {
            it.onSuccess(searchListNonSingle(searchText, page, list))
        }
    }*/

}

object AnimeToonApi : AnimeToon(
    allPath = "cartoon",
    recentPath = "updates"
) {
    override val serviceName: String get() = "ANIMETOON"
}

object AnimeToonDubbed : AnimeToon(
    allPath = "dubbed-anime",
    recentPath = "updates"
) {
    override val serviceName: String get() = "DUBBED_ANIME"
}

object AnimeToonMovies : AnimeToon(
    allPath = "movies",
    recentPath = "updates"
) {
    override val serviceName: String get() = "ANIMETOON_MOVIES"
}