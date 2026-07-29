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
        .map { folder ->
            val numberRegex = Regex("[0-9]+(?:\\.[0-9]+)?")

            folder
                .groupBy { it.folder }
                // 1. Sort the OUTER map by 'folder' name
                .toList()
                .sortedBy { it.first }
                .toMap()
                .mapValues { entry ->
                    entry.value
                        .groupBy { c -> c.chapterFolder }
                        // 2. Sort the INNER map by 'chapterFolder' name DESCENDING (Latest first in the UI)
                        .toList()
                        .sortedWith(
                            compareByDescending<Pair<String, List<DownloadedChapters>>> { innerEntry ->
                                numberRegex.find(innerEntry.first)?.value?.toDoubleOrNull() ?: 0.0
                            }.thenByDescending { innerEntry -> innerEntry.first }
                        )
                        .toMap()
                        // 3. Sort the actual chapters inside those folders by 'chapterName' DESCENDING
                        .mapValues { sortedInnerEntry ->
                            sortedInnerEntry.value.sortedWith(
                                compareByDescending<DownloadedChapters> { c ->
                                    numberRegex.find(c.chapterName)?.value?.toDoubleOrNull() ?: 0.0
                                }.thenByDescending { c -> c.chapterName }
                            )
                        }
                }
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
