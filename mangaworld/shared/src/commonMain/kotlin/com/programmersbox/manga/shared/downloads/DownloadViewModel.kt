package com.programmersbox.manga.shared.downloads

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object DownloadRoute : NavKey

class DownloadViewModel(
    private val downloadedMediaHandler: DownloadedMediaHandler,
    private val mangaDownloadManager: MangaDownloadManager,
) : ViewModel() {

    val fileList = mutableStateMapOf<String, Map<String, List<DownloadedChapters>>>()

    val activeDownloads = mangaDownloadManager.observeDownloads()

    init {
        downloadedMediaHandler.init("")

        downloadedMediaHandler
            .listenToUpdates()
            .map { f ->
                f
                    .groupBy { it.folder }
                    .entries
                    .toList()
                    .fastMap { it.key to it.value.groupBy { c -> c.chapterFolder } }
                    .toMap()
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                fileList.clear()
                fileList.putAll(it)
            }
            .launchIn(viewModelScope)
    }

    fun cancelDownload(chapterUrl: String) {
        mangaDownloadManager.cancelDownload(chapterUrl)
    }

    fun delete(downloadedChapters: DownloadedChapters) {
        viewModelScope.launch {
            downloadedMediaHandler.delete(downloadedChapters)
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadedMediaHandler.clear()
    }
}