package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpUntrustedApiService
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlin.coroutines.cancellation.CancellationException

sealed interface RemoteItemState {
    data object NotInstalled : RemoteItemState
    data class Installed(val installed: KmpSourceInformation) : RemoteItemState
    data class UpdateAvailable(val installed: KmpSourceInformation, val installedVersion: String) : RemoteItemState
    data class Installing(val status: DownloadAndInstallStatus) : RemoteItemState
}

data class RemoteItem(
    val remote: KmpRemoteSources,
    val catalog: String,
    val state: RemoteItemState,
) {
    val key: String get() = "$catalog/${remote.packageName}"
}

data class InstalledItem(
    val info: KmpSourceInformation,
    val version: String,
    /** Remote entry with a newer version, or null when up to date or unmatched. */
    val update: KmpRemoteSources?,
    /** Active or failed download for [update], if any. */
    val installing: DownloadAndInstallStatus?,
) {
    val untrusted: KmpUntrustedApiService? get() = info.apiService as? KmpUntrustedApiService
}

enum class InstalledFilter { None, Updates, Untrusted }

/** Tapping the active filter (or [InstalledFilter.None]) clears it. */
fun InstalledFilter.toggle(tapped: InstalledFilter): InstalledFilter = if (tapped == this) InstalledFilter.None else tapped

data class InstalledSummary(val installed: Int, val updates: Int, val untrusted: Int)

sealed interface CatalogUi {
    data object Loading : CatalogUi
    data class Error(val message: String?) : CatalogUi
    data class Loaded(val items: List<RemoteItem>) : CatalogUi
}

/** Package name first; falls back to the legacy rule of a shared source base URL. Blank keys never match. */
fun KmpRemoteSources.findInstalled(installed: List<KmpSourceInformation>): KmpSourceInformation? =
    installed.find { packageName.isNotBlank() && it.packageName == packageName }
        ?: installed.find { i -> i.apiService.baseUrl.isNotBlank() && sources.any { it.baseUrl == i.apiService.baseUrl } }

fun KmpSourceInformation.findRemote(remotes: List<KmpRemoteSources>): KmpRemoteSources? =
    remotes.find { packageName.isNotBlank() && it.packageName == packageName }
        ?: remotes.find { r -> apiService.baseUrl.isNotBlank() && r.sources.any { it.baseUrl == apiService.baseUrl } }

/** Latest download entry for [url], unless it already finished or was cancelled. Errors stay visible. */
fun List<DownloadAndInstallState>.statusFor(url: String): DownloadAndInstallStatus? =
    if (url.isBlank()) {
        null
    } else {
        lastOrNull { it.url == url }
            ?.status
            ?.takeUnless { it == DownloadAndInstallStatus.Installed || it == DownloadAndInstallStatus.Cancelled }
    }

/** Shown for a requested download whose job is still queued (queued jobs report no url). */
private val Queued = DownloadAndInstallStatus.Downloading(0f)

private fun DownloadAndInstallStatus.isTerminal() =
    this == DownloadAndInstallStatus.Installed || this == DownloadAndInstallStatus.Cancelled || this is DownloadAndInstallStatus.Error

/** Requested download links that have not yet reached a terminal status in [downloads]. */
fun pendingAfter(requested: Set<String>, downloads: List<DownloadAndInstallState>): Set<String> =
    requested.filterNot { url -> downloads.lastOrNull { it.url == url }?.status?.isTerminal() == true }.toSet()

fun resolveState(
    remote: KmpRemoteSources,
    installed: List<KmpSourceInformation>,
    versionOf: (KmpSourceInformation) -> String,
    downloads: List<DownloadAndInstallState>,
    requested: Set<String> = emptySet(),
): RemoteItemState {
    downloads.statusFor(remote.downloadLink)?.let { return RemoteItemState.Installing(it) }
    val queued = remote.downloadLink in requested
    val match = remote.findInstalled(installed)
        ?: return if (queued) RemoteItemState.Installing(Queued) else RemoteItemState.NotInstalled
    val version = versionOf(match)
    return when {
        !AppUpdate.checkForUpdate(version, remote.version) -> RemoteItemState.Installed(match)
        queued -> RemoteItemState.Installing(Queued)
        else -> RemoteItemState.UpdateAvailable(match, version)
    }
}

fun installedItems(
    installed: List<KmpSourceInformation>,
    remotes: List<KmpRemoteSources>,
    versionOf: (KmpSourceInformation) -> String,
    downloads: List<DownloadAndInstallState>,
    requested: Set<String> = emptySet(),
): List<InstalledItem> = installed
    .distinctBy { it.packageName.ifBlank { it.name } }
    .map { info ->
        val version = versionOf(info)
        val update = info.findRemote(remotes)?.takeIf { AppUpdate.checkForUpdate(version, it.version) }
        val installing = update?.let { downloads.statusFor(it.downloadLink) ?: Queued.takeIf { _ -> it.downloadLink in requested } }
        InstalledItem(info, version, update, installing)
    }

fun List<InstalledItem>.summary() = InstalledSummary(
    installed = size,
    updates = count { it.update != null },
    untrusted = count { it.untrusted != null },
)

fun List<InstalledItem>.filteredBy(filter: InstalledFilter): List<InstalledItem> = when (filter) {
    InstalledFilter.None -> this
    InstalledFilter.Updates -> filter { it.update != null }
    InstalledFilter.Untrusted -> filter { it.untrusted != null }
}

fun List<InstalledItem>.byCatalog(): Map<String?, List<InstalledItem>> = groupBy { it.info.catalog?.name }

/** Download links for every pending update not already downloading/installing; failed ones are retried. */
fun List<InstalledItem>.updateLinks(): List<String> = updateRemotes().map { it.downloadLink }

/** Same selection as [updateLinks], but the remote entries themselves. */
fun List<InstalledItem>.updateRemotes(): List<KmpRemoteSources> =
    filter { it.installing == null || it.installing is DownloadAndInstallStatus.Error }.mapNotNull { it.update }

/**
 * Offers [remote] to each catalog in turn; the first that installs it itself wins. False means fall back to the APK installer.
 * A catalog that throws still counts as the owner (its link is not an APK), so the failure is logged and no fallback happens.
 */
suspend fun installViaCatalogs(catalogs: List<KmpExternalCustomApiServicesCatalog>, remote: KmpRemoteSources): Boolean =
    catalogs.any {
        try {
            it.install(remote)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

/** A catalog group can only be uninstalled as a unit when it is one APK (e.g. a bridge). */
fun List<KmpSourceInformation>.canUninstallAll(): Boolean = map { it.packageName }.distinct().size == 1

fun catalogUi(
    states: Map<String, RemoteState>,
    installed: List<KmpSourceInformation>,
    versionOf: (KmpSourceInformation) -> String,
    downloads: List<DownloadAndInstallState>,
    requested: Set<String> = emptySet(),
): Map<String, CatalogUi> = states.mapValues { (name, state) ->
    when (state) {
        is RemoteViewState -> CatalogUi.Loaded(
            state.sources
                .newestPerPackage()
                .map { RemoteItem(it, name, resolveState(it, installed, versionOf, downloads, requested)) }
                .shelfOrder()
        )

        is RemoteErrorState -> CatalogUi.Error(state.message)
        RemoteLoadingState -> CatalogUi.Loading
    }
}

/** A catalog can merge several repos that ship the same package; keep one entry (the newest) per package. */
private fun List<KmpRemoteSources>.newestPerPackage(): List<KmpRemoteSources> =
    groupBy { it.packageName.ifBlank { it.downloadLink } }
        .values
        .map { same -> same.reduce { best, next -> if (AppUpdate.checkForUpdate(best.version, next.version)) next else best } }

private fun RemoteItemState.rank() = when (this) {
    is RemoteItemState.UpdateAvailable -> 0
    is RemoteItemState.Installing -> 1
    RemoteItemState.NotInstalled -> 2
    is RemoteItemState.Installed -> 3
}

fun List<RemoteItem>.shelfOrder(): List<RemoteItem> =
    sortedWith(compareBy<RemoteItem> { it.state.rank() }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.remote.name })

fun List<RemoteItem>.search(query: String): List<RemoteItem> {
    val q = query.trim()
    return if (q.isEmpty()) this else filter { it.remote.name.contains(q, ignoreCase = true) }
}

/** Distinct non-blank languages, "all" first. */
fun List<RemoteItem>.languages(): List<String> =
    map { it.remote.lang }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy<String> { it != "all" }.thenBy { it })

fun List<RemoteItem>.filterCatalog(lang: String?, hideNsfw: Boolean): List<RemoteItem> =
    filter { (lang == null || it.remote.lang == lang) && !(hideNsfw && it.remote.nsfw) }

fun displayLang(lang: String): String = if (lang == "all") "Multi" else lang
