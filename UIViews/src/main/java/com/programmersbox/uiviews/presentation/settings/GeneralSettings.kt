package com.programmersbox.uiviews.presentation.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.datastore.rememberHistorySave
import com.programmersbox.kmpuiviews.presentation.components.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.ShowMoreSetting
import com.programmersbox.kmpuiviews.presentation.components.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.visibleName
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.rememberSwatchStyle
import com.programmersbox.uiviews.datastore.rememberSwatchType
import com.programmersbox.uiviews.presentation.components.SliderSetting
import com.programmersbox.uiviews.presentation.components.ThemeItem
import com.programmersbox.uiviews.presentation.components.seedColor
import com.programmersbox.uiviews.presentation.details.PaletteSwatchType
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalWindowSizeClass
import com.programmersbox.uiviews.utils.PreviewTheme
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun GeneralSettings(
    customSettings: @Composable () -> Unit = {},
) {
    SettingsScaffold(
        title = stringResource(R.string.general_menu_title),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val handling: NewSettingsHandling = koinInject()

        var isAmoledMode by handling.rememberIsAmoledMode()

        ThemeSetting(
            handling = handling,
            isAmoledMode = isAmoledMode
        )

        AmoledModeSetting(
            isAmoledMode = isAmoledMode,
            onAmoledModeChange = { isAmoledMode = it }
        )

        ExpressivenessSetting(handling = handling)

        BlurSetting(handling = handling)

        HorizontalDivider()

        PaletteSetting(handling = handling)

        HorizontalDivider()

        NavigationBarSettings(handling = handling)

        GridTypeSettings(handling = handling)

        ShareChapterSettings(handling = handling)

        DetailPaneSettings(handling = handling)

        ShowDownloadSettings(handling = handling)

        HistorySettings(handling = handling)

        customSettings()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NavigationBarSettings(handling: NewSettingsHandling) {
    var floatingNavigation by rememberFloatingNavigation()

    SwitchSetting(
        settingTitle = { Text("Floating Navigation") },
        settingIcon = { Icon(Icons.Default.Navigation, null, modifier = Modifier.fillMaxSize()) },
        value = floatingNavigation,
        updateValue = { floatingNavigation = it }
    )

    if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded) {
        var showAllScreen by handling.rememberShowAll()

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.show_all_screen)) },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showAllScreen,
            updateValue = { showAllScreen = it }
        )
    } else {

        var middleNavigationAction by handling.rememberMiddleNavigationAction()
        ListSetting(
            settingTitle = { Text("Middle Navigation Destination") },
            dialogIcon = { Icon(Icons.Default.LocationOn, null) },
            settingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text("Choose a middle navigation destination") },
            summaryValue = { Text(middleNavigationAction.visibleName) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
            value = middleNavigationAction,
            options = MiddleNavigationAction.entries,
            updateValue = { it, d ->
                d.value = false
                middleNavigationAction = it
            }
        )

        MultipleActionsSetting(
            handling = handling,
            middleNavigationAction = middleNavigationAction
        )

        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultipleActionsSetting(
    handling: NewSettingsHandling,
    middleNavigationAction: MiddleNavigationAction,
) {
    var multipleActions by handling.rememberMiddleMultipleActions()

    val multipleActionOptions = MiddleNavigationAction
        .entries
        .filter { it != MiddleNavigationAction.Multiple }

    ShowWhen(middleNavigationAction == MiddleNavigationAction.Multiple) {
        PreferenceSetting(
            settingTitle = { },
            summaryValue = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        leadingContent = {
                            var showMenu by remember { mutableStateOf(false) }
                            DropdownMenu(
                                showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                multipleActionOptions.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.name) },
                                        leadingIcon = {
                                            Icon(
                                                it.item?.icon?.invoke(true) ?: Icons.Default.Add,
                                                null,
                                            )
                                        },
                                        onClick = {
                                            multipleActions = multipleActions?.copy(
                                                startAction = it,
                                            )
                                            showMenu = false
                                        }
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showMenu = true }
                            ) {
                                Icon(
                                    multipleActions
                                        ?.startAction
                                        ?.item
                                        ?.icon
                                        ?.invoke(true)
                                        ?: Icons.Default.Add,
                                    null
                                )
                            }
                        },
                        trailingContent = {
                            var showMenu by remember { mutableStateOf(false) }
                            DropdownMenu(
                                showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                multipleActionOptions.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.visibleName) },
                                        leadingIcon = {
                                            Icon(
                                                it.item?.icon?.invoke(true) ?: Icons.Default.Add,
                                                null,
                                            )
                                        },
                                        onClick = {
                                            multipleActions = multipleActions?.copy(endAction = it)
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showMenu = true }
                            ) {
                                Icon(
                                    multipleActions
                                        ?.endAction
                                        ?.item
                                        ?.icon
                                        ?.invoke(true)
                                        ?: Icons.Default.Add,
                                    null
                                )
                            }
                        },
                    ) {
                        FilledIconButton(
                            modifier = Modifier.width(64.dp),
                            onClick = {}
                        ) {
                            Icon(
                                Icons.Filled.UnfoldLess,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                }
            }
        )
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ThemeSetting(
    handling: NewSettingsHandling,
    isAmoledMode: Boolean,
) {
    var themeSetting by handling.rememberSystemThemeMode()

    val themeText by remember {
        derivedStateOf {
            when (themeSetting) {
                SystemThemeMode.FollowSystem -> "System"
                SystemThemeMode.Day -> "Light"
                SystemThemeMode.Night -> "Dark"
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
            themeSetting = it
        }
    )

    var themeColor by handling.rememberThemeColor()

    ShowMoreSetting(
        settingTitle = { Text("Theme Color") },
        settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
        summaryValue = { Text(themeColor.name) },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeColor.entries
                //TODO: For later
                .filter { it != ThemeColor.Custom }
                .forEach {
                    ThemeItem(
                        themeColor = it,
                        onClick = { themeColor = it },
                        selected = it == themeColor,
                        colorScheme = if (it == ThemeColor.Dynamic)
                            MaterialTheme.colorScheme
                        else
                            rememberDynamicColorScheme(
                                it.seedColor,
                                isDark = when (themeSetting) {
                                    SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
                                    SystemThemeMode.Day -> false
                                    SystemThemeMode.Night -> true
                                },
                                isAmoled = isAmoledMode
                            )
                    )
                }
        }
    }
}

@Composable
private fun AmoledModeSetting(
    isAmoledMode: Boolean,
    onAmoledModeChange: (Boolean) -> Unit,
) {
    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.amoled_mode)) },
        settingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.fillMaxSize()) },
        value = isAmoledMode,
        updateValue = onAmoledModeChange
    )
}

@Composable
private fun ExpressivenessSetting(handling: NewSettingsHandling) {
    var showExpressiveness by handling.rememberShowExpressiveness()
    SwitchSetting(
        settingTitle = { Text("Show Expressiveness") },
        settingIcon = { Icon(Icons.Default.Animation, null, modifier = Modifier.fillMaxSize()) },
        summaryValue = { Text("Have the animations be expressive!") },
        value = showExpressiveness,
        updateValue = { showExpressiveness = it }
    )
}

@Composable
fun BlurSetting(handling: NewSettingsHandling) {
    var showBlur by handling.rememberShowBlur()

    SwitchSetting(
        settingTitle = { Text("Show Blur") },
        summaryValue = { Text("Use blurring to get a glassmorphic look") },
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun PaletteSetting(handling: NewSettingsHandling) {
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun GridTypeSettings(handling: NewSettingsHandling) {
    var gridChoice by handling.rememberGridChoice()

    ListSetting(
        settingTitle = { Text("Grid Type") },
        settingIcon = { Icon(Icons.Default.GridView, null, modifier = Modifier.fillMaxSize()) },
        value = gridChoice,
        updateValue = { it, d ->
            d.value = false
            gridChoice = it
        },
        options = listOf(
            GridChoice.FullAdaptive,
            GridChoice.Adaptive,
            GridChoice.Fixed
        ),
        summaryValue = {
            Text(
                when (gridChoice) {
                    GridChoice.FullAdaptive -> "Full Adaptive: This will have a dynamic number of columns."
                    GridChoice.Adaptive -> "Adaptive: This will be adaptive as best it can."
                    GridChoice.Fixed -> "Fixed: Have a fixed amount of columns. This will be 3 for compact, 5 for medium, and 6 for large."
                }
            )
        },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
        dialogTitle = { Text("Grid Type") },
        dialogIcon = { Icon(Icons.Default.GridView, null) },
    )
}

@Composable
fun ShareChapterSettings(handling: NewSettingsHandling) {
    var shareChapter by handling.rememberShareChapter()

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.share_chapters)) },
        settingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.fillMaxSize()) },
        value = shareChapter,
        updateValue = { shareChapter = it }
    )
}

@Composable
private fun DetailPaneSettings(handling: NewSettingsHandling) {
    var showListDetail by handling.rememberShowListDetail()

    SwitchSetting(
        value = showListDetail,
        settingTitle = { Text(stringResource(R.string.show_list_detail_pane_for_lists)) },
        settingIcon = {
            Icon(
                if (showListDetail) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Filled.ListAlt,
                null,
                modifier = Modifier.fillMaxSize()
            )
        },
        updateValue = { showListDetail = it }
    )
}

@Composable
fun ShowDownloadSettings(handling: NewSettingsHandling) {
    var showDownload by handling.rememberShowDownload()

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.show_download_button)) },
        settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
        value = showDownload,
        updateValue = { showDownload = it }
    )
}

@Composable
private fun HistorySettings(handling: NewSettingsHandling) {
    var sliderValue by rememberHistorySave()

    SliderSetting(
        sliderValue = sliderValue.toFloat(),
        settingTitle = { Text(stringResource(R.string.history_save_title)) },
        settingSummary = { Text(stringResource(R.string.history_save_summary)) },
        settingIcon = { Icon(Icons.Default.ChangeHistory, null) },
        range = -1f..100f,
        updateValue = { sliderValue = it.toInt() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@LightAndDarkPreviews
@Composable
private fun GeneralSettingsPreview() {
    PreviewTheme {
        GeneralSettings()
    }
}