package com.programmersbox.anime_sources.anime


/*
object GogoAnimeVC : ShowApi(
    baseUrl = "https://gogoanime.sk",
    recentPath = "",
    allPath = ""
) {

    override val serviceName: String get() = "GOGOANIME_VC"
*/
/**
    private val headers = mapOf(
        "authority" to "ajax.gogo-load.com",
        "sec-ch-ua" to "\"Google Chrome\";v=\"89\", \"Chromium\";v=\"89\", \";Not A Brand\";v=\"99\"",
    "accept" to "text/html, */
/**; q=0.01",
        "dnt" to "1",
        "sec-ch-ua-mobile" to "?0",
        "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36",
        "origin" to baseUrl,
        "sec-fetch-site" to "cross-site",
        "sec-fetch-mode" to "cors",
        "sec-fetch-dest" to "empty",
        "referer" to "$baseUrl/"
    )
 **/
/*
    private val parseRegex = Regex("""<li>\s*\n.*\n.*<a\s*href=["'](.*?-episode-(\d+))["']\s*title=["'](.*?)["']>\n.*?img src="(.*?)"""")

    override suspend fun recent(page: Int): List<ItemModel> {
        val params = mapOf("page" to "$page", "type" to "1")
        return Jsoup.connect("https://ajax.gogo-load.com/ajax/page-recent-release.html")
            .data(params)
            .headers(headers)
            .get()
            .select("ul.items")
            .select("li")
            .fastMap {
                ItemModel(
                    title = it.select("a").attr("title"),
                    description = it.text(),
                    imageUrl = it.select("img").attr("abs:src"),
                    url = "$baseUrl/category/" + it.select("a").attr("href").replace(Regex("(-episode-(\\d+))"), ""),
                    source = Sources.GOGOANIME_VC
                )
            }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        val params = mapOf("page" to "$page", "type" to "2")
        return Jsoup.connect("https://ajax.gogo-load.com/ajax/page-recent-release.html")
            .data(params)
            .headers(headers)
            .get()
            .select("ul.items")
            .select("li")
            .fastMap {
                ItemModel(
                    title = it.select("a").attr("title"),
                    description = it.text(),
                    imageUrl = it.select("img").attr("abs:src"),
                    url = "$baseUrl/category/" + it.select("a").attr("href").replace(Regex("(-episode-(\\d+))"), ""),
                    source = Sources.GOGOANIME_VC
                )
            }
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = (if (!url.contains(baseUrl)) "$baseUrl$url" else url).toJsoup()

        val animeBody = doc.selectFirst(".anime_info_body_bg")
        val title = animeBody?.selectFirst("h1")?.text().orEmpty()
        val poster = animeBody?.selectFirst("img")?.attr("src").orEmpty()

        var description: String? = null

        animeBody?.select("p.type")?.fastForEach {
            when (it.selectFirst("span")?.text()?.trim()) {
                "Plot Summary:" -> {
                    description = it.text().replace("Plot Summary:", "").trim()
                }
            }
        }

        return ItemModel(
            title = title,
            description = description.orEmpty(),
            imageUrl = poster,
            url = url,
            source = Sources.GOGOANIME_VC
        )
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return Jsoup.connect("$baseUrl/search.html?keyword=$searchText").get()
            .select(""".last_episodes li""")
            .fastMap {
                ItemModel(
                    title = it.selectFirst(".name")?.text()?.replace(" (Dub)", "").orEmpty(),
                    description = it.text(),
                    imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                    url = fixUrl(it.selectFirst(".name > a")?.attr("href").orEmpty()),
                    source = Sources.GOGOANIME_VC
                )
            }
    }

    private const val episodeloadApi = "https://ajax.gogo-load.com/ajax/load-list-episode"

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = Jsoup.connect(model.url).get()

        val animeBody = doc.selectFirst(".anime_info_body_bg")
        val title = animeBody?.selectFirst("h1")?.text() ?: model.title
        val poster = animeBody?.selectFirst("img")?.attr("src") ?: model.imageUrl

        var description: String? = null
        val genre = mutableListOf<String>()

        animeBody?.select("p.type")?.fastForEach {
            when (it.selectFirst("span")?.text()?.trim()) {
                "Plot Summary:" -> {
                    description = it.text().replace("Plot Summary:", "").trim()
                }
                "Genre:" -> {
                    genre.addAll(it.select("a").fastMap { g -> g.attr("title") })
                }
            }
        }

        val animeId = doc.selectFirst("#movie_id")?.attr("value")
        val params = mapOf("ep_start" to "0", "ep_end" to "2000", "id" to animeId)

        val chapters = Jsoup.connect(episodeloadApi)
            .data(params)
            .get()
            .select("a")
            .fastMap {
                ChapterModel(
                    "Episode " + it.selectFirst(".name")?.text()?.replace("EP", "")?.trim().orEmpty(),
                    fixUrl(it.attr("href").trim()),
                    "",
                    model.url,
                    Sources.GOGOANIME_VC
                )
            }

        return InfoModel(
            source = Sources.GOGOANIME_VC,
            title = title,
            url = model.url,
            alternativeNames = emptyList(),
            description = description.orEmpty(),
            imageUrl = poster,
            genres = genre,
            chapters = chapters
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val doc = Jsoup.connect(chapterModel.url).get()
        val iframe = "https:" + doc.selectFirst("div.play-video > iframe")?.attr("src").orEmpty()
        val link = iframe.replace("streaming.php", "download")
        return Jsoup.connect(link)
            .headers(mapOf("Referer" to iframe))
            .get()
            .select(".dowload > a")
            .fastMap {
                if (it.hasAttr("download")) {
                    val qual = if (it.text().contains("HDP"))
                        "1080"
                    else
                        qualityRegex.find(it.text())?.destructured?.component1().toString()
                    listOf(
                        Storage(
                            link = it.attr("href"),
                            source = link,
                            filename = "${chapterModel.name}.mp4",
                            quality = qual,
                            sub = getQualityFromName(qual).value.toString()
                        )
                    )
                } else {
                    val url = it.attr("href")
                    extractors
                        .flatMap { e ->
                            if (url.startsWith(e.mainUrl)) {
                                //println(url + "\t" + e.name)
                                e.getUrl(url)
                            } else emptyList()
                        }
                }
            }
            .flatten()
            //.filter { it.link?.endsWith(".mp4") == true }
            .sortedByDescending { it.sub?.toIntOrNull() }
    }

    private val packedRegex = Regex("""eval\(function\(p,a,c,k,e,.*\)\)""")
    private fun getPacked(string: String): String? = packedRegex.find(string)?.value
    private fun getAndUnpack(string: String): String? = JsUnpacker(getPacked(string)).unpack()

    private val qualityRegex = Regex("(\\d+)P")
}*/

