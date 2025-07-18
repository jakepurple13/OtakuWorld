@file:OptIn(ExperimentalTime::class)

package com.programmersbox.kmpuiviews.presentation.settings.prerelease

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.repository.GitHubPrerelease
import com.programmersbox.kmpuiviews.repository.PrereleaseRepository
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class PrereleaseViewModel(
    private val prereleaseRepository: PrereleaseRepository,
    private val downloadStateRepository: DownloadStateInterface,
) : ViewModel() {

    var uiState by mutableStateOf<PrereleaseUiState>(PrereleaseUiState.Loading)

    val downloadMap = mutableStateMapOf<String, DownloadAndInstallStatus>()

    init {
        reload()

        downloadStateRepository
            .downloadList
            .onEach { list ->
                list.forEach {
                    downloadMap[it.url] = it.status
                }
            }
            .launchIn(viewModelScope)
    }

    fun reload() {
        viewModelScope.launch {
            uiState = PrereleaseUiState.Loading
            uiState = runCatching { prereleaseRepository.getReleases() }
                .mapCatching {
                    it
                        .filter { it.prerelease }
                        .maxBy { it.createdAt }
                }
                .map { it.copy(assets = it.assets.sortedBy { it.name }) }
                .onFailure { it.printStackTrace() }
                .fold(
                    onSuccess = { PrereleaseUiState.Success(it) },
                    onFailure = { PrereleaseUiState.Error(it.message.orEmpty()) }
                )
        }
    }

    fun update(apkString: String) {
        downloadStateRepository.downloadThenInstall(apkString)
        /*downloadAndInstaller
            .downloadAndInstall(apkString, "")
            .onEach { downloadMap[apkString] = it }
            .launchIn(viewModelScope)*/
    }
}

sealed class PrereleaseUiState {
    data object Loading : PrereleaseUiState()
    data class Success(val latestRelease: GitHubPrerelease) : PrereleaseUiState()
    data class Error(val message: String) : PrereleaseUiState()
}
