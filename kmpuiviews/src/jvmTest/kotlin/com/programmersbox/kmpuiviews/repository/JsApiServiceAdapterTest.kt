package com.programmersbox.kmpuiviews.repository

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import com.programmersbox.jsextensionloader.HostBridge
import com.programmersbox.jsextensionloader.JsExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsApiServiceAdapterTest {

    private class StubHostBridge(private val response: String = "") : HostBridge {
        override suspend fun httpGet(url: String, headersJson: String): String = response
    }

    private val manifest = ExtensionManifest(
        id = "adapter-test",
        name = "Adapter Test Extension",
        version = "1.0.0",
        author = null,
        description = null,
        iconUrl = null,
        updateUrl = null,
    )

    private var quickJs: QuickJs? = null

    @AfterTest
    fun tearDown() {
        quickJs?.close()
    }

    private fun buildAdapter(): JsApiServiceAdapter {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(FIXTURE_SCRIPT, "adapter-fixture.js")
        return JsApiServiceAdapter(JsExtension(manifest, js, StubHostBridge(), Dispatchers.Default))
    }

    @Test
    fun recentMapsGetLatestIntoKmpItemModel() = runTest {
        val adapter = buildAdapter()
        val result = adapter.recent(page = 1)
        assertEquals(1, result.size)
        assertEquals("Latest Item", result.first().title)
        assertEquals("", result.first().description)
        assertEquals(adapter, result.first().source)
    }

    @Test
    fun allListMapsGetPopularIntoKmpItemModel() = runTest {
        val adapter = buildAdapter()
        val result = adapter.allList(page = 1)
        assertEquals("Popular Item", result.first().title)
    }

    @Test
    fun searchCallsExtensionSearchDirectly() = runTest {
        val adapter = buildAdapter()
        val result = adapter.search("dragon", page = 1, list = emptyList())
        assertEquals("Search Result for dragon", result.first().title)
    }

    @Test
    fun itemInfoMapsGetDetailIntoKmpInfoModelWithChapters() = runTest {
        val adapter = buildAdapter()
        val item = adapter.recent(page = 1).first()
        val info = adapter.itemInfo(item)
        assertEquals("Detail Title", info.title)
        assertEquals(1, info.chapters.size)
        assertEquals("Chapter 1", info.chapters.first().name)
        assertEquals(item.url, info.chapters.first().sourceUrl)
        assertEquals(adapter, info.chapters.first().source)
    }

    @Test
    fun chapterInfoMapsGetContentIntoKmpStorageWithHeaders() = runTest {
        val adapter = buildAdapter()
        val chapter = adapter.itemInfo(adapter.recent(page = 1).first()).chapters.first()
        val storages = adapter.chapterInfo(chapter)
        assertEquals(1, storages.size)
        assertEquals("https://example.com/content/1.png", storages.first().link)
        assertEquals("1.png", storages.first().filename)
        assertEquals("bar", storages.first().headers["foo"])
    }

    @Test
    fun sourceByUrlMapsGetDetailIntoKmpItemModel() = runTest {
        val adapter = buildAdapter()
        val result = adapter.sourceByUrl("https://example.com/detail")
        assertEquals("Detail Title", result.title)
        assertEquals("https://example.com/detail", result.url)
    }

    @Test
    fun baseUrlIsSyntheticAndDerivedFromManifestId() {
        val adapter = buildAdapter()
        assertEquals("https://adapter-test.jsextension/", adapter.baseUrl)
    }

    @Test
    fun serviceNameUsesManifestName() {
        val adapter = buildAdapter()
        assertEquals("Adapter Test Extension", adapter.serviceName)
    }

    companion object {
        private const val FIXTURE_SCRIPT = """
            function getPopularRequest(page) { return { url: "https://example.com/popular", headers: {} }; }
            function getPopularParse(page, responseBody) {
                return [{ title: "Popular Item", url: "https://example.com/popular/1", imageUrl: null }];
            }
            function getLatestRequest(page) { return { url: "https://example.com/latest", headers: {} }; }
            function getLatestParse(page, responseBody) {
                return [{ title: "Latest Item", url: "https://example.com/latest/1", imageUrl: null }];
            }
            function searchRequest(query, page) { return { url: "https://example.com/search", headers: {} }; }
            function searchParse(query, page, responseBody) {
                return [{ title: "Search Result for " + query, url: "https://example.com/search/1", imageUrl: null }];
            }
            function getDetailRequest(url) { return { url: url, headers: {} }; }
            function getDetailParse(url, responseBody) {
                return {
                    title: "Detail Title",
                    url: url,
                    imageUrl: null,
                    description: null,
                    genres: [],
                    chapters: [ { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null } ]
                };
            }
            function getContentRequest(url) { return { url: url, headers: {} }; }
            function getContentParse(url, responseBody) {
                return { urls: ["https://example.com/content/1.png"], headers: { foo: "bar" } };
            }
        """
    }
}
