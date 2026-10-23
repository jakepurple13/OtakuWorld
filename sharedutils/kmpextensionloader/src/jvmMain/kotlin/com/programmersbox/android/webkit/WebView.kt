package android.webkit

import android.content.Context
import android.widget.AbsoluteLayout

/**
 * No browser engine on desktop: nothing loads, scripts never run, and clients get no callbacks.
 * Extensions that wait on a WebView (token generation, JS challenges) hit their own timeout instead
 * of failing to load with NoClassDefFoundError.
 */
open class WebView(context: Context) : AbsoluteLayout(context) {
    private val settings: WebSettings = DesktopWebSettings()
    private var webViewClient: WebViewClient = WebViewClient()
    private var webChromeClient: WebChromeClient? = null

    open fun getSettings(): WebSettings = settings

    open fun setWebViewClient(client: WebViewClient) {
        webViewClient = client
    }

    open fun getWebViewClient(): WebViewClient = webViewClient

    open fun setWebChromeClient(client: WebChromeClient?) {
        webChromeClient = client
    }

    open fun getWebChromeClient(): WebChromeClient? = webChromeClient

    open fun loadUrl(url: String) {}
    open fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {}
    open fun loadData(data: String, mimeType: String?, encoding: String?) {}
    open fun loadDataWithBaseURL(baseUrl: String?, data: String, mimeType: String?, encoding: String?, historyUrl: String?) {}
    open fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {}
    open fun addJavascriptInterface(obj: Any, name: String) {}
    open fun removeJavascriptInterface(name: String) {}
    open fun stopLoading() {}
    open fun reload() {}
    open fun destroy() {}
    open fun getUrl(): String? = null

    companion object {
        @JvmStatic fun getWebViewClassLoader(): ClassLoader = WebView::class.java.classLoader
    }
}

open class WebViewClient

open class WebChromeClient

fun interface ValueCallback<T> {
    fun onReceiveValue(value: T)
}

abstract class WebSettings {
    abstract fun getJavaScriptEnabled(): Boolean
    abstract fun setJavaScriptEnabled(flag: Boolean)
    abstract fun getDomStorageEnabled(): Boolean
    abstract fun setDomStorageEnabled(flag: Boolean)
    abstract fun getBlockNetworkImage(): Boolean
    abstract fun setBlockNetworkImage(flag: Boolean)
    abstract fun getLoadWithOverviewMode(): Boolean
    abstract fun setLoadWithOverviewMode(overview: Boolean)
    abstract fun getUseWideViewPort(): Boolean
    abstract fun setUseWideViewPort(use: Boolean)
    abstract fun getUserAgentString(): String?
    abstract fun setUserAgentString(ua: String?)
}

private class DesktopWebSettings : WebSettings() {
    private var javaScript = false
    private var domStorage = false
    private var blockNetworkImage = false
    private var overview = false
    private var wideViewPort = false
    private var userAgent: String? = null

    override fun getJavaScriptEnabled() = javaScript
    override fun setJavaScriptEnabled(flag: Boolean) {
        javaScript = flag
    }

    override fun getDomStorageEnabled() = domStorage
    override fun setDomStorageEnabled(flag: Boolean) {
        domStorage = flag
    }

    override fun getBlockNetworkImage() = blockNetworkImage
    override fun setBlockNetworkImage(flag: Boolean) {
        blockNetworkImage = flag
    }

    override fun getLoadWithOverviewMode() = overview
    override fun setLoadWithOverviewMode(overview: Boolean) {
        this.overview = overview
    }

    override fun getUseWideViewPort() = wideViewPort
    override fun setUseWideViewPort(use: Boolean) {
        wideViewPort = use
    }

    override fun getUserAgentString() = userAgent
    override fun setUserAgentString(ua: String?) {
        userAgent = ua
    }
}

interface WebResourceRequest {
    fun getUrl(): android.net.Uri
    fun isForMainFrame(): Boolean
    fun isRedirect(): Boolean
    fun hasGesture(): Boolean
    fun getMethod(): String
    fun getRequestHeaders(): Map<String, String>
}

open class WebResourceResponse(
    private var mimeType: String?,
    private var encoding: String?,
    private var statusCode: Int,
    private var reasonPhrase: String?,
    private var responseHeaders: Map<String, String>?,
    private var data: java.io.InputStream?,
) {
    constructor(mimeType: String?, encoding: String?, data: java.io.InputStream?) : this(mimeType, encoding, 200, "OK", null, data)

    fun getMimeType(): String? = mimeType
    fun getEncoding(): String? = encoding
    fun getStatusCode(): Int = statusCode
    fun getReasonPhrase(): String? = reasonPhrase
    fun getResponseHeaders(): Map<String, String>? = responseHeaders
    fun getData(): java.io.InputStream? = data
}

abstract class WebResourceError {
    abstract fun getErrorCode(): Int
    abstract fun getDescription(): CharSequence
}

abstract class RenderProcessGoneDetail {
    abstract fun didCrash(): Boolean
    abstract fun rendererPriorityAtExit(): Int
}

open class ConsoleMessage(
    private val message: String,
    private val sourceId: String,
    private val lineNumber: Int,
    private val messageLevel: MessageLevel,
) {
    enum class MessageLevel { TIP, LOG, WARNING, ERROR, DEBUG }

    fun message(): String = message
    fun sourceId(): String = sourceId
    fun lineNumber(): Int = lineNumber
    fun messageLevel(): MessageLevel = messageLevel
}
