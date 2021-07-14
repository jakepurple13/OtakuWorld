package com.programmersbox.anime_sources

import androidx.annotation.WorkerThread
import com.programmersbox.anime_sources.anime.*
import com.programmersbox.gsonutils.getApi
import com.programmersbox.gsonutils.header
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun putlockerTest() {

        //val f = "http://putlockers.fm/recently-added.html".toJsoup()
        //println(f)

        //PutlockerTV.IS_TEST = true

        /*val list = PutlockerTV.getList().blockingGet().randomOrNull()
            ?.toInfoModel()?.blockingGet()
            ?.chapters?.firstOrNull()
            ?.getChapterInfo()?.blockingGet()?.firstOrNull()
        //println(list.joinToString("\n"))
        println(list?.link)*/

        val search = PutlockerTV.searchList("the mask", list = emptyList()).blockingGet()
        println(search.first().toInfoModel().blockingGet())

        //println(list?.link?.toJsoup())

        /*val items = f.select("ul.list").select("div.item").map { it.select("a.thumb").attr("abs:href") }
        //println(items.joinToString("\n"))

        val d = items.firstOrNull()?.toJsoup()
        //println(d)

        val regex = "Base64.decode\\(\"(.*)\"\\)".toRegex().find(d?.toString().orEmpty())?.groups?.get(1)?.value

        //println(regex)

        val b = Jsoup.parse(String(Base64.getDecoder().decode(regex))).select("iframe").attr("abs:src")
        //println(b)

        val links = b.toJsoup()
        println(links)*/

    }

    @Test
    fun watchmovieTest() {
        val f = Jsoup.connect("https://watchmovie.movie/search.html").data("keyword", "One").get()
        println(f)
    }

    @Test
    fun animesimpleTest() {

        //val f = AnimeSimpleSubbed.getList().blockingGet()

        //println(f.joinToString("\n"))

        //println("https://ww1.animesimple.com/watch/197438-shokugeki-no-souma-shin-no-sara-episode-12-anime.html".toJsoup())

        //println("https://ww1.animesimple.com".toJsoup())

        val f = AnimeSimpleSubbed.searchList("mushi", list = emptyList()).blockingGet()
        //println(f.joinToString("\n"))
        val d = f.random().toInfoModel().blockingGet()
        println(d)
        val e = d.chapters.first().getChapterInfo().blockingGet().first().link
        println(e)
        println(e?.toJsoup())

        /*val f = com.programmersbox.anime_sources.utilities.getApi("https://animesimple.com/search") {
            addEncodedQueryParameter("q", "mushishi")
        }

        //println(f)

        //ww1.animesimple.com/series-list/

        val s = Jsoup.parse((f as ApiResponse.Success).body)
            .select("div#explore-container")
            .select("div.card-query")
            .map {
                ItemModel(
                    title = it.select("div.card[title]").attr("title"),
                    description = "",
                    imageUrl = it.select("img.card-img-top").attr("abs:src"),
                    url = "https:${it.select("h6.card-text a[href]").attr("href")}",
                    source = Sources.VIDSTREAMING
                )
            }

        println(s)

        val model = s.first()

        val doc = model.url.toJsoup()

        //println(doc)

        val id2 = com.programmersbox.anime_sources.utilities.getApi("https://animesimple.com/request") {
            addEncodedQueryParameter("anime-id", doc.select("input#animeid").attr("value"))
            addEncodedQueryParameter("epi-page", "1")
            addEncodedQueryParameter("top", "10000")
            addEncodedQueryParameter("bottom", "0")
        }.let { (it as? ApiResponse.Success)?.body }

        val id = InfoModel(
            title = model.title,
            description = doc.select("p.synopsis").text(),
            url = model.url,
            imageUrl = doc.select("img#series-image").attr("abs:src"),
            chapters = Jsoup.parse(id2).select("a.list-group-item").map {
                ChapterModel(
                    name = it.text(),
                    url = "https:${it.select("a").attr("href")}",
                    uploaded = "",
                    sourceUrl = model.url,
                    source = Sources.VIDSTREAMING
                )
            },
            genres = doc.select("a.badge, a.badge-secondary").select("a").eachText(),
            alternativeNames = emptyList(),
            source = Sources.VIDSTREAMING
        )

        println(id)

        val e = id.chapters.first().url.toJsoup()

        //println(e)

        val r = "var json = ([^;]*)".toRegex().find(e.toString())?.groups?.get(1)?.value?.fromJson<List<AnimeSimpleEpisode>>()

        println(r)

        val streamUrls = r?.map {

            val u = "src=['|\\\"]([^\\'|^\\\"]*)".toRegex().find(it.player!!)?.groups?.get(1)?.value

            Storage(
                link = u,
                source = it.host,
                quality = "Good",
                sub = it.type.toString()
            )
        }

        println(streamUrls)

        val url = streamUrls?.first()?.link!!

        val u = Jsoup.connect(url).header("Referer", "https://anistream.xyz").get()

        //println(u)

        val source = "<source src=\"(.*?)\"".toRegex()
        val token = "token\\s*=\\s*['\\\"|']([^\\\"']*)".toRegex()

        val finalUrl = when {
            source.containsMatchIn(u.toString()) -> source.find(u.toString())?.groups?.get(1)?.value
            token.containsMatchIn(u.toString()) -> {
                val id3 = url.split("/").last()
                Jsoup.connect("https://mp4.sh/v/$id3")
                    .header("Referer", url)
                    .data("token", token.find(u.toString())?.groups?.get(1)?.value)
                    .ignoreContentType(true)
                    .post()
                    .text()
                    .fromJson<AnimeSimpleFinalUrl>()
                    ?.file
            }
            else -> ""
        }

        println(finalUrl)*/

    }

    @Test
    fun animeflixTest() {

        val res = com.programmersbox.anime_sources.utilities.getApi("https://animeflix.io/api/search") {
            addEncodedQueryParameter("q", "Mushi")
        }

        println(res)

        /*val streamUrl = "https://animeflix.io/api/videos?episode_id"
        val episodeApi = "https://animeflix.io/api/episode"

        val a = "var episode = (.*?)\\}".toRegex().find(e.toString())?.groups?.get(1)?.value?.let { "$it}" }?.fromJson<AnimeFlixEpisode>()

        println(a)

        *//*val res = com.programmersbox.anime_sources.utilities.getApi(episodeApi) {
            addEncodedQueryParameter("episode_num", a?.episode?.toString())
            addEncodedQueryParameter("slug", a?.?.toString())
        }*/
    }

    data class AnimeFlixEpisode(
        val name: String?,
        val episode: Number?,
        val anime_id: Number?,
        val mal_id: Number?,
        val episode_id: Number?,
        val next_episode_url: String?
    )

    @Test
    fun vidStreamingTest() {

        //val f = "https://vidstreaming.io/popular".toJsoup()
        //println(f)

        //val f = Vidstreaming.getList().blockingGet().firstOrNull()?.toInfoModel()?.blockingGet()

        //println(f)

        val f = Vidstreaming.searchList("one punch", list = emptyList()).blockingGet().firstOrNull()

        println(f)

        /*val e = "https://vidstreaming.io/videos/tensei-shitara-slime-datta-ken-episode-24-9".toJsoup()
        //println(e)

        val v = e.select("div.play-video").select("iframe").attr("abs:src")
        println(v)

        val s = v.toJsoup()
        //println(s)
        val links = s.select("li.linkserver")
        println(links.joinToString("\n"))

        val xstream = links.find { it.text() == "Xstreamcdn" }?.attr("abs:data-video")
        println(xstream)

        //val x = xstream?.toJsoup()
        //println(x)

        val xApi = "https://fcdn.stream/api/source/${xstream?.split("/")?.last()}"
        println(xApi)
        val api = getApiPost(xApi).fromJson<Xstream>()
        println(api)
        val file = api?.data?.firstOrNull()?.file
        println(getApi(file!!))*/
    }

    @Test
    fun fboxTest() {
        //val f = "https://putlockernew.site/all-movies".toJsoup()//"https://putlockernew.site".toJsoup()
        //println(f)

        val g = "https://putlockernew.site/watch-movie/america-the-motion-picture-2021_cw9q88erb/976x909-full-movie-online?watchnow=1".toJsoup()
        //println(g)

        val d = g.select("div.show_player").select("iframe").attr("abs:src").toJsoup()
        //println(d)

        //val mix = "<iframe[^>]+src=\"([^\"]+)\"[^>]*><\\/iframe>".toRegex().find(doc.toString())!!.groups[1]!!.value
        val doc2 = d//Jsoup.connect(mix.trim()).get()
        val r = "\\}\\('(.+)',(\\d+),(\\d+),'([^']+)'\\.split\\('\\|'\\)".toRegex().find(doc2.toString())!!
        fun encodeBaseN(num: Int, n: Int): String {
            var num1 = num
            val fullTable = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            //println("$num - $n")
            val table = fullTable.substring(0..n - 1)
            if (num1 == 0) return table[0].toString()
            var ret = ""
            while (num1 > 0) {
                ret = (table[num1 % n].toString() + ret)
                num1 = Math.floorDiv(num, n)
                if (num1 == 1) num1 = 0
                //println("$ret - $num1")
            }
            return ret
        }
        val (obfucastedCode, baseTemp, countTemp, symbolsTemp) = r.destructured
        //println(r.destructured.toList().joinToString("\n"))
        val base = baseTemp.toInt()
        var count = countTemp.toInt()
        val symbols = symbolsTemp.split("|")
        val symbolTable = mutableMapOf<String, String>()
        while (count > 0) {
            count--
            val baseNCount = encodeBaseN(count, base)
            symbolTable[baseNCount] = if (symbols[count].isNotEmpty()) symbols[count] else baseNCount
        }
        val unpacked = "\\b(\\w+)\\b".toRegex().replace(obfucastedCode) { symbolTable[it.groups[0]!!.value].toString() }
        println(unpacked)
        val search = "MDCore\\.v.*?=\"([^\"]+)".toRegex().find(unpacked)?.groups?.get(1)?.value
        //"https:$search"
        println(search)
    }

    @Test
    fun kickassanimeTest() {

        val api = com.programmersbox.anime_sources.utilities.getApi("https://kickassanime.rs/search") {
            addEncodedQueryParameter("q", "mushi")
        }

        println(api)

    }

    @Test
    fun animekisaTest() {

        /*val f = AnimeKisaSubbed.getList().blockingGet().take(10)
        println(f.joinToString("\n"))

        val e = f.first().toInfoModel().blockingGet()
        println(e)

        val c = e.chapters.first().getChapterInfo().blockingGet()
        println(c)*/

        val f = AnimeKisaSubbed.searchList("mushi", list = emptyList()).blockingGet().take(10)
        println(f.joinToString("\n"))

        //val v = c.first().link?.toJsoup()
        //println(v)

    }

    @Test
    fun animeheavenpro() {

        val url = "https://www.animeheaven.pro"

        //val r = getApiPost("$url/ajax/list/recently_updated")

        //println(r)

        val recent = AnimeHeaven.getRecent().blockingGet()

        println(recent)

        val info = recent.first().toInfoModel().blockingGet()

        println(info)

        val chapter = info.chapters.first().getChapterInfo().blockingGet()

        println(chapter)

    }

    object AnimeHeaven : ShowApi(
        baseUrl = "https://www.animeheaven.pro",
        allPath = "",
        recentPath = ""
    ) {

        override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
            doc
                .select("section#RecentlyUpdated")
                .select("div.flw-item")
                .select("div.film-poster")
                .map {
                    //println(it)
                    ItemModel(
                        title = it.select("img").attr("title"),
                        description = "",
                        imageUrl = it.select("img").attr("data-src"),
                        url = it.select("a").attr("abs:href")
                            .let { it.dropLast(9 + it.split("-").last().length) }
                            .replace("/watch/", "/anime/"),
                        source = AnimeHeaven
                    )
                }
                .let(emitter::onSuccess)
        }

        override fun getList(doc: Document): Single<List<ItemModel>> {
            TODO("Not yet implemented")
        }

        override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create { emitter ->
            //println(doc)
            emitter.onSuccess(
                InfoModel(
                    source = AnimeHeaven,
                    url = source.url,
                    title = source.title,
                    description = doc.select("div.description").text(),
                    imageUrl = source.imageUrl,
                    genres = emptyList(),
                    chapters = doc
                        .select("div.slce-list")
                        .select("ul.nav")
                        .select("li.nav-item")
                        .map {
                            ChapterModel(
                                name = it.select("a").attr("title"),
                                url = it.select("a").attr("abs:href"),
                                uploaded = "",
                                source = AnimeHeaven,
                                sourceUrl = source.url
                            )
                        },
                    alternativeNames = emptyList()
                )
            )
        }

        data class Graph(
            val `@context`: String?,
            val `@type`: String?,
            val url: String?,
            val name: String?,
            val episodeNumber: String?,
            val position: String?,
            val dateModified: String?,
            val thumbnailUrl: String?,
            val director: String?,
            val datePublished: String?,
            val potentialAction: PotentialAction?,
            val video: Video?,
            val inLanguage: String?,
            val subtitleLanguage: String?
        )

        data class Base(val `@context`: String?, val `@graph`: List<Graph>?)

        data class PotentialAction(val `@type`: String?, val target: String?)

        data class Video(
            val `@type`: String?,
            val url: String?,
            val name: String?,
            val uploadDate: String?,
            val thumbnailUrl: String?,
            val description: String?,
            val videoQuality: String?,
            val embedUrl: String?,
            val potentialAction: PotentialAction?
        )

        override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
            val f = chapterModel.url.toJsoup()
                .also { println(it) }

            //https://storage.googleapis.com/134634f609a35cb2c21b/28893/3f2aad38a1e38efba011cbd4cb0491fb.mp4

            val a = f
                .select("div#servers-list")
                .select("ul.nav")
                .select("li.nav-item")
                .select("a")
                .map { Triple(it.attr("data-embed"), it.text(), it.attr("data-eid")) }

            println(a)

            a.forEach { p ->
                Jsoup.connect(p.first)
                    .data("Referer", chapterModel.url)
                    .data("id", p.third)
                    .get()
                    .let {
                        println("${p.second}${"------".repeat(10)}")
                        println(it)
                    }
            }

            /*val v = f
                .select("div#main-wrapper")
                .select("script")
                .first()
                .data()
                .fromJson<Base>()
                ?.`@graph`?.first()?.let {
                    Jsoup.connect(it.video?.embedUrl).get()
                }

            println(v)*/

            /*
            Storage(
                link = chapterModel.url.toJsoup().select("a[download^=http]").attr("abs:download"),
                source = chapterModel.url,
                quality = "Good",
                sub = "Yes"
            )
             */

            it.onSuccess(emptyList())
        }

    }

    @WorkerThread
    fun getApiPost(url: String, builder: okhttp3.Request.Builder.() -> Unit = {}): String? {
        val request = okhttp3.Request.Builder()
            .url(url)
            .apply(builder)
            .post(object : RequestBody() {
                override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()

                override fun writeTo(sink: BufferedSink) {
                }

            })
            .build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.code == 200) response.body!!.string() else null
    }

    @Test
    fun wcostreamTest() = runBlocking {

        /*val f = WcoDubbed.getList().blockingGet()

        println(f.size)

        val i = f.find { it.title == "Mushishi" }!!.toInfoModel().blockingGet()

        println(i)

        val e = i.chapters.first().getChapterInfo().blockingGet()

        println(e)*/

        /*val f = Jsoup.connect("https://www.wcostream.com/wonder-egg-priority-episode-7-english-dubbed").get()
            .select("div.ildate").select("a").attr("abs:href")
        println(f)*/

        //val f1 = WcoDubbed.getRecent().blockingGet()

        //println(f1.size)
        //println(f1.joinToString("\n"))

        //println(WcoDubbed.getSourceByUrl("https://www.wcostream.com/anime/mushi-shi-english-dubbed-guide"))

        val f = WcoDubbed.searchList("one", list = emptyList()).blockingGet()

        println(f.joinToString("\n"))

    }

    @Test
    fun addition_isCorrect() = runBlocking {
        /*val f = YtsService.build()//getJsonApi<Base>("https://yts.mx/api/v2/list_movies.json")
        val f1 = f.listMovies(YTSQuery.ListMoviesBuilder.getDefault().setQuery("big bang").build())
        val movies = f1?.blockingGet()?.data?.movies
        println(movies?.joinToString("\n"))

        val m1 = f.getMovie(
            YTSQuery.MovieBuilder().setMovieId(movies?.first()?.id?.toInt() ?: 30478).build()
        )

        println(m1.blockingGet())*/

        val f = Yts.getSourceByUrl("https://yts.mx/movies/doing-time-1979")
        println(f)

    }

    @Test
    fun animetoontvTest() {

        val f = "http://api.animetoon.tv/GetNewDubbed"

        val doc = getApi(f) {
            header(
                "User-Agent" to "okhttp/2.3.0",
                "App-LandingPage" to "http://www.mobi24.net/toon.html",
                "App-Name" to "#Toonmania",
                "Connection" to "Keep-Alive",
                "Host" to "api.animetoon.tv",
                "App-Version" to "7.7"
            )
        }

        println(doc)

    }

    @Test
    fun animetoon() {
        val f = AnimeToonDubbed

        val r = f.getRecent().blockingGet()

        println(r)

        val d = r.first().toInfoModel().blockingGet()

        println(d)

        val v = d.chapters.first().getChapterInfo().blockingGet()

        println(v)

        println(Jsoup.connect(v.random().link!!).get())
    }

    @Test
    fun dubbedAnime() {
        val f = DubbedAnimeBiz.getRecent()

        //println(info.joinToString("\n"))

        val v = f.blockingGet().first()
            .also { println(it) }
            .toInfoModel()
            .blockingGet()

        println(v)

        val c = v.chapters.last()
            .getChapterInfo()
            .blockingGet()

        println(c)
        /*val x = c.first().link?.let { getApi(it) }

        println(x)

        val b = x?.let { getApi("https:$it") }

        println(b)*/

    }

}


object DubbedAnimeBiz : ShowApi(
    baseUrl = "https://www.dubbedanime.biz",
    allPath = "topic/series/page/",
    recentPath = "status/ongoing/page/"
) {

    override fun recentPage(page: Int): String = page.toString()
    override fun allPage(page: Int): String = page.toString()

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc
            .select("div.ml-item")
            .map {
                ItemModel(
                    title = it.select("a.ml-mask").attr("oldtitle"),
                    description = "",
                    imageUrl = it.select("img").attr("abs:src"),
                    url = it.select("a.ml-mask").attr("abs:href"),
                    source = this
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc
            .select("div.ml-item")
            .map {
                ItemModel(
                    title = it.select("a.ml-mask").attr("oldtitle"),
                    description = "",
                    imageUrl = it.select("img").attr("abs:src"),
                    url = it.select("a.ml-mask").attr("abs:href"),
                    source = this
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create { emitter ->
        InfoModel(
            source = this,
            url = source.url,
            title = source.title,
            description = "",
            imageUrl = source.imageUrl,
            genres = doc.select("div.mvic-info").select("div.mvici-left").select("a[rel=tag]").eachText(),
            chapters = doc.select("div#seasonss").select("div.les-title").map {
                ChapterModel(
                    name = it.select("a").text(),
                    url = it.select("a").attr("abs:href"),
                    uploaded = "",
                    source = this,
                    sourceUrl = source.url
                )
            },
            alternativeNames = emptyList()
        )
            .let(emitter::onSuccess)
    }

    private val serverList = listOf(
        "rapidvideo",
        "vip",
        "drive",
        "drives",
        "photo",
        "openload",
        "streamango"
    )
        .map { "$baseUrl/ajax-get-link-stream/?server=$it&filmId=" }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->

        val f = Jsoup.connect(chapterModel.url).get()
            //.also { println(it) }
            .select("div.list-episodes")
            .select("select.form-control")
            .select("option")
            .map { it.attr("abs:href") to it.attr("episodeid") }
            .first { it.first == chapterModel.url }
            .second
            .let {
                val link = Jsoup.parse(getApi("https:${getApi("$baseUrl/ajax-get-link-stream/?server=rapidvideo&filmId=$it")}"))
                    .select("li.linkserver")
                    .eachAttr("data-video")
                    //.also { println(it) }
                    .first { it.contains(".mp4") }
                    .let { Jsoup.connect(it).get().select("div#videolink").text().let { "https:$it" } }
                /*serverList.map { s -> "$s$it" }
                    .first {  }*/
                Storage(
                    link = link,
                    source = chapterModel.url,
                    quality = "Good",
                    sub = "Yes"
                )
            }

        emitter.onSuccess(listOf(f))
    }

}
