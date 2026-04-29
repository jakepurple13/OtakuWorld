package com.programmersbox.source_utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewFeature
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

private const val TAG = "NetworkHelper"

fun Request.Builder.header(pair: Pair<String, String>) = header(pair.first, pair.second)
fun Request.Builder.header(vararg pair: Pair<String, String>) = apply { pair.forEach { header(it.first, it.second) } }
fun Connection.headers(vararg pair: Pair<String, String>) = apply { headers(pair.toMap()) }

fun Response.asJsoup(html: String? = null): Document = Jsoup.parse(html ?: body!!.string(), request.url.toString())

fun cloudflare(networkHelper: NetworkHelper, url: String, vararg headers: Pair<String, String>) = networkHelper
    .cloudflareClient.newCall(
        Request.Builder()
            .url(url)
            .header(*headers)
            .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
            .build()
    )

fun OkHttpClient.cloudflare(url: String, vararg headers: Pair<String, String>) = newCall(
    Request.Builder()
        .url(url)
        .header(*headers)
        .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
        .build()
)

// Wraps non-IOException from extension code so OkHttp's enqueue() handles them gracefully
// instead of crashing the app.
class UncaughtExceptionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            if (e is IOException) throw e
            throw IOException(e)
        }
    }
}

// Per-host sliding-window rate limiter. Runs as a network interceptor so cached responses
// don't consume a permit. Also handles 429 responses with Retry-After backoff.
class RateLimitInterceptor(
    private val requestsPerPeriod: Int = 10,
    private val periodMillis: Long = 1_000L,
) : Interceptor {
    private val hostQueues = ConcurrentHashMap<String, ArrayDeque<Long>>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        val queue = hostQueues.getOrPut(host) { ArrayDeque(requestsPerPeriod) }

        synchronized(queue) {
            val now = SystemClock.elapsedRealtime()
            val windowStart = now - periodMillis
            while (queue.isNotEmpty() && queue.first() < windowStart) {
                queue.removeFirst()
            }
            if (queue.size >= requestsPerPeriod) {
                val sleepMs = queue.first() + periodMillis - now
                if (sleepMs > 0) Thread.sleep(sleepMs)
                queue.removeFirst()
            }
            queue.addLast(SystemClock.elapsedRealtime())
        }

        val response = chain.proceed(chain.request())

        if (response.code == 429) {
            val retryAfterMs = (response.header("Retry-After")?.toLongOrNull() ?: 60L) * 1000L
            Log.w(TAG, "429 from ${host}, backing off ${retryAfterMs}ms")
            response.close()
            Thread.sleep(retryAfterMs)
            return chain.proceed(chain.request())
        }

        return response
    }
}

class CloudflareInterceptor(private val context: Context) : Interceptor, KoinComponent {

    private val handler = Handler(Looper.getMainLooper())
    private val networkHelper: NetworkHelper by inject()
    private val lock = ReentrantLock()

    private val initWebView by lazy {
        // Trigger WebView init early; some devices crash on getDefaultUserAgent on main thread
        // if called for the first time under load.
        runCatching { WebSettings.getDefaultUserAgent(context) }.getOrNull()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        initWebView
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.code !in CF_ERROR_CODES || response.header("Server") !in SERVER_CHECK) {
            return response
        }

        // Peek at the body to confirm this is a solvable CF challenge, not a geo-block or hard 403.
        val bodyHtml = response.peekBody(Long.MAX_VALUE).string()
        if (!isChallengeBody(bodyHtml)) {
            Log.d(TAG, "CF error on ${originalRequest.url.host} but not a solvable challenge — skipping bypass")
            return response
        }

        // Capture clearance state before waiting for the lock so we can detect if another
        // thread solved the challenge while we were blocked.
        val clearanceBefore = networkHelper.cookieManager.get(originalRequest.url)
            .firstOrNull { it.name == "cf_clearance" }

        response.close()

        lock.lock()
        try {
            // If another thread obtained cf_clearance while we waited, just retry.
            val clearanceNow = networkHelper.cookieManager.get(originalRequest.url)
                .firstOrNull { it.name == "cf_clearance" }
            if (clearanceNow != null && clearanceNow != clearanceBefore) {
                return chain.proceed(originalRequest)
            }

            networkHelper.cookieManager.remove(originalRequest.url, COOKIE_NAMES, 0)
            val oldCookie = networkHelper.cookieManager.get(originalRequest.url)
                .firstOrNull { it.name == "cf_clearance" }
            resolveWithWebView(originalRequest, oldCookie)
            return chain.proceed(originalRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Cloudflare bypass failed for ${originalRequest.url.host}", e)
            throw IOException(e.localizedMessage)
        } finally {
            lock.unlock()
        }
    }

    private fun isChallengeBody(html: String): Boolean {
        return html.contains("challenge-error-title") ||
                html.contains("Just a moment") ||
                html.contains("Enable JavaScript and cookies to continue") ||
                html.contains("cf-challenge-running")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request, oldCookie: Cookie?) {
        val latch = CountDownLatch(1)
        var webView: WebView? = null
        var challengeFound = false
        var cloudflareBypassed = false
        var isWebViewOutdated = false

        val origRequestUrl = request.url.toString()

        // Filter headers that WebView rejects to avoid net::ERR_INVALID_ARGUMENT
        val safeHeaders = request.headers.toMultimap()
            .filterKeys { it.lowercase() !in UNSAFE_HEADERS }
            .mapValues { it.value.getOrNull(0) ?: "" }

        handler.post {
            val webview = WebView(context)
            webView = webview
            webview.settings.javaScriptEnabled = true
            webview.settings.domStorageEnabled = true
            webview.settings.userAgentString = request.header("User-Agent") ?: DEFAULT_USER_AGENT

            webview.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed() = networkHelper.cookieManager
                        .get(origRequestUrl.toHttpUrl())
                        .firstOrNull { it.name == "cf_clearance" }
                        .let { it != null && it != oldCookie }

                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR) &&
                        url == origRequestUrl && !challengeFound
                    ) {
                        latch.countDown()
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame) {
                        if (errorResponse.statusCode == 503) {
                            challengeFound = true
                        } else {
                            latch.countDown()
                        }
                    }
                }
            }

            webView?.loadUrl(origRequestUrl, safeHeaders)
        }

        // 30 seconds — CF challenges on slow networks can take longer than the old 12s limit.
        latch.await(30, TimeUnit.SECONDS)

        handler.post {
            if (!cloudflareBypassed) {
                isWebViewOutdated = webView?.isOutdated() == true
            }
            webView?.stopLoading()
            webView?.destroy()
        }

        if (!cloudflareBypassed) {
            if (isWebViewOutdated) {
                Toast.makeText(context, "Update WebView to load this source", Toast.LENGTH_SHORT).show()
            }
            throw IOException("Failed to bypass Cloudflare for ${request.url.host}")
        }
    }

    companion object {
        private val CF_ERROR_CODES = setOf(403, 503)
        private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
        private val COOKIE_NAMES = listOf("__cfduid", "cf_clearance")
        private val UNSAFE_HEADERS = setOf(
            "content-length", "host", "trailer", "te", "upgrade",
            "proxy-authorization", "proxy-authenticate", "proxy-connection"
        )
    }
}

object WebViewUtil {
    val WEBVIEW_UA_VERSION_REGEX by lazy { Regex(""".*Chrome/(\d+)\..*""") }
    const val MINIMUM_WEBVIEW_VERSION = 80

    fun supportsWebView(context: Context): Boolean {
        try {
            CookieManager.getInstance()
        } catch (e: Exception) {
            return false
        }
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
    }
}

fun WebView.isOutdated(): Boolean = getWebViewMajorVersion(this) < WebViewUtil.MINIMUM_WEBVIEW_VERSION

private fun getWebViewMajorVersion(webview: WebView): Int {
    val originalUA = webview.settings.userAgentString
    webview.settings.userAgentString = null
    val uaRegexMatch = WebViewUtil.WEBVIEW_UA_VERSION_REGEX.matchEntire(webview.settings.userAgentString)
    val webViewVersion = if (uaRegexMatch != null && uaRegexMatch.groupValues.size > 1) {
        uaRegexMatch.groupValues[1].toInt()
    } else {
        0
    }
    webview.settings.userAgentString = originalUA
    return webViewVersion
}

class AndroidCookieJar : CookieJar {

    private val manager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        cookies.forEach { manager.setCookie(urlString, it.toString()) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = get(url)

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())
        return if (!cookies.isNullOrEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else {
            emptyList()
        }
    }

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1) {
        val urlString = url.toString()
        val cookies = manager.getCookie(urlString) ?: return

        fun List<String>.filterNames() = if (cookieNames != null) filter { it in cookieNames } else this

        cookies.split(";")
            .map { it.substringBefore("=") }
            .filterNames()
            .onEach { manager.setCookie(urlString, "$it=;Max-Age=$maxAge") }
    }

    fun removeAll() {
        manager.removeAllCookies {}
    }
}

// Chains multiple Dns resolvers. Falls back through the list; falls back to system DNS last.
class ChainedDns(private val resolvers: List<Dns>) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        for (resolver in resolvers) {
            try {
                val result = resolver.lookup(hostname)
                if (result.isNotEmpty()) return result
            } catch (e: Exception) {
                Log.w(TAG, "DNS resolver failed for $hostname, trying next", e)
            }
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}

const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

// API 34+ (UPSIDE_DOWN_CAKE) enforces that Views (including WebView) may only be constructed
// from a context that has proper display configuration. Application context fails this check.
// createDisplayContext returns a ContextWrapper that satisfies the assertion while delegating
// all other operations (file system, SharedPreferences, ContentResolver) to the base context.
fun Context.asDisplayContext(): Context {
    val displayManager = getSystemService(DisplayManager::class.java)
    val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return this
    return createDisplayContext(display)
}

class NetworkHelper(context: Context) {

    val cookieManager = AndroidCookieJar()

    // Display-context-backed context safe for WebView construction on API 34+.
    private val displayContext: Context = context.asDisplayContext()

    val client by lazy {
        val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val cloudflareDoh = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf(
                    InetAddress.getByName("162.159.36.1"),
                    InetAddress.getByName("162.159.46.1"),
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("162.159.132.53"),
                    InetAddress.getByName("2606:4700:4700::1111"),
                    InetAddress.getByName("2606:4700:4700::1001"),
                    InetAddress.getByName("2606:4700:4700::0064"),
                    InetAddress.getByName("2606:4700:4700::6400"),
                )
            )
            .build()

        val googleDoh = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf(
                    InetAddress.getByName("8.8.8.8"),
                    InetAddress.getByName("8.8.4.4"),
                )
            )
            .build()

        // we use "Unfiltered"
        val dohAdGuard =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns-unfiltered.adguard.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("94.140.14.140"),
                    InetAddress.getByName("94.140.14.141"),
                    InetAddress.getByName("2a10:50c0::1:ff"),
                    InetAddress.getByName("2a10:50c0::2:ff"),
                )
                .build()

        val dohQuad9 =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns.quad9.net/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("9.9.9.9"),
                    InetAddress.getByName("149.112.112.112"),
                    InetAddress.getByName("2620:fe::fe"),
                    InetAddress.getByName("2620:fe::9"),
                )
                .build()

        val dohAliDNS =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns.alidns.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("223.5.5.5"),
                    InetAddress.getByName("223.6.6.6"),
                    InetAddress.getByName("2400:3200::1"),
                    InetAddress.getByName("2400:3200:baba::1"),
                )
                .build()

        val dohDNSPod =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://doh.pub/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("1.12.12.12"),
                    InetAddress.getByName("120.53.53.53"),
                )
                .build()

        val doh360 =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://doh.360.cn/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("101.226.4.6"),
                    InetAddress.getByName("218.30.118.6"),
                    InetAddress.getByName("123.125.81.6"),
                    InetAddress.getByName("140.207.198.6"),
                    InetAddress.getByName("180.163.249.75"),
                    InetAddress.getByName("101.199.113.208"),
                    InetAddress.getByName("36.99.170.86"),
                )
                .build()

        val dohQuad101 =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns.twnic.tw/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("101.101.101.101"),
                    InetAddress.getByName("2001:de4::101"),
                    InetAddress.getByName("2001:de4::102"),
                )
                .build()

        /*
         * Mullvad DoH
         * without ad blocking option
         * Source: https://mullvad.net/en/help/dns-over-https-and-dns-over-tls
         */
        val dohMullvad =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url(" https://dns.mullvad.net/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("194.242.2.2"),
                    InetAddress.getByName("2a07:e340::2"),
                )
                .build()

        /*
         * Control D
         * unfiltered option
         * Source: https://controld.com/free-dns/?
         */
        val dohControlD =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://freedns.controld.com/p0".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("76.76.2.0"),
                    InetAddress.getByName("76.76.10.0"),
                    InetAddress.getByName("2606:1a40::"),
                    InetAddress.getByName("2606:1a40:1::"),
                )
                .build()

        /*
         * Njalla
         * Non logging and uncensored
         */
        val dohNajalla =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns.njal.la/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("95.215.19.53"),
                    InetAddress.getByName("2001:67c:2354:2::53"),
                )
                .build()

        /**
         * Source: https://shecan.ir/
         */
        val dohShecan =
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://free.shecan.ir/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("178.22.122.100"),
                    InetAddress.getByName("185.51.200.2"),
                )
                .build()

        OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .cache(Cache(File(context.cacheDir, "network_cache"), 5L * 1024 * 1024))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .dns(
                ChainedDns(
                    listOf(
                        cloudflareDoh,
                        googleDoh,
                        doh360,
                        dohQuad101,
                        dohQuad9,
                        dohAdGuard,
                        dohAliDNS,
                        dohDNSPod,
                        dohMullvad,
                        dohControlD,
                    )
                )
            )
            // Safety: wraps non-IO extension exceptions so the app doesn't crash
            .addInterceptor(UncaughtExceptionInterceptor())
            .addInterceptor(UserAgentInterceptor())
            // BrotliInterceptor adds Accept-Encoding: br alongside gzip (which BridgeInterceptor
            // sets). BridgeInterceptor still handles transparent gzip decompression; BrotliInterceptor
            // handles Brotli. Both are decompressed before application interceptors see the body.
            .addNetworkInterceptor(BrotliInterceptor)
            // Per-host sliding window; also retries on 429 with Retry-After backoff
            .addNetworkInterceptor(RateLimitInterceptor())
            .build()
    }

    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(CloudflareInterceptor(displayContext))
            .build()
    }
}

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            chain.proceed(
                originalRequest.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", DEFAULT_USER_AGENT)
                    .build()
            )
        } else {
            chain.proceed(originalRequest)
        }
    }
}

