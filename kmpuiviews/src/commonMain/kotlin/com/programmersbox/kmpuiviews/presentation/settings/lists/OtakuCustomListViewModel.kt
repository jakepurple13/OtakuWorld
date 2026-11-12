package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.resolvedCoverImageUrl
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.AiTextRunner
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.utils.printLogs
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class OtakuCustomListViewModel(
    screen: Screen.CustomListScreen.CustomListItem,
    private val listDao: ListDao,
    private val listRepository: ListRepository,
    private val aiText: AiText,
) : ViewModel() {
    var customItem: CustomList? by mutableStateOf(null)

    var customList by mutableStateOf<CustomList?>(null)

    val filtered = mutableStateListOf<String>()

    var sort by mutableStateOf(ListSort.RecentlyAdded)
    var viewMode by mutableStateOf(ListViewMode.Grid)

    var searchQuery by mutableStateOf(TextFieldState())

    val items by derivedStateOf {
        customList?.let {
            when (viewMode) {
                ListViewMode.BySource -> OtakuListState.BySource(it, searchQuery.text.toString(), filtered, sort)
                ListViewMode.Grid -> OtakuListState.ByTitle(it, searchQuery.text.toString(), filtered, sort)
            }
        } ?: OtakuListState.Empty
    }

    val sources by derivedStateOf {
        customList
            ?.list
            .orEmpty()
            .groupingBy { it.source }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    val allSourcesShown by derivedStateOf { sources.all { it.first in filtered } }

    private val selectedIds = mutableStateListOf<String>()

    val selectedItems by derivedStateOf {
        customList?.list.orEmpty().filter { it.uniqueId in selectedIds }
    }

    val inSelectionMode by derivedStateOf { selectedIds.isNotEmpty() }

    val coverImageUrl by derivedStateOf { customList?.resolvedCoverImageUrl().orEmpty() }

    val searchItems by derivedStateOf {
        customList
            ?.list
            ?.filter { it.title.contains(searchQuery.text.toString(), ignoreCase = true) }
            ?.filter { filtered.contains(it.source) }
            .orEmpty()
    }

    init {
        snapshotFlow { customItem }
            .flatMapMerge {
                if (it == null) flowOf(null)
                else listDao.getCustomListItemFlow(it.item.uuid)
            }
            .onEach { customList = it }
            .launchIn(viewModelScope)

        listDao
            .getCustomListItemFlow(screen.uuid)
            .onEach { setList(it) }
            .launchIn(viewModelScope)
    }

    fun setList(customList: CustomList?) {
        // Keep a narrowed source filter across list updates (like removing items).
        val previous = if (allSourcesShown) emptyList() else filtered.toList()
        this.customList = customList
        val sources = customList
            ?.list
            ?.map { it.source }
            .orEmpty()
            .distinct()
        val kept = previous.filter { it in sources }
        filtered.clear()
        filtered.addAll(kept.ifEmpty { sources })
        searchQuery = TextFieldState()
    }

    /**
     * With every source shown, picking one narrows to just that source.
     * After that, sources toggle, and deselecting the last one shows everything again.
     */
    fun selectSource(source: String) {
        when {
            allSourcesShown -> {
                filtered.clear()
                filtered.add(source)
            }

            source in filtered -> {
                filtered.remove(source)
                if (filtered.isEmpty()) clearFilter()
            }

            else -> filtered.add(source)
        }
    }

    fun clearFilter() {
        filtered.clear()
        filtered.addAll(customList?.list?.map { it.source }.orEmpty())
    }

    suspend fun removeItems(items: List<CustomListInfo>): Result<Boolean> = runCatching {
        items.forEach { item -> listRepository.removeItem(item) }
        customList?.item?.let { listRepository.updateFullList(it) }
        true
    }

    fun updateDetails(name: String, description: String) {
        viewModelScope.launch {
            customList
                ?.item
                ?.copy(name = name, description = description)
                ?.let { listRepository.updateFullList(it) }
        }
    }

    /** True when an on-device engine is ready and the list has enough titles to say something about. */
    val suggestAvailable: StateFlow<Boolean> = snapshotFlow { customList?.list?.size ?: 0 }
        .map { it >= MIN_SUGGEST_TITLES && aiText.engineFor(AiFeature.ListSuggest) != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val nameRunner = AiTextRunner(viewModelScope)
    private val descriptionRunner = AiTextRunner(viewModelScope)
    private var nameJob: Job? = null
    private var descriptionJob: Job? = null

    /** Done.text is the parsed names, newline-joined. */
    val nameSuggestions: StateFlow<AiTextState> = nameRunner.state
        .map { state ->
            if (state is AiTextState.Done) {
                state.copy(text = AiTextPrompts.parseNames(state.text).joinToString("\n"))
            } else state
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiTextState.Idle)

    val descriptionDraft: StateFlow<AiTextState> = descriptionRunner.state

    fun suggestNames() {
        nameJob?.cancel()
        nameRunner.reset()
        nameJob = suggest(nameRunner) { aiText.listNames(it) }
    }

    fun draftDescription() {
        descriptionJob?.cancel()
        descriptionRunner.reset()
        descriptionJob = suggest(descriptionRunner) { aiText.listDescription(it) }
    }

    /** Drops any suggestions. Nothing here is saved; saving only goes through [updateDetails]. */
    fun clearDrafts() {
        nameJob?.cancel()
        descriptionJob?.cancel()
        nameJob = null
        descriptionJob = null
        nameRunner.reset()
        descriptionRunner.reset()
    }

    private fun suggest(runner: AiTextRunner, block: (List<String>) -> Flow<String>): Job {
        val titles = customList?.list
            .orEmpty()
            .take(AiTextPrompts.MAX_LIST_TITLES)
            .map { "${it.title} (${it.source})" }
        return viewModelScope.launch {
            val engine = aiText.engineFor(AiFeature.ListSuggest) ?: return@launch
            // Still on Main, so a clearDrafts either cancels this before the run or resets after it.
            ensureActive()
            runner.run(engine) { block(titles) }
        }
    }

    private fun visibleItems(): List<CustomListInfo> = when (val state = items) {
        is OtakuListState.ByTitle -> state.items
        is OtakuListState.BySource -> state.items.flatMap { it.value }
        OtakuListState.Empty -> emptyList()
    }

    fun toggleSelection(group: List<CustomListInfo>) {
        val ids = group.map { it.uniqueId }
        if (selectedIds.containsAll(ids)) selectedIds.removeAll(ids)
        else selectedIds.addAll(ids.filterNot { it in selectedIds })
    }

    fun isSelected(group: List<CustomListInfo>) = group.isNotEmpty() && group.all { it.uniqueId in selectedIds }

    fun selectAllVisible() {
        selectedIds.addAll(visibleItems().map { it.uniqueId }.filterNot { it in selectedIds })
    }

    fun clearSelection() = selectedIds.clear()

    suspend fun removeSelected(): Result<Boolean> = removeItems(selectedItems).onSuccess { clearSelection() }

    /** Copies the selected items into each of [targets]. Returns how many were new across them. */
    suspend fun addSelectedTo(targets: List<CustomList>): Result<Int> = runCatching {
        targets
            .sumOf { target ->
                selectedItems.count { item ->
                    listRepository.addToList(
                        uuid = target.item.uuid,
                        title = item.title,
                        description = item.description,
                        url = item.url,
                        imageUrl = item.imageUrl,
                        source = item.source,
                    )
                }
            }
            .also { clearSelection() }
    }

    /** A random item from what is currently visible. */
    fun randomPick(): List<CustomListInfo>? = when (val state = items) {
        is OtakuListState.ByTitle -> state.items.randomOrNull()?.let(::listOf)
        is OtakuListState.BySource -> state.items.flatMap { it.value }.randomOrNull()?.let(::listOf)
        OtakuListState.Empty -> null
    }

    fun updateCoverImage(coverImageUrl: String) {
        viewModelScope.launch { customList?.item?.uuid?.let { listRepository.updateCoverImage(it, coverImageUrl) } }
    }

    fun deleteAll() {
        viewModelScope.launch { customList?.let { item -> listRepository.removeList(item) } }
    }

    fun setQuery(query: String) {
        searchQuery = TextFieldState(query)
    }

    fun writeToFile(document: PlatformFile) {
        runCatching {
            viewModelScope.launch {
                runCatching {
                    if (!document.exists()) document.createDirectories()
                    customList
                        ?.let { listOf(it) }
                        ?.let { Json.encodeToString(it) }
                        ?.let { document.writeString(it) }
                }.onFailure { it.printStackTrace() }
                /*try {
                    context.contentResolver.openFileDescriptor(document, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { f ->
                            customList
                                ?.let { listOf(it) }
                                ?.let { Json.encodeToString(it).toByteArray() }
                                ?.let { f.write(it) }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }*/
            }
        }
            .onSuccess { printLogs { "Written!" } }
            .onFailure { it.printStackTrace() }
    }
}

private const val MIN_SUGGEST_TITLES = 3

enum class ListSort { RecentlyAdded, Title, Source }

enum class ListViewMode { Grid, BySource }

/**
 * Items added before createdAt was recorded have 0, so they fall back to list order,
 * which is insertion order. Either way, newest comes first.
 */
private fun List<CustomListInfo>.sortedForList(
    sort: ListSort,
    order: Map<String, Int>,
): List<CustomListInfo> = when (sort) {
    ListSort.RecentlyAdded -> sortedWith(
        compareByDescending<CustomListInfo> { it.createdAt }
            .thenByDescending { order[it.uniqueId] ?: 0 }
    )

    ListSort.Title -> sortedBy { it.title.lowercase() }
    ListSort.Source -> sortedWith(
        compareBy<CustomListInfo> { it.source.lowercase() }
            .thenBy { it.title.lowercase() }
    )
}

private fun CustomList.insertionOrder() = list.withIndex().associate { it.value.uniqueId to it.index }

private fun CustomList.visible(searchQuery: String, filtered: List<String>) = list
    .filter { it.title.contains(searchQuery, ignoreCase = true) }
    .filter { filtered.contains(it.source) }

sealed class OtakuListState {
    data class ByTitle(val items: List<CustomListInfo>) : OtakuListState() {
        constructor(
            customList: CustomList,
            searchQuery: String,
            filtered: List<String>,
            sort: ListSort = ListSort.RecentlyAdded,
        ) : this(
            customList
                .visible(searchQuery, filtered)
                .sortedForList(sort, customList.insertionOrder())
        )
    }

    data class BySource(
        val items: List<Map.Entry<String, List<CustomListInfo>>>,
        val sourceShower: Map<String, MutableState<Boolean>> = items
            .associate { it.key to mutableStateOf(false) },
    ) : OtakuListState() {
        constructor(
            customList: CustomList,
            searchQuery: String,
            filtered: List<String>,
            sort: ListSort = ListSort.RecentlyAdded,
        ) : this(
            customList
                .visible(searchQuery, filtered)
                .groupBy { it.source }
                .mapValues { (_, sourceItems) ->
                    sourceItems.sortedForList(sort, customList.insertionOrder())
                }
                .entries
                .toList()
                .sortedByDescending { it.value.size }
        )
    }

    data object Empty : OtakuListState()
}
