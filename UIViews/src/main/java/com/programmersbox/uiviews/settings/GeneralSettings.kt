package com.programmersbox.uiviews.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.utils.HISTORY_SAVE
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.ListSetting
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.SliderSetting
import com.programmersbox.uiviews.utils.SwitchSetting
import com.programmersbox.uiviews.utils.historySave
import com.programmersbox.uiviews.utils.updatePref
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun GeneralSettings(
    customSettings: (@Composable () -> Unit)? = null,
) {
    SettingsScaffold(stringResource(R.string.general_menu_title)) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val handling = LocalSettingsHandling.current

        val showDownload = remember(key1 = handling) { handling.showDownload }

        val themeSetting by handling.systemThemeMode.collectAsStateWithLifecycle(SystemThemeMode.FollowSystem)

        val themeText by remember {
            derivedStateOf {
                when (themeSetting) {
                    SystemThemeMode.FollowSystem -> "System"
                    SystemThemeMode.Day -> "Light"
                    SystemThemeMode.Night -> "Dark"
                    else -> "None"
                }
            }
        }

        ListSetting(
            settingTitle = { Text(stringResource(R.string.theme_choice_title)) },
            dialogIcon = { Icon(Icons.Default.SettingsBrightness, null) },
            settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text(stringResource(R.string.choose_a_theme)) },
            summaryValue = { Text(themeText) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
            value = themeSetting,
            options = listOf(SystemThemeMode.FollowSystem, SystemThemeMode.Day, SystemThemeMode.Night),
            updateValue = { it, d ->
                d.value = false
                scope.launch { handling.setSystemThemeMode(it) }
                when (it) {
                    SystemThemeMode.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    SystemThemeMode.Day -> AppCompatDelegate.MODE_NIGHT_NO
                    SystemThemeMode.Night -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> null
                }?.let(AppCompatDelegate::setDefaultNightMode)
            }
        )

        val shareChapter by handling.shareChapter.collectAsStateWithLifecycle(true)

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.share_chapters)) },
            settingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.fillMaxSize()) },
            value = shareChapter,
            updateValue = { scope.launch { handling.setShareChapter(it) } }
        )

        val showAllScreen by handling.showAll.collectAsStateWithLifecycle(true)

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.show_all_screen)) },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showAllScreen,
            updateValue = { scope.launch { handling.setShowAll(it) } }
        )

        val showListDetail by handling.showListDetail.collectAsStateWithLifecycle(true)

        SwitchSetting(
            value = showListDetail,
            settingTitle = { Text("Show List Detail Pane for Lists") },
            settingIcon = {
                Icon(
                    if (showListDetail) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Filled.ListAlt,
                    null,
                    modifier = Modifier.fillMaxSize()
                )
            },
            updateValue = { scope.launch { handling.setShowListDetail(it) } }
        )

        SwitchSetting(
            settingTitle = { Text("Show Download Button") },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showDownload.flow.collectAsStateWithLifecycle(initialValue = true).value,
            updateValue = { scope.launch { showDownload.updateSetting(it) } }
        )

        var sliderValue by remember { mutableFloatStateOf(runBlocking { context.historySave.first().toFloat() }) }

        SliderSetting(
            sliderValue = sliderValue,
            settingTitle = { Text(stringResource(R.string.history_save_title)) },
            settingSummary = { Text(stringResource(R.string.history_save_summary)) },
            settingIcon = { Icon(Icons.Default.ChangeHistory, null) },
            range = -1f..100f,
            updateValue = {
                sliderValue = it
                scope.launch { context.updatePref(HISTORY_SAVE, sliderValue.toInt()) }
            }
        )

        customSettings?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@LightAndDarkPreviews
@Composable
private fun GeneralSettingsPreview() {
    PreviewTheme {
        GeneralSettings(null)
    }
}