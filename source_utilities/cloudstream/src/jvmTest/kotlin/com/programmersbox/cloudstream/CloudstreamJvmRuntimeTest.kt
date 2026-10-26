package com.programmersbox.cloudstream

import android.app.Application
import android.content.Context
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.Coroutines
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.Coroutines.mainWork
import com.programmersbox.kmpextensionloader.FileSharedPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class CloudstreamJvmRuntimeTest {
    private val tempDir = createTempDirectory("cloudstream-jvm").toFile()

    private val previousContext = CloudStreamApp.context

    @AfterTest
    fun cleanUp() {
        CloudStreamApp.context = previousContext
        tempDir.deleteRecursively()
    }

    // No Looper and no Dispatchers.Main module in this test classpath.
    @Test
    fun mainThreadHelpersRunWithoutLooper() = runBlocking {
        withTimeout(2.seconds) {
            withContext(Dispatchers.IO) {
                val ran = CompletableDeferred<Boolean>()
                Coroutines.runOnMainThread {
                    // Already on the main thread: runs inline instead of waiting on itself.
                    var nested = false
                    Coroutines.runOnMainThread { nested = true }
                    ran.complete(nested)
                }
                assertTrue(ran.await(), "nested runOnMainThread ran inline")

                val mainRan = CompletableDeferred<String>()
                "value".main { mainRan.complete(it) }
                assertEquals("value", mainRan.await())

                assertEquals(42, 41.mainWork { it + 1 })
            }
        }
    }

    // Plugins bind to these by name; on desktop they must exist with the same JVM names.
    @Test
    fun pluginFacadeClassesExistOnJvm() {
        val lines = File("shims.md").readLines()
        val jvmSection = lines.indexOf(JVM_MISSING_HEADING).also { assertTrue(it >= 0, "no \"$JVM_MISSING_HEADING\" section") }
        // Types the desktop host deliberately lacks (only touched inside never-run UI code).
        val jvmMissing = lines.drop(jvmSection).mapNotNull { JVM_MISSING_ROW.find(it)?.groupValues?.get(1) }.toSet()
        assertTrue(jvmMissing.isNotEmpty(), "\"$JVM_MISSING_HEADING\" table not parsed")
        val rows = lines.take(jvmSection)
            .mapNotNull { ROW.find(it) }
            .map { it.groupValues[1] to it.groupValues[2] }
            .filterNot { (_, status) -> status == "missing" || status.startsWith("app ") }
            .map { it.first }
            .filterNot { it in jvmMissing }
        assertTrue(rows.size > 20, "shims.md table not parsed: ${rows.size} rows")

        val unresolved = (rows + FACADES).filter { name ->
            runCatching { Class.forName(name, false, javaClass.classLoader) }.isFailure
        }
        if (unresolved.isNotEmpty()) fail("Unresolved types: $unresolved")

        // Member-level references seen in the reference plugins (shims.md).
        val companion = CloudStreamApp.Companion::class.java
        companion.getMethod("getContext")
        companion.getMethod("setKey", String::class.java, Any::class.java)
        val commonActivity = Class.forName("com.lagradost.cloudstream3.CommonActivity")
        commonActivity.getField("INSTANCE")
        commonActivity.getMethod("getActivity")
        commonActivity.getMethod(
            "showToast\$default",
            commonActivity, String::class.java, Integer::class.java, Int::class.javaPrimitiveType, Any::class.java,
        )
        val dataStore = Class.forName("com.lagradost.cloudstream3.utils.DataStore")
        dataStore.getField("INSTANCE")
        dataStore.getMethod("getSharedPrefs", Context::class.java)
        Class.forName("com.lagradost.cloudstream3.plugins.Plugin").getDeclaredConstructor()
    }

    @Test
    fun cloudflareChallengeFailsClearly() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("plain").build())
            server.enqueue(
                MockResponse.Builder()
                    .code(503)
                    .addHeader("Server", "cloudflare")
                    .body("<title>Just a moment...</title>")
                    .build()
            )
            val client = OkHttpClient.Builder().addInterceptor(CloudflareKiller()).build()

            client.newCall(Request(server.url("/ok"))).execute().use { assertEquals("plain", it.body.string()) }
            val e = assertFailsWith<IOException> { client.newCall(Request(server.url("/cf"))).execute() }
            assertContains(e.message.orEmpty(), "Cloudflare")
        }
    }

    @Test
    fun webViewResolverFailsClearly() = runBlocking {
        val resolver = WebViewResolver(Regex(".*"))
        val e = assertFailsWith<IOException> { resolver.resolveUsingWebView("https://example.com/page") }
        assertContains(e.message.orEmpty(), "requires a WebView")
        assertNull(WebViewResolver.getWebViewUserAgent())

        MockWebServer().use { server ->
            server.start()
            val client = OkHttpClient.Builder().addInterceptor(resolver).build()
            assertFailsWith<IOException> { client.newCall(Request(server.url("/"))).execute() }
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun dataStoreRoundTrip() {
        val app = Application("cloudstream.test", tempDir)
        CloudStreamApp.context = app

        CloudStreamApp.setKey("folder/answer", Stored("x", 42))
        assertEquals(Stored("x", 42), CloudStreamApp.getKey<Stored>("folder/answer", null))
        assertEquals(listOf("folder/answer"), CloudStreamApp.getKeys("folder"))

        // Persisted to disk, in CloudStream's own preferences file.
        val onDisk = FileSharedPreferences(File(app.dataDir, "prefs/cloudstream_data.properties"))
        assertTrue(onDisk.contains("folder/answer"))

        CloudStreamApp.removeKey("folder/answer")
        assertNull(CloudStreamApp.getKey<Stored>("folder/answer"))
        assertEquals(Stored("d", 0), CloudStreamApp.getKey("missing", Stored("d", 0)))
    }

    data class Stored(val name: String, val value: Int)

    private companion object {
        val ROW = Regex("""^\| `([\w.$]+)` \|[^|]*\| ([^|]+?) \|$""")
        const val JVM_MISSING_HEADING = "## JVM: missing at runtime only"
        val JVM_MISSING_ROW = Regex("""^\| `([\w.$]+)` \|""")
        val FACADES = listOf(
            "com.lagradost.api.ContextHelper_androidKt",
            "com.lagradost.api.Log",
            "com.lagradost.cloudstream3.MainAPIKt",
            "com.lagradost.cloudstream3.mvvm.ArchComponentExt_jvmCommonKt",
            "com.lagradost.cloudstream3.network.WebViewResolver",
            "com.lagradost.cloudstream3.network.WebViewResolver_androidKt",
            "com.lagradost.cloudstream3.utils.AppUtils",
            "com.lagradost.cloudstream3.utils.Coroutines",
            "com.lagradost.cloudstream3.utils.Coroutines_androidKt",
            "com.lagradost.cloudstream3.utils.Coroutines_jvmCommonKt",
            "com.lagradost.cloudstream3.utils.ExtractorApiKt",
            "com.lagradost.cloudstream3.utils.SubtitleHelperPlatform_androidKt",
        )
    }
}
