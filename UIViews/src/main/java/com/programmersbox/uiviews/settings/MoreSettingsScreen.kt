package com.programmersbox.uiviews.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.CategorySetting
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.PreferenceSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    dao: ItemDao = LocalItemDao.current,
    viewModel: MoreSettingsViewModel = viewModel { MoreSettingsViewModel(dao) },
) {
    val toaster = rememberToasterState(
        onToastDismissed = { viewModel.importExportListStatus = ImportExportListStatus.Idle }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.importExportListStatus }
            .onEach {
                when (it) {
                    is ImportExportListStatus.Error -> toaster.show(
                        "Error: ${it.throwable.message}",
                        type = ToastType.Error,
                    )

                    ImportExportListStatus.Success -> toaster.show(
                        "Completed!",
                        type = ToastType.Success
                    )

                    else -> {}
                }
            }
            .launchIn(this)
    }

    val appName = stringResource(id = R.string.app_name)
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { document -> document?.let { viewModel.writeToFile(it, context) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { document -> document?.let { viewModel.importFavorites(it, context) } }

    SettingsScaffold("More Settings") {
        CategorySetting(
            settingIcon = {
                Icon(Icons.Default.Star, null)
            }
        ) { Text(stringResource(R.string.viewFavoritesMenu)) }

        PreferenceSetting(
            settingTitle = { Text("Export Favorites") },
            modifier = Modifier.clickable(
                enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                indication = ripple(),
                interactionSource = null
            ) { exportLauncher.launch("${appName}_favorites.json") }
        )

        PreferenceSetting(
            settingTitle = { Text("Import Favorites") },
            modifier = Modifier.clickable(
                enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                indication = ripple(),
                interactionSource = null
            ) { importLauncher.launch(arrayOf("application/json")) }
        )

        HorizontalDivider()
    }

    Toaster(
        state = toaster,
        darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    )
}

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