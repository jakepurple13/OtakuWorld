package com.programmersbox.manga_sources.manga

import com.programmersbox.manga_sources.Sources
import com.programmersbox.manga_sources.utilities.GET
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.manga_sources.utilities.POST
import com.programmersbox.manga_sources.utilities.asJsoup
import com.programmersbox.models.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.random.Random

object MangaRead : ApiService, KoinComponent {

    override val baseUrl: String get() = "https://www.mangaread.org"

    override val canScroll: Boolean = true

    private val helper: NetworkHelper by inject()

    private val client: OkHttpClient by lazy {
        helper.cloudflareClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val userAgentRandomizer = " ${Random.nextInt().absoluteValue}"

    private fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/78.0$userAgentRandomizer")
        .add("Referer", baseUrl)

    private fun formBuilder(page: Int, popular: Boolean) = FormBody.Builder().apply {
        add("action", "madara_load_more")
        add("page", (page - 1).toString())
        add("template", "madara-core/content/content-archive")
        add("vars[orderby]", "meta_value_num")
        add("vars[paged]", "1")
        add("vars[posts_per_page]", "20")
        add("vars[post_type]", "wp-manga")
        add("vars[post_status]", "publish")
        add("vars[meta_key]", if (popular) "_wp_manga_views" else "_latest_update")
        add("vars[order]", "desc")
        add("vars[sidebar]", if (popular) "full" else "right")
        add("vars[manga_archives_item_layout]", "big_thumbnail")
    }

    override suspend fun recent(page: Int): List<ItemModel> {
        val request = client.newCall(
            POST(
                "$baseUrl/wp-admin/admin-ajax.php",
                headersBuilder().build(),
                formBuilder(page, false).build(),
                CacheControl.FORCE_NETWORK
            )
        ).execute()
        return request.asJsoup()
            .select("div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))")
            .map {
                val info = it.select("div.post-title a")
                ItemModel(
                    url = info.attr("abs:href"), // intentionally not using setUrlWithoutDomain
                    title = info.text(),
                    imageUrl = it.select("img").first()?.let { imageFromElement(it) }.orEmpty(),
                    source = Sources.MANGA_READ,
                    description = ""
                ).also { it.extras["Referer"] = it.url }
            }
    }

    private fun imageFromElement(element: Element): String? {
        return when {
            element.hasAttr("data-src") -> element.attr("abs:data-src")
            element.hasAttr("data-lazy-src") -> element.attr("abs:data-lazy-src")
            element.hasAttr("srcset") -> element.attr("abs:srcset").substringBefore(" ")
            else -> element.attr("abs:src")
        }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        val request = client.newCall(
            POST(
                "$baseUrl/wp-admin/admin-ajax.php",
                headersBuilder().build(),
                formBuilder(page, true).build(),
                CacheControl.FORCE_NETWORK
            )
        ).execute()
        return request.asJsoup()
            .select("div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))")
            .map {
                val info = it.select("div.post-title a")
                ItemModel(
                    url = info.attr("abs:href"), // intentionally not using setUrlWithoutDomain
                    title = info.text(),
                    imageUrl = it.select("img").first()?.let { imageFromElement(it) }.orEmpty(),
                    source = Sources.MANGA_READ,
                    description = ""
                ).also { it.extras["Referer"] = it.url }
            }
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = Jsoup.connect(model.url).get()

        val title = doc.select("div.post-title h3, div.post-title h1")
        val description =
            doc.select("div.description-summary div.summary__content, div.summary_content div.post-content_item > h5 + div, div.summary_content div.manga-excerpt")
                .let {
                    if (it.select("p").text().isNotEmpty()) {
                        it.select("p").joinToString(separator = "\n\n") { p ->
                            p.text().replace("<br>", "\n")
                        }
                    } else {
                        it.text()
                    }
                }

        val genres = doc.select("div.genres-content a")
            .map { element -> element.text().lowercase(Locale.ROOT) }
            .toMutableSet()

        doc.select("div.tags-content a").forEach { element ->
            if (genres.contains(element.text()).not()) {
                genres.add(element.text().lowercase(Locale.ROOT))
            }
        }

        return InfoModel(
            title = title.text(),
            genres = genres.toList(),
            url = model.url,
            alternativeNames = emptyList(),
            chapters = chapterListParse(client.newCall(GET(model.url, headersBuilder().build())).execute(), model.url),
            description = description,
            imageUrl = doc.select("div.summary_image img").first()?.let { imageFromElement(it) }.orEmpty(),
            source = Sources.MANGA_READ
        )
    }

    private fun oldXhrChaptersRequest(mangaId: String): Request {
        val form = FormBody.Builder()
            .add("action", "manga_get_chapters")
            .add("manga", mangaId)
            .build()

        val xhrHeaders = headersBuilder()
            .add("Content-Length", form.contentLength().toString())
            .add("Content-Type", form.contentType().toString())
            .add("Referer", baseUrl)
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$baseUrl/wp-admin/admin-ajax.php", xhrHeaders, form)
    }

    private fun xhrChaptersRequest(mangaUrl: String): Request {
        val xhrHeaders = headersBuilder()
            .add("Referer", baseUrl)
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$mangaUrl/ajax/chapters", xhrHeaders)
    }

    private val useNewChapterEndpoint: Boolean = false

    /**
     * Internal attribute to control if it should always use the
     * new chapter endpoint after a first check if useNewChapterEndpoint is
     * set to false. Using a separate variable to still allow the other
     * one to be overridable manually in each source.
     */
    private var oldChapterEndpointDisabled: Boolean = false


    private fun chapterListParse(response: Response, sourceUrl: String): List<ChapterModel> {
        val document = response.asJsoup()
        val chaptersWrapper = document.select("div[id^=manga-chapters-holder]")

        var chapterElements = document.select("li.wp-manga-chapter")

        if (chapterElements.isEmpty() && !chaptersWrapper.isNullOrEmpty()) {
            val mangaUrl = document.location().removeSuffix("/")
            val mangaId = chaptersWrapper.attr("data-id")

            var xhrRequest = if (useNewChapterEndpoint || oldChapterEndpointDisabled)
                xhrChaptersRequest(mangaUrl) else oldXhrChaptersRequest(mangaId)
            var xhrResponse = client.newCall(xhrRequest).execute()

            // Newer Madara versions throws HTTP 400 when using the old endpoint.
            if (!useNewChapterEndpoint && xhrResponse.code == 400) {
                xhrResponse.close()
                // Set it to true so following calls will be made directly to the new endpoint.
                oldChapterEndpointDisabled = true

                xhrRequest = xhrChaptersRequest(mangaUrl)
                xhrResponse = client.newCall(xhrRequest).execute()
            }

            chapterElements = xhrResponse.asJsoup().select("li.wp-manga-chapter")
            xhrResponse.close()
        }

        return chapterElements.map { chapterFromElement(it, sourceUrl) }
    }

    private fun chapterFromElement(element: Element, sourceUrl: String): ChapterModel {
        val info = element.select("a")

        return ChapterModel(
            url = info.attr("abs:href").let {
                it.substringBefore("?style=paged") + if (!it.endsWith("?style=list")) "?style=list" else ""
            },
            name = info.text(),
            source = Sources.MANGA_READ,
            sourceUrl = sourceUrl,
            uploaded = element.select("img:not(.thumb)").firstOrNull()?.attr("alt")
                ?: element.select("span a").firstOrNull()?.attr("title") ?: ""
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val doc = client.newCall(GET(chapterModel.url, headersBuilder().build())).execute().asJsoup()

        return doc.select("div.page-break, li.blocks-gallery-item, .reading-content .text-left:not(:has(.blocks-gallery-item)) img")
            .mapIndexed { index, element ->
                Storage(
                    sub = index.toString(),
                    filename = doc.location(),
                    link = element.select("img").first()?.let {
                        it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src")
                    },
                    source = doc.location()
                ).also { it.headers["Referer"] = chapterModel.url }
            }
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = Jsoup.connect(url).get()

        val title = doc.select("div.post-title h3, div.post-title h1")
        val description =
            doc.select("div.description-summary div.summary__content, div.summary_content div.post-content_item > h5 + div, div.summary_content div.manga-excerpt")
                .let {
                    if (it.select("p").text().isNotEmpty()) {
                        it.select("p").joinToString(separator = "\n\n") { p ->
                            p.text().replace("<br>", "\n")
                        }
                    } else {
                        it.text()
                    }
                }

        val genres = doc.select("div.genres-content a")
            .map { element -> element.text().lowercase(Locale.ROOT) }
            .toMutableSet()

        doc.select("div.tags-content a").forEach { element ->
            if (genres.contains(element.text()).not()) {
                genres.add(element.text().lowercase(Locale.ROOT))
            }
        }

        return ItemModel(
            title = title.text(),
            url = url,
            description = description,
            imageUrl = doc.select("div.summary_image img").first()?.let { imageFromElement(it) }.orEmpty(),
            source = Sources.MANGA_READ
        )
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        val url = "$baseUrl/page/$page/".toHttpUrlOrNull()!!.newBuilder()
        url.addQueryParameter("s", searchText.toString())
        url.addQueryParameter("post_type", "wp-manga")

        return client.newCall(GET(url.toString(), headersBuilder().build())).execute().asJsoup()
            .select("div.c-tabs-item__content")
            .map {
                val info = it.select("div.post-title a")
                ItemModel(
                    title = info.text(),
                    url = info.attr("abs:href"),
                    description = "",
                    imageUrl = it.select("img").first()?.let { imageFromElement(it) }.orEmpty(),
                    source = Sources.MANGA_READ
                )
            }
    }

}