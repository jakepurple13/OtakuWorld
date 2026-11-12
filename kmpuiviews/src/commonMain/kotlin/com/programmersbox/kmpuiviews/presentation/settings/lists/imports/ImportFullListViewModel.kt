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
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.utils.printLogs
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImportFullListViewModel(
    private val listRepository: ListRepository,
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

    /**
     * Imports each list under a new uuid, like [ImportListViewModel], so an existing or
     * soft-deleted list with the exported uuid can't make the insert silently do nothing.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun importList(): Result<Unit> = runCatching {
        importingList.forEach {
            val newUUID = Uuid.random().toString()
            listRepository.createList(it.item.copy(uuid = newUUID))
            it.list.forEach { item -> listRepository.addItem(item.copy(uniqueId = Uuid.random().toString(), uuid = newUUID)) }
        }
    }
        .onSuccess { printLogs { "Read!" } }
        .onFailure { it.printStackTrace() }
}

sealed class ImportFullListStatus {
    data object Loading : ImportFullListStatus()
    class Error(val throwable: Throwable) : ImportFullListStatus()
    class Success(val customList: List<CustomList>) : ImportFullListStatus()
}