package android.webkit

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cookie store. On Android this is the store OkHttp's cookie jar reads, so extensions call
 * [setCookie] to pass e.g. consent or age-gate cookies to their requests; the desktop cookie jar merges
 * [getCookie] into each request for the same reason. Only Domain and expiry (Max-Age/Expires in the past
 * deletes) are honored; Path is ignored.
 */
abstract class CookieManager {
    abstract fun setAcceptCookie(accept: Boolean)
    abstract fun acceptCookie(): Boolean
    abstract fun setCookie(url: String, value: String)
    abstract fun setCookie(url: String, value: String, callback: ValueCallback<Boolean>?)
    abstract fun getCookie(url: String): String?
    abstract fun hasCookies(): Boolean
    abstract fun removeAllCookies(callback: ValueCallback<Boolean>?)
    abstract fun removeSessionCookies(callback: ValueCallback<Boolean>?)
    abstract fun flush()

    companion object {
        private val shared: CookieManager by lazy { DesktopCookieManager() }

        @JvmStatic fun getInstance(): CookieManager = shared
    }
}

private class DesktopCookieManager : CookieManager() {
    /** (domain, name) -> value; domain without a leading dot. */
    private val cookies = ConcurrentHashMap<Pair<String, String>, String>()
    @Volatile private var accept = true

    override fun setAcceptCookie(accept: Boolean) {
        this.accept = accept
    }

    override fun acceptCookie(): Boolean = accept

    override fun setCookie(url: String, value: String) {
        val host = host(url) ?: return
        val parts = value.split(';').map { it.trim() }
        val name = parts.first().substringBefore('=', "").trim().ifEmpty { return }
        val cookieValue = parts.first().substringAfter('=', "")
        val attributes = parts.drop(1).associate { it.substringBefore('=').trim().lowercase() to it.substringAfter('=', "").trim() }
        val domain = attributes["domain"]?.removePrefix(".")?.lowercase()?.ifEmpty { null } ?: host
        val key = domain to name
        if (isExpired(attributes)) cookies.remove(key) else cookies[key] = cookieValue
    }

    override fun setCookie(url: String, value: String, callback: ValueCallback<Boolean>?) {
        setCookie(url, value)
        callback?.onReceiveValue(true)
    }

    override fun getCookie(url: String): String? {
        val host = host(url) ?: return null
        return cookies.entries
            .filter { (key, _) -> host == key.first || host.endsWith(".${key.first}") }
            .joinToString("; ") { (key, value) -> "${key.second}=$value" }
            .ifEmpty { null }
    }

    override fun hasCookies(): Boolean = cookies.isNotEmpty()

    override fun removeAllCookies(callback: ValueCallback<Boolean>?) {
        val had = cookies.isNotEmpty()
        cookies.clear()
        callback?.onReceiveValue(had)
    }

    // Expiry is not tracked, so every cookie counts as persistent.
    override fun removeSessionCookies(callback: ValueCallback<Boolean>?) {
        callback?.onReceiveValue(false)
    }

    override fun flush() {}

    private fun host(url: String): String? =
        runCatching { URI(if ("://" in url) url else "https://$url").host?.lowercase() }.getOrNull()

    private fun isExpired(attributes: Map<String, String>): Boolean {
        attributes["max-age"]?.toLongOrNull()?.let { return it <= 0 }
        val expires = attributes["expires"] ?: return false
        return runCatching {
            java.time.ZonedDateTime.parse(expires, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .isBefore(java.time.Instant.now())
        }.getOrDefault(false)
    }
}
