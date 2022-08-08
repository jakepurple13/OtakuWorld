package com.programmersbox.manga_sources.manga

import androidx.compose.ui.util.fastMap
import app.cash.zipline.Zipline
import com.programmersbox.models.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object MangaHere : ApiService {

    override val baseUrl = "https://www.mangahere.cc"

    override val serviceName: String get() = "MANGA_HERE"

    private val client by lazy {
        createHttpClient {
            defaultRequest {
                header("Referer", baseUrl)
                cookie("isAdult", "1")
                url.takeFrom(URLBuilder().takeFrom(baseUrl).appendPathSegments(url.encodedPath))
            }
            install(HttpCache)
            followRedirects = true
        }
    }

    override suspend fun recent(page: Int): List<ItemModel> {
        return client.get("/directory/$page.htm?latest").body<Document>()
            .select(".manga-list-1-list li").fastMap {
                ItemModel(
                    title = it.select("a").first()!!.attr("title"),
                    description = "",
                    url = it.select("a").first()!!.attr("abs:href"),
                    imageUrl = it.select("img.manga-list-1-cover").first()?.attr("src") ?: "",
                    source = this
                ).apply { extras["Referer"] = baseUrl }
            }.filter { it.title.isNotEmpty() }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return client.get("/directory/$page.htm").body<Document>()
            .select(".manga-list-1-list li").fastMap {
                ItemModel(
                    title = it.select("a").first()!!.attr("title"),
                    description = "",
                    url = it.select("a").first()!!.attr("abs:href"),
                    imageUrl = it.select("img.manga-list-1-cover").first()?.attr("src") ?: "",
                    source = this
                ).apply { extras["Referer"] = baseUrl }
            }.filter { it.title.isNotEmpty() }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return client.get("/search") {
            parameter("page", page.toString())
            parameter("title", searchText.toString())
            parameter("sort", null)
            parameter("stype", 1.toString())
            parameter("name", null)
            parameter("author_method", "cw")
            parameter("author", null)
            parameter("artist_method", "cw")
            parameter("artist", null)
            parameter("rating_method", "eq")
            parameter("rating", null)
            parameter("released_method", "eq")
            parameter("released", null)
        }.body<Document>()
            .select(".manga-list-4-list > li")
            .fastMap {
                ItemModel(
                    title = it.select("a").first()!!.attr("title"),
                    description = it.select("p.manga-list-4-item-tip").last()!!.text(),
                    url = "$baseUrl${it.select(".manga-list-4-item-title > a").first()!!.attr("href")}",
                    imageUrl = it.select("img.manga-list-4-cover").first()!!.attr("abs:src"),
                    source = this
                ).apply { extras["Referer"] = baseUrl }
            }
            .filter { it.title.isNotEmpty() }
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = client.get { url(model.url) }.body<Document>()
        return InfoModel(
            title = model.title,
            description = doc.select("p.fullcontent").text(),
            url = model.url,
            imageUrl = doc.select("img.detail-info-cover-img").select("img[src^=http]").attr("abs:src"),
            chapters = doc.select("div[id=chapterlist]").select("ul.detail-main-list").select("li").map {
                ChapterModel(
                    name = it.select("a").select("p.title3").text(),
                    url = it.select("a").attr("abs:href"),
                    uploaded = it.select("a").select("p.title2").text(),
                    sourceUrl = model.url,
                    source = this
                ).apply { uploadedTime = parseChapterDate(uploaded) }
            },
            genres = doc.select("p.detail-info-right-tag-list").select("a").eachText(),
            alternativeNames = emptyList(),
            source = this
        ).apply { extras["Referer"] = baseUrl }
    }

    private fun parseChapterDate(date: String): Long {
        return if ("Today" in date || " ago" in date) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else if ("Yesterday" in date) {
            Calendar.getInstance().apply {
                add(Calendar.DATE, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            try {
                SimpleDateFormat("MMM dd,yyyy", Locale.ENGLISH).parse(date)?.time ?: 0L
            } catch (e: ParseException) {
                0L
            }
        }
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = client.get { url(url) }.body<Document>()
        return ItemModel(
            title = doc.select("span.detail-info-right-title-font").text(),
            description = doc.select("p.fullcontent").text(),
            url = url,
            imageUrl = doc.select("img.detail-info-cover-img").select("img[src^=http]").attr("abs:src"),
            source = this
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        return pageListParse(Jsoup.connect(chapterModel.url).header("Referer", baseUrl).get())
            .fastMap { p ->
                Storage(link = p, source = chapterModel.url, quality = "Good", sub = "Yes").apply {
                    headers["Referer"] = baseUrl
                }
            }
    }

    private fun pageListParse(document: Document): List<String> {
        val bar = document.select("script[src*=chapter_bar]")
        val zipline = Zipline.create(Dispatchers.IO)

        /*
            function to drop last imageUrl if it's broken/unneccesary, working imageUrls are incremental (e.g. t001, t002, etc); if the difference between
            the last two isn't 1 or doesn't have an Int at the end of the last imageUrl's filename, drop last Page
        */
        fun List<String>.dropLastIfBroken(): List<String> {
            val list = this.takeLast(2).fastMap { page ->
                try {
                    page.substringBeforeLast(".").substringAfterLast("/").takeLast(2).toInt()
                } catch (_: NumberFormatException) {
                    return this.dropLast(1)
                }
            }
            return when {
                list[0] == 0 && 100 - list[1] == 1 -> this
                list[1] - list[0] == 1 -> this
                else -> this.dropLast(1)
            }
        }

        // if-branch is for webtoon reader, else is for page-by-page
        return if (bar.isNotEmpty()) {
            val script = document.select("script:containsData(function(p,a,c,k,e,d))").html().removePrefix("eval")
            val deobfuscatedScript = zipline.quickJs.evaluate(script).toString()
            val urls = deobfuscatedScript.substringAfter("newImgs=['").substringBefore("'];").split("','")
            zipline.close()
            urls.fastMap { s -> "https:$s" }
        } else {
            val html = document.html()
            val link = document.location()
            var secretKey = extractSecretKey(html, zipline)
            val chapterIdStartLoc = html.indexOf("chapterid")
            val chapterId = html.substring(
                chapterIdStartLoc + 11,
                html.indexOf(";", chapterIdStartLoc)
            ).trim()
            val chapterPagesElement = document.select(".pager-list-left > span").first()
            val pagesLinksElements = chapterPagesElement!!.select("a")
            val pagesNumber = pagesLinksElements[pagesLinksElements.size - 2].attr("data-page").toInt()
            val pageBase = link.substring(0, link.lastIndexOf("/"))
            IntRange(1, pagesNumber).map { i ->
                val pageLink = "$pageBase/chapterfun.ashx?cid=$chapterId&page=$i&key=$secretKey"
                var responseText = ""
                for (tr in 1..3) {
                    val request = Request.Builder()
                        .url(pageLink)
                        .addHeader("Referer", link)
                        .addHeader("Accept", "*/*")
                        .addHeader("Accept-Language", "en-US,en;q=0.9")
                        .addHeader("Connection", "keep-alive")
                        .addHeader("Host", "www.mangahere.cc")
                        .addHeader("User-Agent", System.getProperty("http.agent") ?: "")
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build()
                    val response = OkHttpClient().newCall(request).execute()
                    responseText = response.body?.string().toString()
                    if (responseText.isNotEmpty())
                        break
                    else
                        secretKey = ""
                }
                val deobfuscatedScript = zipline.quickJs.evaluate(responseText.removePrefix("eval")).toString()
                val baseLinkStartPos = deobfuscatedScript.indexOf("pix=") + 5
                val baseLinkEndPos = deobfuscatedScript.indexOf(";", baseLinkStartPos) - 1
                val baseLink = deobfuscatedScript.substring(baseLinkStartPos, baseLinkEndPos)
                val imageLinkStartPos = deobfuscatedScript.indexOf("pvalue=") + 9
                val imageLinkEndPos = deobfuscatedScript.indexOf("\"", imageLinkStartPos)
                val imageLink = deobfuscatedScript.substring(imageLinkStartPos, imageLinkEndPos)
                "https:$baseLink$imageLink"
            }
        }
            .dropLastIfBroken()
            .also { zipline.close() }
    }

    private fun extractSecretKey(html: String, zipline: Zipline): String {
        val secretKeyScriptLocation = html.indexOf("eval(function(p,a,c,k,e,d)")
        val secretKeyScriptEndLocation = html.indexOf("</script>", secretKeyScriptLocation)
        val secretKeyScript = html.substring(secretKeyScriptLocation, secretKeyScriptEndLocation).removePrefix("eval")
        val secretKeyDeobfuscatedScript = zipline.quickJs.evaluate(secretKeyScript).toString()
        val secretKeyStartLoc = secretKeyDeobfuscatedScript.indexOf("'")
        val secretKeyEndLoc = secretKeyDeobfuscatedScript.indexOf(";")
        val secretKeyResultScript = secretKeyDeobfuscatedScript.substring(
            secretKeyStartLoc, secretKeyEndLoc
        )
        return zipline.quickJs.evaluate(secretKeyResultScript).toString()
    }

    override val canScroll: Boolean = true
}