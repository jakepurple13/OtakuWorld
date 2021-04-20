package com.programmersbox.manga_sources

import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getApi
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.manga_sources.manga.MangaFourLife
import com.programmersbox.models.ItemModel
import org.junit.Test
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

        val j = getJsonApi<List<Life>>("https://manga4life.com/_search.php") {
            addHeader("vm.SortBy", "lt")
            addHeader("filter:vm.Search.SeriesName", "Mushi")
        }
            ?.also { println(it.take(5)) }
            ?.map {
                ItemModel(
                    title = it.s.toString(),
                    description = "",
                    url = "https://manga4life.com/manga/${it.i}",
                    imageUrl = "https://static.mangaboss.net/cover/${it.i}.jpg",
                    source = MangaFourLife
                )
            }
            ?.random()?.toInfoModel()?.blockingGet()

        println(j)

        val toMangaModel: (LifeBase) -> ItemModel = {
            println(it)
            ItemModel(
                title = it.s.toString(),
                description = "Last updated: ${it.ls}",
                url = "https://manga4life.com/manga/${it.i}",
                imageUrl = "https://cover.mangabeast01.com/cover/${it.i}.jpg",
                source = MangaFourLife
            )
        }

        val url = "https://manga4life.com/search/?sort=vm&desc=true"

        val r = "vm\\.Directory = (.*?.*;)".toRegex()
            .find(
                /*Jsoup.connect("")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:71.0) Gecko/20100101 Firefox/71.0")
                    .header("Referer", "https://manga4life.com")
                    .get()
                    .html()*/
                getApi(url).toString()
            )
            ?.groupValues?.get(1)?.dropLast(1)
            .also { println(it?.length) }
            ?.fromJson<List<LifeBase>>()
            ?.sortedByDescending { m -> m.lt?.let { 1000 * it.toDouble() } }
            ?.map(toMangaModel)

        println(r)

        val file = File("/Users/jrein/Documents/testjson.json")

        println(file.readText().length)

        //?.sortedByDescending { it["v"].string }

        //println(j?.joinToString("\n"))

        /*val f = "vm\\.Directory = (.*?.*;)".toRegex()
            .find(Jsoup.connect("https://manga4life.com/search/?sort=vm&desc=true").get().html())
            ?.groupValues?.get(1)?.dropLast(1)

        println(f?.takeLast(10))

        val f1 = f?.split("},")?.map { "$it}" }

        println(f1?.take(5)?.joinToString("}\n"))

        println("---".repeat(10))

        val f2 = f1?.get(4)

        println(f2)

        println(f2?.fromJson<BaseOfLife>())

        println(f?.fromJson<List<BaseOfLife>>())*/

        /*MangaFourLife.getList(1)
            .subscribeBy { println(it) }*/
    }

    private data class Life(val i: String?, val s: String?, val a: List<String>?)

    private data class LifeChapter(val Chapter: String?, val Type: String?, val Date: String?, val ChapterName: String?)

    private data class LifeBase(
        val i: String?,
        val s: String?,
        val o: String?,
        val ss: String?,
        val ps: String?,
        val t: String?,
        val v: String?,
        val vm: String?,
        val y: String?,
        val a: List<String>?,
        val al: List<String>?,
        val l: String?,
        val lt: Number?,
        val ls: String?,
        val g: List<String>?,
        val h: Boolean?
    )
}