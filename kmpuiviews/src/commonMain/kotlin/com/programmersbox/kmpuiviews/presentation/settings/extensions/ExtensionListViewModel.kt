package com.programmersbox.kmpuiviews.presentation.settings.extensions

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
import com.programmersbox.kmpmodels.KmpUntrustedApiService
import com.programmersbox.kmpmodels.NativeCatalogs
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.repository.SourceInfoRepository
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExtensionListViewModel(
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    otakuWorldCatalog: OtakuWorldCatalog,
    settingsHandling: NewSettingsHandling,
    val downloadAndInstaller: DownloadAndInstaller,
    private val downloadStateRepository: DownloadStateInterface,
    private val extensionShareHandler: ExtensionShareHandler,
    private val nativeCatalogs: NativeCatalogs,
    private val sourceInfoRepository: SourceInfoRepository,
) : ViewModel() {
    private val installedSources = mutableStateListOf<KmpSourceInformation>()
    private val downloads = mutableStateListOf<DownloadAndInstallState>()
    private val remoteCatalogs = RemoteCatalogs()
    private val versionOf: (KmpSourceInformation) -> String = sourceInfoRepository::versionName

    private val allRemote by derivedStateOf {
        remoteCatalogs.states.values.filterIsInstance<RemoteViewState>().flatMap { it.sources }
    }

    val installedItems by derivedStateOf { installedItems(installedSources, allRemote, versionOf, downloads) }

    val summary by derivedStateOf { installedItems.summary() }

    var filter by mutableStateOf(InstalledFilter.None)

    val visibleInstalled by derivedStateOf { installedItems.filteredBy(filter).byCatalog() }

    /** User expand/collapse choices per catalog; absent = default (expanded when it needs attention). */
    val expanded = mutableStateMapOf<String?, Boolean>()

    val catalogs by derivedStateOf { catalogUi(remoteCatalogs.states, installedSources, versionOf, downloads) }

    fun item(key: String): RemoteItem? = catalogs.values
        .filterIsInstance<CatalogUi.Loaded>()
        .firstNotNullOfOrNull { c -> c.items.find { it.key == key } }

    val hasCustomBridge by derivedStateOf {
        nativeCatalogs.catalogs.isNotEmpty() ||
            installedSources.any { it.catalog is KmpExternalCustomApiServicesCatalog && it.name == "Custom Tachiyomi Bridge" }
    }

    fun install(remote: KmpRemoteSources) {
        downloadStateRepository.downloadAndInstall(remote.downloadLink)
    }

    fun updateAll() {
        installedItems.updateLinks().forEach(downloadStateRepository::downloadAndInstall)
    }

    fun retry(catalog: String) {
        viewModelScope.launch { remoteCatalogs.retry(catalog) }
    }

    fun uninstall(source: KmpSourceInformation) {
        sourceInfoRepository.uninstall(source)
    }

    init {
        downloadStateRepository.downloadList
            .onEach {
                downloads.clear()
                downloads.addAll(it)
            }
            .launchIn(viewModelScope)

        sourceRepository.sources
            .onEach {
                installedSources.clear()
                installedSources.addAll(it)
            }
            .onEach { sources ->
                remoteCatalogs.load("${otakuWorldCatalog.name}World") { otakuWorldCatalog.getRemoteSources() }
                sources
                    .asSequence()
                    .map { it.catalog }
                    .filterIsInstance<KmpExternalApiServicesCatalog>()
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c -> remoteCatalogs.load(c.name) { c.getRemoteSources() } }
            }
            .combine(settingsHandling.customUrls) { sources, urls ->
                (sources.asSequence().map { it.catalog }.filterIsInstance<KmpExternalCustomApiServicesCatalog>() +
                    nativeCatalogs.catalogs.asSequence())
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c -> remoteCatalogs.load(c.name) { c.getRemoteSources(urls) } }
            }
            .launchIn(viewModelScope)
    }

    fun trust(source: KmpSourceInformation) {
        viewModelScope.launch {
            (source.apiService as? KmpUntrustedApiService)?.trust()
            sourceLoader.blockingLoad()
        }
    }

    fun refreshExtensions() {
        viewModelScope.launch { sourceLoader.blockingLoad() }
    }

    var extensionSharing by mutableStateOf(false)

    fun shareExtensions(platformFile: PlatformFile) {
        viewModelScope.launch {
            extensionSharing = true
            extensionShareHandler.shareExtensions(
                platformFile,
                installedSources.distinctBy { it.packageName }
            )
            extensionSharing = false
        }
    }
}
