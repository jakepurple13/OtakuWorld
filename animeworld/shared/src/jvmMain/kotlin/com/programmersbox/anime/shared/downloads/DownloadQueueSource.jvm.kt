package com.programmersbox.anime.shared.downloads

import kotlinx.coroutines.flow.Flow

actual class DownloadQueueSource(private val manager: AnimeDownloadManager) {
    actual fun observe(): Flow<List<QueuedDownload>> = manager.queue
    actual fun cancel(key: String) = manager.cancel(key)
    actual fun retry(key: String) = manager.retry(key)
    actual fun remove(key: String) = manager.remove(key)
}
