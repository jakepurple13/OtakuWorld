package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.Extension
import com.programmersbox.extensioninterfaces.ExtensionContent
import com.programmersbox.extensioninterfaces.ExtensionDetail
import com.programmersbox.extensioninterfaces.ExtensionItem
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val jsExtensionJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

class JsExtension(
    override val manifest: ExtensionManifest,
    private val quickJs: QuickJs,
) : Extension {

    override suspend fun getPopular(page: Int): List<ExtensionItem> =
        call("getPopular($page)")

    override suspend fun getLatest(page: Int): List<ExtensionItem> =
        call("getLatest($page)")

    override suspend fun search(query: String, page: Int): List<ExtensionItem> =
        call("search(${jsExtensionJson.encodeToString(String.serializer(), query)}, $page)")

    override suspend fun getDetail(url: String): ExtensionDetail =
        call("getDetail(${jsExtensionJson.encodeToString(String.serializer(), url)})")

    override suspend fun getContent(url: String): ExtensionContent =
        call("getContent(${jsExtensionJson.encodeToString(String.serializer(), url)})")

    private suspend inline fun <reified T> call(callExpression: String): T = withContext(Dispatchers.Default) {
        val resultJson = quickJs.evaluate("JSON.stringify($callExpression)", "extension-call.js") as String
        jsExtensionJson.decodeFromString(serializer(), resultJson)
    }

    fun close() {
        quickJs.close()
    }
}
