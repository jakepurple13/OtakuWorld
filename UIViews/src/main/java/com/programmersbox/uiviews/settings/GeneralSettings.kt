package com.programmersbox.uiviews.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.materialkolor.PaletteStyle
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.details.PaletteSwatchType
import com.programmersbox.uiviews.utils.HISTORY_SAVE
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.ListSetting
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.SliderSetting
import com.programmersbox.uiviews.utils.SwitchSetting
import com.programmersbox.uiviews.utils.historySave
import com.programmersbox.uiviews.utils.rememberSwatchStyle
import com.programmersbox.uiviews.utils.rememberSwatchType
import com.programmersbox.uiviews.utils.updatePref
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun GeneralSettings(
    customSettings: @Composable () -> Unit = {},
) {
    SettingsScaffold(stringResource(R.string.general_menu_title)) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val handling = LocalSettingsHandling.current

        var showDownload by handling.rememberShowDownload()

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

        var isAmoledMode by handling.rememberIsAmoledMode()

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.amoled_mode)) },
            settingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.fillMaxSize()) },
            value = isAmoledMode,
            updateValue = { isAmoledMode = it }
        )

        var showBlur by handling.rememberShowBlur()

        SwitchSetting(
            settingTitle = { Text("Show Blur") },
            summaryValue = {
                Text("Use blurring to get a glassmorphic look")
            },
            settingIcon = {
                Icon(
                    imageVector = if (showBlur) Icons.Default.BlurOn else Icons.Default.BlurOff,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            },
            value = showBlur,
            updateValue = { showBlur = it }
        )

        HorizontalDivider()

        var usePalette by handling.rememberUsePalette()

        SwitchSetting(
            settingTitle = { Text("Use Palette") },
            summaryValue = {
                Text("Use Palette to color the details screen if possible")
            },
            settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
            value = usePalette,
            updateValue = { usePalette = it }
        )

        ShowWhen(usePalette) {
            var paletteSwatchType by rememberSwatchType()
            ListSetting(
                settingTitle = { Text("Swatch Type") },
                dialogIcon = { Icon(Icons.Default.Palette, null) },
                settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                dialogTitle = { Text("Choose a Swatch Type to use") },
                summaryValue = { Text(paletteSwatchType.name) },
                confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
                value = paletteSwatchType,
                options = PaletteSwatchType.entries,
                updateValue = { it, d ->
                    d.value = false
                    paletteSwatchType = it
                }
            )

            var paletteStyle by rememberSwatchStyle()
            ListSetting(
                settingTitle = { Text("Swatch Style") },
                dialogIcon = { Icon(Icons.Default.Palette, null) },
                settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                dialogTitle = { Text("Choose a Swatch Style to use") },
                summaryValue = { Text(paletteStyle.name) },
                confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
                value = paletteStyle,
                options = PaletteStyle.entries,
                updateValue = { it, d ->
                    d.value = false
                    paletteStyle = it
                }
            )
        }

        HorizontalDivider()

        var shareChapter by handling.rememberShareChapter()

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.share_chapters)) },
            settingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.fillMaxSize()) },
            value = shareChapter,
            updateValue = { shareChapter = it }
        )

        var showAllScreen by handling.rememberShowAll()

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.show_all_screen)) },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showAllScreen,
            updateValue = { showAllScreen = it }
        )

        var showListDetail by handling.rememberShowListDetail()

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
            updateValue = { showListDetail = it }
        )

        SwitchSetting(
            settingTitle = { Text("Show Download Button") },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showDownload,
            updateValue = { showDownload = it }
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

        customSettings()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@LightAndDarkPreviews
@Composable
private fun GeneralSettingsPreview() {
    PreviewTheme {
        GeneralSettings()
    }
}