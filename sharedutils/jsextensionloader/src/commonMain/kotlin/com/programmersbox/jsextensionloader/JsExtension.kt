package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.Extension
import com.programmersbox.extensioninterfaces.ExtensionContent
import com.programmersbox.extensioninterfaces.ExtensionDetail
import com.programmersbox.extensioninterfaces.ExtensionItem
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

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
    private val deadline: JsEvalDeadline = JsEvalDeadline(Duration.INFINITE),
) : Extension {

    @Volatile
    private var closeRequested = false

    /** Set on [dispatcher] when the context is freed; evaluations check it there. */
    @Volatile
    private var closed = false

    private val closedSignal = CompletableDeferred<Unit>()

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
        val requestJson = evaluate("JSON.stringify($requestCall)", "extension-request.js")
        val request = jsExtensionJson.decodeFromString<JsRequest>(requestJson)
        val headersJson = jsExtensionJson.encodeToString(request.headers)
        val responseBody = withContext(Dispatchers.IO) { hostBridge.httpGet(request.url, headersJson) }
        val bodyLiteral = jsExtensionJson.encodeToString(responseBody)
        val resultJson = evaluate("JSON.stringify(${parseCall(bodyLiteral)})", "extension-parse.js")
        return jsExtensionJson.decodeFromString<T>(resultJson)
    }

    private suspend fun evaluate(script: String, fileName: String): String = withContext(dispatcher) {
        check(!closed) { "${manifest.id} was unloaded" }
        deadline.guard { quickJs.evaluate(script, fileName) } as String
    }

    /**
     * Frees the QuickJs context on its own thread, after any evaluation already running there, then stops
     * that thread. Closing from the caller's thread could free the context while it is in use.
     */
    fun close() {
        if (closeRequested) return
        closeRequested = true
        dispatcher.dispatch(EmptyCoroutineContext, Runnable {
            try {
                closed = true
                quickJs.close()
                closeQuickJsDispatcher(dispatcher)
            } finally {
                closedSignal.complete(Unit)
            }
        })
    }

    /** Suspends until a [close] call has released the context. */
    suspend fun awaitClosed() = closedSignal.await()
}
