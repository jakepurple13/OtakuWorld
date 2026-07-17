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

    private var quickJs: QuickJs? = null

    @AfterTest
    fun tearDown() {
        quickJs?.close()
    }

    private fun loadSampleExtension(): JsExtension {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "sample-extension.js")
        return JsExtension(manifest, js)
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
        assertEquals(listOf("search", "getDetail", "getContent"), missing)
    }
}
