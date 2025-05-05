package com.programmersbox.kmpuiviews.presentation.settings.lists.imports

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
import com.programmersbox.kmpuiviews.readPlatformFile
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ImportFullListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val document = savedStateHandle
        .toRoute<Screen.ImportFullListScreen>()
        .uri

    val importingList = mutableStateListOf<CustomList>()

    var importStatus: ImportFullListStatus by mutableStateOf(ImportFullListStatus.Loading)
        private set

    init {
        viewModelScope.launch {
            importStatus = runCatching {
                /*
                context.contentResolver.openInputStream(document!!)
                    ?.use { inputStream -> BufferedReader(InputStreamReader(inputStream)).readText() }
                    ?.let { Json.decodeFromString<List<CustomList>>(it) }
                    .let { requireNotNull(it) }
                 */
                readPlatformFile(document)
                    .readString()
                    .let { Json.decodeFromString<List<CustomList>>(it) }
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