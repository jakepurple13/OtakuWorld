package com.programmersbox.manga.shared.downloads

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal data class DownloadRequest(
    val chapterUrl: String,
    val chapterName: String,
    val mangaTitle: String,
    val imageUrls: List<String>,
    val headers: Map<String, String>,
)

internal suspend fun executeDownload(
    client: HttpClient,
    request: DownloadRequest,
    maxRetries: Int = 3,
    onProgress: suspend (imagesDownloaded: Int, totalImages: Int) -> Unit,
    writeBytes: suspend (index: Int, bytes: ByteArray) -> Unit,
) {
    val urls = request.imageUrls
    onProgress(0, urls.size)

    urls.forEachIndexed { index, url ->
        var handled = false
        var attempt = 0

        while (!handled) {
            try {
                client.prepareGet(url) {
                    headers { request.headers.forEach { (k, v) -> append(k, v) } }
                }.execute { response ->
                    when {
                        response.status.isSuccess() -> {
                            writeBytes(index, response.bodyAsBytes())
                            handled = true
                        }
                        response.status.value in 400..499 -> {
                            handled = true // image unavailable, skip without retry
                        }
                        else -> throw Exception("HTTP ${response.status.value} for $url")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempt++
                if (attempt > maxRetries) throw e
                delay((1L shl (attempt - 1)) * 1000L) // 1s, 2s, 4s
            }
        }

        onProgress(index + 1, urls.size)
    }
}

internal fun String.sanitize(): String = replace(Regex("[/\\\\:*?\"<>|]"), "_")
