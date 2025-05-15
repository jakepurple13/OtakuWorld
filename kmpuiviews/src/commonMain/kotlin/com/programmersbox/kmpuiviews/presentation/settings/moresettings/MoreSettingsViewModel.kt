package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MoreSettingsViewModel(
    private val dao: ItemDao,
    private val favoritesRepository: FavoritesRepository,
    private val listDao: ListDao,
    private val kmpFirebaseConnection: KmpFirebaseConnection,
) : ViewModel() {

    val lists = listDao.getAllLists()

    var importExportListStatus: ImportExportListStatus by mutableStateOf(ImportExportListStatus.Idle)

    suspend fun exportFavorites() = favoritesRepository.getAllFavorites()

    fun importFavorites(document: PlatformFile) {
        viewModelScope.launch {
            importExportListStatus = ImportExportListStatus.Loading
            runCatching { Json.decodeFromString<List<DbModel>>(document.readString()) }
                .onSuccess { list ->
                    list.forEach { favoritesRepository.addFavorite(it) }
                    importExportListStatus = ImportExportListStatus.Success
                }
                .onFailure {
                    it.printStackTrace()
                    importExportListStatus = ImportExportListStatus.Error(it)
                }
        }
    }

    fun writeToFile(document: PlatformFile) {
        importExportListStatus = ImportExportListStatus.Loading
        runCatching {
            viewModelScope.launch {
                runCatching {
                    val exportFavorites = withContext(Dispatchers.IO) { exportFavorites() }
                    if (!document.exists()) document.createDirectories()
                    document.writeString(Json.encodeToString(exportFavorites))
                    /*context.contentResolver.openFileDescriptor(document, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { f ->
                            f.write(exportFavorites.toJson().toByteArray())
                        }
                    }*/
                }.onFailure { it.printStackTrace() }
            }
        }
            .onSuccess {
                println("Written!")
                importExportListStatus = ImportExportListStatus.Success
            }
            .onFailure {
                it.printStackTrace()
                importExportListStatus = ImportExportListStatus.Error(it)
            }
    }

    fun writeListsToFile(document: PlatformFile) {
        importExportListStatus = ImportExportListStatus.Loading
        runCatching {
            viewModelScope.launch {
                runCatching {
                    val exportLists = withContext(Dispatchers.IO) { listDao.getAllListsSync() }
                    if (!document.exists()) document.createDirectories()
                    document.writeString(Json.encodeToString(exportLists))
                }.onFailure { it.printStackTrace() }
            }
        }
            .onSuccess {
                println("Written!")
                importExportListStatus = ImportExportListStatus.Success
            }
            .onFailure {
                it.printStackTrace()
                importExportListStatus = ImportExportListStatus.Error(it)
            }
    }

    fun writeListsToFile(
        document: PlatformFile,
        exportLists: List<CustomList>,
    ) {
        importExportListStatus = ImportExportListStatus.Loading
        viewModelScope.launch {
            runCatching {
                if (!document.exists()) document.createDirectories()
                document.writeString(Json.encodeToString(exportLists))
            }
                .onSuccess {
                    println("Written!")
                    importExportListStatus = ImportExportListStatus.Success
                }
                .onFailure {
                    it.printStackTrace()
                    importExportListStatus = ImportExportListStatus.Error(it)
                }
        }
    }

    var cloudToLocalSync: CloudLocalSync by mutableStateOf(CloudLocalSync.Idle)
    var localToCloudSync: CloudLocalSync by mutableStateOf(CloudLocalSync.Idle)

    fun pullCloudToLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            cloudToLocalSync = CloudLocalSync.Loading
            runCatching {
                val allShows = dao.getAllFavoritesSync()
                val cloudShows = kmpFirebaseConnection.getAllShows()
                val newShows = cloudShows.filter { allShows.any { s -> s.url != it.url } }
                newShows.forEach { dao.insertFavorite(it) }
                newShows.size
            }
                .onSuccess { cloudToLocalSync = CloudLocalSync.Success(it) }
                .onFailure { cloudToLocalSync = CloudLocalSync.Error(it) }
        }
    }

    fun pullLocalToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            localToCloudSync = CloudLocalSync.Loading
            runCatching {
                val allShows = dao.getAllFavoritesSync()
                val cloudShows = kmpFirebaseConnection.getAllShows()
                val newShows = allShows.filter { cloudShows.any { s -> s.url != it.url } }
                newShows.forEach { kmpFirebaseConnection.insertShowFlow(it).collect() }
                newShows.size
            }
                .onSuccess { localToCloudSync = CloudLocalSync.Success(it) }
                .onFailure { localToCloudSync = CloudLocalSync.Error(it) }
        }
    }
}

sealed class ImportExportListStatus {
    data object Idle : ImportExportListStatus()
    data object Loading : ImportExportListStatus()
    class Error(val throwable: Throwable) : ImportExportListStatus()
    data object Success : ImportExportListStatus()
}

sealed class CloudLocalSync {
    data object Idle : CloudLocalSync()
    data object Loading : CloudLocalSync()
    class Error(val throwable: Throwable) : CloudLocalSync()
    data class Success(val size: Int) : CloudLocalSync()
}