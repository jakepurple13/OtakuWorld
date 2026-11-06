package com.programmersbox.kmpuiviews.translation.ollama

import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OllamaClientTest {
    private lateinit var settings: DesktopTranslationSettings

    @BeforeTest
    fun setUp() {
        createTestDataStoreHandling()
        settings = DesktopTranslationSettings()
        runBlocking {
            settings.ollamaUrl.set("http://localhost:11434")
            settings.ollamaModel.set("")
        }
    }

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        OllamaClient(HttpClient(MockEngine(handler)), settings)

    @Test
    fun `status parses version and tags sizes`() = runBlocking {
        val c = client { req ->
            when (req.url.encodedPath) {
                "/api/version" -> respond("""{"version":"0.5.1"}""", headers = json)
                "/api/tags" -> respond(
                    """{"models":[{"name":"llama3:8b","size":4661224676},{"name":"qwen2.5:3b","size":1929912432}]}""",
                    headers = json
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val s = assertIs<OllamaStatus.Connected>(c.status())
        assertEquals("0.5.1", s.version)
        assertEquals(listOf(OllamaModel("llama3:8b", 4661224676), OllamaModel("qwen2.5:3b", 1929912432)), s.models)
    }

    @Test
    fun `status unreachable on connect error`() = runBlocking {
        val c = client { throw IOException("refused") }
        assertEquals(OllamaStatus.Unreachable("http://localhost:11434"), c.status())
    }

    @Test
    fun `pull emits fractions then 1f`() = runBlocking {
        val body = """
            {"status":"pulling manifest"}
            {"status":"downloading","total":100,"completed":25}
            {"status":"downloading","total":100,"completed":75}
            {"status":"success"}
        """.trimIndent()
        val c = client { respond(body, headers = json) }
        assertEquals(listOf(null, 0.25f, 0.75f, null, 1f), c.pull("llama3").toList())
    }

    @Test
    fun `pull failure surfaces error`() = runBlocking {
        val c = client { respond("""{"error":"pull model manifest: file does not exist"}""", headers = json) }
        val e = assertFailsWith<IllegalStateException> { c.pull("nope").toList() }
        assertTrue(e.message!!.contains("file does not exist"))
    }

    @Test
    fun `delete sends DELETE with name`() = runBlocking {
        var seen: HttpRequestData? = null
        var body = ""
        val c = client { req -> seen = req; body = String(req.body.toByteArray()); respond("") }
        c.delete("llama3")
        assertEquals(HttpMethod.Delete, seen!!.method)
        assertEquals("/api/delete", seen!!.url.encodedPath)
        assertTrue(body.contains("\"llama3\""))
    }

    @Test
    fun `chat sends system prompt and returns content`() = runBlocking {
        var body = ""
        val c = client { req ->
            body = String(req.body.toByteArray())
            respond("""{"message":{"role":"assistant","content":"  Hello  "}}""", headers = json)
        }
        assertEquals("  Hello  ", c.chat("m", "SYS", "konnichiwa"))
        assertTrue(body.contains("\"stream\":false"))
        assertTrue(body.contains("SYS"))
        assertTrue(body.contains("konnichiwa"))
        assertTrue(body.contains("\"model\":\"m\""))
    }

    @Test
    fun `engine strips surrounding quotes`() = runBlocking {
        settings.ollamaModel.set("m")
        val c = client { respond("""{"message":{"role":"assistant","content":"\"Hello\"\n"}}""", headers = json) }
        assertEquals("Hello", OllamaEngine(c, settings).translate("konnichiwa", "ja", "en"))
    }

    @Test
    fun `engine without model throws unavailable`() = runBlocking {
        val e = assertFailsWith<TranslationUnavailable> {
            OllamaEngine(client { respond("") }, settings).translate("x", "ja", "en")
        }
        assertEquals("Pick an Ollama model in Settings → Translation", e.message)
    }

    @Test
    fun `engine unreachable throws unavailable`() = runBlocking {
        settings.ollamaModel.set("m")
        val e = assertFailsWith<TranslationUnavailable> {
            OllamaEngine(client { throw IOException("refused") }, settings).translate("x", "ja", "en")
        }
        assertEquals("Can't reach Ollama at http://localhost:11434. Is it running?", e.message)
    }

    @Test
    fun `engine maps request timeout to unavailable`() = runBlocking {
        settings.ollamaModel.set("m")
        settings.ollamaUrl.set("http://localhost:11434/")
        val c = OllamaClient(
            HttpClient(MockEngine { throw HttpRequestTimeoutException("http://localhost:11434/api/chat", 1L) }),
            settings
        )
        val e = assertFailsWith<TranslationUnavailable> { OllamaEngine(c, settings).translate("x", "ja", "en") }
        assertEquals("Ollama took too long to answer at http://localhost:11434.", e.message)
    }

    @Test
    fun `engine maps connect timeout to unavailable`() = runBlocking {
        settings.ollamaModel.set("m")
        val c = OllamaClient(
            HttpClient(MockEngine { throw ConnectTimeoutException("connect timed out") }),
            settings
        )
        val e = assertFailsWith<TranslationUnavailable> { OllamaEngine(c, settings).translate("x", "ja", "en") }
        assertEquals("Ollama took too long to answer at http://localhost:11434.", e.message)
    }

    @Test
    fun `engine targets empty without model`() = runBlocking {
        val c = client { req ->
            if (req.url.encodedPath == "/api/version") respond("""{"version":"1"}""", headers = json)
            else respond("""{"models":[]}""", headers = json)
        }
        val engine = OllamaEngine(c, settings)
        assertEquals(EngineKind.Ollama, engine.kind)
        assertTrue(engine.targets().isEmpty())
        settings.ollamaModel.set("m")
        assertTrue(engine.targets().isNotEmpty())
    }
}
