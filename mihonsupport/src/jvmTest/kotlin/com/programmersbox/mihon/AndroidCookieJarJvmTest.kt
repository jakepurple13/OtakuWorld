package com.programmersbox.mihon

import android.webkit.CookieManager
import eu.kanade.tachiyomi.network.AndroidCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidCookieJarJvmTest {
    // Extensions (Webtoons, Tapas, ...) set consent cookies through CookieManager and expect requests to carry them.
    @Test fun requestsCarryCookiesSetThroughCookieManager() {
        val url = "https://www.cookiejar-test.example/list".toHttpUrl()
        CookieManager.getInstance().setCookie(url.toString(), "consent=yes; Domain=.cookiejar-test.example")
        CookieManager.getInstance().setCookie(url.toString(), "shared=webkit")
        val jar = AndroidCookieJar()
        jar.saveFromResponse(url, listOf(Cookie.parse(url, "shared=response; Max-Age=600")!!))

        val sent = jar.loadForRequest(url).associate { it.name to it.value }

        assertEquals(mapOf("consent" to "yes", "shared" to "response"), sent)
    }
}
