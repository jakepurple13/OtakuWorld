package com.programmersbox.jsextensionloader

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JSExtensionLoaderTest {

    private class NoOpHostBridge : HostBridge {
        override suspend fun httpGet(url: String, headersJson: String): String = ""
    }

    private var loaded: JsExtension? = null

    @AfterTest
    fun tearDown() {
        loaded?.close()
    }

    @Test
    fun loadsAndValidatesAPlainJavaScriptExtension() = runTest {
        val loader = JSExtensionLoader(NoOpHostBridge())
        val extension = loader.load(
            scriptText = SampleExtensionFixture.SCRIPT_TEXT,
            fileName = "sample-extension.js",
            companionManifestJson = null,
        )
        loaded = extension
        assertEquals("Sample Extension", extension.manifest.name)
        assertEquals(1, extension.getPopular(1).size)
    }

    @Test
    fun transpilesAndLoadsATypeScriptExtension() = runTest {
        val ts = """
            // name: TS Sample
            // version: 1.0.0
            interface Item { title: string; url: string; }
            interface Request { url: string; headers: Record<string, string>; }
            function getPopularRequest(page: number): Request {
                return { url: "https://example.com/popular?page=" + page, headers: {} };
            }
            function getPopularParse(page: number, responseBody: string): Item[] {
                return [{ title: "TS Item", url: "https://example.com/1", imageUrl: null }];
            }
            function getLatestRequest(page: number): Request { return { url: "https://example.com/latest", headers: {} }; }
            function getLatestParse(page: number, responseBody: string): Item[] { return []; }
            function searchRequest(query: string, page: number): Request { return { url: "https://example.com/search", headers: {} }; }
            function searchParse(query: string, page: number, responseBody: string): Item[] { return []; }
            function getDetailRequest(url: string): Request { return { url: url, headers: {} }; }
            function getDetailParse(url: string, responseBody: string) {
                return { title: "TS Item", url: url, imageUrl: null, description: null, genres: [], chapters: [] };
            }
            function getContentRequest(url: string): Request { return { url: url, headers: {} }; }
            function getContentParse(url: string, responseBody: string) {
                return { urls: [], headers: {} };
            }
        """.trimIndent()

        val loader = JSExtensionLoader(NoOpHostBridge())
        val extension = loader.load(scriptText = ts, fileName = "ts-sample.ts", companionManifestJson = null)
        loaded = extension

        assertEquals("TS Sample", extension.manifest.name)
        assertEquals("TS Item", extension.getPopular(1).first().title)
    }

    @Test
    fun rejectsExtensionsMissingRequiredFunctions() = runTest {
        val loader = JSExtensionLoader(NoOpHostBridge())
        val exception = assertFailsWith<ExtensionValidationException> {
            loader.load(
                scriptText = "// name: Incomplete\n// version: 1.0.0\n" + SampleExtensionFixture.MISSING_FUNCTIONS_SCRIPT,
                fileName = "incomplete.js",
                companionManifestJson = null,
            )
        }
        assertEquals(
            listOf("searchRequest", "searchParse", "getDetailRequest", "getDetailParse", "getContentRequest", "getContentParse"),
            exception.missing,
        )
    }
}
