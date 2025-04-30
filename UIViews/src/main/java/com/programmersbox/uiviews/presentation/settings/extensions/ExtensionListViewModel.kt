package com.programmersbox.uiviews.presentation.settings.extensions

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.uiviews.OtakuWorldCatalog
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstaller
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExtensionListViewModel(
    sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    otakuWorldCatalog: OtakuWorldCatalog,
    settingsHandling: NewSettingsHandling,
    val downloadAndInstaller: DownloadAndInstaller,
    private val downloadStateRepository: DownloadStateRepository,
) : ViewModel() {
    private val installedSources = mutableStateListOf<KmpSourceInformation>()
    val remoteSources = mutableStateMapOf<String, RemoteState>()

    val installed by derivedStateOf {
        installedSources
            .groupBy { it.catalog?.name }
            .mapValues { InstalledViewState(it.value) }
    }

    val remoteSourcesVersions by derivedStateOf {
        remoteSources.values.filterIsInstance<RemoteViewState>().flatMap { it.sources }
    }

    val remoteSourcesShowing = mutableStateMapOf<String, Boolean>()

    val hasCustomBridge by derivedStateOf {
        installedSources.any { it.catalog is KmpExternalCustomApiServicesCatalog && it.name == "Custom Tachiyomi Bridge" }
    }

    fun downloadAndInstall(downloadLink: String, destinationPath: String) {
        /*downloadAndInstaller
            .downloadAndInstall(downloadLink, destinationPath)
            .launchIn(viewModelScope)*/
        downloadStateRepository.downloadAndInstall(downloadLink)
    }

    fun uninstall(packageName: String) {
        viewModelScope.launch {
            downloadAndInstaller.uninstall(packageName)
            sourceLoader.load()
        }
    }

    init {
        sourceRepository.sources
            .onEach {
                installedSources.clear()
                installedSources.addAll(it)
            }
            .onEach { sources ->
                remoteSourcesShowing.clear()
                remoteSources["${otakuWorldCatalog.name}World"] = RemoteViewState(otakuWorldCatalog.getRemoteSources())
                remoteSourcesShowing["${otakuWorldCatalog.name}World"] = false
                sources
                    .asSequence()
                    .map { it.catalog }
                    .filterIsInstance<KmpExternalApiServicesCatalog>()
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c ->
                        remoteSources[c.name] = runCatching { c.getRemoteSources() }
                            .onFailure { it.printStackTrace() }
                            .fold(
                                onSuccess = { RemoteViewState(it) },
                                onFailure = { RemoteErrorState() }
                            )

                        remoteSourcesShowing[c.name] = false
                    }
            }
            .combine(settingsHandling.customUrls) { sources, urls ->
                sources.asSequence()
                    .map { it.catalog }
                    .filterIsInstance<KmpExternalCustomApiServicesCatalog>()
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c ->
                        remoteSources[c.name] = runCatching { c.getRemoteSources(urls) }
                            .onFailure { it.printStackTrace() }
                            .fold(
                                onSuccess = { RemoteViewState(it) },
                                onFailure = { RemoteErrorState() }
                            )
                    }
            }
            .launchIn(viewModelScope)
    }

    fun refreshExtensions() {
        sourceLoader.load()
    }
}

class InstalledViewState(
    val sourceInformation: List<KmpSourceInformation>,
) {
    var showItems by mutableStateOf(false)
}

sealed class RemoteState

class RemoteViewState(
    val sources: List<KmpRemoteSources>,
) : RemoteState()

class RemoteErrorState : RemoteState()