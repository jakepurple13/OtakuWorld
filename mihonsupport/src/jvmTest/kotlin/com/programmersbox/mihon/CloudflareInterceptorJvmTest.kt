package com.programmersbox.mihon

import android.app.Application
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudflareInterceptorJvmTest {
    private val server = MockWebServer()
    private val cookieJar = AndroidCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(
            CloudflareInterceptor(
                context = Application("test", createTempDirectory().toFile()),
                cookieManager = cookieJar,
                scope = CoroutineScope(Dispatchers.Default),
                defaultUserAgentProvider = { "ua" },
            )
        )
        .build()

    @BeforeTest
    fun setUp() = server.start()

    @AfterTest
    fun tearDown() = server.close()

    @Test
    fun cloudflareChallengeFailsWithReadableError() {
        server.enqueue(MockResponse.Builder().code(403).addHeader("Server", "cloudflare").build())
        val ex = assertFailsWith<IOException> { client.newCall(Request(server.url("/"))).execute() }
        assertEquals("Cloudflare-protected; not supported on desktop yet", ex.message)
    }

    @Test
    fun normalResponsePassesThrough() {
        server.enqueue(MockResponse.Builder().code(200).body("hello").build())
        val body = client.newCall(Request(server.url("/"))).execute().use { it.body.string() }
        assertEquals("hello", body)
    }

    @Test
    fun cookieJarStoresAndRemovesCookies() {
        val url = server.url("/")
        server.enqueue(MockResponse.Builder().code(200).addHeader("Set-Cookie", "a=1").addHeader("Set-Cookie", "b=2").build())
        client.newCall(Request(url)).execute().close()
        assertEquals(setOf("a" to "1", "b" to "2"), cookieJar.get(url).map { it.name to it.value }.toSet())

        assertEquals(1, cookieJar.remove(url, listOf("a")))
        assertEquals(listOf("b"), cookieJar.get(url).map { it.name })

        cookieJar.removeAll()
        assertEquals(emptyList(), cookieJar.get(url))
    }
}
