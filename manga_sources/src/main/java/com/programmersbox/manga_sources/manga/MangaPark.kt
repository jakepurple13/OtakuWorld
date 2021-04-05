package com.programmersbox.manga_sources.manga

import android.annotation.SuppressLint
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.manga_sources.Sources
import com.programmersbox.manga_sources.utilities.asJsoup
import com.programmersbox.manga_sources.utilities.cloudflare
import com.programmersbox.models.*
import io.reactivex.Single
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

object MangaPark : ApiService {

    override val baseUrl = "https://mangapark.net"

    override val serviceName: String get() = "MANGA_PARK"

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = try {
        if (searchText.isBlank()) {
            super.searchList(searchText, page, list)
        } else {
            Single.create { emitter ->
                emitter.onSuccess(
                    cloudflare("$baseUrl/search?q=$searchText&page=$page&st-ss=1").execute().asJsoup()
                        .select("div.item").map {
                            val title = it.select("a.cover")
                            ItemModel(
                                title = title.attr("title"),
                                description = it.select("p.summary").text(),
                                url = "${baseUrl}${title.attr("href")}",
                                imageUrl = title.select("img").attr("abs:src"),
                                source = this
                            )
                        }
                )
            }

        }
    } catch (e: Exception) {
        super.searchList(searchText, page, list)
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        cloudflare("$baseUrl/genre/$page").execute().asJsoup()
            .select("div.ls1").select("div.d-flex, div.flex-row, div.item")
            .map {
                ItemModel(
                    title = it.select("a.cover").attr("title"),
                    description = "",
                    url = "${baseUrl}${it.select("a.cover").attr("href")}",
                    imageUrl = it.select("a.cover").select("img").attr("abs:src"),
                    source = Sources.MANGA_PARK
                )
            }
            .let { emitter.onSuccess(it) }
    }

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        cloudflare("$baseUrl/latest/$page").execute().asJsoup()
            .select("div.ls1").select("div.d-flex, div.flex-row, div.item")
            .map {
                ItemModel(
                    title = it.select("a.cover").attr("title"),
                    description = "",
                    url = "${baseUrl}${it.select("a.cover").attr("href")}",
                    imageUrl = it.select("a.cover").select("img").attr("abs:src"),
                    source = Sources.MANGA_PARK
                )
            }
            .let { emitter.onSuccess(it) }
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create { emitter ->
        val doc = cloudflare(model.url).execute().asJsoup()//Jsoup.connect(model.mangaUrl).get()
        val genres = mutableListOf<String>()
        val alternateNames = mutableListOf<String>()
        doc.select(".attr > tbody > tr").forEach {
            when (it.getElementsByTag("th").first().text().trim().toLowerCase(Locale.getDefault())) {
                "genre(s)" -> genres.addAll(it.getElementsByTag("a").map(Element::text))
                "alternative" -> alternateNames.addAll(it.text().split("l"))
            }
        }
        emitter.onSuccess(
            InfoModel(
                title = model.title,
                description = doc.select("p.summary").text(),
                url = model.url,
                imageUrl = model.imageUrl,
                chapters = chapterListParse(doc),
                genres = genres,
                alternativeNames = alternateNames,
                source = this
            )
        )
    }

    private fun chapterListParse(response: Document): List<ChapterModel> {
        fun List<SChapter>.getMissingChapters(allChapters: List<SChapter>): List<SChapter> {
            val chapterNums = this.map { it.chapterNumber }
            return allChapters.filter { it.chapterNumber !in chapterNums }.distinctBy { it.chapterNumber }
        }

        fun List<SChapter>.filterOrAll(source: String): List<SChapter> {
            val chapters = this.filter { it.scanlator!!.contains(source) }
            return if (chapters.isNotEmpty()) {
                (chapters + chapters.getMissingChapters(this)).sortedByDescending { it.chapterNumber }
            } else {
                this
            }
        }

        val mangaBySource = response.select("div[id^=stream]")
            .map { sourceElement ->
                var lastNum = 0F
                val sourceName = sourceElement.select("i + span").text()

                sourceElement.select(".volume .chapter li")
                    .reversed() // so incrementing lastNum works
                    .map { chapterElement ->
                        chapterFromElement(chapterElement, sourceName, lastNum)
                            .also { lastNum = it.chapterNumber }
                    }
                    .distinctBy { it.chapterNumber } // there's even duplicate chapters within a source ( -.- )
            }

        val chapters = mangaBySource.maxByOrNull { it.count() }!!
        return (chapters + chapters.getMissingChapters(mangaBySource.flatten())).sortedByDescending { it.chapterNumber }.map {
            ChapterModel(
                name = it.name,
                url = "${baseUrl}${it.url}",
                uploaded = it.originalDate,
                source = this
            ).apply { uploadedTime = it.dateUploaded }
        }
    }

    private class SChapter {
        var url: String = ""
        var name: String = ""
        var chapterNumber: Float = 0f
        var dateUploaded: Long? = null
        var originalDate: String = ""
        var scanlator: String? = null
    }

    private fun chapterFromElement(element: Element, source: String, lastNum: Float): SChapter {
        fun Float.incremented() = this + .00001F
        fun Float?.orIncrementLastNum() = if (this == null || this < lastNum) lastNum.incremented() else this

        return SChapter().apply {
            url = element.select(".tit > a").first().attr("href").replaceAfterLast("/", "")
            name = element.select(".tit > a").first().text()
            // Get the chapter number or create a unique one if it's not available
            chapterNumber = Regex("""\b\d+\.?\d?\b""").findAll(name)
                .toList()
                .map { it.value.toFloatOrNull() }
                .let { nums ->
                    when {
                        nums.count() == 1 -> nums[0].orIncrementLastNum()
                        nums.count() >= 2 -> nums[1].orIncrementLastNum()
                        else -> lastNum.incremented()
                    }
                }
            dateUploaded = element.select(".time").firstOrNull()?.text()?.trim()?.let { parseDate(it) }
            originalDate = element.select(".time").firstOrNull()?.text()?.trim().toString()
            scanlator = source
        }
    }


    private val dateFormat = SimpleDateFormat("MMM d, yyyy, HH:mm a", Locale.ENGLISH)
    private val dateFormatTimeOnly = SimpleDateFormat("HH:mm a", Locale.ENGLISH)

    @SuppressLint("DefaultLocale")
    private fun parseDate(date: String): Long? {
        val lcDate = date.toLowerCase()
        if (lcDate.endsWith("ago")) return parseRelativeDate(lcDate)

        // Handle 'yesterday' and 'today'
        var relativeDate: Calendar? = null
        if (lcDate.startsWith("yesterday")) {
            relativeDate = Calendar.getInstance()
            relativeDate.add(Calendar.DAY_OF_MONTH, -1) // yesterday
        } else if (lcDate.startsWith("today")) {
            relativeDate = Calendar.getInstance()
        }

        relativeDate?.let {
            // Since the date is not specified, it defaults to 1970!
            val time = dateFormatTimeOnly.parse(lcDate.substringAfter(' '))
            val cal = Calendar.getInstance()
            cal.time = time!!

            // Copy time to relative date
            it.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
            it.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
            return it.timeInMillis
        }

        return dateFormat.parse(lcDate)?.time
    }

    /**
     * Parses dates in this form:
     * `11 days ago`
     */
    private fun parseRelativeDate(date: String): Long? {
        val trimmedDate = date.split(" ")

        if (trimmedDate[2] != "ago") return null

        val number = when (trimmedDate[0]) {
            "a" -> 1
            else -> trimmedDate[0].toIntOrNull() ?: return null
        }
        val unit = trimmedDate[1].removeSuffix("s") // Remove 's' suffix

        val now = Calendar.getInstance()

        // Map English unit to Java unit
        val javaUnit = when (unit) {
            "year" -> Calendar.YEAR
            "month" -> Calendar.MONTH
            "week" -> Calendar.WEEK_OF_MONTH
            "day" -> Calendar.DAY_OF_MONTH
            "hour" -> Calendar.HOUR
            "minute" -> Calendar.MINUTE
            "second" -> Calendar.SECOND
            else -> return null
        }

        now.add(javaUnit, -number)

        return now.timeInMillis
    }

    /*override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = cloudflare(url).execute().asJsoup()
        val titleAndImg = doc.select("div.w-100, div.cover").select("img")
        return MangaModel(
            title = titleAndImg.attr("title"),
            description = doc.select("p.summary").text(),
            mangaUrl = url,
            imageUrl = titleAndImg.attr("abs:src"),
            source = Sources.MANGA_PARK
        )
    }*/

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->
        cloudflare(chapterModel.url).execute().asJsoup().toString()
            .substringAfter("var _load_pages = ").substringBefore(";").fromJson<List<Pages>>().orEmpty()
            .map { if (it.u.orEmpty().startsWith("//")) "https:${it.u}" else it.u.orEmpty() }
            .map { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }
            .let { emitter.onSuccess(it) }
    }

    private data class Pages(val n: Number?, val w: String?, val h: String?, val u: String?)

    override val canScroll: Boolean = true
}