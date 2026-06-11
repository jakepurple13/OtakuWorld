package com.programmersbox.manga.shared.downloads

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

actual class MangaDownloadManager(
    context: Context,
    private val settingsHandling: NewSettingsHandling,
    mangaNewSettingsHandling: MangaNewSettingsHandling,
) {

    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Progress from workers — only Queued/Downloading entries
    private val _activeDownloads = MutableStateFlow<List<ChapterDownloadProgress>>(emptyList())

    // Signals filesystem mutations; replay=1 so new subscribers get the latest tick immediately
    private val _dirTick = MutableSharedFlow<Unit>(replay = 1)

    private var rootDir: DocumentFile? = null

    init {
        mangaNewSettingsHandling
            .downloadPath
            .asFlow()
            .onEach {

                rootDir = if (it.isEmpty()) {
                    DocumentFile.fromFile(File(context.filesDir, it).also { it.mkdirs() })
                } else {
                    DocumentFile.fromTreeUri(context, it.toUri())
                }
            }
            .launchIn(scope)

        // Forward FileObserver events into _dirTick so observeDownloads re-evaluates filesystem
        rootDirObserverFlow(context)
            .onEach { _dirTick.emit(Unit) }
            .launchIn(scope)

        workManager
            .getWorkInfosByTagFlow(DownloadChapterWorker.DOWNLOAD_TAG)
            .onEach { infos ->
                _activeDownloads.value = infos
                    .filter {
                        it.state == WorkInfo.State.ENQUEUED ||
                                it.state == WorkInfo.State.BLOCKED ||
                                it.state == WorkInfo.State.RUNNING
                    }
                    .map { info ->
                        val chapterUrl = info.tags
                            .firstOrNull { it != DownloadChapterWorker.DOWNLOAD_TAG } ?: ""
                        val chapterName = info.progress.getString(DownloadChapterWorker.KEY_CHAPTER_NAME) ?: ""
                        val mangaTitle = info.progress.getString(DownloadChapterWorker.KEY_MANGA_TITLE) ?: ""
                        ChapterDownloadProgress(
                            chapterUrl = chapterUrl,
                            chapterName = chapterName,
                            mangaTitle = mangaTitle,
                            state = when (info.state) {
                                WorkInfo.State.ENQUEUED,
                                WorkInfo.State.BLOCKED,
                                    -> DownloadState.Queued

                                WorkInfo.State.RUNNING -> DownloadState.Downloading(
                                    imagesDownloaded = info.progress.getInt(DownloadChapterWorker.KEY_PROGRESS_DONE, 0),
                                    totalImages = info.progress.getInt(DownloadChapterWorker.KEY_PROGRESS_TOTAL, 0),
                                )

                                else -> DownloadState.Queued
                            },
                        )
                    }
                // When a worker reaches a terminal state, tick so the filesystem check re-runs
                if (infos.any {
                        it.state == WorkInfo.State.SUCCEEDED ||
                                it.state == WorkInfo.State.FAILED ||
                                it.state == WorkInfo.State.CANCELLED
                    }
                ) _dirTick.emit(Unit)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)
    }

    // Watches rootDir for directory-level events (title dirs created or removed)
    private fun rootDirObserverFlow(context: Context): Flow<Unit> = callbackFlow {
        // Note: DocumentFile does not have a self-creating mkdirs() method.
        // You typically ensure the root directory exists when the user initially
        // grants the SAF tree permission.

        val contentResolver = context.contentResolver

        // Create a ContentObserver. It requires a Handler to dispatch changes.
        // Using the main looper is standard practice here.
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // A change occurred in the directory or its descendants
                trySend(Unit)
            }
        }

        // Register the observer.
        // The 'true' parameter means "notifyForDescendants",
        // which effectively watches for files added/deleted inside the directory.
        rootDir?.uri?.let { contentResolver.registerContentObserver(it, true, observer) }

        // Clean up the observer when the Flow collection is cancelled
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }
        .flowOn(Dispatchers.IO)
        .onStart { emit(Unit) }

    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
        scope.launch {
            val storages = chapter.getChapterInfo().firstOrNull() ?: return@launch
            val urls = storages.mapNotNull { it.link }
            if (urls.isEmpty()) return@launch
            val headers = storages
                .flatMap { it.headers.entries }
                .associate { it.key to it.value }

            val inputData = workDataOf(
                DownloadChapterWorker.KEY_MANGA_TITLE to mangaTitle,
                DownloadChapterWorker.KEY_CHAPTER_NAME to chapter.name,
                DownloadChapterWorker.KEY_CHAPTER_URL to chapter.url,
                DownloadChapterWorker.KEY_IMAGE_URLS to Json.encodeToString<List<String>>(urls),
                DownloadChapterWorker.KEY_HEADERS to Json.encodeToString<Map<String, String>>(headers),
            )

            val mediaCheckerSettings = settingsHandling
                .mediaCheckerSettings
                .get()

            val workRequest = OneTimeWorkRequestBuilder<DownloadChapterWorker>()
                .setInputData(inputData)
                .addTag(DownloadChapterWorker.DOWNLOAD_TAG)
                .addTag(chapter.url)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            when (mediaCheckerSettings.networkType) {
                                MediaCheckerNetworkType.Connected -> NetworkType.CONNECTED
                                MediaCheckerNetworkType.Metered -> NetworkType.METERED
                                MediaCheckerNetworkType.Unmetered -> NetworkType.UNMETERED
                            }
                        )
                        .setRequiresCharging(mediaCheckerSettings.requiresCharging)
                        .setRequiresBatteryNotLow(mediaCheckerSettings.requiresBatteryNotLow)
                        .build()
                )
                .build()

            workManager
                .beginUniqueWork(
                    DownloadChapterWorker.DOWNLOAD_QUEUE,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    workRequest,
                )
                .enqueue()
        }
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        workManager.cancelAllWorkByTag(chapterUrl)
    }

    actual fun cancelAll() {
        workManager.cancelAllWorkByTag(DownloadChapterWorker.DOWNLOAD_TAG)
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? {
        val titleDir = rootDir?.findFile(mangaTitle.sanitize()) ?: return null
        val chapterDir = titleDir.findFile(chapter.name.sanitize()) ?: return null

        // DocumentFile.listFiles() returns an empty array if empty, never null
        return if (chapterDir.exists() && chapterDir.listFiles().isNotEmpty()) {
            chapterDir.uri.toString() // Use URI string instead of absolutePath
        } else {
            null
        }
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> =
        _activeDownloads.combine(
            _dirTick.onStart { emit(Unit) }
        ) { downloads, _ ->
            val activeUrls = downloads.mapTo(mutableSetOf()) { it.chapterUrl }
            val completedFromDisk = rootDir
                ?.listFiles()
                ?.flatMap { titleDir ->
                    titleDir
                        .listFiles()
                        .filter { it.isDirectory && it.listFiles().isNotEmpty() }
                        .flatMap { chapterDir ->
                            chapterDir
                                .listFiles()
                                .map { file ->
                                    ChapterDownloadProgress(
                                        // Map absolutePath to the file's URI
                                        chapterUrl = file.uri.toString(),
                                        // DocumentFile names are nullable, provide safe fallbacks
                                        chapterName = chapterDir.name ?: "Unknown Chapter",
                                        mangaTitle = titleDir.name ?: "Unknown Title",
                                        state = DownloadState.Completed,
                                    )
                                }
                        }
                }
                ?: emptyList()

            println(completedFromDisk.joinToString("\n"))

            downloads + completedFromDisk.filter { it.chapterUrl !in activeUrls }
        }.flowOn(Dispatchers.IO)

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        rootDir?.findFile(mangaTitle.sanitize())
            ?.findFile(chapter.name.sanitize())
            ?.delete() // Inherently deletes recursively if it's a TreeDocumentFile

        scope.launch { _dirTick.emit(Unit) }
    }
}
