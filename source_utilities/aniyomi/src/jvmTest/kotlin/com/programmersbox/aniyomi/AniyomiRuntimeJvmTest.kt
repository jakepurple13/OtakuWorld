package com.programmersbox.aniyomi

import android.app.Application
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AniyomiRuntimeJvmTest {
    private val tempDir = createTempDirectory("aniyomi-jvm").toFile()

    @Test
    fun runtimeRegistersNetworkHelper() {
        AniyomiRuntime.init(Application("aniyomi-test", tempDir))
        assertNotNull(Injekt.get<NetworkHelper>().client)
    }

    @Test
    fun factoryNeverConfigurableOnJvm() {
        assertFalse(aniyomiApiService(FakeConfigurableAnimeSource(), 16.0) is KmpConfigurableApiService)
    }

    @Test
    fun cloudflareChallengeFailsClearly() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse.Builder()
                    .code(403)
                    .addHeader("Server", "cloudflare")
                    .body("<title>Just a moment...</title><div id=\"challenge-form\"></div>")
                    .build()
            )
            val cookieJar = AndroidCookieJar()
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(
                    CloudflareInterceptor(
                        context = Application("aniyomi-test", tempDir),
                        cookieManager = cookieJar,
                        defaultUserAgentProvider = { "ua" },
                    )
                )
                .build()
            val e = assertFailsWith<IOException> { client.newCall(Request(server.url("/"))).execute() }
            assertContains(e.message!!, "Cloudflare")
        }
    }

    @Test
    fun normalResponsePassesThroughCloudflareInterceptor() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(200).body("hello").build())
            val cookieJar = AndroidCookieJar()
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(
                    CloudflareInterceptor(
                        context = Application("aniyomi-test", tempDir),
                        cookieManager = cookieJar,
                        defaultUserAgentProvider = { "ua" },
                    )
                )
                .build()
            val body = client.newCall(Request(server.url("/"))).execute().use { it.body.string() }
            assertEquals("hello", body)
        }
    }

    @Test
    fun cookieJarStoresAndRemovesCookies() {
        MockWebServer().use { server ->
            server.start()
            val url = server.url("/")
            server.enqueue(
                MockResponse.Builder().code(200).addHeader("Set-Cookie", "a=1").addHeader("Set-Cookie", "b=2").build()
            )
            val cookieJar = AndroidCookieJar()
            OkHttpClient.Builder().cookieJar(cookieJar).build().newCall(Request(url)).execute().close()
            assertEquals(setOf("a" to "1", "b" to "2"), cookieJar.get(url).map { it.name to it.value }.toSet())

            assertEquals(1, cookieJar.remove(url, listOf("a")))
            assertEquals(listOf("b"), cookieJar.get(url).map { it.name })

            cookieJar.removeAll()
            assertEquals(emptyList(), cookieJar.get(url))
        }
    }

    @Test
    fun httpServerStartsOnJvm() {
        val port = AniyomiHttpServers.portFor(FakeHttpServerSource())
        assertTrue(port > 0)
    }
}
