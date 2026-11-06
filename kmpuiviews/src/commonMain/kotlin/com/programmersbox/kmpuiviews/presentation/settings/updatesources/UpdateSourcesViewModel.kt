package com.programmersbox.kmpuiviews.presentation.settings.updatesources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SkippedUpdateSource
import com.programmersbox.favoritesdatabase.SourceCount
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One source that has favorites, as shown on the update sources screen. [source] is the service name. */
data class UpdateSourceRow(
    val source: String,
    val name: String,
    val favorites: Int,
    val skipped: Boolean,
    val failedLastCheck: Boolean,
)

/** Emits (checked, total) for sources that have favorites. */
fun ItemDao.updateSourcesCheckedCount(): Flow<Pair<Int, Int>> =
    combine(getFavoriteCountsBySource(), getSkippedUpdateSources()) { counts, skipped ->
        counts.count { it.source !in skipped } to counts.size
    }

/** Builds one row per source with favorites, sorted by name. */
fun buildUpdateSourceRows(
    counts: List<SourceCount>,
    skipped: Set<String>,
    failedSources: Set<String>,
    displayName: (String) -> String,
): List<UpdateSourceRow> = counts
    .map {
        UpdateSourceRow(
            source = it.source,
            name = displayName(it.source),
            favorites = it.count,
            skipped = it.source in skipped,
            failedLastCheck = it.source in failedSources,
        )
    }
    .sortedBy { it.name.lowercase() }

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
        // Re-map names once sources finish loading.
        sourceRepository.sources,
    ) { counts, skipped, report, _ ->
        buildUpdateSourceRows(
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
