package com.programmersbox.manga.shared.downloads

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual class MangaDownloadManager(
    private val scope: CoroutineScope,
    mangaDesktopSettings: MangaDesktopSettings,
    private val trayState: TrayState,
) {

    private val httpClient = HttpClient()
    private val queue = Channel<DownloadRequest>(Channel.UNLIMITED)
    private val _downloads = MutableStateFlow<List<ChapterDownloadProgress>>(emptyList())
    private val cancelledUrls = mutableSetOf<String>()
    private val mutex = Mutex()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    private var rootDir: String = "${System.getProperty("user.home")}/Downloads/MangaWorld"

    init {
        mangaDesktopSettings
            .downloadsDirectory
            .asFlow()
            .onEach { root ->
                rootDir = root
                _downloads.update {
                    File(root)
                        .listFiles()
                        ?.flatMap { file ->
                            val title = file.name
                            file
                                .listFiles { chapterDir -> isCompleteChapter(chapterDir.list()) }
                                ?.map {
                                    val path = it.absolutePath
                                    ChapterDownloadProgress(
                                        chapterUrl = path,
                                        chapterName = it.name,
                                        mangaTitle = title ?: "",
                                        state = DownloadState.Completed
                                    )
                                }
                                ?: emptyList()
                        }
                        ?: emptyList()
                }
            }
            .launchIn(scope)
    }

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion { httpClient.close() }

        // Start 3 concurrent worker loops reading from the same channel
        repeat(CONCURRENT_DOWNLOADS) {
            scope.launch {
                for (request in queue) {
                    // Launch the actual download in a child coroutine.
                    // This allows us to cancel the specific download without killing the worker loop.
                    val job = launch(start = CoroutineStart.LAZY) {
                        val destDir = File(
                            "$rootDir/${request.mangaTitle.sanitize()}/${request.chapterName.sanitize()}"
                        ).also { it.mkdirs() }
                        // Stays behind if the download is cancelled or fails part way, so the chapter isn't counted
                        runCatching { File(destDir, INCOMPLETE_MARKER).createNewFile() }

                        println(destDir.absolutePath)

                        try {
                            executeDownload(
                                client = httpClient,
                                request = request,
                                onProgress = { done, total ->
                                    updateState(request.chapterUrl) {
                                        it.copy(state = DownloadState.Downloading(done, total))
                                    }
                                },
                                writeBytes = { index, bytes ->
                                    File(destDir, "%03d.png".format(index)).writeBytes(bytes)
                                },
                            )
                            File(destDir, INCOMPLETE_MARKER).delete()
                            updateState(request.chapterUrl) { it.copy(state = DownloadState.Completed) }
                        } catch (e: CancellationException) {
                            updateState(request.chapterUrl) { it.copy(state = DownloadState.Cancelled) }
                            throw e
                        } catch (e: Exception) {
                            updateState(request.chapterUrl) {
                                it.copy(state = DownloadState.Failed(e.message ?: "Unknown"))
                            }
                        }
                    }

                    // Checking the cancel list and tracking the job under one lock means a cancel
                    // either finds the running job or stops it here, never neither
                    val cancelled = mutex.withLock {
                        cancelledUrls.remove(request.chapterUrl).also { if (!it) activeJobs[request.chapterUrl] = job }
                    }
                    if (cancelled) {
                        job.cancel()
                        updateState(request.chapterUrl) { it.copy(state = DownloadState.Cancelled) }
                        continue
                    }

                    // Wait for it to finish, then clean it up
                    job.start()
                    job.join()
                    activeJobs.remove(request.chapterUrl, job)
                }
            }
        }
    }

    // Observes _downloads and fires tray notifications on state transitions.
    // Queued→Downloading fires "Downloading"; →Completed fires "Downloaded";
    // →Failed fires "Download Failed". Cancelled is silent.
    // Startup-loaded items (prev == null, state == Completed) are skipped
    // because the Completed/Failed guards require prev != null.
    init {
        var previousStates = emptyMap<String, DownloadState>()
        _downloads
            .onEach { list ->
                val currentStates = list.associateBy({ it.chapterUrl }, { it.state })
                list.forEach { progress ->
                    val prev = previousStates[progress.chapterUrl]
                    when {
                        // Transition from Queued to Downloading (chapter actually started)
                        prev is DownloadState.Queued && progress.state is DownloadState.Downloading ->
                            withContext(Dispatchers.Main) {
                                trayState.sendNotification(
                                    Notification(
                                        title = "Downloading",
                                        message = "${progress.mangaTitle} — ${progress.chapterName}",
                                        type = Notification.Type.Info
                                    )
                                )
                            }

                        // Transition to complete
                        prev != null &&
                                prev !is DownloadState.Completed &&
                                progress.state is DownloadState.Completed ->
                            withContext(Dispatchers.Main) {
                                trayState.sendNotification(
                                    Notification(
                                        title = "Downloaded",
                                        message = "${progress.mangaTitle} — ${progress.chapterName}",
                                        type = Notification.Type.Info
                                    )
                                )
                            }

                        // Transition to failed
                        prev != null &&
                                prev !is DownloadState.Failed &&
                                progress.state is DownloadState.Failed ->
                            withContext(Dispatchers.Main) {
                                trayState.sendNotification(
                                    Notification(
                                        title = "Download Failed",
                                        message = "${progress.chapterName}: ${progress.state.reason}",
                                        type = Notification.Type.Error
                                    )
                                )
                            }
                    }
                }
                previousStates = currentStates
            }
            .launchIn(scope)
    }

    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
        scope.launch {
            val storages = chapter.getChapterInfo().firstOrNull() ?: return@launch
            val urls = storages.mapNotNull { it.link }
            if (urls.isEmpty()) return@launch
            val headers = storages
                .flatMap { it.headers.entries }
                .associate { it.key to it.value }

            val request = DownloadRequest(
                chapterUrl = chapter.url,
                chapterName = chapter.name,
                mangaTitle = mangaTitle,
                imageUrls = urls,
                headers = headers,
            )

            mutex.withLock {
                _downloads.update { list ->
                    list + ChapterDownloadProgress(
                        chapterUrl = chapter.url,
                        chapterName = chapter.name,
                        mangaTitle = mangaTitle,
                        state = DownloadState.Queued,
                    )
                }
            }
            queue.send(request)
        }
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
        println("Downloading chapters ${chapters.size} for manga $mangaTitle")
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        scope.launch {
            mutex.withLock {
                // Only queued chapters go on the cancel list: a running one is cancelled directly,
                // and leaving its url there would silently cancel the next download of it
                val running = activeJobs[chapterUrl]
                if (running != null) running.cancel() else cancelledUrls.add(chapterUrl)
            }
        }
    }

    actual fun cancelAll() {
        val pending = _downloads.value
            .filter { it.state is DownloadState.Queued || it.state is DownloadState.Downloading }
            .map { it.chapterUrl }

        scope.launch {
            mutex.withLock { cancelledUrls.addAll(pending.filterNot { activeJobs.containsKey(it) }) }
        }

        // Cancel all currently running jobs and clear the map
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()

        _downloads.update { list ->
            list.map { p ->
                if (p.state is DownloadState.Queued || p.state is DownloadState.Downloading)
                    p.copy(state = DownloadState.Cancelled)
                else p
            }
        }
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? {
        val dir = File("$rootDir/${mangaTitle.sanitize()}/${chapter.name.sanitize()}")
        return if (isCompleteChapter(dir.list())) dir.absolutePath else null
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> = _downloads.asStateFlow()

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        val dir = File("$rootDir/${mangaTitle.sanitize()}/${chapter.name.sanitize()}")
        dir.deleteRecursively()
        // Chapters found on disk at startup are keyed by their folder path
        _downloads.update { list -> list.filter { it.chapterUrl != chapter.url && it.chapterUrl != dir.absolutePath } }
    }

    private fun updateState(
        chapterUrl: String,
        transform: (ChapterDownloadProgress) -> ChapterDownloadProgress,
    ) {
        _downloads.update { list -> list.map { if (it.chapterUrl == chapterUrl) transform(it) else it } }
    }
}
