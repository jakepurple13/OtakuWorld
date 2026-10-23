// OTAKU: desktop replacement for the vendored Android CloudflareInterceptor. Solving a challenge needs a
// WebView, which desktop does not have yet, so a challenge fails the request with a readable error.
package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import eu.kanade.tachiyomi.network.AndroidCookieJar
import kotlinx.coroutines.CoroutineScope
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

@Suppress("unused")
class CloudflareInterceptor(
    private val context: Context,
    private val cookieManager: AndroidCookieJar,
    scope: CoroutineScope,
    defaultUserAgentProvider: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!shouldIntercept(response)) return response
        response.close()
        throw IOException("Cloudflare-protected; not supported on desktop yet")
    }

    private fun shouldIntercept(response: Response): Boolean =
        response.code in ERROR_CODES && response.header("Server") in SERVER_CHECK
}

private val ERROR_CODES = listOf(403, 503)
private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
