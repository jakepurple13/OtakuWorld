package com.programmersbox.kmpuiviews.presentation.settings.updatesources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SkippedUpdateSource
import com.programmersbox.favoritesdatabase.SourceCount
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One extension installed in this app, as shown on the update sources screen. Covers every language the extension
 * installs; [languages] is how many sources it has. [favorites] and [failedLastCheck] add up across them.
 */
data class UpdateSourceRow(
    val packageName: String,
    val name: String,
    val languages: Int,
    val favorites: Int,
    val skipped: Boolean,
    val failedLastCheck: Boolean,
)

/**
 * Emits (checked, total) extensions installed in this app. Skipped entries for extensions that aren't installed
 * (e.g. another app's, or an uninstalled extension) don't count.
 */
fun ItemDao.updateSourcesCheckedCount(sourceRepository: SourceRepository): Flow<Pair<Int, Int>> =
    combine(getSkippedUpdateSources(), sourceRepository.sources) { skipped, sources ->
        val installed = sources.mapTo(HashSet()) { it.packageName }
        installed.count { it !in skipped } to installed.size
    }

/** Builds one row per installed extension (grouped by package name), sorted by name. */
fun buildUpdateSourceRows(
    installed: List<KmpSourceInformation>,
    counts: List<SourceCount>,
    skipped: Set<String>,
    failedSources: Set<String>,
    displayName: (List<KmpSourceInformation>) -> String,
): List<UpdateSourceRow> {
    val favoritesBySource = counts.associate { it.source to it.count }
    return installed
        .groupBy { it.packageName }
        .map { (packageName, sources) ->
            val serviceNames = sources.map { it.apiService.serviceName }
            UpdateSourceRow(
                packageName = packageName,
                name = displayName(sources),
                languages = sources.size,
                favorites = serviceNames.sumOf { favoritesBySource[it] ?: 0 },
                skipped = packageName in skipped,
                failedLastCheck = serviceNames.any { it in failedSources },
            )
        }
        .sortedBy { it.name.lowercase() }
}

/** The extension's name: the source chooser's group name for several languages, else the source's display name. */
private fun SourceRepository.extensionName(sources: List<KmpSourceInformation>): String {
    val first = sources.first()
    return if (sources.size > 1) {
        first.name.removeSuffix(" (${first.apiService.language})")
    } else {
        displayNameFor(first.apiService.serviceName)
    }
}

class UpdateSourcesViewModel(
    private val itemDao: ItemDao,
    sourceRepository: SourceRepository,
    reportStore: UpdateCheckReportStore,
) : ViewModel() {

    /** Null until the first load finishes. */
    val rows: StateFlow<List<UpdateSourceRow>?> = combine(
        itemDao.getFavoriteCountsBySource(),
        itemDao.getSkippedUpdateSources(),
        reportStore.report,
        // Rows are the extensions installed in this app; re-emits as sources finish loading.
        sourceRepository.sources,
    ) { counts, skipped, report, sources ->
        buildUpdateSourceRows(
            installed = sources,
            counts = counts,
            skipped = skipped.toSet(),
            failedSources = report?.sourceFailures.orEmpty().mapTo(HashSet()) { it.source },
            displayName = sourceRepository::extensionName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Skips or checks the extensions with these package names, in every language. */
    suspend fun setSkipped(packageNames: List<String>, skipped: Boolean) {
        if (skipped) {
            itemDao.insertSkippedUpdateSources(packageNames.map(::SkippedUpdateSource))
        } else {
            itemDao.deleteSkippedUpdateSources(packageNames)
        }
    }
}
