package com.programmersbox.uiviews.presentation.lists

import android.content.Context
import android.net.Uri
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
import com.programmersbox.gsonutils.toJson
import com.programmersbox.uiviews.datastore.DataStoreHandler
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class OtakuCustomListViewModel(
    private val listDao: ListDao,
    private val showBySourceFlow: DataStoreHandler<Boolean>,
) : ViewModel() {
    var customItem: CustomList? by mutableStateOf(null)

    var customList by mutableStateOf<CustomList?>(null)

    val filtered = mutableStateListOf<String>()
    var showBySource by mutableStateOf(false)

    var searchBarActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    val items by derivedStateOf {
        customList?.let {
            when {
                showBySource -> OtakuListState.BySource(it, searchQuery, filtered)
                else -> OtakuListState.ByTitle(it, searchQuery, filtered)
            }
        } ?: OtakuListState.Empty
    }

    val searchItems by derivedStateOf {
        customList
            ?.list
            ?.filter { it.title.contains(searchQuery, ignoreCase = true) }
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
    }

    fun toggleShowSource(context: Context, value: Boolean) {
        viewModelScope.launch {
            //context.updatePref(SHOW_BY_SOURCE, value)
            showBySourceFlow.set(value)
        }
    }

    fun setList(customList: CustomList?) {
        this.customItem = customList
        val sources = customList?.list
            ?.map { it.source }
            .orEmpty()
            .distinct()
        filtered.clear()
        filtered.addAll(sources)
        searchQuery = ""
        searchBarActive = false
    }

    fun filter(source: String) {
        if (filtered.contains(source)) filtered.removeIf { t -> t == source }
        else filtered.add(source)
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
        searchQuery = query
    }

    fun writeToFile(document: Uri, context: Context) {
        runCatching {
            viewModelScope.launch {
                try {
                    context.contentResolver.openFileDescriptor(document, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { f ->
                            f.write(customList?.toJson()?.toByteArray())
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
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

    data class BySource(val items: List<Map.Entry<String, List<CustomListInfo>>>) : OtakuListState() {
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