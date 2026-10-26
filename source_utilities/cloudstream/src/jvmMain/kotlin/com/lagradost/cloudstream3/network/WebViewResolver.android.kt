// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: desktop replacement for the androidMain WebViewResolver.android.kt. Same file name (facade
// WebViewResolver_androidKt), class, constructor, defaults and overloads, so plugins link. There is
// no WebView on desktop yet: resolving throws an IOException instead of loading a page.
package com.lagradost.cloudstream3.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.mvvm.debugException
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.nicehttp.requestCreator
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * When used as Interceptor additionalUrls cannot be returned, use WebViewResolver(...).resolveUsingWebView(...)
 * @param interceptUrl will stop the WebView when reaching this url.
 * @param additionalUrls this will make resolveUsingWebView also return all other requests matching the list of Regex.
 * @param userAgent if null then will use the default user agent
 * @param useOkhttp will try to use the okhttp client as much as possible, but this might cause some requests to fail. Disable for cloudflare.
 * @param script pass custom js to execute
 * @param scriptCallback will be called with the result from custom js
 * @param timeout close webview after timeout
 * */
class WebViewResolver constructor(
    val interceptUrl: Regex,
    val additionalUrls: List<Regex> = emptyList(),
    val userAgent: String? = USER_AGENT,
    val useOkhttp: Boolean = true,
    val script: String? = null,
    val scriptCallback: ((String) -> Unit)? = null,
    val timeout: Long = DEFAULT_TIMEOUT
) :
    Interceptor {

    companion object {
        var webViewUserAgent: String? = null
        val DEFAULT_TIMEOUT = 60_000L

        /** Without a WebView there is no WebView user agent unless one was set. */
        @JvmName("getWebViewUserAgent1")
        fun getWebViewUserAgent(): String? = webViewUserAgent
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return runBlocking {
            val fixedRequest = resolveUsingWebView(request).first
            return@runBlocking chain.proceed(fixedRequest ?: request)
        }
    }

    suspend fun resolveUsingWebView(
        url: String,
        referer: String? = null,
        method: String = "GET",
        requestCallBack: (Request) -> Boolean = { false },
    ): Pair<Request?, List<Request>> =
        resolveUsingWebView(url, referer, emptyMap(), method, requestCallBack)

    suspend fun resolveUsingWebView(
        url: String,
        referer: String? = null,
        headers: Map<String, String> = emptyMap(),
        method: String = "GET",
        requestCallBack: (Request) -> Boolean = { false },
    ): Pair<Request?, List<Request>> {
        return try {
            resolveUsingWebView(
                requestCreator(method, url, referer = referer, headers = headers), requestCallBack
            )
        } catch (e: java.lang.IllegalArgumentException) {
            logError(e)
            debugException { "ILLEGAL URL IN resolveUsingWebView!" }
            return null to emptyList()
        }
    }

    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
    suspend fun resolveUsingWebView(
        request: Request,
        requestCallBack: (Request) -> Boolean = { false }
    ): Pair<Request?, List<Request>> {
        throw IOException("WebViewResolver requires a WebView; not supported on desktop yet (${request.url})")
    }
}

fun WebResourceRequest.toRequest(): Request? {
    // The desktop stub is Kotlin, so its getters have no property syntax.
    val webViewUrl = this.getUrl().toString()

    // If invalid url then it can crash with
    // java.lang.IllegalArgumentException: Expected URL scheme 'http' or 'https' but was 'data'
    // At Request.Builder().url(addParamsToUrl(url, params))
    return safe {
        requestCreator(
            this.getMethod(),
            webViewUrl,
            this.getRequestHeaders(),
        )
    }
}

fun Response.toWebResourceResponse(): WebResourceResponse {
    val contentTypeValue = this.header("Content-Type")
    // 1. contentType. 2. charset
    val typeRegex = Regex("""(.*);(?:.*charset=(.*)(?:|;)|)""")
    return if (contentTypeValue != null) {
        val found = typeRegex.find(contentTypeValue)
        val contentType = found?.groupValues?.getOrNull(1)?.ifBlank { null } ?: contentTypeValue
        val charset = found?.groupValues?.getOrNull(2)?.ifBlank { null }
        WebResourceResponse(contentType, charset, this.body.byteStream())
    } else {
        WebResourceResponse("application/octet-stream", null, this.body.byteStream())
    }
}
