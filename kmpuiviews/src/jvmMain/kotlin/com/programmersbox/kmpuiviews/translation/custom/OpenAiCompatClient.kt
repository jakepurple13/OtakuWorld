package com.programmersbox.kmpuiviews.translation.custom

import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException

private const val CONNECT_TIMEOUT_MS = 5_000L
private const val MODELS_TIMEOUT_MS = 5_000L
private const val CHAT_TIMEOUT_MS = 5 * 60_000L

/** HttpClient config for [OpenAiCompatClient]: short connect timeout; per-call timeouts are set on each request. */
fun HttpClientConfig<*>.installOpenAiCompatTimeouts() {
    install(HttpTimeout) { connectTimeoutMillis = CONNECT_TIMEOUT_MS }
}

/** Client for any OpenAI-compatible server (LM Studio, llama.cpp, vLLM, ...). Failures are [TranslationUnavailable]. */
class OpenAiCompatClient(
    private val http: HttpClient,
    private val settings: DesktopTranslationSettings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun baseUrl() = settings.customServerUrl.get().trim().trimEnd('/')

    /** Number of models the server lists. */
    suspend fun check(): Result<Int> {
        val base = baseUrl()
        return try {
            val response = http.get("$base/models") {
                applyKey()
                timeout { requestTimeoutMillis = MODELS_TIMEOUT_MS }
            }
            val text = ensureSuccess(response, base)
            Result.success(json.parseToJsonElement(text).jsonObject["data"]?.jsonArray?.size ?: 0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TranslationUnavailable) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(map(e, base))
        }
    }

    suspend fun chat(system: String, user: String): String {
        val base = baseUrl()
        val model = settings.customServerModel.get()
        return try {
            val response = http.post("$base/chat/completions") {
                applyKey()
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
            val text = ensureSuccess(response, base)
            json.parseToJsonElement(text).jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                ?: throw TranslationUnavailable("The server at $base returned no message.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: TranslationUnavailable) {
            throw e
        } catch (e: Exception) {
            throw map(e, base)
        }
    }

    private suspend fun HttpRequestBuilder.applyKey() {
        val key = settings.customServerKey.get().trim()
        if (key.isNotEmpty()) header(HttpHeaders.Authorization, "Bearer $key")
    }

    private suspend fun ensureSuccess(response: HttpResponse, base: String): String {
        if (!response.status.isSuccess()) {
            throw TranslationUnavailable("The server at $base answered HTTP ${response.status.value}.")
        }
        return response.bodyAsText()
    }

    private fun map(e: Exception, base: String): TranslationUnavailable = when (e) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            TranslationUnavailable("The server took too long to answer at $base.")
        is IOException -> TranslationUnavailable("Can't reach the server at $base. Is it running?")
        else -> TranslationUnavailable(e.message ?: "The server at $base sent a reply that couldn't be read.")
    }
}
