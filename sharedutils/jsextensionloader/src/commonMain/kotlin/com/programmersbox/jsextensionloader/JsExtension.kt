package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.Extension
import com.programmersbox.extensioninterfaces.ExtensionContent
import com.programmersbox.extensioninterfaces.ExtensionDetail
import com.programmersbox.extensioninterfaces.ExtensionItem
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val jsExtensionJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

@Serializable
private data class JsRequest(val url: String, val headers: Map<String, String> = emptyMap())

/**
 * Wraps a validated [QuickJs] instance. The sandbox never receives a live
 * network capability: for each operation, a pure "request" JS function
 * describes what to fetch, [hostBridge] performs the actual fetch (a plain
 * Kotlin call — not a sandbox binding), and a pure "parse" JS function turns
 * the fetched body into the result.
 */
class JsExtension(
    override val manifest: ExtensionManifest,
    private val quickJs: QuickJs,
    private val hostBridge: HostBridge,
    private val dispatcher: CoroutineDispatcher,
) : Extension {

    override suspend fun getPopular(page: Int): List<ExtensionItem> =
        fetchAndParse("getPopularRequest($page)") { body -> "getPopularParse($page, $body)" }

    override suspend fun getLatest(page: Int): List<ExtensionItem> =
        fetchAndParse("getLatestRequest($page)") { body -> "getLatestParse($page, $body)" }

    override suspend fun search(query: String, page: Int): List<ExtensionItem> {
        val q = jsExtensionJson.encodeToString(query)
        return fetchAndParse("searchRequest($q, $page)") { body -> "searchParse($q, $page, $body)" }
    }

    override suspend fun getDetail(url: String): ExtensionDetail {
        val u = jsExtensionJson.encodeToString(url)
        return fetchAndParse("getDetailRequest($u)") { body -> "getDetailParse($u, $body)" }
    }

    override suspend fun getContent(url: String): ExtensionContent {
        val u = jsExtensionJson.encodeToString(url)
        return fetchAndParse("getContentRequest($u)") { body -> "getContentParse($u, $body)" }
    }

    private suspend inline fun <reified T> fetchAndParse(
        requestCall: String,
        crossinline parseCall: (bodyJsonLiteral: String) -> String,
    ): T {
        val requestJson = withContext(dispatcher) {
            quickJs.evaluate("JSON.stringify($requestCall)", "extension-request.js") as String
        }
        val request = jsExtensionJson.decodeFromString<JsRequest>(requestJson)
        val headersJson = jsExtensionJson.encodeToString(request.headers)
        val responseBody = withContext(Dispatchers.IO) { hostBridge.httpGet(request.url, headersJson) }
        val bodyLiteral = jsExtensionJson.encodeToString(responseBody)
        val resultJson = withContext(dispatcher) {
            quickJs.evaluate("JSON.stringify(${parseCall(bodyLiteral)})", "extension-parse.js") as String
        }
        return jsExtensionJson.decodeFromString<T>(resultJson)
    }

    fun close() {
        quickJs.close()
        closeQuickJsDispatcher(dispatcher)
    }
}
