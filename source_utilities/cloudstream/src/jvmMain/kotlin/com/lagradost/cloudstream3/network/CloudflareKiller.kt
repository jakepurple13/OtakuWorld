// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: desktop replacement for the androidMain CloudflareKiller, same public members. Solving a
// challenge needs a WebView, which desktop does not have yet, so a challenge fails the request with
// a readable IOException. Cookies put into `savedCookies` are still sent.
package com.lagradost.cloudstream3.network

import androidx.annotation.AnyThread
import com.lagradost.nicehttp.cookies
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.URI

@AnyThread
class CloudflareKiller : Interceptor {
    companion object {
        const val TAG = "CloudflareKiller"
        private val ERROR_CODES = listOf(403, 503)
        private val CLOUDFLARE_SERVERS = listOf("cloudflare-nginx", "cloudflare")
        fun parseCookieMap(cookie: String): Map<String, String> {
            return cookie.split(";").associate {
                val split = it.split("=")
                (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
            }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
        }
    }

    val savedCookies: MutableMap<String, Map<String, String>> = mutableMapOf()

    /**
     * Gets the headers with cookies, webview user agent included!
     * */
    fun getCookieHeaders(url: String): Headers {
        val userAgentHeaders = WebViewResolver.webViewUserAgent?.let {
            mapOf("user-agent" to it)
        } ?: emptyMap()

        return getHeaders(userAgentHeaders, savedCookies[URI(url).host] ?: emptyMap())
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cookies = savedCookies[request.url.host]
        val response = if (cookies == null) {
            chain.proceed(request)
        } else {
            val headers = getHeaders(request.headers.toMap(), cookies + request.cookies)
            chain.proceed(request.newBuilder().headers(headers).build())
        }
        if (response.header("Server") in CLOUDFLARE_SERVERS && response.code in ERROR_CODES) {
            response.close()
            throw IOException("Cloudflare-protected (${request.url.host}); solving it requires a WebView; not supported on desktop yet")
        }
        return response
    }
}
