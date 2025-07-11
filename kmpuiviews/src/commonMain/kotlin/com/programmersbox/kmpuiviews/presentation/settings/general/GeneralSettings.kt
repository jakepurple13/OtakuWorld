package com.programmersbox.kmpuiviews.presentation.settings.general

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ColorLens
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
import androidx.compose.material3.FloatingToolbarDefaults
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
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PaletteSwatchType
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.datastore.rememberHistorySave
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.ThemeItem
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowMoreSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.visibleName
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.kmpuiviews.utils.seedColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.amoled_mode
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.choose_a_theme
import otakuworld.kmpuiviews.generated.resources.general_menu_title
import otakuworld.kmpuiviews.generated.resources.history_save_summary
import otakuworld.kmpuiviews.generated.resources.history_save_title
import otakuworld.kmpuiviews.generated.resources.share_chapters
import otakuworld.kmpuiviews.generated.resources.show_all_screen
import otakuworld.kmpuiviews.generated.resources.show_download_button
import otakuworld.kmpuiviews.generated.resources.show_list_detail_pane_for_lists
import otakuworld.kmpuiviews.generated.resources.theme_choice_title

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun GeneralSettings(
    customSettings: @Composable () -> Unit = {},
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = stringResource(Res.string.general_menu_title),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val handling: NewSettingsHandling = koinInject()

        CategoryGroup {
            item {
                PreferenceSetting(
                    settingTitle = { Text("Theme Settings") },
                    settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                    onClick = { navActions.navigate(Screen.ThemeSettings) }
                )
            }
        }

        CategoryGroup {
            item {
                NavigationBarSettings(handling = handling)
            }
        }

        CategoryGroup {
            item {
                Spacer(Modifier.height(16.dp))
                GridTypeSettings(handling = handling)
            }

            item { ShareChapterSettings(handling = handling) }
            item { DetailPaneSettings(handling = handling) }
            item { ShowDownloadSettings(handling = handling) }
            item { HistorySettings(handling = handling) }
        }

        customSettings()
    }
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
fun GridTypeSettings(handling: NewSettingsHandling) {
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
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
        dialogTitle = { Text("Grid Type") },
        dialogIcon = { Icon(Icons.Default.GridView, null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ColorBlindTypeSettings(handling: NewSettingsHandling) {
    var colorBlindType by handling.rememberColorBlindType()

    ListSetting(
        settingTitle = { Text("Color Blindness") },
        settingIcon = { Icon(Icons.Default.ColorLens, null, modifier = Modifier.fillMaxSize()) },
        value = colorBlindType,
        updateValue = { it, d ->
            d.value = false
            colorBlindType = it
        },
        options = ColorBlindnessType.entries,
        summaryValue = {
            Text(
                when (colorBlindType) {
                    ColorBlindnessType.None -> "None - No Color Blindness"
                    ColorBlindnessType.Protanopia -> "Protanopia - Red-green color blindness"
                    ColorBlindnessType.Deuteranopia -> "Deuteranopia - Blue-yellow color blindness"
                    ColorBlindnessType.Tritanopia -> "Tritanopia - Green-blue color blindness"
                }
            )
        },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
        dialogTitle = { Text("Color Blindness") },
        dialogIcon = { Icon(Icons.Default.ColorLens, null) },
    )
}

@Composable
fun ShareChapterSettings(handling: NewSettingsHandling) {
    var shareChapter by handling.rememberShareChapter()

    SwitchSetting(
        settingTitle = { Text(stringResource(Res.string.share_chapters)) },
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
        settingTitle = { Text(stringResource(Res.string.show_list_detail_pane_for_lists)) },
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
        settingTitle = { Text(stringResource(Res.string.show_download_button)) },
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
        settingTitle = { Text(stringResource(Res.string.history_save_title)) },
        settingSummary = { Text(stringResource(Res.string.history_save_summary)) },
        settingIcon = { Icon(Icons.Default.ChangeHistory, null) },
        range = -1f..100f,
        updateValue = { sliderValue = it.toInt() }
    )
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

    CategoryGroupDefaults.Divider()

    if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded) {
        var showAllScreen by handling.rememberShowAll()

        SwitchSetting(
            settingTitle = { Text(stringResource(Res.string.show_all_screen)) },
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
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
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
        CategoryGroupDefaults.Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
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
}