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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

/** [totalBytes] is null when the size is unknown (HLS); [bytes] is bytes written so far. */
data class DownloadProgress(val fraction: Float, val bytes: Long, val totalBytes: Long?)

private const val TAG = "EpisodeDownloader"
private const val SEGMENT_RETRIES = 3
private const val BUFFER_SIZE = 64 * 1024

/**
 * Downloads an episode (HLS or progressive) into [EpisodeDownloadRequest.folder], followed by its
 * subtitle sidecars. Data is written to `<name>.part` and renamed only on success; on failure or
 * cancellation every file written by this call is deleted.
 */
class EpisodeDownloader(
    private val client: OkHttpClient,
    private val retryDelayMillis: Long = 1_000,
) {

    suspend fun download(request: EpisodeDownloadRequest, onProgress: (DownloadProgress) -> Unit): File = withContext(Dispatchers.IO) {
        val folder = File(request.folder).apply { mkdirs() }
        val written = mutableListOf<File>()
        try {
            val video = if (isHlsHint(request)) {
                downloadHls(request, fetchText(request.url, request.headers), folder, written, onProgress)
            } else {
                val response = execute(request.url, request.headers)
                val isPlaylist = try {
                    response.peekBody(16).string().trimStart('﻿').startsWith("#EXTM3U")
                } catch (t: Throwable) {
                    response.close()
                    throw t
                }
                if (isPlaylist) {
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

    private fun reserveBase(folder: File, title: String): String =
        uniqueBaseName(sanitizeFileName(title)) { base -> isBaseTaken(base) { File(folder, it).exists() } }

    // region progressive

    private suspend fun downloadProgressive(
        request: EpisodeDownloadRequest,
        response: Response,
        folder: File,
        written: MutableList<File>,
        onProgress: (DownloadProgress) -> Unit,
    ): File = response.use {
        val ext = progressiveExtension(request.url, response.header("Content-Type"))
        val base = reserveBase(folder, request.title)
        val part = File(folder, "$base.$ext.part").also(written::add)
        val total = response.body.contentLength()
        part.outputStream().use { out ->
            response.body.byteStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    read += n
                    if (total > 0) onProgress(DownloadProgress((read.toFloat() / total).coerceAtMost(1f), read, total))
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
        onProgress: (DownloadProgress) -> Unit,
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
        (media.segments.mapNotNull { it.key } + listOfNotNull(media.map?.key)).forEach { key ->
            if (key.method != "AES-128") throw IOException("Unsupported HLS encryption: ${key.method}")
            if (key.uri == null) throw IOException("HLS AES-128 key has no URI")
        }
        // RFC 8216 §4.3.2.5: an encrypted init section must carry an explicit IV (no sequence number to derive one).
        if (media.map?.key != null && media.map.key.iv == null) {
            throw UnsupportedHlsException("Encrypted EXT-X-MAP without an IV is not supported")
        }

        val fmp4 = media.map != null || media.segments.first().uri
            .substringBefore('?').lowercase().let { it.endsWith(".m4s") || it.endsWith(".mp4") }
        val ext = if (fmp4) "mp4" else "ts"
        val base = reserveBase(folder, request.title)
        val part = File(folder, "$base.$ext.part").also(written::add)
        val keys = mutableMapOf<String, ByteArray>()

        FileOutputStream(part).use { file ->
            val out = BufferedOutputStream(file, BUFFER_SIZE)
            media.map?.let { map ->
                val decrypt = map.key?.let { key -> keyBytes(key, request.headers, keys) to key.iv!! }
                streamWithRetry(map.uri, request.headers, decrypt, out, file)
            }
            media.segments.forEachIndexed { index, segment ->
                currentCoroutineContext().ensureActive()
                val decrypt = segment.key?.let { key -> keyBytes(key, request.headers, keys) to aesIv(segment) }
                streamWithRetry(segment.uri, request.headers, decrypt, out, file)
                out.flush()
                onProgress(DownloadProgress((index + 1).toFloat() / media.segments.size, file.channel.position(), null))
            }
            out.flush()
        }
        return finish(part, File(folder, "$base.$ext"), written)
    }

    /** Fetches the AES-128 key once per key URI. */
    private suspend fun keyBytes(key: HlsKey, headers: Map<String, String>, keys: MutableMap<String, ByteArray>): ByteArray {
        val uri = key.uri!!
        return keys.getOrPut(uri) {
            fetchBytes(uri, headers).also {
                if (it.size != 16) throw IOException("Invalid HLS AES-128 key length: ${it.size}")
            }
        }
    }

    /**
     * Streams [url] into [out] (decrypting with `key to iv` when [decrypt] is set). A failed attempt
     * is truncated away and retried up to [SEGMENT_RETRIES] times.
     */
    private suspend fun streamWithRetry(
        url: String,
        headers: Map<String, String>,
        decrypt: Pair<ByteArray, ByteArray>?,
        out: BufferedOutputStream,
        file: FileOutputStream,
    ) {
        out.flush()
        val start = file.channel.position()
        var attempt = 0
        while (true) {
            try {
                streamOnce(url, headers, decrypt, out)
                return
            } catch (e: IOException) {
                if (attempt >= SEGMENT_RETRIES) throw e
                attempt++
                out.flush()
                file.channel.truncate(start)
                file.channel.position(start)
                delay(retryDelayMillis * attempt)
            }
        }
    }

    private suspend fun streamOnce(
        url: String,
        headers: Map<String, String>,
        decrypt: Pair<ByteArray, ByteArray>?,
        out: BufferedOutputStream,
    ) {
        val cipher = decrypt?.let { (key, iv) ->
            Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            }
        }
        execute(url, headers).use { response ->
            response.body.byteStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (cipher == null) out.write(buffer, 0, n) else cipher.update(buffer, 0, n)?.let(out::write)
                }
                cipher?.doFinal()?.let(out::write)
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

    internal fun writeAtomically(target: File, bytes: ByteArray, written: MutableList<File>) {
        val part = File(target.parentFile, target.name + ".part").also(written::add)
        try {
            part.writeBytes(bytes)
            finish(part, target, written)
        } catch (t: Throwable) {
            part.delete()
            throw t
        }
    }

    // endregion

    /** Renames [part] to [target]; [target] is tracked for cleanup only once this call created it. */
    private fun finish(part: File, target: File, written: MutableList<File>): File {
        if (!part.renameTo(target)) throw IOException("Could not rename ${part.name} to ${target.name}")
        written += target
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
