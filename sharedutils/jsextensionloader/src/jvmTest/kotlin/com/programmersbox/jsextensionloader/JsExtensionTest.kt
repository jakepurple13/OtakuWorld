package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsExtensionTest {

    private val manifest = ExtensionManifest(
        id = "sample-extension",
        name = "Sample Extension",
        version = "1.0.0",
        author = "OtakuWorld",
        description = null,
        iconUrl = null,
        updateUrl = null,
    )

    private class StubHostBridge(private val response: String = "") : HostBridge {
        var lastUrl: String? = null
        var lastHeadersJson: String? = null
        override fun httpGet(url: String, headersJson: String): String {
            lastUrl = url
            lastHeadersJson = headersJson
            return response
        }
    }

    private var quickJs: QuickJs? = null

    @AfterTest
    fun tearDown() {
        quickJs?.close()
    }

    private fun loadSampleExtension(hostBridge: HostBridge = StubHostBridge()): JsExtension {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "sample-extension.js")
        return JsExtension(manifest, js, hostBridge)
    }

    @Test
    fun getPopularReturnsParsedItems() = runTest {
        val extension = loadSampleExtension()
        val items = extension.getPopular(page = 1)
        assertEquals(1, items.size)
        assertEquals("Sample Item", items.first().title)
    }

    @Test
    fun getDetailReturnsParsedDetail() = runTest {
        val extension = loadSampleExtension()
        val detail = extension.getDetail("https://example.com/item/1")
        assertEquals("Sample Item", detail.title)
        assertEquals(1, detail.chapters.size)
        assertEquals("Chapter 1", detail.chapters.first().name)
    }

    @Test
    fun getContentReturnsParsedContent() = runTest {
        val extension = loadSampleExtension()
        val content = extension.getContent("https://example.com/item/1")
        assertEquals(listOf("https://example.com/content/1.png"), content.urls)
    }

    @Test
    fun searchIncludesQueryInResult() = runTest {
        val extension = loadSampleExtension()
        val items = extension.search("dragon", page = 1)
        assertEquals("Search Result for dragon", items.first().title)
    }

    @Test
    fun hostBridgeReceivesTheUrlFromTheRequestPhase() = runTest {
        val hostBridge = StubHostBridge()
        val extension = loadSampleExtension(hostBridge)
        extension.getPopular(page = 3)
        assertEquals("https://example.com/popular?page=3", hostBridge.lastUrl)
    }

    @Test
    fun parsePhaseReceivesTheHostFetchedResponseBody() = runTest {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(
            """
            function getPopularRequest(page) { return { url: "https://example.com/x", headers: {} }; }
            function getPopularParse(page, responseBody) {
                return [{ title: "echo:" + responseBody, url: "https://example.com/1", imageUrl: null }];
            }
            function getLatestRequest(page) { return { url: "https://example.com/x", headers: {} }; }
            function getLatestParse(page, responseBody) { return []; }
            function searchRequest(query, page) { return { url: "https://example.com/x", headers: {} }; }
            function searchParse(query, page, responseBody) { return []; }
            function getDetailRequest(url) { return { url: url, headers: {} }; }
            function getDetailParse(url, responseBody) {
                return { title: "t", url: url, imageUrl: null, description: null, genres: [], chapters: [] };
            }
            function getContentRequest(url) { return { url: url, headers: {} }; }
            function getContentParse(url, responseBody) { return { urls: [], headers: {} }; }
            """.trimIndent(),
            "echo-extension.js",
        )
        val extension = JsExtension(manifest, js, StubHostBridge(response = "fetched-body"))

        val items = extension.getPopular(page = 1)

        assertEquals("echo:fetched-body", items.first().title)
    }

    @Test
    fun validatorReportsNoMissingFunctionsForSampleExtension() {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "sample-extension.js")
        assertEquals(emptyList(), ExtensionValidator.validate(js))
    }

    @Test
    fun validatorReportsMissingFunctions() {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.MISSING_FUNCTIONS_SCRIPT, "incomplete-extension.js")
        val missing = ExtensionValidator.validate(js)
        assertEquals(
            listOf("searchRequest", "searchParse", "getDetailRequest", "getDetailParse", "getContentRequest", "getContentParse"),
            missing,
        )
    }
}
