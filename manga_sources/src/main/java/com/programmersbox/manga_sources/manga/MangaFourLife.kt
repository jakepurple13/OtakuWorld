package com.programmersbox.manga_sources.manga

import android.annotation.SuppressLint
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonElement
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getApi
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.models.*
import io.reactivex.Single
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

object MangaFourLife : ApiService {

    override val baseUrl: String = "https://manga4life.com"

    override val serviceName: String get() = "MANGA_4_LIFE"

    private val headers: List<Pair<String, String>> = listOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:71.0) Gecko/20100101 Firefox/71.0"
    )

    private val mangaList = mutableListOf<ItemModel>()

    private fun getManga(pageNumber: Int): List<ItemModel> {
        if (mangaList.isEmpty()) {
            mangaList.addAll(
                ("vm\\.Directory = (.*?.*;)".toRegex()
                    .find(getApi("https://manga4life.com/search/?sort=lt&desc=true").toString())
                    ?.groupValues?.get(1)?.dropLast(1)
                    ?.fromJson<List<LifeBase>>()
                    ?.sortedByDescending { m -> m.lt?.let { 1000 * it.toDouble() } }
                    ?.map(toMangaModel) ?: getApiVersion())
                    .orEmpty()
            )
        }
        val endRange = ((pageNumber * 24) - 1).let { if (it <= mangaList.lastIndex) it else mangaList.lastIndex }
        return mangaList.subList((pageNumber - 1) * 24, endRange)
    }

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        try {
            getManga(page).let(emitter::onSuccess)
        } catch (e: Exception) {
            emitter.onError(e)
        }
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        try {
            getManga(page).let(emitter::onSuccess)
        } catch (e: Exception) {
            emitter.onError(e)
        }
    }

    private fun getApiVersion() = getJsonApi<List<Life>>("https://manga4life.com/_search.php")?.map {
        ItemModel(
            title = it.s.toString(),
            description = "",
            url = "https://manga4life.com/manga/${it.i}",
            imageUrl = "https://cover.nep.li/cover/${it.i}.jpg",
            source = this
        )
    }

    private val toMangaModel: (LifeBase) -> ItemModel = {
        ItemModel(
            title = it.s.toString(),
            description = "Last updated: ${it.ls}",
            url = "https://manga4life.com/manga/${it.i}",
            imageUrl = "https://cover.nep.li/cover/${it.i}.jpg",
            source = this
        )
    }

    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create { emitter ->
        val doc = Jsoup.connect(model.url).get()
        val description = doc.select("div.BoxBody > div.row").select("div.Content").text()
        val genres = "\"genre\":[^:]+(?=,|\$)".toRegex().find(doc.html())
            ?.groupValues?.get(0)?.removePrefix("\"genre\": ")?.fromJson<List<String>>().orEmpty()
        val altNames = "\"alternateName\":[^:]+(?=,|\$)".toRegex().find(doc.html())
            ?.groupValues?.get(0)?.removePrefix("\"alternateName\": ")?.fromJson<List<String>>().orEmpty()
        emitter.onSuccess(
            InfoModel(
                title = model.title,
                description = description,
                url = model.url,
                imageUrl = model.imageUrl,
                chapters = "vm.Chapters = (.*?);".toRegex().find(doc.html())
                    ?.groupValues?.get(0)?.removePrefix("vm.Chapters = ")?.removeSuffix(";")
                    ?.fromJson<List<LifeChapter>>()?.map {
                        ChapterModel(
                            name = chapterImage(it.Chapter!!),
                            url = "https://manga4life.com/read-online/${
                                model.url.split("/")
                                    .last()
                            }${chapterURLEncode(it.Chapter)}",
                            uploaded = it.Date.toString(),
                            source = this
                        ).apply {
                            try {
                                uploadedTime = dateFormat.parse(uploaded.substringBefore(" "))?.time
                            } catch (_: Exception) {
                            }
                        }
                    }.orEmpty(),
                genres = genres,
                alternativeNames = altNames,
                source = this
            )
        )
    }

    override fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {
        val doc = Jsoup.connect(url).get()
        val title = doc.select("li.list-group-item, li.d-none, li.d-sm-block").select("h1").text()
        val description = doc.select("div.BoxBody > div.row").select("div.Content").text()
        it.onSuccess(
            ItemModel(
                title = title,
                description = description,
                url = url,
                imageUrl = doc.select("img.img-fluid, img.bottom-5").attr("abs:src"),
                source = this
            )
        )
    }

    private fun chapterURLEncode(e: String): String {
        var index = ""
        val t = e.substring(0, 1).toInt()
        if (1 != t) index = "-index-$t"
        val n = e.substring(1, e.length - 1)
        var suffix = ""
        val path = e.substring(e.length - 1).toInt()
        if (0 != path) suffix = ".$path"
        return "-chapter-$n$index$suffix.html"
    }

    private fun chapterImage(e: String): String {
        val a = e.substring(1, e.length - 1)
        val b = e.substring(e.length - 1).toInt()
        return if (b == 0) a else "$a.$b"
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->
        val document = Jsoup.connect(chapterModel.url).get()
        val script = document.select("script:containsData(MainFunction)").first().data()
        val curChapter = script.substringAfter("vm.CurChapter = ").substringBefore(";").fromJson<JsonElement>()!!

        val pageTotal = curChapter["Page"].string.toInt()

        val host = "https://" + script.substringAfter("vm.CurPathName = \"").substringBefore("\"")
        val titleURI = script.substringAfter("vm.IndexName = \"").substringBefore("\"")
        val seasonURI = curChapter["Directory"].string
            .let { if (it.isEmpty()) "" else "$it/" }
        val path = "$host/manga/$titleURI/$seasonURI"

        val chNum = chapterImage(curChapter["Chapter"].string)

        IntRange(1, pageTotal).mapIndexed { i, _ ->
            val imageNum = (i + 1).toString().let { "000$it" }.let { it.substring(it.length - 3) }
            "$path$chNum-$imageNum.png"
        }
            .map { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }
            .let(emitter::onSuccess)
    }

    override val canScroll: Boolean = true

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