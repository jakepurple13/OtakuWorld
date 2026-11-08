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

/** One source installed in this app, as shown on the update sources screen. [source] is the service name. */
data class UpdateSourceRow(
    val source: String,
    val name: String,
    val favorites: Int,
    val skipped: Boolean,
    val failedLastCheck: Boolean,
)

/**
 * Emits (checked, total) for the sources installed in this app. Skipped entries for sources that aren't
 * installed (e.g. another app's, or an uninstalled extension) don't count.
 */
fun ItemDao.updateSourcesCheckedCount(sourceRepository: SourceRepository): Flow<Pair<Int, Int>> =
    combine(getSkippedUpdateSources(), sourceRepository.sources) { skipped, sources ->
        val installed = sources.serviceNames()
        installed.count { it !in skipped } to installed.size
    }

private fun List<KmpSourceInformation>.serviceNames(): Set<String> = mapTo(LinkedHashSet()) { it.apiService.serviceName }

/** Builds one row per installed source, with its favorite count (0 if none), sorted by name. */
fun buildUpdateSourceRows(
    installed: Set<String>,
    counts: List<SourceCount>,
    skipped: Set<String>,
    failedSources: Set<String>,
    displayName: (String) -> String,
): List<UpdateSourceRow> {
    val favoritesBySource = counts.associate { it.source to it.count }
    return installed
        .map {
            UpdateSourceRow(
                source = it,
                name = displayName(it),
                favorites = favoritesBySource[it] ?: 0,
                skipped = it in skipped,
                failedLastCheck = it in failedSources,
            )
        }
        .sortedBy { it.name.lowercase() }
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
        // Rows are the sources installed in this app; re-emits as sources finish loading.
        sourceRepository.sources,
    ) { counts, skipped, report, sources ->
        buildUpdateSourceRows(
            installed = sources.serviceNames(),
            counts = counts,
            skipped = skipped.toSet(),
            failedSources = report?.sourceFailures.orEmpty().mapTo(HashSet()) { it.source },
            displayName = sourceRepository::displayNameFor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun setSkipped(sources: List<String>, skipped: Boolean) {
        if (skipped) {
            itemDao.insertSkippedUpdateSources(sources.map(::SkippedUpdateSource))
        } else {
            itemDao.deleteSkippedUpdateSources(sources)
        }
    }
}
