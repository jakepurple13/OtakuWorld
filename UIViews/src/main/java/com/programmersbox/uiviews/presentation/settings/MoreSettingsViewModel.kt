package com.programmersbox.uiviews.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.sharedutils.FirebaseDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

class MoreSettingsViewModel(
    private val dao: ItemDao,
) : ViewModel() {

    var importExportListStatus: ImportExportListStatus by mutableStateOf(ImportExportListStatus.Idle)

    fun exportFavorites() = listOf(
        dao.getAllFavoritesSync(),
        FirebaseDb.getAllShows().requireNoNulls()
    )
        .flatten()
        .groupBy(DbModel::url)
        .map { it.value.fastMaxBy(DbModel::numChapters)!! }

    fun importFavorites(document: Uri, context: Context) {
        viewModelScope.launch {
            importExportListStatus = ImportExportListStatus.Loading
            runCatching {
                context.contentResolver.openInputStream(document)
                    ?.use { inputStream -> BufferedReader(InputStreamReader(inputStream)).readText() }
                    .fromJson<List<DbModel>>()
            }
                .onSuccess { list ->
                    list?.forEach {
                        dao.insertFavorite(it)
                        FirebaseDb.insertShowFlow(it)
                    }
                    importExportListStatus = ImportExportListStatus.Success
                }
                .onFailure {
                    it.printStackTrace()
                    importExportListStatus = ImportExportListStatus.Error(it)
                }
        }
    }

    fun writeToFile(document: Uri, context: Context) {
        importExportListStatus = ImportExportListStatus.Loading
        runCatching {
            viewModelScope.launch {
                try {
                    val exportFavorites = withContext(Dispatchers.IO) { exportFavorites() }
                    context.contentResolver.openFileDescriptor(document, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { f ->
                            f.write(exportFavorites.toJson().toByteArray())
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
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
}

sealed class ImportExportListStatus {
    data object Idle : ImportExportListStatus()
    data object Loading : ImportExportListStatus()
    class Error(val throwable: Throwable) : ImportExportListStatus()
    data object Success : ImportExportListStatus()
}