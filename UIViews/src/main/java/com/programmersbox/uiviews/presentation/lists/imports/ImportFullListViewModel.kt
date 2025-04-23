package com.programmersbox.uiviews.presentation.lists.imports

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportFullListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
    context: Context,
) : ViewModel() {

    private val document = savedStateHandle
        .toRoute<Screen.ImportFullListScreen>()
        .uri
        .let(Uri::parse)

    val importingList = mutableStateListOf<CustomList>()

    var importStatus: ImportFullListStatus by mutableStateOf(ImportFullListStatus.Loading)
        private set

    init {
        viewModelScope.launch {
            importStatus = runCatching {
                context.contentResolver.openInputStream(document!!)
                    ?.use { inputStream -> BufferedReader(InputStreamReader(inputStream)).readText() }
                    ?.let { Json.decodeFromString<List<CustomList>>(it) }
                    .let { requireNotNull(it) }
            }.fold(
                onSuccess = {
                    importingList.addAll(it)
                    ImportFullListStatus.Success(it)
                },
                onFailure = { ImportFullListStatus.Error(it) }
            )
        }
    }

    suspend fun importList() {
        runCatching {
            importingList.let { list ->
                list.forEach {
                    listDao.createList(it.item)
                    it.list.forEach { item -> listDao.addItem(item) }
                }
            }
        }
            .onSuccess { println("Read!") }
            .onFailure { it.printStackTrace() }
    }
}

sealed class ImportFullListStatus {
    data object Loading : ImportFullListStatus()
    class Error(val throwable: Throwable) : ImportFullListStatus()
    class Success(val customList: List<CustomList>) : ImportFullListStatus()
}