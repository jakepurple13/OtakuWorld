package com.programmersbox.kmpuiviews.presentation.settings.general

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.kmpuiviews.presentation.components.ThemeItem
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowMoreSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.theme.UglyDarkColorScheme
import com.programmersbox.kmpuiviews.theme.UglyLightColorScheme
import com.programmersbox.kmpuiviews.utils.seedColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.amoled_mode
import otakuworld.kmpuiviews.generated.resources.choose_a_theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen() {
    val handling: NewSettingsHandling = koinInject()

    SettingsScaffold(
        title = "Theme",
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var isAmoledMode by handling.rememberIsAmoledMode()
        CategoryGroup {
            item {
                ThemeSetting(
                    handling = handling,
                    isAmoledMode = isAmoledMode
                )
            }

            item {
                AmoledModeSetting(
                    isAmoledMode = isAmoledMode,
                    onAmoledModeChange = { isAmoledMode = it }
                )
            }

            item { ExpressivenessSetting(handling = handling) }
            item { BlurSetting(handling = handling) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ThemeSetting(
    handling: NewSettingsHandling,
    isAmoledMode: Boolean,
) {
    var themeSetting by handling.rememberSystemThemeMode()

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.choose_a_theme)) },
        settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
        summaryValue = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                SingleChoiceSegmentedButtonRow {
                    SystemThemeMode.entries.forEachIndexed { index, systemThemeMode ->
                        SegmentedButton(
                            onClick = { themeSetting = systemThemeMode },
                            selected = themeSetting == systemThemeMode,
                            icon = {
                                Icon(
                                    when (systemThemeMode) {
                                        SystemThemeMode.FollowSystem -> Icons.Default.SettingsSystemDaydream
                                        SystemThemeMode.Day -> Icons.Default.LightMode
                                        SystemThemeMode.Night -> Icons.Default.DarkMode
                                    },
                                    null
                                )
                            },
                            label = { Text(systemThemeMode.name) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = SystemThemeMode.entries.size
                            )
                        )
                    }
                }
            }
        },
    )

    CategoryGroupDefaults.Divider()

    var themeColor by handling.rememberThemeColor()

    ShowMoreSetting(
        settingTitle = { Text("Theme Color") },
        settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
        summaryValue = { Text(themeColor.name) },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
        ) {
            ThemeColor.entries
                //TODO: For later
                .filter { it != ThemeColor.Custom }
                .forEach {
                    ThemeItem(
                        themeColor = it,
                        onClick = { themeColor = it },
                        selected = it == themeColor,
                        colorScheme = when (it) {
                            ThemeColor.Dynamic -> MaterialTheme.colorScheme
                            ThemeColor.Ugly -> when (themeSetting) {
                                SystemThemeMode.FollowSystem -> if (isSystemInDarkTheme()) UglyDarkColorScheme else UglyLightColorScheme
                                SystemThemeMode.Day -> UglyLightColorScheme
                                SystemThemeMode.Night -> UglyDarkColorScheme
                            }

                            ThemeColor.MyEyes -> when (themeSetting) {
                                SystemThemeMode.FollowSystem -> if (isSystemInDarkTheme()) UglyDarkColorScheme else UglyLightColorScheme
                                SystemThemeMode.Day -> UglyLightColorScheme
                                SystemThemeMode.Night -> UglyDarkColorScheme
                            }

                            else -> rememberDynamicColorScheme(
                                it.seedColor,
                                isDark = when (themeSetting) {
                                    SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
                                    SystemThemeMode.Day -> false
                                    SystemThemeMode.Night -> true
                                },
                                isAmoled = isAmoledMode
                            )
                        }
                    )
                }
        }
    }
}

@Composable
fun AmoledModeSetting(
    isAmoledMode: Boolean,
    onAmoledModeChange: (Boolean) -> Unit,
) {
    SwitchSetting(
        settingTitle = { Text(stringResource(Res.string.amoled_mode)) },
        settingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.fillMaxSize()) },
        value = isAmoledMode,
        updateValue = onAmoledModeChange
    )
}

@Composable
fun ExpressivenessSetting(handling: NewSettingsHandling) {
    var showExpressiveness by handling.rememberShowExpressiveness()
    SwitchSetting(
        settingTitle = { Text("Show Expressiveness") },
        settingIcon = { Icon(Icons.Default.Animation, null, modifier = Modifier.fillMaxSize()) },
        summaryValue = { Text("Have the animations be expressive!") },
        value = showExpressiveness,
        updateValue = { showExpressiveness = it }
    )
}


