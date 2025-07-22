package com.programmersbox.manga.shared.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class DownloadedMediaHandler {
    actual fun init(folderLocation: String) {
    }

    actual fun listenToUpdates(): Flow<List<DownloadedChapters>> = emptyFlow()

    actual fun delete(downloadedChapters: DownloadedChapters) {
    }

    actual fun clear() {
    }
}