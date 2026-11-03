package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ftsMatch
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.repository.FavoriteSearchHit
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.FavoritesSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock

@Immutable
data class FavoritesOverview(
    val titleCount: Int = 0,
    val sourceCount: Int = 0,
    val chapterCount: Int = 0,
    val recent: List<FavoriteGroup> = emptyList(),
    val multiSource: List<FavoriteGroup> = emptyList(),
    val updatesOff: List<FavoriteGroup> = emptyList(),
)

private const val SEARCH_DEBOUNCE_MS = 150L
private const val RECENT_SHELF_SIZE = 10

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FavoriteViewModel(
    sourceRepository: SourceRepository,
    favoritesRepository: FavoritesRepository,
    searchRepository: FavoritesSearchRepository,
) : ViewModel() {

    private var favorites by mutableStateOf<List<DbModel>>(emptyList())
    private var installedSources by mutableStateOf(sourceRepository.list.map { it.apiService.serviceName })
    private var searchHits by mutableStateOf<List<FavoriteSearchHit>>(emptyList())

    val searchText = TextFieldState()

    /** The query the current [sections] were built from (after debounce). */
    var searchQuery by mutableStateOf("")
        private set

    var sort by mutableStateOf(FavoriteSort.TITLE)

    var quickFilters by mutableStateOf<Set<QuickFilter>>(emptySet())
        private set

    /** Empty means every source. */
    var selectedSources by mutableStateOf<Set<String>>(emptySet())
        private set

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private val groups by derivedStateOf { groupFavorites(favorites) }

    private val filtered by derivedStateOf { filterGroups(groups, quickFilters, selectedSources, now()) }

    val isSearching by derivedStateOf { ftsMatch(searchQuery) != null }

    val sections by derivedStateOf {
        if (isSearching) buildSearchSections(filtered, searchHits)
        else buildSections(filtered, sort, now())
    }

    val resultCount by derivedStateOf { sections.sumOf { it.groups.size } }

    val totalCount by derivedStateOf { groups.size }

    val quickCounts by derivedStateOf {
        val now = now()
        QuickFilter.entries.associateWith { filter -> groups.count { filter.matches(it, now) } }
    }

    val activeFilterCount by derivedStateOf { quickFilters.size + if (selectedSources.isEmpty()) 0 else 1 }

    /** Every installed source plus every source that has favorites, most favorites first. */
    val sourceCounts by derivedStateOf {
        val counts = favorites.groupingBy { it.source }.eachCount()
        (installedSources + counts.keys)
            .distinct()
            .map { it to (counts[it] ?: 0) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }

    val overview by derivedStateOf {
        val now = now()
        FavoritesOverview(
            titleCount = groups.size,
            sourceCount = favorites.map { it.source }.distinct().size,
            chapterCount = groups.sumOf { it.maxChapters },
            recent = groups
                .filter { QuickFilter.RECENT.matches(it, now) }
                .sortedByDescending { it.latestCreatedAt }
                .take(RECENT_SHELF_SIZE),
            multiSource = groups.filter { QuickFilter.MULTI_SOURCE.matches(it, now) },
            updatesOff = groups.filter { QuickFilter.UPDATES_OFF.matches(it, now) },
        )
    }

    init {
        favoritesRepository
            .getAllFavoritesFlow()
            .onEach { favorites = it }
            .launchIn(viewModelScope)

        sourceRepository.sources
            .onEach { list -> installedSources = list.map { it.apiService.serviceName } }
            .launchIn(viewModelScope)

        snapshotFlow { searchText.text.toString() }
            .debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest { query -> searchRepository.search(query).map { query to it } }
            .onEach { (query, hits) ->
                searchHits = hits
                searchQuery = query
            }
            .launchIn(viewModelScope)
    }

    fun toggleQuickFilter(filter: QuickFilter) {
        quickFilters = if (filter in quickFilters) quickFilters - filter else quickFilters + filter
    }

    fun removeQuickFilter(filter: QuickFilter) {
        quickFilters = quickFilters - filter
    }

    fun toggleSource(source: String) {
        selectedSources = if (source in selectedSources) selectedSources - source else selectedSources + source
    }

    fun onlySource(source: String) {
        selectedSources = setOf(source)
    }

    fun clearSources() {
        selectedSources = emptySet()
    }

    fun selectAll(visible: List<String>) {
        selectedSources = selectedSources + visible
    }

    fun selectNone(visible: List<String>) {
        selectedSources = selectedSources - visible.toSet()
    }

    fun resetFilters() {
        quickFilters = emptySet()
        selectedSources = emptySet()
    }

    /** Opens Library on what an Overview shelf previews. */
    fun seeAll(filter: QuickFilter) {
        quickFilters = setOf(filter)
        selectedSources = emptySet()
        sort = if (filter == QuickFilter.RECENT) FavoriteSort.ADDED else FavoriteSort.TITLE
    }
}
