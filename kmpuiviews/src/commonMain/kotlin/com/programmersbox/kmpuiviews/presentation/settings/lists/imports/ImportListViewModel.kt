package com.programmersbox.kmpuiviews.presentation.settings.lists.imports

import androidx.compose.runtime.getValue
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
import com.programmersbox.kmpuiviews.utils.printLogs
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImportListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val document = savedStateHandle
        .toRoute<Screen.ImportListScreen>()
        .uri

    var importStatus: ImportListStatus by mutableStateOf(ImportListStatus.Loading)
        private set

    init {
        viewModelScope.launch {
            importStatus = runCatching {
                /*context.contentResolver.openInputStream(document!!)
                    ?.use { inputStream -> BufferedReader(InputStreamReader(inputStream)).readText() }
                    .fromJson<CustomList>()
                    .let { requireNotNull(it) }*/
                readPlatformFile(document)
                    .readString()
                    .let { Json.decodeFromString<CustomList>(it) }
                    .let { requireNotNull(it) }
            }.fold(
                onSuccess = { ImportListStatus.Success(it) },
                onFailure = { ImportListStatus.Error(it) }
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    suspend fun importList(name: String) {
        runCatching {
            (importStatus as? ImportListStatus.Success)?.customList?.let { list ->
                val newUUID = Uuid.random().toString()
                listDao.createList(list.item.copy(uuid = newUUID, name = name, time = Clock.System.now().toEpochMilliseconds()))
                list.list.forEach { listDao.addItem(it.copy(uniqueId = Uuid.random().toString(), uuid = newUUID)) }
            }
        }
            .onSuccess { printLogs { "Read!" } }
            .onFailure { it.printStackTrace() }
    }
}

sealed class ImportListStatus {
    data object Loading : ImportListStatus()
    class Error(val throwable: Throwable) : ImportListStatus()
    class Success(val customList: CustomList?) : ImportListStatus()
}