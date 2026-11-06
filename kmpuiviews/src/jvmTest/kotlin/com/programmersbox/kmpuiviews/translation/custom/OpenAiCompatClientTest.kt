package com.programmersbox.kmpuiviews.translation.custom

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotModelFiles
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotRuntime
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenAiCompatClientTest {
    private lateinit var settings: DesktopTranslationSettings
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @BeforeTest
    fun setUp() {
        createTestDataStoreHandling()
        settings = DesktopTranslationSettings(AppDirs { appName = "test"; appAuthor = "test" })
        runBlocking {
            settings.customServerUrl.set("http://localhost:1234/v1/")
            settings.customServerModel.set("m")
            settings.customServerKey.set("")
            settings.customMode.set("server")
        }
    }

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        OpenAiCompatClient(HttpClient(MockEngine(handler)), settings)

    private val reply = """{"choices":[{"message":{"role":"assistant","content":" Hi "}}]}"""

    @Test
    fun `check counts models`() = runBlocking {
        var path = ""
        val c = client { req -> path = req.url.encodedPath; respond("""{"data":[{"id":"a"},{"id":"b"}]}""", headers = json) }
        assertEquals(2, c.check().getOrThrow())
        assertEquals("/v1/models", path)
    }

    @Test
    fun `server sends bearer only when key set`() = runBlocking {
        var auth: String? = "unset"
        var body = ""
        val c = client { req ->
            auth = req.headers[HttpHeaders.Authorization]
            body = String(req.body.toByteArray())
            respond(reply, headers = json)
        }
        assertEquals(" Hi ", c.chat("SYS", "konnichiwa"))
        assertNull(auth)
        assertTrue(body.contains("SYS") && body.contains("konnichiwa") && body.contains("\"model\":\"m\""))
        settings.customServerKey.set("sk-1")
        c.chat("s", "u")
        assertEquals("Bearer sk-1", auth)
    }

    @Test
    fun `server unreachable gives unavailable message with url`() = runBlocking {
        val c = client { throw IOException("refused") }
        val msg = "Can't reach the server at http://localhost:1234/v1. Is it running?"
        assertEquals(msg, assertFailsWith<TranslationUnavailable> { c.chat("s", "u") }.message)
        assertEquals(msg, c.check().exceptionOrNull()!!.message)
    }

    @Test
    fun `timeout gives unavailable message with url`() = runBlocking {
        val c = client { throw HttpRequestTimeoutException("http://x", 1) }
        val e = assertFailsWith<TranslationUnavailable> { c.chat("s", "u") }
        assertEquals("The server took too long to answer at http://localhost:1234/v1.", e.message)
    }

    @Test
    fun `non-2xx gives unavailable message with url and code`() = runBlocking {
        val c = client { respond("nope", HttpStatusCode.Unauthorized) }
        val e = assertFailsWith<TranslationUnavailable> { c.chat("s", "u") }
        assertEquals("The server at http://localhost:1234/v1 answered HTTP 401.", e.message)
    }

    @Test
    fun `engine strips quotes and requires model`() = runBlocking {
        val c = client { respond("""{"choices":[{"message":{"content":"\"Hello\""}}]}""", headers = json) }
        val engine = CustomEngine(settings, NoRuntime, c)
        assertEquals("Hello", engine.translate("x", "ja", "en"))
        settings.customServerModel.set("")
        val e = assertFailsWith<TranslationUnavailable> { engine.translate("x", "ja", "en") }
        assertEquals("Pick a model in Settings → Translation", e.message)
    }

    private object NoRuntime : BergamotRuntime {
        override suspend fun unavailableReason(): String? = null
        override suspend fun install(onProgress: (Float) -> Unit) {}
        override suspend fun translate(text: String, model: BergamotModelFiles) = text
    }
}
