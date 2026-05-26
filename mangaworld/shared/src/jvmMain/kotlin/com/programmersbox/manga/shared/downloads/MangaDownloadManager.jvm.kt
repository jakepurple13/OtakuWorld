package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
import java.io.File
import java.util.concurrent.atomic.AtomicReference

actual class MangaDownloadManager(
    private val scope: CoroutineScope,
    mangaDesktopSettings: MangaDesktopSettings,
) {

    private val httpClient = HttpClient()
    private val queue = Channel<DownloadRequest>(Channel.UNLIMITED)
    private val _downloads = MutableStateFlow<List<ChapterDownloadProgress>>(emptyList())
    private val cancelledUrls = mutableSetOf<String>()
    private val mutex = Mutex()
    private val activeJob = AtomicReference<Pair<String, Job>?>(null)

    private var rootDir: String = "${System.getProperty("user.home")}/Downloads/MangaWorld"

    init {
        mangaDesktopSettings
            .downloadsDirectory
            .asFlow()
            .onEach { rootDir = it }
            .launchIn(scope)
    }

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion { httpClient.close() }
        scope.launch {
            for (request in queue) {
                val cancelled = mutex.withLock { cancelledUrls.remove(request.chapterUrl) }
                if (cancelled) {
                    updateState(request.chapterUrl) { it.copy(state = DownloadState.Cancelled) }
                    continue
                }

                val job = scope.launch {
                    val destDir = File(
                        "$rootDir/${request.mangaTitle.sanitize()}/${request.chapterName.sanitize()}"
                    ).also { it.mkdirs() }

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

                activeJob.set(request.chapterUrl to job)
                job.join()
                activeJob.set(null)
            }
        }
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
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        scope.launch {
            mutex.withLock { cancelledUrls.add(chapterUrl) }
            val (url, job) = activeJob.get() ?: return@launch
            if (url == chapterUrl) job.cancel()
        }
    }

    actual fun cancelAll() {
        val pending = _downloads.value
            .filter { it.state is DownloadState.Queued || it.state is DownloadState.Downloading }
            .map { it.chapterUrl }
        scope.launch {
            mutex.withLock { cancelledUrls.addAll(pending) }
        }
        activeJob.get()?.second?.cancel()
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
        return if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) dir.absolutePath else null
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> = _downloads.asStateFlow()

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        File("$rootDir/${mangaTitle.sanitize()}/${chapter.name.sanitize()}").deleteRecursively()
        _downloads.update { list -> list.filter { it.chapterUrl != chapter.url } }
    }

    private fun updateState(
        chapterUrl: String,
        transform: (ChapterDownloadProgress) -> ChapterDownloadProgress,
    ) {
        _downloads.update { list -> list.map { if (it.chapterUrl == chapterUrl) transform(it) else it } }
    }
}
