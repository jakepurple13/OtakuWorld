package com.programmersbox.anime_sources

import androidx.annotation.WorkerThread
import com.programmersbox.anime_sources.anime.AnimeKisaSubbed
import com.programmersbox.anime_sources.anime.AnimeToonDubbed
import com.programmersbox.anime_sources.anime.WcoDubbed
import com.programmersbox.anime_sources.anime.Yts
import com.programmersbox.gsonutils.getApi
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
    fun animekisaTest() {

        val f = AnimeKisaSubbed.getList().blockingGet().take(10)
        println(f.joinToString("\n"))

        val e = f.first().toInfoModel().blockingGet()
        println(e)

        val c = e.chapters.first().getChapterInfo().blockingGet()
        println(c)

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
                                source = AnimeHeaven
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

        println(WcoDubbed.getSourceByUrl("https://www.wcostream.com/anime/mushi-shi-english-dubbed-guide"))

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
    fun animetoon() {
        val f = AnimeToonDubbed

        val r = f.getRecent().blockingGet()

        println(r)

        val d = r.first().toInfoModel().blockingGet()

        println(d)

        val v = d.chapters.first().getChapterInfo().doOnError { }.blockingGet()

        println(v)
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
                    source = this
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
