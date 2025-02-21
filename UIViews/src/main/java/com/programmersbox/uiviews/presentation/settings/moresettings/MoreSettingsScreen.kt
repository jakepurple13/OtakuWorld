package com.programmersbox.uiviews.presentation.settings.moresettings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dokar.sonner.ToastType
import com.dokar.sonner.rememberToasterState
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.CategorySetting
import com.programmersbox.uiviews.presentation.components.PreferenceSetting
import com.programmersbox.uiviews.presentation.settings.SettingsScaffold
import com.programmersbox.uiviews.utils.ToasterSetup
import com.programmersbox.uiviews.utils.ToasterUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    viewModel: MoreSettingsViewModel = koinViewModel(),
) {
    val toaster = rememberToasterState(
        onToastDismissed = { viewModel.importExportListStatus = ImportExportListStatus.Idle }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.importExportListStatus }
            .onEach {
                toaster.dismiss(ToasterUtils.LOADING_TOAST_ID)
                when (it) {
                    is ImportExportListStatus.Error -> toaster.show(
                        "Error: ${it.throwable.message}",
                        type = ToastType.Error,
                    )

                    ImportExportListStatus.Success -> toaster.show(
                        "Completed!",
                        type = ToastType.Success,
                    )

                    ImportExportListStatus.Loading -> toaster.show(
                        "Working...",
                        id = ToasterUtils.LOADING_TOAST_ID,
                        icon = ToasterUtils.LOADING_TOAST_ID,
                        duration = Duration.INFINITE,
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

    SettingsScaffold(stringResource(R.string.more_settings)) {
        CategorySetting(
            settingIcon = {
                Icon(Icons.Default.Star, null)
            }
        ) { Text(stringResource(R.string.viewFavoritesMenu)) }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.export_favorites)) },
            modifier = Modifier.clickable(
                enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                indication = ripple(),
                interactionSource = null
            ) { exportLauncher.launch("${appName}_favorites.json") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.import_favorites)) },
            modifier = Modifier.clickable(
                enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                indication = ripple(),
                interactionSource = null
            ) { importLauncher.launch(arrayOf("application/json")) }
        )

        HorizontalDivider()
    }

    //TODO: Remove toaster and switch back to snackbar
    ToasterSetup(toaster = toaster)
}