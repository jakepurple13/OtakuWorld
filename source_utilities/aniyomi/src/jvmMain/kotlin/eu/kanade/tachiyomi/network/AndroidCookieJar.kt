// OTAKU: desktop replacement for the vendored Android AndroidCookieJar (no WebView CookieManager on JVM).
// Same public members; cookies are kept in memory only. Requests also carry what extensions set through the
// android.webkit.CookieManager stub, which on Android is the store this jar reads.
package eu.kanade.tachiyomi.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class AndroidCookieJar : CookieJar {

    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this.cookies) {
            cookies.forEach { new ->
                this.cookies.removeAll { it.name == new.name && it.domain == new.domain && it.path == new.path }
                if (new.expiresAt > System.currentTimeMillis()) this.cookies += new
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val own = get(url)
        val names = own.mapTo(mutableSetOf()) { it.name }
        val fromWebkit = CookieManager.getInstance().getCookie(url.toString())
            ?.split("; ")
            ?.mapNotNull { Cookie.parse(url, it) }
            ?.filter { it.name !in names }
            .orEmpty()
        return own + fromWebkit
    }

    fun get(url: HttpUrl): List<Cookie> = synchronized(cookies) {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt <= now }
        cookies.filter { it.matches(url) }
    }

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1): Int = synchronized(cookies) {
        val removed = get(url).filter { cookieNames == null || it.name in cookieNames }
        cookies.removeAll(removed)
        removed.size
    }

    fun removeAll() {
        synchronized(cookies) { cookies.clear() }
    }
}
