package com.programmersbox.kmpuiviews.presentation.settings.downloadstate

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DownloadStateViewModel(
    private val downloadStateRepository: DownloadStateInterface,
) : ViewModel() {

    val downloadList = mutableStateListOf<DownloadAndInstallState>()

    init {
        downloadStateRepository
            .downloadList
            .onEach {
                downloadList.clear()
                downloadList.addAll(it.filter { it.name.isNotEmpty() })
            }
            .launchIn(viewModelScope)
    }

    fun cancelWorker(id: String) {
        downloadStateRepository.cancelDownload(id)
    }

    fun install(url: String) {
        downloadStateRepository
            .install(url = url)
            .onEach {
                for ((index, item) in downloadList.withIndex()) {
                    if (item.url == url) {
                        downloadList[index] = item.copy(status = it)
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}