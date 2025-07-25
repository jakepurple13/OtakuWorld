package com.programmersbox.kmpuiviews.presentation.settings.moreinfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreInfoScreen(
    infoViewModel: MoreInfoViewModel = koinViewModel(),
    usedLibraryClick: () -> Unit,
    onViewAccountInfoClick: () -> Unit,
) {
    val navController = LocalNavActions.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val appUpdateCheck: AppUpdateCheck = koinInject()

    SettingsScaffold(
        stringResource(Res.string.more_info_category),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CategoryGroup {
            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.view_libraries_used)) },
                    settingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null,
                        onClick = usedLibraryClick
                    )
                )
            }
        }

        CategoryGroup {
            item {
                PreferenceSetting(
                    settingTitle = { Text("View Account Info") },
                    settingIcon = { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null,
                        onClick = onViewAccountInfoClick
                    )
                )
            }

            item {
                var onboarding by dataStoreHandling.hasGoneThroughOnboarding.asState()
                PreferenceSetting(
                    settingTitle = { Text("View Onboarding Again") },
                    settingIcon = { Icon(Icons.Default.CatchingPokemon, null) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) {
                        //navController.clearBackStack(Screen.RecentScreen)
                        onboarding = false
                        navController.toOnboarding()
                    }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("View Background Worker Info") },
                    settingIcon = { Icon(Icons.Default.Engineering, null) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.workerInfo() }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("View Exceptions") },
                    settingIcon = { Icon(Icons.Default.Error, null) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(Screen.ExceptionScreen) }
                )
            }
        }

        DebugPrereleaseOptions()

        CategoryGroup {
            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.view_on_github)) },
                    settingIcon = { Icon(Icons.Github, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest") }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.join_discord)) },
                    settingIcon = { Icon(Icons.Discord, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { uriHandler.openUri("https://discord.gg/MhhHMWqryg") }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.support)) },
                    summaryValue = { Text(stringResource(Res.string.support_summary)) },
                    settingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { uriHandler.openUri("https://ko-fi.com/V7V3D3JI") }
                )
            }

            item {
                val appUpdate by appUpdateCheck.updateAppCheck.collectAsStateWithLifecycle(null)
                PreferenceSetting(
                    settingIcon = {
                        Image(
                            painterLogo(),
                            null,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    settingTitle = {
                        Column {
                            Text(
                                platform(),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(stringResource(Res.string.currentVersion, appVersion()))
                            Text(
                                "Version code: ${versionCode()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.clickable { scope.launch(Dispatchers.IO) { infoViewModel.updateChecker() } }
                )

                ShowWhen(
                    visibility = AppUpdate.checkForUpdate(appVersion(), appUpdate?.updateRealVersion.orEmpty())
                ) {
                    var showDialog by remember { mutableStateOf(false) }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text(stringResource(Res.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
                            text = { Text(stringResource(Res.string.please_update_for_latest_features)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        /*(activity as? FragmentActivity)?.requestPermissions(
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) {
                                            if (it.isGranted) {
                                                appUpdateCheck
                                                    .updateAppCheck
                                                    .value
                                                    ?.let { a -> infoViewModel.update(a) }
                                            }
                                        }*/
                                        showDialog = false
                                    }
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

                    PreferenceSetting(
                        settingTitle = { Text(stringResource(Res.string.update_available)) },
                        summaryValue = { Text(stringResource(Res.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) { showDialog = true },
                        settingIcon = {
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
}

@Composable
private fun DebugPrereleaseOptions() {
    val appConfig = koinInject<AppConfig>()

    if (BuildKonfig.IS_PRERELEASE || appConfig.isDebug) {
        val navActions = LocalNavActions.current

        CategoryGroup {
            if(appConfig.isDebug) {
                item {
                    PreferenceSetting(
                        settingTitle = { Text("Debug Menu") },
                        settingIcon = { Icon(Icons.Default.Android, null, modifier = Modifier.fillMaxSize()) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null,
                            onClick = navActions::debug
                        )
                    )
                }
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Update to latest pre release") },
                    settingIcon = { Icon(Icons.Default.Bento, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null,
                        onClick = navActions::prerelease
                    )
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Color Helper") },
                    settingIcon = { Icon(Icons.Default.Colorize, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null,
                        onClick = { navActions.navigate(Screen.ColorHelper) }
                    )
                )
            }
        }
    }
}
