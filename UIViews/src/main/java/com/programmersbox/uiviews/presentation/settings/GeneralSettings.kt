package com.programmersbox.uiviews.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.visibleName
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun GeneralSettings(
    customSettings: @Composable () -> Unit = {},
) {
    val handling: NewSettingsHandling = koinInject()

    GeneralSettings(
        customSettings = customSettings,
        navigationBarSettings = { NavigationBarSettings(handling = handling) }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@LightAndDarkPreviews
@Composable
private fun GeneralSettingsPreview() {
    PreviewTheme {
        GeneralSettings()
    }
}