package com.programmersbox.manga.shared.downloads

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object DownloadRoute : NavKey

class DownloadViewModel(
    private val downloadedMediaHandler: DownloadedMediaHandler,
) : ViewModel() {

    val fileList = downloadedMediaHandler.listenToUpdates()
        .map { f ->
            f
                .groupBy { it.folder }
                .entries
                .toList()
                .fastMap { it.key to it.value.groupBy { c -> c.chapterFolder } }
                .toMap()
        }

    init {
        downloadedMediaHandler.init("")
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