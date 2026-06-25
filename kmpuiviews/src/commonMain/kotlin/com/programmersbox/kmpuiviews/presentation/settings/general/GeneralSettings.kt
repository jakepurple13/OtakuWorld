package com.programmersbox.kmpuiviews.presentation.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.settingsDialog
import com.programmersbox.kmpuiviews.presentation.components.visibleName
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.general_menu_title
import otakuworld.kmpuiviews.generated.resources.history_save_summary
import otakuworld.kmpuiviews.generated.resources.history_save_title
import otakuworld.kmpuiviews.generated.resources.share_chapters
import otakuworld.kmpuiviews.generated.resources.show_download_button
import otakuworld.kmpuiviews.generated.resources.show_list_detail_pane_for_lists

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
        CategoryGroupListItem {
            segmentedListItem(
                onClick = { navActions.navigate(Screen.ThemeSettings) },
                leadingContent = { Icon(Icons.Default.Palette, null) },
                content = { Text("Theme Settings") },
            )

            segmentedListItem(
                onClick = { navActions.navigate(Screen.DetailsSettings) },
                leadingContent = { Icon(Icons.Default.Animation, null) },
                content = { Text("Details Settings") },
            )

            segmentedListItem(
                onClick = { navActions.navigate(Screen.Settings.Blur) },
                leadingContent = { Icon(Icons.Default.BlurOn, null) },
                content = { Text("Blur Settings") },
            )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GridTypeSettings(handling: NewSettingsHandling) {
    var gridChoice by handling.rememberGridChoice()

    var settingsDialog by settingsDialog(
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
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
        dialogTitle = { Text("Grid Type") },
        dialogIcon = { Icon(Icons.Default.GridView, null) },
    )

    SegmentedListItem(
        onClick = { settingsDialog = true },
        content = { Text("Grid Type") },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = { Icon(Icons.Default.GridView, null) },
        shapes = ListItemDefaults.segmentedShapes(0, 3),
        supportingContent = {
            Text(
                when (gridChoice) {
                    GridChoice.FullAdaptive -> "Full Adaptive: This will have a dynamic number of columns."
                    GridChoice.Adaptive -> "Adaptive: This will be adaptive as best it can."
                    GridChoice.Fixed -> "Fixed: Have a fixed amount of columns. This will be 3 for compact, 5 for medium, and 6 for large."
                }
            )
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailPaneSettings(handling: NewSettingsHandling) {
    var showListDetail by handling.rememberShowListDetail()

    SegmentedListItem(
        checked = showListDetail,
        onCheckedChange = { showListDetail = it },
        shapes = ListItemDefaults.segmentedShapes(1, 3),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        content = { Text(stringResource(Res.string.show_list_detail_pane_for_lists)) },
        leadingContent = {
            Icon(
                if (showListDetail) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Filled.ListAlt,
                null,
            )
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HistorySettings(dataStoreHandling: DataStoreHandling) {
    var sliderValue by dataStoreHandling.historySave.asState()

    ElevatedCard(
        shape = ListItemDefaults.segmentedShapes(2, 3).shape,
    ) {
        SliderSetting(
            sliderValue = sliderValue.toFloat(),
            settingTitle = { Text(stringResource(Res.string.history_save_title)) },
            settingSummary = { Text(stringResource(Res.string.history_save_summary)) },
            settingIcon = { Icon(Icons.Default.ChangeHistory, null) },
            range = -1f..100f,
            updateValue = { sliderValue = it.toInt() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationBarSettings(handling: NewSettingsHandling) {
    var floatingNavigation by rememberFloatingNavigation()

    var middleNavigationAction by handling.rememberMiddleNavigationAction()

    SegmentedListItem(
        checked = floatingNavigation,
        onCheckedChange = { floatingNavigation = it },
        content = { Text("Floating Navigation") },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = { Icon(Icons.Default.Navigation, null) },
        trailingContent = { Switch(checked = floatingNavigation, onCheckedChange = null) },
        shapes = ListItemDefaults.segmentedShapes(
            index = 0,
            if (middleNavigationAction == MiddleNavigationAction.Multiple) 3 else 2
        )
    )

    var listSettings by settingsDialog(
        dialogIcon = { Icon(Icons.Default.LocationOn, null) },
        dialogTitle = { Text("Choose a middle navigation destination") },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
        value = middleNavigationAction,
        options = MiddleNavigationAction.entries,
        updateValue = { it, d ->
            d.value = false
            middleNavigationAction = it
        }
    )

    SegmentedListItem(
        onClick = { listSettings = true },
        content = { Text(middleNavigationAction.visibleName) },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = { Icon(Icons.Default.LocationOn, null) },
        shapes = ListItemDefaults.segmentedShapes(
            index = 1,
            if (middleNavigationAction == MiddleNavigationAction.Multiple) 3 else 2
        )
    )

    MultipleActionsSetting(
        handling = handling,
        middleNavigationAction = middleNavigationAction
    )
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
        ElevatedCard(
            shape = ListItemDefaults.segmentedShapes(2, 3).shape,
        ) {
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
}