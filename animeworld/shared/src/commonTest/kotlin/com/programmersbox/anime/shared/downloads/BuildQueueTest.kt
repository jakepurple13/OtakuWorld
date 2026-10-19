package com.programmersbox.anime.shared.downloads

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildQueueTest {
    private fun item(key: String, state: QueuedDownloadState) = QueuedDownload(key, key, state)

    @Test fun ordersRunningWaitingFailed() {
        val queue = buildQueue(
            live = listOf(item("w", QueuedDownloadState.Waiting), item("r", QueuedDownloadState.Running)),
            failed = listOf(item("f", QueuedDownloadState.Failed)),
        )
        assertEquals(
            listOf(QueuedDownloadState.Running, QueuedDownloadState.Waiting, QueuedDownloadState.Failed),
            queue.map { it.state },
        )
    }

    @Test fun liveKeyHidesStaleFailure() {
        val queue = buildQueue(
            live = listOf(item("k", QueuedDownloadState.Running)),
            failed = listOf(item("k", QueuedDownloadState.Failed)),
        )
        assertEquals(listOf(item("k", QueuedDownloadState.Running)), queue)
    }

    @Test fun emptyInputsGiveEmptyQueue() = assertEquals(emptyList(), buildQueue(emptyList(), emptyList()))
}
