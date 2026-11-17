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
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.repository.FavoriteSearchHit
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.FavoritesSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
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
private const val MIN_DESCRIBE_WORDS = 4
// Japanese and Chinese have no spaces, so count characters there instead
private const val MIN_DESCRIBE_CJK_CHARS = 8
private val describeWord = Regex("[\\p{L}\\p{N}\\p{M}]+")
// Hiragana, Katakana, CJK ideographs (incl. extension A) and Hangul syllables; plain ranges so every KMP target supports it
private val cjkChar = Regex("[\\u3040-\\u30FF\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uAC00-\\uD7AF]")

/** True for a query that reads like a sentence: 4+ words, or 8+ CJK characters. */
internal fun looksLikeDescription(text: String): Boolean =
    describeWord.findAll(text).count() >= MIN_DESCRIBE_WORDS || cjkChar.findAll(text).count() >= MIN_DESCRIBE_CJK_CHARS
private const val RECENT_SHELF_SIZE = 10

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FavoriteViewModel(
    sourceRepository: SourceRepository,
    favoritesRepository: FavoritesRepository,
    searchRepository: FavoritesSearchRepository,
    private val aiText: AiText,
) : ViewModel() {

    private var favorites by mutableStateOf<List<DbModel>>(emptyList())
    private var installedSources by mutableStateOf(sourceRepository.list.map { it.apiService.serviceName })
    private var searchHits by mutableStateOf<List<FavoriteSearchHit>>(emptyList())

    val searchText = TextFieldState()

    /** The query the current [sections] were built from (after debounce). */
    var searchQuery by mutableStateOf("")
        private set

    /** AI keywords for a described query; search matches any of them. Null for normal word search. */
    var keywords by mutableStateOf<List<String>?>(null)
        private set

    /** True while the AI turns the query into [keywords]. */
    var describing by mutableStateOf(false)
        private set

    private var aiReady by mutableStateOf(false)

    // The query the current keywords came from; typing anything else drops them
    private var describedText: String? = null
    private var describeJob: Job? = null
    // The query a running describe was started for
    private var describingText: String? = null

    /** True when the query reads like a description and an engine is ready. */
    val canDescribe by derivedStateOf {
        aiReady && !describing && keywords == null &&
            looksLikeDescription(searchText.text.toString())
    }

    var sort by mutableStateOf(FavoriteSort.TITLE)

    var quickFilters by mutableStateOf<Set<QuickFilter>>(emptySet())
        private set

    /** Empty means every source. */
    var selectedSources by mutableStateOf<Set<String>>(emptySet())
        private set

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private val groups by derivedStateOf { groupFavorites(favorites) }

    private val filtered by derivedStateOf { filterGroups(groups, quickFilters, selectedSources, now()) }

    val isSearching by derivedStateOf { keywords != null || ftsMatch(searchQuery) != null }

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

        aiText.availability(AiFeature.SearchKeywords)
            .onEach { aiReady = it != null }
            .launchIn(viewModelScope)

        snapshotFlow { searchText.text.toString() }
            .onEach { text ->
                if (keywords != null && text != describedText) {
                    keywords = null
                    describedText = null
                }
                // The user moved on: stop the AI so the button comes back and the shared AI lock frees up
                if (describing && text != describingText) {
                    describeJob?.cancel()
                    describeJob = null
                    describing = false
                }
            }
            .launchIn(viewModelScope)

        // Only typing is debounced; keywords come from a tap and should show right away
        combine(
            snapshotFlow { searchText.text.toString() }.debounce(SEARCH_DEBOUNCE_MS),
            snapshotFlow { keywords },
        ) { query, keywords -> query to keywords }
            .flatMapLatest { (query, keywords) ->
                val hits = if (keywords != null) searchRepository.searchAny(keywords) else searchRepository.search(query)
                hits.map { query to it }
            }
            .onEach { (query, hits) ->
                searchHits = hits
                searchQuery = query
            }
            .launchIn(viewModelScope)
    }

    /** Asks the AI for keywords from the current query. On failure the normal search stays. */
    fun describe() {
        val query = searchText.text.toString()
        if (describing) return
        describing = true
        describingText = query
        describeJob = viewModelScope.launch {
            try {
                val raw = aiText.run(AiFeature.SearchKeywords, query).fold("") { acc, delta -> acc + delta }
                val found = AiFeature.SearchKeywords.finish(raw).lines().filter { it.isNotBlank() }
                // Only apply them if the user hasn't typed something else meanwhile
                if (found.isNotEmpty() && searchText.text.toString() == query) {
                    describedText = query
                    keywords = found
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the normal word search
            } finally {
                if (describingText == query) describing = false
            }
        }
    }

    fun removeKeyword(keyword: String) {
        val left = keywords.orEmpty() - keyword
        keywords = left.takeIf { it.isNotEmpty() }
        if (keywords == null) describedText = null
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
