package com.programmersbox.manga.shared.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object DownloadRoute : NavKey

class DownloadViewModel(
    private val downloadedMediaHandler: DownloadedMediaHandler,
    private val mangaDownloadManager: MangaDownloadManager,
) : ViewModel() {

    val fileList = downloadedMediaHandler
        .listenToUpdates()
        .map { f ->
            f
                .groupBy { it.folder }
                .mapValues { entry -> entry.value.groupBy { c -> c.chapterFolder } }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val activeDownloads = mangaDownloadManager
        .observeDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        downloadedMediaHandler.init("")
    }

    fun cancelDownload(chapterUrl: String) {
        mangaDownloadManager.cancelDownload(chapterUrl)
    }

    fun delete(downloadedChapters: DownloadedChapters) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadedMediaHandler.delete(downloadedChapters)
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadedMediaHandler.clear()
    }
}
