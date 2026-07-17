package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val hostBridgeJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The ONLY bridge exposed into the QuickJs sandbox. Extension code can reach
 * the network exclusively through [httpGet] — there is no ambient fetch/fs.
 */
interface HostBridge {
    fun httpGet(url: String, headersJson: String): String
}

class KtorHostBridge(private val client: HttpClient) : HostBridge {
    override fun httpGet(url: String, headersJson: String): String = runBlocking {
        val headers: Map<String, String> = hostBridgeJson.decodeFromString(headersJson)
        client.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }.bodyAsText()
    }
}
