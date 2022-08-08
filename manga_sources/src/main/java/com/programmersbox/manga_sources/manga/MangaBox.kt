package com.programmersbox.manga_sources.manga

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import androidx.core.os.LocaleListCompat
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.putObject
import com.programmersbox.manga_sources.Sources
import com.programmersbox.manga_sources.utilities.AndroidCookieJar
import com.programmersbox.manga_sources.utilities.GET
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.manga_sources.utilities.cloudflare
import com.programmersbox.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.newParser
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.filterNot
import kotlin.collections.firstOrNull
import kotlin.collections.lastOrNull
import kotlin.collections.map
import kotlin.collections.mapIndexed
import kotlin.collections.orEmpty
import kotlin.collections.reversed
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMap
import kotlin.collections.toTypedArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class MangaBox(
    override val serviceName: String,
    override val baseUrl: String,
    private val dateformat: SimpleDateFormat = SimpleDateFormat("MMM-dd-yy", Locale.ENGLISH)
) : ApiService, KoinComponent {

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 Edg/88.0.705.63"
    }

    fun Response.asJsoup(html: String? = null): Document {
        return Jsoup.parse(html ?: body!!.string(), request.url.toString())
    }

    /*override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> = Single.create { emitter ->
        client.newCall(searchMangaRequest(page, searchText.toString()))
            .execute()
            .let { searchMangaParse(it) }
            .let { emitter.onSuccess(it) }
    }*/

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

    private val helper: NetworkHelper by inject()

    //override val supportsLatest = true

    private val client: OkHttpClient by lazy {
        helper.cloudflareClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun cloudFlareClient(url: String) = cloudflare(helper, url, *headers().toMap().toList().toTypedArray())

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
            it!!.attr("abs:href")/*.substringAfter(baseUrl)*/ to it!!.text()
        }

        return ItemModel(
            url = f.first, // intentionally not using setUrlWithoutDomain
            title = f.second,
            imageUrl = element.select("img").first()!!.attr("abs:src"),
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
            chapters = document.select(chapterListSelector())
                .map { chapterFromElement(it, itemModel.url) }
                .also { if (it.isEmpty()) checkForRedirectMessage(document) },//client.newCall(chapterListRequest(itemModel.url)).execute().let { chapterListParse(it) },
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
            .map { chapterFromElement(it, "") }
            .also { if (it.isEmpty()) checkForRedirectMessage(document) }
    }

    protected open val alternateChapterDateSelector = String()

    private fun Element.selectDateFromElement(): Element {
        val defaultChapterDateSelector = "span"
        return this.select(defaultChapterDateSelector).lastOrNull() ?: this.select(alternateChapterDateSelector).last()!!
    }

    private fun chapterFromElement(element: Element, mangaUrl: String): ChapterModel {
        return ChapterModel(
            url = element.select("a").attr("abs:href"),//.substringAfter(baseUrl),
            name = element.select("a").text(),
            uploaded = parseChapterDate(element.selectDateFromElement().text()).toString(),
            sourceUrl = mangaUrl,
            source = this
        )
    }

    private fun parseChapterDate(date: String, host: String = "manganato"): Long? {
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
                if (host.contains("manganato", ignoreCase = true)) {
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
        var str = query.lowercase()
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

object Manganelo : MangaBox("Manganato", "https://manganato.com") {
    // Nelo's date format is part of the base class
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/genre-all/$page?type=topview", headers())
    override fun popularMangaSelector() = "div.content-genres-item"
    override val latestUrlPath = "genre-all/"
    override val simpleQueryPath = "search/story/"
    override fun searchMangaSelector() = "div.search-story-item, div.content-genres-item"
    override val serviceName: String get() = "MANGANELO"
}

object MangaTown : ApiService, KoinComponent {

    override val notWorking: Boolean get() = true

    private val context: Context by inject()

    override val baseUrl: String get() = parser.getDomain()

    private val parser by lazy {
        MangaSource.MANGATOWN.newParser(
            MangaLoaderContextImpl(
                OkHttpClient(),
                AndroidCookieJar(),
                context
            )
        )
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return parser.getList(page, sortOrder = SortOrder.POPULARITY, tags = emptySet()).toItemModel()
    }

    override suspend fun recent(page: Int): List<ItemModel> {
        return parser.getList(page, sortOrder = SortOrder.UPDATED, tags = emptySet()).toItemModel()
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        return parser.getDetails(model.toManga()).let {
            InfoModel(
                title = it.title,
                description = it.description.orEmpty(),
                url = it.url,
                imageUrl = it.largeCoverUrl ?: it.coverUrl,
                genres = it.tags.map { it.title },
                chapters = it.chapters.orEmpty().reversed().map {
                    ChapterModel(
                        name = it.name,
                        url = it.url,
                        uploaded = it.uploadDate.toString(),
                        source = Sources.MANGATOWN,
                        sourceUrl = model.url
                    )
                },
                source = Sources.MANGATOWN,
                alternativeNames = emptyList()
            ).apply { extras["referer"] = it.publicUrl }
        }
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        return parser.getPages(
            MangaChapter(
                id = 1,
                number = 1,
                url = chapterModel.url,
                scanlator = "",
                uploadDate = 1L,
                branch = "",
                source = MangaSource.MANGATOWN,
                name = chapterModel.name
            )
        ).map { Storage(link = parser.getPageUrl(it)).apply { headers["referer"] = it.referer } }
    }

    private fun List<Manga>.toItemModel() = map {
        ItemModel(
            title = it.title,
            description = it.description.orEmpty(),
            imageUrl = it.largeCoverUrl ?: it.coverUrl,
            url = it.url,
            source = Sources.MANGATOWN
        ).apply { extras["referer"] = it.publicUrl }
    }

    private fun ItemModel.toManga() = Manga(
        title = title,
        description = description,
        coverUrl = imageUrl,
        url = url,
        id = 1,
        altTitle = "",
        rating = 5f,
        isNsfw = false,
        largeCoverUrl = imageUrl,
        tags = emptySet(),
        state = MangaState.ONGOING,
        author = "",
        chapters = emptyList(),
        source = MangaSource.MANGATOWN,
        publicUrl = url
    )

}

class MangaLoaderContextImpl(
    override val httpClient: OkHttpClient,
    override val cookieJar: AndroidCookieJar,
    private val androidContext: Context,
) : MangaLoaderContext() {

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun evaluateJs(script: String): String? = withContext(Dispatchers.Main) {
        val webView = WebView(androidContext)
        webView.settings.javaScriptEnabled = true
        suspendCoroutine { cont ->
            webView.evaluateJavascript(script) { result ->
                cont.resume(result?.takeUnless { it == "null" })
            }
        }
    }

    override fun getConfig(source: MangaSource): MangaSourceConfig {
        return SourceSettings(androidContext, source)
    }

    override fun encodeBase64(data: ByteArray): String {
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_PADDING)
    }

    override fun decodeBase64(data: String): ByteArray {
        return android.util.Base64.decode(data, android.util.Base64.DEFAULT)
    }

    override fun getPreferredLocales(): List<Locale> {
        return LocaleListCompat.getAdjustedDefault().toList()
    }
}

fun LocaleListCompat.toList(): List<Locale> = List(size()) { i -> getOrThrow(i) }

fun LocaleListCompat.getOrThrow(index: Int) = get(index) ?: throw NoSuchElementException()

private const val KEY_SORT_ORDER = "sort_order"

class SourceSettings(context: Context, source: MangaSource) : MangaSourceConfig {

    private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

    var defaultSortOrder: SortOrder?
        get() = prefs.getObject<SortOrder>(KEY_SORT_ORDER)
        set(value) = prefs.edit().putObject(KEY_SORT_ORDER, value).apply()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: ConfigKey<T>): T {
        return when (key) {
            is ConfigKey.Domain -> prefs.getString(key.key, key.defaultValue)?.let {
                if (it.isEmpty()) key.defaultValue else it
            } ?: key.defaultValue
        } as T
    }
}