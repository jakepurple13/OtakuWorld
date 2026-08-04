package com.programmersbox.manga.shared.downloads

import kotlinx.coroutines.flow.Flow

actual class DownloadedMediaHandler {
    actual fun init(folderLocation: String) {
    }

    actual fun listenToUpdates(): Flow<List<DownloadedChapters>> {
        TODO("Not yet implemented")
    }

    actual fun delete(downloadedChapters: DownloadedChapters) {
    }

    actual fun clear() {
    }
}