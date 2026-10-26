package com.programmersbox.anime.shared.downloads

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.videoplayer.playerHeaders
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

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

        /** DASH goes to its own downloader; everything else (HLS or progressive) to [EpisodeDownloader]. */
        fun request(folder: File): EpisodeDownloadRequest {
            val url = storage.link?.takeIf { it.isNotBlank() } ?: throw IOException("No stream link")
            val mime = storage.mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
            val path = url.substringBefore('#').substringBefore('?').lowercase()
            // Routing point for DASH downloads.
            if (mime == "application/dash+xml" || path.endsWith(".mpd")) {
                throw UnsupportedOperationException("DASH downloads aren't supported yet")
            }
            return EpisodeDownloadRequest(
                url = url,
                headers = playerHeaders(storage.headers, referer),
                mimeType = storage.mimeType,
                qualityHint = storage.quality,
                subtitles = storage.subtitles,
                title = episodeName,
                folder = folder.absolutePath,
            )
        }
    }

    private val downloader = EpisodeDownloader(
        client = client,
        vttSubtitles = true,
        warn = { message, t -> System.err.println("$message: $t") },
    )

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
                var lastPercent = -1
                downloader.download(entry.request(folder)) { p ->
                    // Known-length progress arrives per buffer; publish whole-percent steps only.
                    val percent = p.fraction?.let { (it * 100).toInt() }
                    if (percent == null || percent != lastPercent) {
                        lastPercent = percent ?: -1
                        update(entry) { it.copy(progress = p.fraction, bytesDownloaded = p.bytes, totalBytes = p.totalBytes) }
                    }
                }
            }
            synchronized(lock) {
                if (entries[entry.key] === entry) entries.remove(entry.key)
                publish()
            }
            notify("Downloaded", entry.title, isError = false)
        } catch (e: CancellationException) {
            // cancel() already dropped the entry; the downloader deleted its partial files.
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
