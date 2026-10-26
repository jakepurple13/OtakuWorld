package com.programmersbox.anime.shared.downloads

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.cast.toWebVtt
import com.programmersbox.anime.shared.videoplayer.playerHeaders
import com.programmersbox.anime.shared.videoplayer.upstreamRequest
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
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
import com.programmersbox.anime.shared.cast.SubtitleFormat as VttSource

/** Starts episode downloads; [AnimeDownloadManager] in the app. */
fun interface EpisodeDownloadQueue {
    /** Queues [storage] for download into the [animeTitle] folder and returns its queue key. */
    fun download(storage: KmpStorage, referer: String?, animeTitle: String, episodeName: String): String
}

/**
 * Downloads extension episodes (progressive or HLS, plus subtitle sidecars) into
 * `downloadsDirectory/<animeTitle>/`, one at a time in the order they were queued.
 *
 * [queue] lists running and waiting downloads and failed ones (kept for [retry] until [remove]d).
 * Every request carries [playerHeaders] of the stream's headers and the referer.
 */
class AnimeDownloadManager(
    private val scope: CoroutineScope,
    private val animeDesktopSettings: AnimeDesktopSettings,
    private val trayState: TrayState?,
    client: OkHttpClient = OkHttpClient(),
) : EpisodeDownloadQueue {

    private class Entry(
        val key: String,
        val storage: KmpStorage,
        val referer: String?,
        val animeTitle: String,
        val episodeName: String,
    ) {
        val title = "$animeTitle - $episodeName"
        var view = QueuedDownload(key, title, QueuedDownloadState.Waiting)
        var job: Job? = null
    }

    private val fetcher = EpisodeFetcher(client)

    /** One download at a time; the mutex is fair, so waiters get it in the order they asked. */
    private val gate = Mutex()
    private val lock = Any()
    private val entries = LinkedHashMap<String, Entry>()
    private val _queue = MutableStateFlow<List<QueuedDownload>>(emptyList())
    val queue: StateFlow<List<QueuedDownload>> = _queue.asStateFlow()

    /** A live download of the same episode is kept as is; a failed one is replaced. */
    override fun download(storage: KmpStorage, referer: String?, animeTitle: String, episodeName: String): String {
        val key = "$animeTitle/$episodeName"
        synchronized(lock) {
            val existing = entries[key]
            if (existing != null && existing.view.state != QueuedDownloadState.Failed) return key
            start(Entry(key, storage, referer, animeTitle, episodeName))
        }
        return key
    }

    fun retry(key: String) {
        synchronized(lock) {
            val entry = entries[key]?.takeIf { it.view.state == QueuedDownloadState.Failed } ?: return
            start(entry)
        }
    }

    /** Stops a running or waiting download (its partial files are deleted) or forgets a failed one. */
    fun cancel(key: String) {
        synchronized(lock) {
            val entry = entries.remove(key) ?: return
            entry.job?.cancel()
            publish()
        }
    }

    fun remove(key: String) = cancel(key)

    /** Call with [lock] held. Moves [entry] to the end of the queue as Waiting and launches it. */
    private fun start(entry: Entry) {
        entries.remove(entry.key)
        entries[entry.key] = entry
        entry.view = QueuedDownload(entry.key, entry.title, QueuedDownloadState.Waiting)
        // Undispatched: the job queues on [gate] before this returns, so downloads start in enqueue order.
        entry.job = scope.launch(start = CoroutineStart.UNDISPATCHED) { run(entry) }
        publish()
    }

    private suspend fun run(entry: Entry) {
        try {
            gate.withLock {
                update(entry) { it.copy(state = QueuedDownloadState.Running, progress = 0f) }
                val folder = File(animeDesktopSettings.downloadsDirectory.get(), sanitizeFileName(entry.animeTitle))
                fetcher.download(entry.storage, entry.referer, folder, entry.episodeName) { p ->
                    update(entry) { it.copy(progress = p.fraction, bytesDownloaded = p.bytes, totalBytes = p.totalBytes) }
                }
            }
            synchronized(lock) {
                if (entries[entry.key] === entry) entries.remove(entry.key)
                publish()
            }
            notify("Downloaded", entry.title, isError = false)
        } catch (e: CancellationException) {
            // cancel() already dropped the entry; the fetcher deleted its partial files.
            throw e
        } catch (t: Throwable) {
            val message = t.message ?: t::class.simpleName.orEmpty()
            update(entry) { QueuedDownload(entry.key, entry.title, QueuedDownloadState.Failed, error = message) }
            notify("Download Failed", "${entry.title}: $message", isError = true)
        }
    }

    /** Applies [change] to [entry]'s row unless the entry was cancelled or replaced meanwhile. */
    private fun update(entry: Entry, change: (QueuedDownload) -> QueuedDownload) {
        synchronized(lock) {
            if (entries[entry.key] !== entry) return
            entry.view = change(entry.view)
            publish()
        }
    }

    /** Call with [lock] held. */
    private fun publish() {
        val views = entries.values.map { it.view }
        _queue.value = buildQueue(
            live = views.filter { it.state != QueuedDownloadState.Failed },
            failed = views.filter { it.state == QueuedDownloadState.Failed },
        )
    }

    private suspend fun notify(title: String, message: String, isError: Boolean) {
        val tray = trayState ?: return
        withContext(Dispatchers.Main) {
            tray.sendNotification(
                Notification(
                    title = title,
                    message = message,
                    type = if (isError) Notification.Type.Error else Notification.Type.Info
                )
            )
        }
    }
}

/**
 * [fraction] is null when the length is unknown (progressive without Content-Length); [totalBytes] is
 * null when the size is unknown (HLS or no Content-Length); [bytes] is bytes written so far.
 */
internal data class FetchProgress(val fraction: Float?, val bytes: Long, val totalBytes: Long?)

private const val SEGMENT_RETRIES = 3
private const val BUFFER_SIZE = 64 * 1024
private const val UNKNOWN_LENGTH_PROGRESS_STEP = 1_000_000L

/**
 * Writes one episode into a folder: the video goes to `<base>.<ext>.part` and is renamed on success, then
 * subtitle sidecars are written as WebVTT with their [sidecarIndexName] index. On failure or cancellation
 * every file written by the call is deleted. Mirrors the Android `EpisodeDownloader`.
 */
internal class EpisodeFetcher(
    private val client: OkHttpClient,
    private val retryDelayMillis: Long = 1_000,
) {

    suspend fun download(
        storage: KmpStorage,
        referer: String?,
        folder: File,
        episodeName: String,
        onProgress: (FetchProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val url = storage.link?.takeIf { it.isNotBlank() } ?: throw IOException("No stream link")
        val headers = playerHeaders(storage.headers, referer)
        folder.mkdirs()
        val written = mutableListOf<File>()
        try {
            val mime = storage.mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
            val path = url.substringBefore('#').substringBefore('?').lowercase()
            val video = when {
                // Routing point for DASH downloads.
                mime == "application/dash+xml" || path.endsWith(".mpd") ->
                    throw UnsupportedOperationException("DASH downloads aren't supported yet")

                "mpegurl" in mime || path.endsWith(".m3u8") ->
                    downloadHls(url, fetchText(url, headers), headers, storage.quality, folder, episodeName, written, onProgress)

                else -> {
                    val response = execute(url, headers)
                    val isPlaylist = try {
                        response.peekBody(16).string().trimStart('﻿').startsWith("#EXTM3U")
                    } catch (t: Throwable) {
                        response.close()
                        throw t
                    }
                    if (isPlaylist) {
                        val text = response.use { it.body.string() }
                        downloadHls(url, text, headers, storage.quality, folder, episodeName, written, onProgress)
                    } else {
                        downloadProgressive(url, response, folder, episodeName, written, onProgress)
                    }
                }
            }
            downloadSubtitles(storage, referer, video, written)
            video
        } catch (t: Throwable) {
            written.forEach { it.delete() }
            throw t
        }
    }

    private fun reserveBase(folder: File, episodeName: String): String =
        uniqueBaseName(sanitizeFileName(episodeName)) { base -> isBaseTaken(base) { File(folder, it).exists() } }

    // region progressive

    private suspend fun downloadProgressive(
        url: String,
        response: Response,
        folder: File,
        episodeName: String,
        written: MutableList<File>,
        onProgress: (FetchProgress) -> Unit,
    ): File = response.use {
        val ext = progressiveExtension(url, response.header("Content-Type"))
        val base = reserveBase(folder, episodeName)
        val part = File(folder, "$base.$ext.part").also(written::add)
        val total = response.body.contentLength()
        part.outputStream().use { out ->
            response.body.byteStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read = 0L
                var reported = 0L
                var lastPercent = -1
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    read += n
                    if (total > 0) {
                        val fraction = (read.toFloat() / total).coerceAtMost(1f)
                        val percent = (fraction * 100).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(FetchProgress(fraction, read, total))
                        }
                    } else if (read - reported >= UNKNOWN_LENGTH_PROGRESS_STEP) {
                        reported = read
                        onProgress(FetchProgress(null, read, null))
                    }
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
        url: String,
        text: String,
        headers: Map<String, String>,
        quality: String?,
        folder: File,
        episodeName: String,
        written: MutableList<File>,
        onProgress: (FetchProgress) -> Unit,
    ): File {
        val media = when (val playlist = parseHlsPlaylist(text, url)) {
            is HlsPlaylist.Media -> playlist
            is HlsPlaylist.Master -> {
                val variant = playlist.chooseVariant(quality)
                if (variant.audioGroup != null && variant.audioGroup in playlist.audioGroupsWithUri) {
                    throw UnsupportedHlsException("HLS streams with separate audio renditions are not supported")
                }
                parseHlsPlaylist(fetchText(variant.uri, headers), variant.uri) as? HlsPlaylist.Media
                    ?: throw UnsupportedHlsException("Nested HLS master playlists are not supported")
            }
        }
        if (media.live) throw UnsupportedHlsException("Live HLS streams cannot be downloaded")
        (media.segments.mapNotNull { it.key } + listOfNotNull(media.map?.key)).forEach { key ->
            if (key.method != "AES-128") throw UnsupportedHlsException("Unsupported HLS encryption: ${key.method}")
            if (key.uri == null) throw UnsupportedHlsException("HLS AES-128 key has no URI")
        }
        // RFC 8216 §4.3.2.5: an encrypted init section must carry an explicit IV (no sequence number to derive one).
        if (media.map?.key != null && media.map.key.iv == null) {
            throw UnsupportedHlsException("Encrypted EXT-X-MAP without an IV is not supported")
        }

        val fmp4 = media.map != null || media.segments.first().uri
            .substringBefore('?').lowercase().let { it.endsWith(".m4s") || it.endsWith(".mp4") }
        val ext = if (fmp4) "mp4" else "ts"
        val base = reserveBase(folder, episodeName)
        val part = File(folder, "$base.$ext.part").also(written::add)
        val keys = mutableMapOf<String, ByteArray>()

        FileOutputStream(part).use { file ->
            val out = BufferedOutputStream(file, BUFFER_SIZE)
            media.map?.let { map ->
                val decrypt = map.key?.let { key -> keyBytes(key, headers, keys) to key.iv!! }
                streamWithRetry(map.uri, headers, decrypt, out, file)
            }
            media.segments.forEachIndexed { index, segment ->
                currentCoroutineContext().ensureActive()
                val decrypt = segment.key?.let { key -> keyBytes(key, headers, keys) to aesIv(segment) }
                streamWithRetry(segment.uri, headers, decrypt, out, file)
                out.flush()
                onProgress(FetchProgress((index + 1).toFloat() / media.segments.size, file.channel.position(), null))
            }
            out.flush()
        }
        return finish(part, File(folder, "$base.$ext"), written)
    }

    /** Fetches each AES-128 key once per key URI. */
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

    /** Saves each subtitle as a WebVTT sidecar (failed ones are skipped) and indexes them. */
    private suspend fun downloadSubtitles(storage: KmpStorage, referer: String?, video: File, written: MutableList<File>) {
        if (storage.subtitles.isEmpty()) return
        val folder = video.parentFile!!
        val base = video.nameWithoutExtension
        val entries = mutableListOf<SidecarEntry>()
        storage.subtitles.forEachIndexed { index, subtitle ->
            try {
                val text = fetchBytes(subtitle.url, playerHeaders(storage.headers + subtitle.headers, referer)).decodeToString()
                val vtt = toWebVtt(text, subtitleFormat(subtitle.url, text.take(64)).toVttSource())
                val name = sidecarName(base, index, subtitle.lang, SubtitleFormat.Vtt)
                writeAtomically(File(folder, name), vtt.encodeToByteArray(), written)
                entries += SidecarEntry(file = name, lang = subtitle.lang, label = subtitle.lang)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                System.err.println("Skipping subtitle ${subtitle.lang} (${subtitle.url}): $e")
            }
        }
        if (entries.isEmpty()) return
        writeAtomically(File(folder, sidecarIndexName(base)), Json.encodeToString(entries).encodeToByteArray(), written)
    }

    private fun SubtitleFormat.toVttSource(): VttSource = when (this) {
        SubtitleFormat.Vtt -> VttSource.Vtt
        SubtitleFormat.Srt -> VttSource.Srt
        SubtitleFormat.Ass -> VttSource.Ass
    }

    private fun writeAtomically(target: File, bytes: ByteArray, written: MutableList<File>) {
        val part = File(target.parentFile, target.name + ".part").also(written::add)
        part.writeBytes(bytes)
        finish(part, target, written)
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
        val call = client.newCall(upstreamRequest(url.toHttpUrl(), headers).build())
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
