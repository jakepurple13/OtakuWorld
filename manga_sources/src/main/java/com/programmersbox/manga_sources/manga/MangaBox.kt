package com.programmersbox.manga_sources.manga

import android.annotation.SuppressLint
import com.programmersbox.manga_sources.MangaContext
import com.programmersbox.manga_sources.utilities.cloudflare
import com.programmersbox.models.*
import io.reactivex.Single
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

abstract class MangaBox(
    override val serviceName: String,
    override val baseUrl: String,
    private val dateformat: SimpleDateFormat = SimpleDateFormat("MMM-dd-yy", Locale.ENGLISH)
) : ApiService {

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 Edg/88.0.705.63"
    }

    fun Response.asJsoup(html: String? = null): Document {
        return Jsoup.parse(html ?: body!!.string(), request.url.toString())
    }

    private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()
    private val DEFAULT_HEADERS = Headers.Builder().build()
    private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

    fun GET(
        url: String,
        headers: Headers = DEFAULT_HEADERS,
        cache: CacheControl = DEFAULT_CACHE_CONTROL
    ): Request {
        return Request.Builder()
            .url(url)
            .headers(headers)
            .cacheControl(cache)
            .build()
    }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create {
        it.onSuccess(mangaDetailsParse(client.newCall(GET("${baseUrl}/${model.url}", headers())).execute().asJsoup(), model))
    }

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        client.newCall(latestUpdatesRequest(page))
            .execute()
            .let { latestUpdatesParse(it) }
            .let { emitter.onSuccess(it) }
    }

    override fun getList(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
        client.newCall(popularMangaRequest(page))
            .execute()
            .let { popularMangaParse(it) }
            .let { emitter.onSuccess(it) }
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = Single.create { emitter ->
        client.newCall(searchMangaRequest(page, searchText.toString()))
            .execute()
            .let { searchMangaParse(it) }
            .let { emitter.onSuccess(it) }
    }

    override val canScroll: Boolean = true

    private fun searchMangaParse(response: Response): List<ItemModel> {
        val document = response.asJsoup()

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }

        val hasNextPage = searchMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return mangas
    }

    private fun popularMangaParse(response: Response): List<ItemModel> {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }

        val hasNextPage = popularMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return mangas
    }

    private fun latestUpdatesParse(response: Response): List<ItemModel> {
        val document = response.asJsoup()

        val mangas = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }

        val hasNextPage = latestUpdatesNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return mangas
    }

    //override val supportsLatest = true

    private val client: OkHttpClient by lazy {
        MangaContext.getInstance(MangaContext.context).cloudflareClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun cloudFlareClient(url: String) = cloudflare(url, *headers().toMap().toList().toTypedArray())

    fun headers(): Headers = Headers.Builder()
        .add("User-Agent", DEFAULT_USER_AGENT)
        .add("Referer", baseUrl) // for covers
        .build()

    open val popularUrlPath = "manga_list?type=topview&category=all&state=all&page="

    open val latestUrlPath = "manga_list?type=latest&category=all&state=all&page="

    open val simpleQueryPath = "search/"

    open fun popularMangaSelector() = "div.truyen-list > div.list-truyen-item-wrap"

    open fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/$popularUrlPath$page", headers())
    }

    private fun latestUpdatesSelector() = popularMangaSelector()

    open fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/$latestUrlPath$page", headers())
    }

    protected fun mangaFromElement(element: Element, urlSelector: String = "h3 a"): ItemModel {

        val f = element.select(urlSelector).first().let {
            it.attr("abs:href").substringAfter(baseUrl) to it.text()
        }

        return ItemModel(
            url = f.first, // intentionally not using setUrlWithoutDomain
            title = f.second,
            imageUrl = element.select("img").first().attr("abs:src"),
            source = this,
            description = ""
        )
    }

    private fun popularMangaFromElement(element: Element): ItemModel = mangaFromElement(element)

    private fun latestUpdatesFromElement(element: Element): ItemModel = mangaFromElement(element)

    private fun popularMangaNextPageSelector() = "div.group_page, div.group-page a:not([href]) + a:not(:contains(Last))"

    private fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    private fun searchMangaRequest(page: Int, query: String): Request {
        return if (query.isNotBlank()) {
            GET("$baseUrl/$simpleQueryPath${normalizeSearchQuery(query)}?page=$page", headers())
        } else {
            val url = baseUrl.toHttpUrlOrNull()!!.newBuilder()
            url.addPathSegment("advanced_search")
            url.addQueryParameter("page", page.toString())
            url.addQueryParameter("keyw", normalizeSearchQuery(query))
            GET(url.toString(), headers())
        }
    }

    open fun searchMangaSelector() = ".panel_story_list .story_item"

    private fun searchMangaFromElement(element: Element) = mangaFromElement(element)

    private fun searchMangaNextPageSelector() = "a.page_select + a:not(.page_last), a.page-select + a:not(.page-last)"

    open val mangaDetailsMainSelector = "div.manga-info-top, div.panel-story-info"

    open val thumbnailSelector = "div.manga-info-pic img, span.info-image img"

    open val descriptionSelector = "div#noidungm, div#panel-story-info-description"

    /*private fun mangaDetailsRequest(manga: SManga): Request {
        if (manga.url.startsWith("http")) {
            return GET(manga.url, headers)
        }
        return super.mangaDetailsRequest(manga)
    }*/

    private fun checkForRedirectMessage(document: Document) {
        if (document.select("body").text().startsWith("REDIRECT :"))
            throw Exception("Source URL has changed")
    }

    private fun mangaDetailsParse(document: Document, itemModel: ItemModel): InfoModel {

        val infoElement = document.select(mangaDetailsMainSelector).firstOrNull()/*?.let { infoElement ->
            title = infoElement.select("h1, h2").first().text()
            author = infoElement.select("li:contains(author) a, td:containsOwn(author) + td").text()
            genre = infoElement.select("div.manga-info-top li:contains(genres)").firstOrNull()
                ?.select("a") // kakalot
                ?: infoElement.select("td:containsOwn(genres) + td a") // nelo
        }*/

        val genres = (infoElement?.select("div.manga-info-top li:contains(genres)")?.firstOrNull()
            ?.select("a") // kakalot
            ?: infoElement?.select("td:containsOwn(genres) + td a")) // nelo
            ?.eachText()
            .orEmpty()

        return InfoModel(
            title = infoElement?.select("h1, h2")?.first()?.text().orEmpty(),
            genres = genres,
            url = itemModel.url,
            alternativeNames = emptyList(),
            chapters = client.newCall(chapterListRequest(itemModel.url)).execute().let { chapterListParse(it) },
            description = document.select(descriptionSelector)?.firstOrNull()?.ownText()
                ?.replace("""^${infoElement?.select("h1, h2")?.first()?.text().orEmpty()} summary:\s""".toRegex(), "")
                ?.replace("""<\s*br\s*/?>""".toRegex(), "\n")
                ?.replace("<[^>]*>".toRegex(), "")
                .orEmpty(),
            imageUrl = document.select(thumbnailSelector).attr("abs:src"),
            source = this
        )
    }

    private fun chapterListSelector() = "div.chapter-list div.row, ul.row-content-chapter li"

    fun chapterListRequest(manga: String): Request {
        if (manga.startsWith("http")) {
            return GET(manga, headers())
        }
        return GET(baseUrl + manga, headers())
    }

    fun chapterListParse(response: Response): List<ChapterModel> {
        val document = response.asJsoup()

        return document.select(chapterListSelector())
            .map { chapterFromElement(it) }
            .also { if (it.isEmpty()) checkForRedirectMessage(document) }
    }

    protected open val alternateChapterDateSelector = String()

    private fun Element.selectDateFromElement(): Element {
        val defaultChapterDateSelector = "span"
        return this.select(defaultChapterDateSelector).lastOrNull() ?: this.select(alternateChapterDateSelector).last()
    }

    private fun chapterFromElement(element: Element): ChapterModel {
        return ChapterModel(
            url = element.select("a").attr("abs:href").substringAfter(baseUrl),
            name = element.select("a").text(),
            uploaded = parseChapterDate(element.selectDateFromElement().text()).toString(),
            source = this
        )
    }

    private fun parseChapterDate(date: String, host: String = "manganelo"): Long? {
        return if ("ago" in date) {
            val value = date.split(' ')[0].toIntOrNull()
            val cal = Calendar.getInstance()
            when {
                value != null && "min" in date -> cal.apply { add(Calendar.MINUTE, value * -1) }
                value != null && "hour" in date -> cal.apply { add(Calendar.HOUR_OF_DAY, value * -1) }
                value != null && "day" in date -> cal.apply { add(Calendar.DATE, value * -1) }
                else -> null
            }?.timeInMillis
        } else {
            try {
                if (host.contains("manganelo", ignoreCase = true)) {
                    // Nelo's date format
                    SimpleDateFormat("MMM dd,yy", Locale.ENGLISH).parse(date)
                } else {
                    dateformat.parse(date)
                }
            } catch (e: ParseException) {
                null
            }?.time
        }
    }

    /*override fun pageListRequest(chapter: SChapter): Request {
        if (chapter.url.startsWith("http")) {
            return GET(chapter.url, headers)
        }
        return super.pageListRequest(chapter)
    }*/

    open val pageListSelector = "div#vungdoc img, div.container-chapter-reader img"

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->
        client.newCall(imageRequest(chapterModel)).execute().asJsoup()
            .let { pageListParse(it) }
            .also { println(it) }
            .let(emitter::onSuccess)
    }

    private fun pageListParse(document: Document): List<Storage> {
        return document.select(pageListSelector)
            // filter out bad elements for mangakakalots
            .filterNot { it.attr("src").endsWith("log") }
            .mapIndexed { i, element ->
                val url = element.attr("abs:src").let { src ->
                    if (src.startsWith("https://convert_image_digi.mgicdn.com")) {
                        "https://images.weserv.nl/?url=" + src.substringAfter("//")
                    } else {
                        src
                    }
                }
                Storage(sub = i.toString(), filename = document.location(), link = url, source = document.location())
            }
    }

    private fun imageRequest(page: ChapterModel): Request {
        return GET("${baseUrl}${page.url}", headers().newBuilder().set("Referer", page.url).build())
    }

    //override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Based on change_alias JS function from Mangakakalot's website
    @SuppressLint("DefaultLocale")
    open fun normalizeSearchQuery(query: String): String {
        var str = query.toLowerCase()
        str = str.replace("[àáạảãâầấậẩẫăằắặẳẵ]".toRegex(), "a")
        str = str.replace("[èéẹẻẽêềếệểễ]".toRegex(), "e")
        str = str.replace("[ìíịỉĩ]".toRegex(), "i")
        str = str.replace("[òóọỏõôồốộổỗơờớợởỡ]".toRegex(), "o")
        str = str.replace("[ùúụủũưừứựửữ]".toRegex(), "u")
        str = str.replace("[ỳýỵỷỹ]".toRegex(), "y")
        str = str.replace("đ".toRegex(), "d")
        str = str.replace("""!|@|%|\^|\*|\(|\)|\+|=|<|>|\?|/|,|\.|:|;|'| |"|&|#|\[|]|~|-|$|_""".toRegex(), "_")
        str = str.replace("_+_".toRegex(), "_")
        str = str.replace("""^_+|_+$""".toRegex(), "")
        return str
    }

    // keyt query parameter
    private fun getKeywordFilters(): Array<Pair<String?, String>> = arrayOf(
        Pair(null, "Everything"),
        Pair("title", "Title"),
        Pair("alternative", "Alt title"),
        Pair("author", "Author")
    )

    private fun getSortFilters(): Array<Pair<String?, String>> = arrayOf(
        Pair("latest", "Latest"),
        Pair("newest", "Newest"),
        Pair("topview", "Top read")
    )
}

object Manganelo : MangaBox("Manganelo", "https://manganelo.com") {
    // Nelo's date format is part of the base class
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/genre-all/$page?type=topview", headers())
    override fun popularMangaSelector() = "div.content-genres-item"
    override val latestUrlPath = "genre-all/"
    override val simpleQueryPath = "search/story/"
    override fun searchMangaSelector() = "div.search-story-item, div.content-genres-item"
    override val serviceName: String get() = "MANGANELO"
}