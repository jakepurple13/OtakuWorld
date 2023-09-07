package com.programmersbox.uiviews.settings

import android.Manifest
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.DownloadAndInstaller
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.components.icons.Discord
import com.programmersbox.uiviews.utils.components.icons.Github
import com.programmersbox.uiviews.utils.navigateChromeCustomTabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSettings(
    infoViewModel: MoreInfoViewModel = viewModel(),
    usedLibraryClick: () -> Unit,
) {
    val activity = LocalActivity.current
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    SettingsScaffold(stringResource(R.string.more_info_category)) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_libraries_used)) },
            settingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = usedLibraryClick
            )
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_on_github)) },
            settingIcon = { Icon(Icons.Github, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { navController.navigateChromeCustomTabs("https://github.com/jakepurple13/OtakuWorld/releases/latest") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.join_discord)) },
            settingIcon = { Icon(Icons.Discord, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { navController.navigateChromeCustomTabs("https://discord.gg/MhhHMWqryg") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.support)) },
            summaryValue = { Text(stringResource(R.string.support_summary)) },
            settingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { navController.navigateChromeCustomTabs("https://ko-fi.com/V7V3D3JI") }
        )

        val appUpdate by updateAppCheck.collectAsState(null)

        PreferenceSetting(
            settingIcon = {
                Image(
                    rememberDrawablePainter(drawable = koinInject<AppLogo>().logo),
                    null,
                    modifier = Modifier.fillMaxSize()
                )
            },
            settingTitle = { Text(stringResource(R.string.currentVersion, appVersion())) },
            modifier = Modifier.clickable { scope.launch(Dispatchers.IO) { infoViewModel.updateChecker(context) } }
        )

        val downloadInstaller = remember { DownloadAndInstaller(context) }

        ShowWhen(
            visibility = AppUpdate.checkForUpdate(appVersion(), appUpdate?.update_real_version.orEmpty())
        ) {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(R.string.updateTo, appUpdate?.update_real_version.orEmpty())) },
                    text = { Text(stringResource(R.string.please_update_for_latest_features)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                activity.requestPermissions(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) {
                                    if (it.isGranted) {
                                        updateAppCheck.value
                                            ?.let { a ->
                                                val isApkAlreadyThere = File(
                                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                                    a.let(genericInfo.apkString).toString()
                                                )
                                                if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()
                                                val url = a.downloadUrl(genericInfo.apkString)
                                                downloadInstaller.downloadAndInstall(
                                                    url = url,
                                                    destinationPath = url.split("/").lastOrNull() ?: "update_apk"
                                                )
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
                summaryValue = { Text(stringResource(R.string.updateTo, appUpdate?.update_real_version.orEmpty())) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
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
            usedLibraryClick = {}
        )
    }
}