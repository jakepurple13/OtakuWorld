package com.programmersbox.anime_sources.anime

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.webkit.*
import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.anime_sources.utilities.*
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import java.util.concurrent.TimeUnit

object Dopebox : Sflix("https://dopebox.to", "DOPEBOX") {
    override val sourceName: Sources get() = Sources.DOPEBOX
}

object SflixS : Sflix("https://sflix.to", "SFLIX") {
    override val sourceName: Sources get() = Sources.SFLIX
}

abstract class Sflix(baseUrl: String, private val servName: String) : ShowApi(
    baseUrl = baseUrl,
    recentPath = "home", allPath = "home"
) {

    abstract val sourceName: Sources

    override val serviceName: String get() = servName
    override val canDownload: Boolean get() = false

    override suspend fun recent(page: Int): List<ItemModel> {
        return recentPath(page)
            .select("section.block_area.block_area_home.section-id-02")
            .select("div.film-poster")
            .fastMap {
                val img = it.select("img")
                val title = img.attr("title")
                val posterUrl = img.attr("data-src")
                val href = fixUrl(it.select("a").attr("href"))

                ItemModel(
                    title = title,
                    description = "",
                    imageUrl = posterUrl,
                    url = href,
                    source = sourceName
                )
            }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        val map = listOf(
            "div#trending-movies",
            "div#trending-tv",
        )
        return map.flatMap { key ->
            all(page)
                .select(key)
                .select("div.film-poster")
                .fastMap {
                    val img = it.select("img")
                    val title = img.attr("title")
                    val posterUrl = img.attr("data-src")
                    val href = fixUrl(it.select("a").attr("href"))

                    ItemModel(
                        title = title,
                        description = "",
                        imageUrl = posterUrl,
                        url = href,
                        source = sourceName
                    )
                }
        }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        return Jsoup.connect("$baseUrl/search/${searchText.toString().replace(" ", "-")}").get()
            .select("div.flw-item")
            .fastMap {
                val title = it.select("h2.film-name").text()
                val href = fixUrl(it.select("a").attr("href"))
                val year = it.select("span.fdi-item").text()
                val image = it.select("img").attr("data-src")
                ItemModel(
                    title = title,
                    description = year,
                    imageUrl = image,
                    url = href,
                    source = sourceName
                )
            }
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val details = model.url.toJsoup().select("div.detail_page-watch")
        val img = details.select("img.film-poster-img")
        val posterUrl = img.attr("src")
        val title = img.attr("title")

        val plot = details.select("div.description").text().replace("Overview:", "").trim()

        val isMovie = model.url.contains("/movie/")

        val idRegex = Regex(""".*-(\d+)""")
        val dataId = details.attr("data-id")
        val id = if (dataId.isNullOrEmpty()) idRegex.find(model.url)?.groupValues?.get(1).orEmpty() else dataId

        val episodes = if (isMovie) {
            val episodesUrl = "$baseUrl/ajax/movie/episodes/$id"
            val episodes = get(episodesUrl).text

            // Supported streams, they're identical
            val sourceId = Jsoup.parse(episodes).select("a").firstOrNull {
                it.select("span").text().trim().equals("RapidStream", ignoreCase = true)
                        || it.select("span").text().trim().equals("Vidcloud", ignoreCase = true)
            }?.attr("data-id")

            val webViewUrl = "$baseUrl${sourceId?.let { ".$it" } ?: ""}".replace("/movie/", "/watch-movie/")
            listOf(
                ChapterModel(
                    title,
                    webViewUrl,
                    "",
                    model.url,
                    this@Sflix.sourceName
                )
            )
        } else {
            val seasonsHtml = get("$baseUrl/ajax/v2/tv/seasons/$id").text
            val seasonsDocument = Jsoup.parse(seasonsHtml)

            seasonsDocument.select("div.dropdown-menu.dropdown-menu-model > a").flatMapIndexed { season, element ->
                val seasonId = element.attr("data-id")
                if (seasonId.isNullOrBlank()) emptyList() else {

                    val seasonHtml = get("$baseUrl/ajax/v2/season/episodes/$seasonId").text
                    val seasonDocument = Jsoup.parse(seasonHtml)
                    seasonDocument.select("div.flw-item.film_single-item.episode-item.eps-item")
                        .mapIndexed { _, it ->
                            val episodeImg = it.select("img")
                            val episodeTitle = episodeImg.attr("title")
                            val episodeData = it.attr("data-id")

                            ChapterModel(
                                "S${season + 1}: $episodeTitle",
                                "${model.url}:::$episodeData",
                                "",
                                model.url,
                                this@Sflix.sourceName
                            )
                        }
                }
            }
        }

        return InfoModel(
            source = this@Sflix.sourceName,
            title = title,
            url = model.url,
            alternativeNames = emptyList(),
            description = plot,
            imageUrl = posterUrl,
            genres = emptyList(),
            chapters = episodes.reversed()
        )
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        return ItemModel(
            title = "",
            description = "",
            imageUrl = "",
            url = url,
            source = this
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        // To transfer url:::id
        val split = chapterModel.url.split(":::")
        // Only used for tv series
        val url = if (split.size == 2) {
            val episodesUrl = "$baseUrl/ajax/v2/episode/servers/${split[1]}"
            val episodes = get(episodesUrl).text

            // Supported streams, they're identical
            val sourceId = Jsoup.parse(episodes).select("a").firstOrNull {
                it.select("span").text().trim().equals("RapidStream", ignoreCase = true)
                        || it.select("span").text().trim().equals("Vidcloud", ignoreCase = true)
            }?.attr("data-id")

            "${split[0]}${sourceId?.let { ".$it" } ?: ""}".replace("/tv/", "/watch-tv/")
        } else {
            chapterModel.url
        }

        val sources = get(
            url,
            interceptor = WebViewResolver(Regex("""/getSources"""))
        ).text

        val mapped = sources.fromJson<SourceObject>()

        val list = listOf(
            mapped?.sources to "source 1",
            mapped?.sources1 to "source 2",
            mapped?.sources2 to "source 3",
            mapped?.sourcesBackup to "source backup"
        )

        return list.flatMap { subList ->
            subList.first?.fastMap { it?.toExtractorLink(chapterModel, subList.second).orEmpty() }.orEmpty()
        }.flatten()
    }

    private fun SourcesDope.toExtractorLink(caller: ChapterModel, name: String): List<Storage>? {
        return this.file?.let { file ->
            val isM3u8 = URI(this.file).path.endsWith(".m3u8") || this.type.equals("hls", ignoreCase = true)
            if (isM3u8) {
                M3u8Helper().m3u8Generation(M3u8Helper.M3u8Stream(this.file, null), true).map { stream ->
                    val qualityString = if ((stream.quality ?: 0) == 0) label ?: "" else "${stream.quality}p"

                    Storage(
                        link = stream.streamUrl,
                        source = caller.url,
                        filename = "${caller.name}.mp4",
                        quality = getQualityFromName(stream.quality.toString()).name + " - " + name,
                        sub = qualityString
                    )
                }
            } else {
                listOf(

                    Storage(
                        link = file,
                        source = caller.url,
                        filename = "${caller.name}.mp4",
                        quality = getQualityFromName(this.type.orEmpty()).name,
                        sub = ""
                    )
                )
            }

        }
    }

    data class Tracks(
        val file: String?,
        val label: String?,
        val kind: String?
    )

    data class SourcesDope(
        val file: String?,
        val type: String?,
        val label: String?
    )

    data class SourceObject(
        val sources: List<SourcesDope?>?,
        val sources1: List<SourcesDope?>?,
        val sources2: List<SourcesDope?>?,
        val sourcesBackup: List<SourcesDope?>?,
        val tracks: List<Tracks?>?
    )

    class WebViewResolver(val interceptUrl: Regex) : Interceptor, KoinComponent {

        fun main(work: suspend (() -> Unit)): Job {
            return CoroutineScope(Dispatchers.Main).launch {
                work()
            }
        }

        private val context: Context by inject()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            return runBlocking {
                val fixedRequest = resolveUsingWebView(request)
                return@runBlocking chain.proceed(fixedRequest ?: request)
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        suspend fun resolveUsingWebView(request: Request): Request? {
            val url = request.url.toString()
            val headers = request.headers
            println("Initial web-view request: $url")
            var webView: WebView? = null

            fun destroyWebView() {
                main {
                    webView?.stopLoading()
                    webView?.destroy()
                    webView = null
                    println("Destroyed webview")
                }
            }

            var fixedRequest: Request? = null

            main {
                // Useful for debugging
//            WebView.setWebContentsDebuggingEnabled(true)
                webView = WebView(context).apply {
                    // Bare minimum to bypass captcha
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = USER_AGENT
                }

                webView?.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val webViewUrl = request.url.toString()
//                    println("Loading WebView URL: $webViewUrl")

                        if (interceptUrl.containsMatchIn(webViewUrl)) {
                            fixedRequest = getRequestCreator(
                                webViewUrl,
                                request.requestHeaders,
                                null,
                                mapOf(),
                                mapOf(),
                                10,
                                TimeUnit.MINUTES
                            )

                            println("Web-view request finished: $webViewUrl")
                            destroyWebView()
                            return null
                        }

                        // Suppress image requests as we don't display them anywhere
                        // Less data, low chance of causing issues.
                        val blacklistedFiles = listOf(".jpg", ".png", ".webp", ".jpeg", ".webm", ".mp4")

                        /** NOTE!  request.requestHeaders is not perfect!
                         *  They don't contain all the headers the browser actually gives.
                         *  Overriding with okhttp might fuck up otherwise working requests,
                         *  e.g the recaptcha request.
                         * **/
                        return try {
                            when {
                                blacklistedFiles.any { URI(webViewUrl).path.endsWith(it) } || webViewUrl.endsWith(
                                    "/favicon.ico"
                                ) -> WebResourceResponse(
                                    "image/png",
                                    null,
                                    null
                                )

                                webViewUrl.contains("recaptcha") -> super.shouldInterceptRequest(view, request)

                                request.method == "GET" -> get(
                                    webViewUrl,
                                    headers = request.requestHeaders
                                ).toWebResourceResponse()

                                request.method == "POST" -> post(
                                    webViewUrl,
                                    headers = request.requestHeaders
                                ).toWebResourceResponse()
                                else -> return super.shouldInterceptRequest(view, request)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.proceed() // Ignore ssl issues
                    }
                }
                webView?.loadUrl(url, headers.toMap())
            }

            var loop = 0
            // Timeouts after this amount, 60s
            val totalTime = 60000L

            val delayTime = 100L

            // A bit sloppy, but couldn't find a better way
            while (loop < totalTime / delayTime) {
                if (fixedRequest != null) return fixedRequest
                delay(delayTime)
                loop += 1
            }

            println("Web-view timeout after ${totalTime / 1000}s")
            destroyWebView()
            return null
        }

        fun Response.toWebResourceResponse(): WebResourceResponse {
            val contentTypeValue = this.header("Content-Type")
            // 1. contentType. 2. charset
            val typeRegex = Regex("""(.*);(?:.*charset=(.*)(?:|;)|)""")
            return if (contentTypeValue != null) {
                val found = typeRegex.find(contentTypeValue)
                val contentType = found?.groupValues?.getOrNull(1)?.ifBlank { null } ?: contentTypeValue
                val charset = found?.groupValues?.getOrNull(2)?.ifBlank { null }
                WebResourceResponse(contentType, charset, this.body?.byteStream())
            } else {
                WebResourceResponse("application/octet-stream", null, this.body?.byteStream())
            }
        }
    }

}


const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
