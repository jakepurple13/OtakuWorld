package com.programmersbox.anime_sources

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
    fun wcostreamTest() {

        val f = WcoDubbed.getList().blockingGet()

        println(f.size)

        val i = f.random().toInfoModel().blockingGet()

        println(i)

        val e = i.chapters.first().getChapterInfo().blockingGet()

        println(e)

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
