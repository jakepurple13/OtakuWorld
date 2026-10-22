package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.supabaseintegration.ui.SupabaseRoutes
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AccountContent(
    navController: NavigationActions,
    importViewModel: MoreSettingsViewModel = koinViewModel(),
) {
    val importBackupLauncher = rememberFilePickerLauncher(
        type = FileKitType.File("zip")
    ) { document -> document?.let { importViewModel.importFullBackup(it) } }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Account & privacy") },
            supportingContent = { Text("Bring your data with you, or keep it in sync across devices.") },
        )

        CategoryGroup {
            categorySetting { Text("Sync & backup") }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Set up Supabase sync") },
                    summaryValue = { Text("Keep favorites in sync across devices") },
                    settingIcon = { Icon(Icons.Default.CloudSync, null, modifier = Modifier.fillMaxSize()) },
                    onClick = { navController.navigate(SupabaseRoutes) },
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Restore from backup") },
                    summaryValue = { Text("Import a .zip backup") },
                    settingIcon = { Icon(Icons.Default.Restore, null, modifier = Modifier.fillMaxSize()) },
                    onClick = { importBackupLauncher.launch() },
                )
            }
        }

        // Biometric settings only do something on Android.
        if (platform() == "Android") {
            CategoryGroup {
                categorySetting { Text("Privacy") }

                item {
                    PreferenceSetting(
                        settingTitle = { Text("Biometric lock") },
                        summaryValue = { Text("Choose how locked lists unlock: fingerprint, face or device PIN") },
                        settingIcon = { Icon(Icons.Default.Fingerprint, null, modifier = Modifier.fillMaxSize()) },
                        onClick = navController::security,
                    )
                }
            }
        }
    }
}
