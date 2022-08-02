package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.*
import com.programmersbox.rxutils.invoke
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

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

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create {
        try {
            it.onSuccess(doc.allElements.select("td").select("a[href^=http]").map(this::toShowInfo))
        } catch (e: Exception) {
            it.onError(e)
        }
    }

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create {
        try {
            var listOfStuff = doc.allElements.select("div.left_col").select("table#updates").select("a[href^=http]")
            if (listOfStuff.size == 0) listOfStuff = doc.allElements.select("div.s_left_col").select("table#updates").select("a[href^=http]")
            it(listOfStuff.map(this::toShowInfo).filter { !it.title.contains("Episode") })
        } catch (e: Exception) {
            it(e)
        }
    }

    @Suppress("RegExpRedundantEscape")
    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        try {
            val m = "<iframe src=\"([^\"]+)\"[^<]+<\\/iframe>".toRegex().toPattern().matcher(getHtml(chapterModel.url)!!)
            val list = arrayListOf<String>()
            while (m.find()) list.add(m.group(1)!!)
            val regex = "(http|https):\\/\\/([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\%\\&\\-\\_\\?\\.\\=\\/])+(part[0-9])+.(\\w*)"
            when (val htmlc = if (regex.toRegex().toPattern().matcher(list[0]).find()) list else getHtml(list[0])) {
                is ArrayList<*> -> {
                    val urlList = mutableListOf<Storage?>()
                    for (i in htmlc) {
                        val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern().matcher(getHtml(i.toString())!!)
                        while (reg.find()) urlList.add(reg.group(1).fromJson<NormalLink>()?.normal?.storage?.get(0))
                    }
                    it.onSuccess(urlList.filterNotNull())
                }
                is String -> {
                    val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern().matcher(htmlc)
                    while (reg.find()) it.onSuccess(listOfNotNull(reg.group(1).fromJson<NormalLink>()?.normal?.storage?.get(0)))
                }
                else -> it.onError(Exception("Something went wrong"))
            }

        } catch (e: Exception) {
            it.onError(e)
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

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create {
        try {
            fun getStuff(document: Document) = document.allElements.select("div#videos").select("a[href^=http]")
                .map { ChapterModel(it.text(), it.attr("abs:href"), "", source.url, source.source) }
            it(
                InfoModel(
                    source = this,
                    url = source.url,
                    title = doc.select("div.right_col h1").text(),
                    description = doc.allElements.select("div#series_details").let { element ->
                        if (element.select("span#full_notes").hasText())
                            element.select("span#full_notes").text().removeSuffix("less")
                        else
                            element.select("div:contains(Description:)").select("div").text().let {
                                try {
                                    it.substring(it.indexOf("Description: ") + 13, it.indexOf("Category: "))
                                } catch (e: StringIndexOutOfBoundsException) {
                                    it
                                }
                            }
                    },
                    imageUrl = doc.select("div.left_col").select("img[src^=http]#series_image")?.attr("abs:src").orEmpty(),
                    genres = doc.select("span.red_box").select("a[href^=http]").eachText(),
                    chapters = getStuff(doc) + doc.allElements.select("ul.pagination").select(" button[href^=http]")
                        .flatMap { getStuff(it.attr("abs:href").toJsoup()) },
                    alternativeNames = emptyList()
                )
            )
        } catch (e: Exception) {
            it(e)
        }
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
    }

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