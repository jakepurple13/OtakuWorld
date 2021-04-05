package com.programmersbox.manga_sources.manga

import com.programmersbox.models.*
import com.squareup.duktape.Duktape
import io.reactivex.Single
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object MangaHere : ApiService {

    override val baseUrl = "https://www.mangahere.cc"

    override val serviceName: String get() = "MANGA_HERE"

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        Jsoup.connect("$baseUrl/directory/$page.htm?latest")
            .cookie("isAdult", "1").get()
            .select(".manga-list-1-list li").map {
                ItemModel(
                    title = it.select("a").first().attr("title"),
                    description = "",
                    url = it.select("a").first().attr("abs:href"),
                    imageUrl = it.select("img.manga-list-1-cover")?.first()?.attr("src") ?: "",
                    source = this
                )
            }.filter { it.title.isNotEmpty() }
            .let { emitter.onSuccess(it) }
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        Jsoup.connect("$baseUrl/directory/$page.htm")
            .cookie("isAdult", "1").get()
            .select(".manga-list-1-list li").map {
                ItemModel(
                    title = it.select("a").first().attr("title"),
                    description = "",
                    url = it.select("a").first().attr("abs:href"),
                    imageUrl = it.select("img.manga-list-1-cover")?.first()?.attr("src") ?: "",
                    source = this
                )
            }.filter { it.title.isNotEmpty() }
            .let { emitter.onSuccess(it) }
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        Single.create { emitter ->
            val url = "$baseUrl/search".toHttpUrlOrNull()!!.newBuilder().apply {
                addEncodedQueryParameter("page", page.toString())
                addEncodedQueryParameter("title", searchText.toString())
                addEncodedQueryParameter("sort", null)
                addEncodedQueryParameter("stype", 1.toString())
                addEncodedQueryParameter("name", null)
                addEncodedQueryParameter("author_method", "cw")
                addEncodedQueryParameter("author", null)
                addEncodedQueryParameter("artist_method", "cw")
                addEncodedQueryParameter("artist", null)
                addEncodedQueryParameter("rating_method", "eq")
                addEncodedQueryParameter("rating", null)
                addEncodedQueryParameter("released_method", "eq")
                addEncodedQueryParameter("released", null)
            }.build()
            val request = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
                .build()
            val client = OkHttpClient().newCall(request).execute()
            Jsoup.parse(client.body?.string()).select(".manga-list-4-list > li")
                .map {
                    ItemModel(
                        title = it.select("a").first().attr("title"),
                        description = it.select("p.manga-list-4-item-tip").last().text(),
                        url = "$baseUrl${it.select(".manga-list-4-item-title > a").first().attr("href")}",
                        imageUrl = it.select("img.manga-list-4-cover").first().attr("abs:src"),
                        source = this
                    )
                }
                .filter { it.title.isNotEmpty() }
                .let { emitter.onSuccess(it) }
        }

    } catch (e: Exception) {
        super.searchList(searchText, page, list)
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create { emitter ->
        val doc = Jsoup.connect(model.url).get()
        emitter.onSuccess(
            InfoModel(
                title = model.title,
                description = doc.select("p.fullcontent").text(),
                url = model.url,
                imageUrl = doc.select("img.detail-info-cover-img").select("img[src^=http]").attr("abs:src"),
                chapters = doc.select("div[id=chapterlist]").select("ul.detail-main-list").select("li").map {
                    ChapterModel(
                        name = it.select("a").select("p.title3").text(),
                        url = it.select("a").attr("abs:href"),
                        uploaded = it.select("a").select("p.title2").text(),
                        source = this
                    ).apply { uploadedTime = parseChapterDate(uploaded) }
                },
                genres = doc.select("p.detail-info-right-tag-list").select("a").eachText(),
                alternativeNames = emptyList(),
                source = this
            )
        )
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

    /*override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = Jsoup.connect(url).get()
        return MangaModel(
            title = doc.select("span.detail-info-right-title-font").text(),
            description = doc.select("p.fullcontent").text(),
            mangaUrl = url,
            imageUrl = doc.select("img.detail-info-cover-img").select("img[src^=http]").attr("abs:src"),
            source = this
        )
    }*/

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        it.onSuccess(
            pageListParse(Jsoup.connect(chapterModel.url).get()).map { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }
        )
    }

    private fun pageListParse(document: Document): List<String> {
        val bar = document.select("script[src*=chapter_bar]")
        val duktape = Duktape.create()

        /*
            function to drop last imageUrl if it's broken/unneccesary, working imageUrls are incremental (e.g. t001, t002, etc); if the difference between
            the last two isn't 1 or doesn't have an Int at the end of the last imageUrl's filename, drop last Page
        */
        fun List<String>.dropLastIfBroken(): List<String> {
            val list = this.takeLast(2).map { page ->
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
            val deobfuscatedScript = duktape.evaluate(script).toString()
            val urls = deobfuscatedScript.substringAfter("newImgs=['").substringBefore("'];").split("','")
            duktape.close()
            urls.map { s -> "https:$s" }
        } else {
            val html = document.html()
            val link = document.location()
            var secretKey = extractSecretKey(html, duktape)
            val chapterIdStartLoc = html.indexOf("chapterid")
            val chapterId = html.substring(
                chapterIdStartLoc + 11,
                html.indexOf(";", chapterIdStartLoc)
            ).trim()
            val chapterPagesElement = document.select(".pager-list-left > span").first()
            val pagesLinksElements = chapterPagesElement.select("a")
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
                val deobfuscatedScript = duktape.evaluate(responseText.removePrefix("eval")).toString()
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
            .also { duktape.close() }
    }

    private fun extractSecretKey(html: String, duktape: Duktape): String {
        val secretKeyScriptLocation = html.indexOf("eval(function(p,a,c,k,e,d)")
        val secretKeyScriptEndLocation = html.indexOf("</script>", secretKeyScriptLocation)
        val secretKeyScript = html.substring(secretKeyScriptLocation, secretKeyScriptEndLocation).removePrefix("eval")
        val secretKeyDeobfuscatedScript = duktape.evaluate(secretKeyScript).toString()
        val secretKeyStartLoc = secretKeyDeobfuscatedScript.indexOf("'")
        val secretKeyEndLoc = secretKeyDeobfuscatedScript.indexOf(";")
        val secretKeyResultScript = secretKeyDeobfuscatedScript.substring(
            secretKeyStartLoc, secretKeyEndLoc
        )
        return duktape.evaluate(secretKeyResultScript).toString()
    }

    override val canScroll: Boolean = true
}