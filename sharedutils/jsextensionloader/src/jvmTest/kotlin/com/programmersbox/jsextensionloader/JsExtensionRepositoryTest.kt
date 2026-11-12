package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsExtensionRepositoryTest {

    private class StubHostBridge : HostBridge {
        override suspend fun httpGet(url: String, headersJson: String): String = ""
    }

    private val quickJsInstances = mutableListOf<QuickJs>()
    private val extensions = mutableListOf<JsExtension>()

    // close() frees the context on the extension's own thread, so wait for it instead of closing here.
    @AfterTest
    fun tearDown() = runBlocking {
        extensions.forEach { it.close(); it.awaitClosed() }
        extensions.clear()
        quickJsInstances.clear()
    }

    private fun extensionWithId(id: String): JsExtension {
        val quickJs = QuickJs.create()
        quickJsInstances.add(quickJs)
        quickJs.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "$id.js")
        val manifest = ExtensionManifest(
            id = id, name = id, version = "1.0.0", author = null,
            description = null, iconUrl = null, updateUrl = null,
        )
        return JsExtension(manifest, quickJs, StubHostBridge(), singleThreadQuickJsDispatcher(id)).also { extensions += it }
    }

    @Test
    fun registerAddsExtension() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        assertEquals(1, repository.extensions.value.size)
        assertEquals("one", repository.extensions.value.first().manifest.id)
    }

    @Test
    fun registeringSameIdReplacesThePrevious() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        repository.register(extensionWithId("one"))
        assertEquals(1, repository.extensions.value.size)
    }

    @Test
    fun registeringSameIdClosesThePreviousInstance() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        val firstQuickJs = quickJsInstances.first()

        repository.register(extensionWithId("one"))
        runBlocking { extensions.first().awaitClosed() }

        assertTrue(
            runCatching { firstQuickJs.evaluate("1", "closed-check.js") }.isFailure,
            "expected the replaced QuickJs instance to be closed",
        )
    }

    @Test
    fun unloadRemovesExtensionAndClosesIt() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        repository.unload("one")
        assertTrue(repository.extensions.value.isEmpty())
    }

    @Test
    fun unloadingUnknownIdIsANoOp() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        repository.unload("does-not-exist")
        assertEquals(1, repository.extensions.value.size)
    }
}
