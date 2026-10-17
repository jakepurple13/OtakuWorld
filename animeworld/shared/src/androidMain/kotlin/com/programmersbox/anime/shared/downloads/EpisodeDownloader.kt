package com.programmersbox.anime.shared.downloads

import android.util.Log
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable
data class EpisodeDownloadRequest(
    val url: String,
    val headers: Map<String, String>,
    val mimeType: String?,
    val qualityHint: String?,
    val subtitles: List<KmpSubtitle>,
    val title: String,
    val folder: String,
)

private const val TAG = "EpisodeDownloader"
private const val SEGMENT_RETRIES = 3

/**
 * Downloads an episode (HLS or progressive) into [EpisodeDownloadRequest.folder], followed by its
 * subtitle sidecars. Data is written to `<name>.part` and renamed only on success; on failure or
 * cancellation every file written by this call is deleted.
 */
class EpisodeDownloader(
    private val client: OkHttpClient,
    private val retryDelayMillis: Long = 1_000,
) {

    suspend fun download(request: EpisodeDownloadRequest, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val folder = File(request.folder).apply { mkdirs() }
        val written = mutableListOf<File>()
        try {
            val video = if (isHlsHint(request)) {
                downloadHls(request, fetchText(request.url, request.headers), folder, written, onProgress)
            } else {
                val response = execute(request.url, request.headers)
                if (response.peekBody(16).string().trimStart('﻿').startsWith("#EXTM3U")) {
                    val text = response.use { it.body.string() }
                    downloadHls(request, text, folder, written, onProgress)
                } else {
                    downloadProgressive(request, response, folder, written, onProgress)
                }
            }
            downloadSubtitles(request, video, written)
            video
        } catch (t: Throwable) {
            written.forEach { it.delete() }
            throw t
        }
    }

    private fun isHlsHint(request: EpisodeDownloadRequest): Boolean {
        val mime = request.mimeType?.lowercase().orEmpty()
        val path = request.url.substringBefore('#').substringBefore('?').lowercase()
        return "mpegurl" in mime || path.endsWith(".m3u8")
    }

    private fun reserveBase(folder: File, title: String, ext: String): String =
        uniqueBaseName(sanitizeFileName(title)) { base ->
            File(folder, "$base.$ext").exists() ||
                File(folder, "$base.$ext.part").exists() ||
                File(folder, sidecarIndexName(base)).exists()
        }

    // region progressive

    private suspend fun downloadProgressive(
        request: EpisodeDownloadRequest,
        response: Response,
        folder: File,
        written: MutableList<File>,
        onProgress: (Float) -> Unit,
    ): File = response.use {
        val ext = progressiveExtension(request.url, response.header("Content-Type"))
        val base = reserveBase(folder, request.title, ext)
        val part = File(folder, "$base.$ext.part").also(written::add)
        val total = response.body.contentLength()
        part.outputStream().use { out ->
            response.body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                var read = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    read += n
                    if (total > 0) onProgress((read.toFloat() / total).coerceAtMost(1f))
                }
            }
        }
        finish(part, File(folder, "$base.$ext"), written)
    }

    private fun progressiveExtension(url: String, contentType: String?): String {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val urlExt = url.substringBefore('#').substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").lowercase()
        return when {
            type == "video/x-matroska" || type == "video/mkv" || urlExt == "mkv" -> "mkv"
            type == "video/webm" || urlExt == "webm" -> "webm"
            else -> "mp4"
        }
    }

    // endregion

    // region HLS

    private suspend fun downloadHls(
        request: EpisodeDownloadRequest,
        text: String,
        folder: File,
        written: MutableList<File>,
        onProgress: (Float) -> Unit,
    ): File {
        val media = when (val playlist = parseHlsPlaylist(text, request.url)) {
            is HlsPlaylist.Media -> playlist
            is HlsPlaylist.Master -> {
                val variant = playlist.chooseVariant(request.qualityHint)
                if (variant.audioGroup != null && variant.audioGroup in playlist.audioGroupsWithUri) {
                    throw UnsupportedHlsException("HLS streams with separate audio renditions are not supported")
                }
                parseHlsPlaylist(fetchText(variant.uri, request.headers), variant.uri) as? HlsPlaylist.Media
                    ?: throw UnsupportedHlsException("Nested HLS master playlists are not supported")
            }
        }
        if (media.live) throw UnsupportedHlsException("Live HLS streams cannot be downloaded")
        media.segments.mapNotNull { it.key }.forEach { key ->
            if (key.method != "AES-128") throw IOException("Unsupported HLS encryption: ${key.method}")
            if (key.uri == null) throw IOException("HLS AES-128 key has no URI")
        }

        val fmp4 = media.map != null || media.segments.first().uri
            .substringBefore('?').lowercase().let { it.endsWith(".m4s") || it.endsWith(".mp4") }
        val ext = if (fmp4) "mp4" else "ts"
        val base = reserveBase(folder, request.title, ext)
        val part = File(folder, "$base.$ext.part").also(written::add)
        val keys = mutableMapOf<String, ByteArray>()

        part.outputStream().buffered().use { out ->
            media.map?.let { out.write(fetchWithRetry(it.uri, request.headers)) }
            media.segments.forEachIndexed { index, segment ->
                currentCoroutineContext().ensureActive()
                writeSegment(segment, request.headers, keys, out)
                onProgress((index + 1).toFloat() / media.segments.size)
            }
        }
        return finish(part, File(folder, "$base.$ext"), written)
    }

    private suspend fun writeSegment(
        segment: HlsSegment,
        headers: Map<String, String>,
        keys: MutableMap<String, ByteArray>,
        out: OutputStream,
    ) {
        val bytes = fetchWithRetry(segment.uri, headers)
        val key = segment.key
        if (key == null) {
            out.write(bytes)
            return
        }
        val keyUri = key.uri!!
        val keyBytes = keys.getOrPut(keyUri) { fetchBytes(keyUri, headers) }
        if (keyBytes.size != 16) throw IOException("Invalid HLS AES-128 key length: ${keyBytes.size}")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(aesIv(segment)))
        out.write(cipher.doFinal(bytes))
    }

    private suspend fun fetchWithRetry(url: String, headers: Map<String, String>): ByteArray {
        var attempt = 0
        while (true) {
            try {
                return fetchBytes(url, headers)
            } catch (e: IOException) {
                if (attempt >= SEGMENT_RETRIES) throw e
                attempt++
                delay(retryDelayMillis * attempt)
            }
        }
    }

    // endregion

    // region subtitles

    private suspend fun downloadSubtitles(request: EpisodeDownloadRequest, video: File, written: MutableList<File>) {
        if (request.subtitles.isEmpty()) return
        val folder = video.parentFile!!
        val base = video.nameWithoutExtension
        val entries = mutableListOf<SidecarEntry>()
        request.subtitles.forEachIndexed { index, subtitle ->
            try {
                val bytes = fetchBytes(subtitle.url, request.headers + subtitle.headers)
                val format = subtitleFormat(subtitle.url, bytes.copyOf(minOf(bytes.size, 64)).decodeToString())
                val name = sidecarName(base, index, subtitle.lang, format)
                writeAtomically(File(folder, name), bytes, written)
                entries += SidecarEntry(file = name, lang = subtitle.lang, label = subtitle.lang)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Skipping subtitle ${subtitle.lang} (${subtitle.url})", e)
            }
        }
        if (entries.isEmpty()) return
        try {
            writeAtomically(
                File(folder, sidecarIndexName(base)),
                Json.encodeToString(entries).encodeToByteArray(),
                written,
            )
        } catch (e: IOException) {
            Log.w(TAG, "Failed to write subtitle index for $base", e)
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray, written: MutableList<File>) {
        val part = File(target.parentFile, target.name + ".part").also(written::add)
        part.writeBytes(bytes)
        finish(part, target, written)
    }

    // endregion

    private fun finish(part: File, target: File, written: MutableList<File>): File {
        written += target
        if (!part.renameTo(target)) throw IOException("Could not rename ${part.name} to ${target.name}")
        return target
    }

    private suspend fun fetchText(url: String, headers: Map<String, String>): String =
        execute(url, headers).use { it.body.string() }

    private suspend fun fetchBytes(url: String, headers: Map<String, String>): ByteArray =
        execute(url, headers).use { it.body.bytes() }

    /** Executes a GET with [headers]; throws [IOException] for non-2xx. Cancelling the coroutine cancels the call. */
    private suspend fun execute(url: String, headers: Map<String, String>): Response {
        val call = client.newCall(Request.Builder().url(url).headers(headers.toHeaders()).build())
        val response = call.await()
        if (!response.isSuccessful) {
            response.close()
            throw IOException("HTTP ${response.code} for $url")
        }
        return response
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                cont.resume(response) { _, value, _ -> value.close() }
            }

            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }
        })
    }
}
