package com.programmersbox.kmpuiviews.repository

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import com.programmersbox.jsextensionloader.HostBridge
import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsExtensionSourceBridgeTest {

    private class StubHostBridge : HostBridge {
        override fun httpGet(url: String, headersJson: String): String = ""
    }

    private val createdQuickJs = mutableListOf<QuickJs>()

    @AfterTest
    fun tearDown() {
        createdQuickJs.forEach { it.close() }
    }

    private fun extensionWithId(id: String, version: String = "1.0.0"): JsExtension {
        val manifest = ExtensionManifest(
            id = id, name = "Extension $id", version = version, author = null,
            description = null, iconUrl = null, updateUrl = null,
        )
        val js = QuickJs.create()
        createdQuickJs.add(js)
        js.evaluate(BRIDGE_FIXTURE_SCRIPT, "$id.js")
        return JsExtension(manifest, js, StubHostBridge())
    }

    @Test
    fun mirrorsNewlyRegisteredExtensionIntoSourceRepository() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        advanceUntilIdle()

        assertEquals(1, sourceRepository.list.size)
        assertEquals("js.a", sourceRepository.list.first().packageName)
        assertEquals("Extension a", sourceRepository.list.first().apiService.serviceName)
    }

    @Test
    fun removesFromSourceRepositoryWhenUnloaded() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        advanceUntilIdle()
        jsExtensionRepository.unload("a")
        advanceUntilIdle()

        assertTrue(sourceRepository.list.isEmpty())
    }

    @Test
    fun swapsSourceRepositoryEntryWhenSameIdExtensionIsReplaced() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a", version = "1.0.0"))
        advanceUntilIdle()
        val firstInfo = sourceRepository.list.first()

        jsExtensionRepository.register(extensionWithId("a", version = "2.0.0"))
        advanceUntilIdle()

        assertEquals(1, sourceRepository.list.size)
        val secondInfo = sourceRepository.list.first()
        assertTrue(firstInfo !== secondInfo)
        assertEquals("js.a", secondInfo.packageName)
    }

    @Test
    fun mirrorsMultipleIndependentExtensions() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        jsExtensionRepository.register(extensionWithId("b"))
        advanceUntilIdle()

        assertEquals(setOf("js.a", "js.b"), sourceRepository.list.map { it.packageName }.toSet())
    }

    @Test
    fun reAddsMirroredEntryIfSomethingElseReplacesSourceRepositoryContents() = runTest {
        // The legacy JAR/APK loader (sharedutils/kmpextensionloader's ExtensionLoader) calls
        // SourceRepository.setSources(...) - a full replace, not a merge - every time its
        // extension-directory watcher fires. That happens independently of anything the JS
        // extension system does, and it silently wipes whatever the bridge already mirrored.
        // Since the legacy loader can't be modified, the bridge must detect and restore its
        // own mirrored entries whenever SourceRepository changes out from under it.
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        advanceUntilIdle()
        assertEquals(1, sourceRepository.list.size)

        sourceRepository.setSources(emptyList())
        advanceUntilIdle()

        assertEquals(1, sourceRepository.list.size)
        assertEquals("js.a", sourceRepository.list.first().packageName)
    }

    companion object {
        private const val BRIDGE_FIXTURE_SCRIPT = """
            function getPopularRequest(page) { return { url: "https://example.com/popular", headers: {} }; }
            function getPopularParse(page, responseBody) { return []; }
            function getLatestRequest(page) { return { url: "https://example.com/latest", headers: {} }; }
            function getLatestParse(page, responseBody) { return []; }
            function searchRequest(query, page) { return { url: "https://example.com/search", headers: {} }; }
            function searchParse(query, page, responseBody) { return []; }
            function getDetailRequest(url) { return { url: url, headers: {} }; }
            function getDetailParse(url, responseBody) {
                return { title: "t", url: url, imageUrl: null, description: null, genres: [], chapters: [] };
            }
            function getContentRequest(url) { return { url: url, headers: {} }; }
            function getContentParse(url, responseBody) { return { urls: [], headers: {} }; }
        """
    }
}
