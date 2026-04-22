package com.programmersbox.kmpuiviews.presentation.settings.moreinfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bento
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.icons.Discord
import com.programmersbox.kmpuiviews.utils.composables.icons.Github
import com.programmersbox.kmpuiviews.versionCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentVersion
import otakuworld.kmpuiviews.generated.resources.gotoBrowser
import otakuworld.kmpuiviews.generated.resources.join_discord
import otakuworld.kmpuiviews.generated.resources.more_info_category
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
fun MoreInfoScreen(
    infoViewModel: MoreInfoViewModel = koinViewModel(),
    usedLibraryClick: () -> Unit,
    onViewAccountInfoClick: () -> Unit,
) {
    val navController = LocalNavActions.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val appUpdateCheck: AppUpdateCheck = koinInject()

    val appUpdate by appUpdateCheck.updateAppCheck.collectAsStateWithLifecycle(null)

    var showDialog by remember { mutableStateOf(false) }

    val appVersion = appVersion()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
            text = { Text(stringResource(Res.string.please_update_for_latest_features)) },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { Text(stringResource(Res.string.update)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(Res.string.notNow)) }
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                        showDialog = false
                    }
                ) { Text(stringResource(Res.string.gotoBrowser)) }
            }
        )
    }

    SettingsScaffold(
        stringResource(Res.string.more_info_category),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_libraries_used)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                onClick = usedLibraryClick
            )
        }

        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("View Account Info") },
                leadingContent = { Icon(Icons.Default.AccountCircle, null) },
                onClick = onViewAccountInfoClick
            )

            segmentedListItem(
                content = { Text("View Onboarding Again") },
                leadingContent = { Icon(Icons.Default.CatchingPokemon, null) },
                onClick = { navController.toOnboarding() },
            )

            segmentedListItem(
                content = { Text("View Background Worker Info") },
                leadingContent = { Icon(Icons.Default.Engineering, null) },
                onClick = { navController.workerInfo() },
            )

            segmentedListItem(
                content = { Text("View Exceptions") },
                leadingContent = { Icon(Icons.Default.Error, null) },
                onClick = { navController.navigate(Screen.ExceptionScreen) },
            )
        }

        DebugPrereleaseOptions()

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
                onClick = { uriHandler.openUri("https://ko-fi.com/V7V3D3JI") }
            )

            segmentedListItem(
                leadingContent = {
                    Image(
                        painterLogo(),
                        null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                },
                overlineContent = { Text(platform()) },
                content = { Text(stringResource(Res.string.currentVersion, appVersion)) },
                supportingContent = { Text("Version code: ${versionCode()}") },
                onClick = { scope.launch(Dispatchers.IO) { infoViewModel.updateChecker() } }
            )

            if (AppUpdate.checkForUpdate(appVersion, appUpdate?.updateRealVersion.orEmpty())) {
                segmentedListItem(
                    content = { Text(stringResource(Res.string.update_available)) },
                    supportingContent = { Text(stringResource(Res.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
                    onClick = { showDialog = true },
                    leadingContent = {
                        Icon(
                            Icons.Default.SystemUpdateAlt,
                            null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DebugPrereleaseOptions() {
    val appConfig = koinInject<AppConfig>()

    if (BuildKonfig.IS_PRERELEASE || appConfig.isDebug) {
        val navActions = LocalNavActions.current

        CategoryGroupListItem {
            if (appConfig.isDebug) {
                segmentedListItem(
                    content = { Text("Debug Menu") },
                    leadingContent = { Icon(Icons.Default.Android, null) },
                    onClick = navActions::debug
                )
            }

            segmentedListItem(
                content = { Text("Update to latest pre-release") },
                leadingContent = { Icon(Icons.Default.Bento, null) },
                onClick = navActions::prerelease
            )

            segmentedListItem(
                content = { Text("Color Helper") },
                leadingContent = { Icon(Icons.Default.Colorize, null) },
                onClick = { navActions.navigate(Screen.ColorHelper) }
            )
        }
    }
}
