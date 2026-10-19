package com.programmersbox.anime.shared.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class DownloadQueueSource {
    actual fun observe(): Flow<List<QueuedDownload>> = flowOf(emptyList())
    actual fun cancel(key: String) = Unit
    actual fun retry(key: String) = Unit
    actual fun remove(key: String) = Unit
}
