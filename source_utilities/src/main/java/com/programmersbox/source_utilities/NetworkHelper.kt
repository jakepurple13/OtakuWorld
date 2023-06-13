package com.programmersbox.source_utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.webkit.*
import android.widget.Toast
import androidx.webkit.WebViewFeature
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

class CloudflareInterceptor(private val context: Context) : Interceptor, KoinComponent {

    private val handler = Handler(Looper.getMainLooper())

    private val networkHelper: NetworkHelper by inject()

    /**
     * When this is called, it initializes the WebView if it wasn't already. We use this to avoid
     * blocking the main thread too much. If used too often we could consider moving it to the
     * Application class.
     */
    private val initWebView by lazy {
        WebSettings.getDefaultUserAgent(context)
    }

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        initWebView

        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // Check if Cloudflare anti-bot is on
        if (response.code != 503 || response.header("Server") !in SERVER_CHECK) {
            return response
        }

        try {
            response.close()
            networkHelper.cookieManager.remove(originalRequest.url, COOKIE_NAMES, 0)
            val oldCookie = networkHelper.cookieManager.get(originalRequest.url)
                .firstOrNull { it.name == "cf_clearance" }
            resolveWithWebView(originalRequest, oldCookie)

            return chain.proceed(originalRequest)
        } catch (e: Exception) {
            // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
            // we don't crash the entire app
            throw IOException(e.localizedMessage)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request, oldCookie: Cookie?) {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webView: WebView? = null

        var challengeFound = false
        var cloudflareBypassed = false
        var isWebViewOutdated = false

        val origRequestUrl = request.url.toString()
        val headers = request.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

        handler.post {
            val webview = WebView(context)
            webView = webview
            webview.settings.javaScriptEnabled = true

            // Avoid sending empty User-Agent, Chromium WebView will reset to default if empty
            webview.settings.userAgentString = request.header("User-Agent")
                ?: "Mozilla/5.0 (Windows NT 6.3; WOW64)"//HttpSource.DEFAULT_USERAGENT

            webview.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed(): Boolean {
                        return networkHelper.cookieManager.get(origRequestUrl.toHttpUrl())
                            .firstOrNull { it.name == "cf_clearance" }
                            .let { it != null && it != oldCookie }
                    }

                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }

                    // HTTP error codes are only received since M
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR) &&
                        url == origRequestUrl && !challengeFound
                    ) {
                        // The first request didn't return the challenge, abort.
                        latch.countDown()
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse
                ) {
                    if (request.isForMainFrame) {
                        if (errorResponse.statusCode == 503) {
                            // Found the Cloudflare challenge page.
                            challengeFound = true
                        } else {
                            // Unlock thread, the challenge wasn't found.
                            latch.countDown()
                        }
                    }
                }
            }

            webView?.loadUrl(origRequestUrl, headers)
        }

        // Wait a reasonable amount of time to retrieve the solution. The minimum should be
        // around 4 seconds but it can take more due to slow networks or server issues.
        latch.await(12, TimeUnit.SECONDS)

        handler.post {
            if (!cloudflareBypassed) {
                isWebViewOutdated = webView?.isOutdated() == true
            }

            webView?.stopLoading()
            webView?.destroy()
        }

        // Throw exception if we failed to bypass Cloudflare
        if (!cloudflareBypassed) {
            // Prompt user to update WebView if it seems too outdated
            if (isWebViewOutdated) {
                //context.toast(R.string.information_webview_outdated, Toast.LENGTH_LONG)
                Toast.makeText(context, "Update WebView", Toast.LENGTH_SHORT).show()
            }

            throw Exception("Failed to bypass Cloudflare")
        }
    }

    companion object {
        private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
        private val COOKIE_NAMES = listOf("__cfduid", "cf_clearance")
    }
}

object WebViewUtil {
    val WEBVIEW_UA_VERSION_REGEX by lazy {
        Regex(""".*Chrome/(\d+)\..*""")
    }

    const val MINIMUM_WEBVIEW_VERSION = 80

    fun supportsWebView(context: Context): Boolean {
        try {
            // May throw android.webkit.WebViewFactory$MissingWebViewPackageException if WebView
            // is not installed
            CookieManager.getInstance()
        } catch (e: Exception) {
            return false
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
    }
}

fun WebView.isOutdated(): Boolean {
    return getWebViewMajorVersion(this) < WebViewUtil.MINIMUM_WEBVIEW_VERSION
}

// Based on https://stackoverflow.com/a/29218966
private fun getWebViewMajorVersion(webview: WebView): Int {
    val originalUA: String = webview.settings.userAgentString

    // Next call to getUserAgentString() will get us the default
    webview.settings.userAgentString = null

    val uaRegexMatch = WebViewUtil.WEBVIEW_UA_VERSION_REGEX.matchEntire(webview.settings.userAgentString)
    val webViewVersion: Int = if (uaRegexMatch != null && uaRegexMatch.groupValues.size > 1) {
        uaRegexMatch.groupValues[1].toInt()
    } else {
        0
    }

    // Revert to original UA string
    webview.settings.userAgentString = originalUA

    return webViewVersion
}

class AndroidCookieJar : CookieJar {

    private val manager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()

        cookies.forEach { manager.setCookie(urlString, it.toString()) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return get(url)
    }

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())

        return if (cookies != null && cookies.isNotEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else {
            emptyList()
        }
    }

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1) {
        val urlString = url.toString()
        val cookies = manager.getCookie(urlString) ?: return

        fun List<String>.filterNames(): List<String> {
            return if (cookieNames != null) {
                this.filter { it in cookieNames }
            } else {
                this
            }
        }

        cookies.split(";")
            .map { it.substringBefore("=") }
            .filterNames()
            .onEach { manager.setCookie(urlString, "$it=;Max-Age=$maxAge") }
    }

    fun removeAll() {
        manager.removeAllCookies {}
    }
}

class NetworkHelper(context: Context) {

    //private val preferences: PreferencesHelper by injectLazy()

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    val client by lazy {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .cache(Cache(cacheDir, cacheSize))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(UserAgentInterceptor())

        /*if (BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addInterceptor(httpLoggingInterceptor)
        }*/

        //if (preferences.enableDoh()) {
        builder.dns(
            DnsOverHttps.Builder().client(builder.build())
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
                        InetAddress.getByName("2606:4700:4700::6400")
                    )
                )
                .build()
        )
        //}

        builder.build()
    }

    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }
}

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}