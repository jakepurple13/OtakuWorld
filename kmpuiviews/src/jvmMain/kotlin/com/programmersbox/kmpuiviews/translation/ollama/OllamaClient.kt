package com.programmersbox.kmpuiviews.translation.ollama

import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val STATUS_TIMEOUT_MS = 5_000L
private const val CHAT_TIMEOUT_MS = 5 * 60_000L
private const val CONNECT_TIMEOUT_MS = 5_000L

/** Ollama HttpClient config: short connect timeout; per-call timeouts are set on each request. */
fun HttpClientConfig<*>.installOllamaTimeouts() {
    install(HttpTimeout) { connectTimeoutMillis = CONNECT_TIMEOUT_MS }
}

data class OllamaModel(val name: String, val sizeBytes: Long)

sealed interface OllamaStatus {
    data class Connected(val version: String, val models: List<OllamaModel>) : OllamaStatus
    data class Unreachable(val url: String) : OllamaStatus
}

class OllamaClient(
    private val http: HttpClient,
    private val settings: DesktopTranslationSettings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun baseUrl() = settings.ollamaUrl.get().trim().trimEnd('/')

    suspend fun status(): OllamaStatus {
        val base = baseUrl()
        return try {
            val version = http.get("$base/api/version") { timeout { requestTimeoutMillis = STATUS_TIMEOUT_MS } }
            check(version.status.isSuccess()) { "HTTP ${version.status.value}" }
            val versionText = json.parseToJsonElement(version.bodyAsText()).jsonObject["version"]
                ?.jsonPrimitive?.contentOrNull.orEmpty()
            val tags = http.get("$base/api/tags") { timeout { requestTimeoutMillis = STATUS_TIMEOUT_MS } }
            check(tags.status.isSuccess()) { "HTTP ${tags.status.value}" }
            val models = json.parseToJsonElement(tags.bodyAsText()).jsonObject["models"]
                ?.jsonArray.orEmpty()
                .map {
                    val o = it.jsonObject
                    OllamaModel(
                        name = o["name"]!!.jsonPrimitive.content,
                        sizeBytes = o["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                    )
                }
            OllamaStatus.Connected(versionText, models)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            OllamaStatus.Unreachable(base)
        }
    }

    /** Emits completed/total (null when unknown) and a final 1f. Throws on failure. */
    fun pull(name: String): Flow<Float?> = flow {
        val base = baseUrl()
        http.prepareRequest {
            method = HttpMethod.Post
            url("$base/api/pull")
            timeout {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("model", name); put("stream", true) }.toString())
        }.execute { response ->
            if (!response.status.isSuccess()) error(errorMessage(response.bodyAsText(), response.status.value))
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank()) continue
                val obj = json.parseToJsonElement(line).jsonObject
                obj["error"]?.jsonPrimitive?.contentOrNull?.let { error(it) }
                val total = obj["total"]?.jsonPrimitive?.doubleOrNull
                val completed = obj["completed"]?.jsonPrimitive?.doubleOrNull
                emit(if (total != null && completed != null && total > 0) (completed / total).toFloat() else null)
            }
        }
        emit(1f)
    }

    suspend fun delete(name: String) {
        val base = baseUrl()
        val response = http.delete("$base/api/delete") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("model", name) }.toString())
        }
        if (!response.status.isSuccess()) error(errorMessage(response.bodyAsText(), response.status.value))
    }

    suspend fun chat(model: String, system: String, user: String): String {
        val base = baseUrl()
        val response = http.post("$base/api/chat") {
            timeout {
                requestTimeoutMillis = CHAT_TIMEOUT_MS
                socketTimeoutMillis = CHAT_TIMEOUT_MS
            }
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("model", model)
                    put("stream", false)
                    put("messages", buildJsonArray {
                        add(buildJsonObject { put("role", "system"); put("content", system) })
                        add(buildJsonObject { put("role", "user"); put("content", user) })
                    })
                }.toString()
            )
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error(errorMessage(text, response.status.value))
        return json.parseToJsonElement(text).jsonObject["message"]?.jsonObject
            ?.get("content")?.jsonPrimitive?.contentOrNull
            ?: error("Ollama returned no message")
    }

    private fun errorMessage(body: String, code: Int): String =
        runCatching { ((json.parseToJsonElement(body) as? JsonObject)?.get("error") as? JsonPrimitive)?.content }
            .getOrNull() ?: "Ollama request failed (HTTP $code)"
}
