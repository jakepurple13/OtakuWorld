package com.programmersbox.uiviews.lists

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.gsonutils.fromJson
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class ImportListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
    context: Context
) : ViewModel() {

    private val document = savedStateHandle.get<String>("uri")?.let(Uri::parse)

    var importStatus: ImportListStatus by mutableStateOf(ImportListStatus.Loading)
        private set

    init {
        viewModelScope.launch {
            importStatus = runCatching {
                context.contentResolver.openInputStream(document!!)
                    ?.use { inputStream -> BufferedReader(InputStreamReader(inputStream)).readText() }
                    .fromJson<CustomList>()
                    .let { requireNotNull(it) }
            }.fold(
                onSuccess = { ImportListStatus.Success(it) },
                onFailure = { ImportListStatus.Error(it) }
            )
        }
    }

    suspend fun importList(name: String) {
        runCatching {
            (importStatus as? ImportListStatus.Success)?.customList?.let { list ->
                val newUUID = UUID.randomUUID()
                listDao.createList(list.item.copy(uuid = newUUID, name = name, time = System.currentTimeMillis()))
                list.list.forEach { listDao.addItem(it.copy(uniqueId = UUID.randomUUID().toString(), uuid = newUUID)) }
            }
        }
            .onSuccess { println("Read!") }
            .onFailure { it.printStackTrace() }
    }
}

sealed class ImportListStatus {
    data object Loading : ImportListStatus()
    class Error(val throwable: Throwable) : ImportListStatus()
    class Success(val customList: CustomList?) : ImportListStatus()
}