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
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.presentation.Screen
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class OtakuCustomListViewModel(
    screen: Screen.CustomListScreen.CustomListItem,
    dataStoreHandling: DataStoreHandling,
    private val listDao: ListDao,
) : ViewModel() {
    private val showBySourceFlow = dataStoreHandling.showBySource
    var customItem: CustomList? by mutableStateOf(null)

    var customList by mutableStateOf<CustomList?>(null)

    val filtered = mutableStateListOf<String>()
    var showBySource by mutableStateOf(false)

    var searchQuery by mutableStateOf(TextFieldState())

    val items by derivedStateOf {
        customList?.let {
            when {
                showBySource -> OtakuListState.BySource(it, searchQuery.text.toString(), filtered)
                else -> OtakuListState.ByTitle(it, searchQuery.text.toString(), filtered)
            }
        } ?: OtakuListState.Empty
    }

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

        showBySourceFlow
            .asFlow()
            .onEach { showBySource = it }
            .launchIn(viewModelScope)

        listDao
            .getCustomListItemFlow(screen.uuid)
            .onEach { setList(it) }
            .launchIn(viewModelScope)
    }

    fun toggleShowSource(value: Boolean) {
        viewModelScope.launch {
            //context.updatePref(SHOW_BY_SOURCE, value)
            showBySourceFlow.set(value)
        }
    }

    fun setList(customList: CustomList?) {
        this.customList = customList
        val sources = customList
            ?.list
            ?.map { it.source }
            .orEmpty()
            .distinct()
        filtered.clear()
        filtered.addAll(sources)
        searchQuery = TextFieldState()
    }

    fun filter(source: String) {
        if (filtered.contains(source)) {
            filtered.removeAll { it == source }
        } else {
            filtered.add(source)
        }
    }

    fun clearFilter() {
        filtered.clear()
        filtered.addAll(customList?.list?.map { it.source }.orEmpty())
    }

    suspend fun removeItems(items: List<CustomListInfo>): Result<Boolean> = runCatching {
        items.forEach { item -> listDao.removeItem(item) }
        customList?.item?.let { listDao.updateFullList(it) }
        true
    }

    fun rename(newName: String) {
        viewModelScope.launch { customList?.item?.copy(name = newName)?.let { listDao.updateFullList(it) } }
    }

    fun deleteAll() {
        viewModelScope.launch { customList?.let { item -> listDao.removeList(item) } }
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
            .onSuccess { println("Written!") }
            .onFailure { it.printStackTrace() }
    }
}

sealed class OtakuListState {
    data class ByTitle(val items: List<Map.Entry<String, List<CustomListInfo>>>) : OtakuListState() {
        constructor(
            customList: CustomList,
            searchQuery: String,
            filtered: List<String>,
        ) : this(
            customList
                .list
                .filter { it.title.contains(searchQuery, ignoreCase = true) }
                .filter { filtered.contains(it.source) }
                .groupBy { it.title }
                .entries
                .toList()
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
        ) : this(
            customList
                .list
                .filter { it.title.contains(searchQuery, ignoreCase = true) }
                .filter { filtered.contains(it.source) }
                .groupBy { it.source }
                .entries
                .toList()
        )
    }

    data object Empty : OtakuListState()
}