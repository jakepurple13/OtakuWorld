package com.programmersbox.anime.shared.downloads

import kotlinx.coroutines.flow.Flow

/** Active and failed episode downloads. Empty on platforms without a download queue. */
expect class DownloadQueueSource {
    fun observe(): Flow<List<QueuedDownload>>
    fun cancel(key: String)
    fun retry(key: String)
    fun remove(key: String)
}

/** Running first, then Waiting, then Failed. A failed entry whose key is also live is stale and dropped. */
fun buildQueue(live: List<QueuedDownload>, failed: List<QueuedDownload>): List<QueuedDownload> {
    val liveKeys = live.mapTo(HashSet()) { it.key }
    return live.filter { it.state == QueuedDownloadState.Running } +
            live.filter { it.state == QueuedDownloadState.Waiting } +
            failed.filter { it.key !in liveKeys }
}
