package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

private val hostBridgeJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The ONLY way extension code reaches the network. [httpGet] is called by the
 * host (a plain Kotlin method call — never bound into the QuickJs sandbox)
 * between an extension's pure "request" and "parse" JS function calls. The
 * sandbox itself has no ambient fetch/fs and never calls back into Kotlin
 * mid-execution.
 */
interface HostBridge {
    suspend fun httpGet(url: String, headersJson: String): String
}

class KtorHostBridge(private val client: HttpClient) : HostBridge {
    override suspend fun httpGet(url: String, headersJson: String): String {
        val headers: Map<String, String> = hostBridgeJson.decodeFromString(headersJson)
        return client.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }.bodyAsText()
    }
}
