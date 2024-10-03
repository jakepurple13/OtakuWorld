package com.programmersbox.uiviews.lists

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.SecurityItem
import com.programmersbox.gsonutils.toJson
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class OtakuListViewModel(
    private val listDao: ListDao,
) : ViewModel() {
    val customLists = mutableStateListOf<CustomList>()

    var customItem: CustomList? by mutableStateOf(null)

    var searchBarActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    val listBySource by derivedStateOf {
        customItem?.list
            .orEmpty()
            .groupBy { it.source }
    }

    val searchItems by derivedStateOf {
        customItem?.list
            .orEmpty()
            .distinctBy { it.title }
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val items by derivedStateOf {
        customItem?.list
            .orEmpty()
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.title }
            .entries
            .toList()
    }

    val securityItems = mutableStateListOf<SecurityItem>()

    init {
        listDao.getAllLists()
            .onEach {
                customLists.clear()
                customLists.addAll(it)
            }
            .launchIn(viewModelScope)

        listDao.getSecurityItems()
            .onEach {
                println(it)
                securityItems.clear()
                securityItems.addAll(it)
            }
            .launchIn(viewModelScope)
    }

    fun removeItem(item: CustomListInfo) {
        viewModelScope.launch {
            listDao.removeItem(item)
            viewModelScope.launch { customItem?.item?.let { listDao.updateFullList(it) } }
        }
    }

    suspend fun removeItems(items: List<CustomListInfo>): Result<Boolean> = runCatching {
        items.forEach { item -> listDao.removeItem(item) }
        customItem?.item?.let { listDao.updateFullList(it) }
        true
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
