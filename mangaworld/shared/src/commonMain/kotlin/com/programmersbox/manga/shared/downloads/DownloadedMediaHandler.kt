package com.programmersbox.manga.shared.downloads

import kotlinx.coroutines.flow.Flow

expect class DownloadedMediaHandler {
    fun init(folderLocation: String)
    fun listenToUpdates(): Flow<List<DownloadedChapters>>
    fun delete(downloadedChapters: DownloadedChapters)
    fun clear()
}

data class DownloadedChapters(
    val name: String,
    val id: String,
    val data: String,
    val assetFileStringUri: String,
    val folder: String = "",
    val folderName: String = "",
    val chapterFolder: String = "",
    val chapterName: String = "",
)