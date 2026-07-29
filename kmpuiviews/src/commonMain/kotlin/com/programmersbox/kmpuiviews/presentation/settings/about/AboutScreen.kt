package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.moreinfo.MoreInfoViewModel
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.icons.Discord
import com.programmersbox.kmpuiviews.utils.composables.icons.Github
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.gotoBrowser
import otakuworld.kmpuiviews.generated.resources.join_discord
import otakuworld.kmpuiviews.generated.resources.notNow
import otakuworld.kmpuiviews.generated.resources.please_update_for_latest_features
import otakuworld.kmpuiviews.generated.resources.support
import otakuworld.kmpuiviews.generated.resources.support_summary
import otakuworld.kmpuiviews.generated.resources.update
import otakuworld.kmpuiviews.generated.resources.updateTo
import otakuworld.kmpuiviews.generated.resources.update_available
import otakuworld.kmpuiviews.generated.resources.view_libraries_used
import otakuworld.kmpuiviews.generated.resources.view_on_github

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
    infoViewModel: MoreInfoViewModel = koinViewModel(),
    usedLibraryClick: () -> Unit,
    onViewAccountInfoClick: () -> Unit,
) {
    val navActions = LocalNavActions.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val appConfig: AppConfig = koinInject()
    val appUpdateCheck: AppUpdateCheck = koinInject()
    val appUpdate by appUpdateCheck.updateAppCheck.collectAsStateWithLifecycle(null)
    val appVersion = appVersion()
    var showUpdateDialog by remember { mutableStateOf(false) }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(stringResource(Res.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
            text = { Text(stringResource(Res.string.please_update_for_latest_features)) },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text(stringResource(Res.string.update)) }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text(stringResource(Res.string.notNow)) }
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                        showUpdateDialog = false
                    }
                ) { Text(stringResource(Res.string.gotoBrowser)) }
            }
        )
    }

    SettingsScaffold(
        title = "About",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            if (AppUpdate.checkForUpdate(appVersion, appUpdate?.updateRealVersion.orEmpty())) {
                segmentedListItem(
                    content = { Text(stringResource(Res.string.update_available)) },
                    supportingContent = { Text(stringResource(Res.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
                    leadingContent = {
                        Icon(Icons.Default.SystemUpdateAlt, null, tint = Color(0xFF00E676))
                    },
                    onClick = { showUpdateDialog = true },
                )
            }

            segmentedListItem(
                content = { Text("View Onboarding Again") },
                leadingContent = { Icon(Icons.Default.CatchingPokemon, null) },
                onClick = { navActions.toOnboarding() },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_libraries_used)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                onClick = usedLibraryClick,
            )
        }

        CategoryGroupListItem {
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_on_github)) },
                leadingContent = { Icon(Icons.Github, null) },
                onClick = { uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest") },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.join_discord)) },
                leadingContent = { Icon(Icons.Discord, null) },
                onClick = { uriHandler.openUri("https://discord.gg/MhhHMWqryg") },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.support)) },
                supportingContent = { Text(stringResource(Res.string.support_summary)) },
                leadingContent = { Icon(Icons.Default.AttachMoney, null) },
                onClick = { uriHandler.openUri("https://ko-fi.com/V7V3D3JI") },
            )
        }

        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Diagnostics") },
                leadingContent = { Icon(Icons.Default.Engineering, null) },
                onClick = navActions::diagnostics,
            )
            if (BuildKonfig.IS_PRERELEASE || appConfig.isDebug) {
                segmentedListItem(
                    content = { Text("Developer") },
                    leadingContent = { Icon(Icons.Default.BugReport, null) },
                    onClick = navActions::developer,
                )
            }
        }

        CategoryGroupListItem {
            apply(composeSettingsDsl.aboutSettings)
        }
    }
}
