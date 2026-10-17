package com.programmersbox.anime.shared.downloads

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

internal fun WorkInfo.toQueuedDownload(): QueuedDownload? {
    val state = when (state) {
        WorkInfo.State.RUNNING -> QueuedDownloadState.Running
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> QueuedDownloadState.Waiting
        else -> return null
    }
    val key = tags.firstOrNull { it.startsWith(KEY_TAG_PREFIX) }?.removePrefix(KEY_TAG_PREFIX) ?: return null
    val title = tags.firstOrNull { it.startsWith(TITLE_TAG_PREFIX) }?.removePrefix(TITLE_TAG_PREFIX).orEmpty()
    return progress.toQueuedDownload(key, title, state)
}

private fun Data.toQueuedDownload(key: String, title: String, state: QueuedDownloadState): QueuedDownload {
    val fraction = getFloat(PROGRESS_KEY, 0f)
    val bytes = getLong(BYTES_KEY, -1L)
    val total = getLong(TOTAL_KEY, -1L)
    return QueuedDownload(
        key = key,
        title = title,
        state = state,
        progress = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f),
        bytesDownloaded = bytes.takeIf { it > 0 },
        totalBytes = total.takeIf { it > 0 },
    )
}

actual class DownloadQueueSource(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refresh = MutableStateFlow(0)
    private val dir: File get() = episodeDownloadsDir(context)

    actual fun observe(): Flow<List<QueuedDownload>> =
        combine(
            WorkManager.getInstance(context).getWorkInfosByTagFlow(DOWNLOAD_TAG),
            refresh,
        ) { infos, _ ->
            val live = infos.mapNotNull { it.toQueuedDownload() }
            val failed = listFailures(dir).map {
                QueuedDownload(it.key, it.title, QueuedDownloadState.Failed, error = it.error)
            }
            buildQueue(live, failed)
        }.flowOn(Dispatchers.IO)

    actual fun cancel(key: String) {
        WorkManager.getInstance(context).cancelUniqueWork(key)
    }

    actual fun retry(key: String) {
        scope.launch {
            val request = readRequestFile(requestFileFor(dir, key)) ?: return@launch
            clearFailure(dir, key)
            enqueueEpisodeDownload(context, request, ExistingWorkPolicy.REPLACE)
            refresh.update { it + 1 }
        }
    }

    actual fun remove(key: String) {
        scope.launch {
            clearFailure(dir, key)
            requestFileFor(dir, key).delete()
            refresh.update { it + 1 }
        }
    }
}
