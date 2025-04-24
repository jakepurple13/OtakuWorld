package com.programmersbox.uiviews.presentation.settings

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bento
import androidx.compose.material.icons.filled.CatchingPokemon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.ShowWhen
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.icons.Discord
import com.programmersbox.uiviews.presentation.components.icons.Github
import com.programmersbox.uiviews.presentation.settings.viewmodels.MoreInfoViewModel
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.navigateChromeCustomTabs
import com.programmersbox.uiviews.utils.versionCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSettings(
    infoViewModel: MoreInfoViewModel = koinViewModel(),
    usedLibraryClick: () -> Unit,
    onPreleaseClick: () -> Unit,
) {
    val activity = LocalActivity.current
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dataStoreHandling = koinInject<DataStoreHandling>()

    SettingsScaffold(stringResource(R.string.more_info_category)) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_libraries_used)) },
            settingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null,
                onClick = usedLibraryClick
            )
        )

        var onboarding by dataStoreHandling.hasGoneThroughOnboarding.asState()
        PreferenceSetting(
            settingTitle = { Text("View Onboarding Again") },
            settingIcon = { Icon(Icons.Default.CatchingPokemon, null) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) {
                navController.clearBackStack<Screen.RecentScreen>()
                onboarding = false
                navController.navigate(Screen.OnboardingScreen) {
                    popUpTo(Screen.RecentScreen) {
                        inclusive = true
                    }
                }
            }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_on_github)) },
            settingIcon = { Icon(Icons.Github, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) { uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest") }
        )

        if (BuildConfig.IS_PRERELEASE || BuildConfig.DEBUG) {
            PreferenceSetting(
                settingTitle = { Text("Update to latest pre release") },
                settingIcon = { Icon(Icons.Default.Bento, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = onPreleaseClick
                )
            )
        }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.join_discord)) },
            settingIcon = { Icon(Icons.Discord, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) { uriHandler.openUri("https://discord.gg/MhhHMWqryg") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.support)) },
            summaryValue = { Text(stringResource(R.string.support_summary)) },
            settingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) { uriHandler.openUri("https://ko-fi.com/V7V3D3JI") }
        )

        val appUpdate by updateAppCheck.collectAsStateWithLifecycle(null)

        PreferenceSetting(
            settingIcon = {
                Image(
                    rememberDrawablePainter(drawable = koinInject<AppLogo>().logo),
                    null,
                    modifier = Modifier.fillMaxSize()
                )
            },
            settingTitle = {
                Column {
                    Text(
                        "Version code: ${versionCode()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(stringResource(R.string.currentVersion, appVersion()))
                    Text(
                        platform(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            modifier = Modifier.clickable { scope.launch(Dispatchers.IO) { infoViewModel.updateChecker(context) } }
        )

        ShowWhen(
            visibility = AppUpdate.checkForUpdate(appVersion(), appUpdate?.updateRealVersion.orEmpty())
        ) {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(R.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
                    text = { Text(stringResource(R.string.please_update_for_latest_features)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                (activity as? FragmentActivity)?.requestPermissions(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) {
                                    if (it.isGranted) {
                                        updateAppCheck.value
                                            ?.let { a ->
                                                /*val isApkAlreadyThere = File(
                                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                                    a.let(genericInfo.apkString).toString()
                                                )
                                                if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()*/
                                                infoViewModel.update(a)
                                            }
                                    }
                                }
                                showDialog = false
                            }
                        ) { Text(stringResource(R.string.update)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.notNow)) }
                        TextButton(
                            onClick = {
                                navController.navigateChromeCustomTabs("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                                showDialog = false
                            }
                        ) { Text(stringResource(R.string.gotoBrowser)) }
                    }
                )
            }

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.update_available)) },
                summaryValue = { Text(stringResource(R.string.updateTo, appUpdate?.updateRealVersion.orEmpty())) },
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

@LightAndDarkPreviews
@Composable
private fun InfoSettingsPreview() {
    PreviewTheme {
        InfoSettings(
            usedLibraryClick = {},
            onPreleaseClick = {}
        )
    }
}