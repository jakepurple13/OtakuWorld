package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToastType
import com.dokar.sonner.rememberToasterState
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.ToasterSetup
import com.programmersbox.kmpuiviews.utils.ToasterUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.more_settings
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    viewModel: MoreSettingsViewModel = koinViewModel(),
) {
    val navController = LocalNavActions.current
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

    val appConfig = koinInject<AppConfig>()
    val appName = appConfig.appName
    SettingsScaffold(
        stringResource(Res.string.more_settings),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CategoryGroup {
            categorySetting(
                settingIcon = {
                    Icon(Icons.Default.Backup, null)
                }
            ) { Text("Backup") }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Create Full Backup") },
                    settingIcon = { Icon(Icons.Default.Backup, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(Screen.BackupWizard) }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Restore Full Backup") },
                    settingIcon = { Icon(Icons.Default.Restore, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(Screen.RestoreWizard) }
                )
            }
        }
    }

    //TODO: Remove toaster and switch back to snackbar
    ToasterSetup(toaster = toaster)
}