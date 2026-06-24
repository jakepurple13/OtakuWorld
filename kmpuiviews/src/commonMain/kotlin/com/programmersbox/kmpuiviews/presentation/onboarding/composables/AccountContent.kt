package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
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
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Account") }
        )

        PreferenceSetting(
            settingTitle = { Text("Setup Supabase") },
            settingIcon = { Icon(Icons.Default.CloudSync, null) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) { navController.navigate(SupabaseRoutes) }
        )

        HorizontalDivider()

        val importBackupLauncher = rememberFilePickerLauncher(
            type = FileKitType.File("zip")
        ) { document -> document?.let { importViewModel.importFullBackup(it) } }

        PreferenceSetting(
            settingTitle = { Text("Restore data from backup") },
            settingIcon = { Icon(Icons.Default.Restore, null) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) { importBackupLauncher.launch() }
        )
    }
}