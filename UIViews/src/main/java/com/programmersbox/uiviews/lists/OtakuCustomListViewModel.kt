package com.programmersbox.uiviews.lists

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.gsonutils.toJson
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID


class OtakuCustomListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val uuid = savedStateHandle.get<String>("uuid")?.let(UUID::fromString)

    var customItem: CustomList? by mutableStateOf(null)
        private set

    var searchBarActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    val items by derivedStateOf {
        customItem?.list
            .orEmpty()
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.title }
            .entries
            .toList()
    }

    init {
        uuid?.let(listDao::getCustomListItemFlow)
            ?.onEach { customItem = it }
            ?.launchIn(viewModelScope)
    }

    fun removeItem(item: CustomListInfo) {
        viewModelScope.launch {
            listDao.removeItem(item)
            viewModelScope.launch { customItem?.item?.let { listDao.updateFullList(it) } }
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch { customItem?.item?.copy(name = newName)?.let { listDao.updateFullList(it) } }
    }

    fun deleteAll() {
        viewModelScope.launch { customItem?.let { item -> listDao.removeList(item) } }
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
                            f.write(customItem?.toJson()?.toByteArray())
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