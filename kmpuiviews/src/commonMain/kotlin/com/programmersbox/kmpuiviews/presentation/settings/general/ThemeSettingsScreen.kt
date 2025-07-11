package com.programmersbox.kmpuiviews.presentation.settings.general

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PaletteSwatchType
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.kmpuiviews.presentation.components.ThemeItem
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowMoreSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.seedColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.amoled_mode
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.choose_a_theme
import otakuworld.kmpuiviews.generated.resources.theme_choice_title

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

        CategoryGroup {
            item { PaletteSetting(handling = handling) }
            item { ColorBlindTypeSettings(handling = handling) }
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
        settingTitle = { Text(stringResource(Res.string.theme_choice_title)) },
        dialogIcon = { Icon(Icons.Default.SettingsBrightness, null) },
        settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
        dialogTitle = { Text(stringResource(Res.string.choose_a_theme)) },
        summaryValue = { Text(themeText) },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
        value = themeSetting,
        options = listOf(SystemThemeMode.FollowSystem, SystemThemeMode.Day, SystemThemeMode.Night),
        updateValue = { it, d ->
            d.value = false
            themeSetting = it
        }
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

    CategoryGroupDefaults.Divider()

    ShowWhen(usePalette) {
        var paletteSwatchType by rememberSwatchType()
        ListSetting(
            settingTitle = { Text("Swatch Type") },
            dialogIcon = { Icon(Icons.Default.Palette, null) },
            settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text("Choose a Swatch Type to use") },
            summaryValue = { Text(paletteSwatchType.name) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
            value = paletteSwatchType,
            options = PaletteSwatchType.entries,
            updateValue = { it, d ->
                d.value = false
                paletteSwatchType = it
            }
        )

        CategoryGroupDefaults.Divider()

        var paletteStyle by rememberSwatchStyle()
        ListSetting(
            settingTitle = { Text("Swatch Style") },
            dialogIcon = { Icon(Icons.Default.Palette, null) },
            settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text("Choose a Swatch Style to use") },
            summaryValue = { Text(paletteStyle.name) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
            value = paletteStyle,
            options = PaletteStyle.entries,
            updateValue = { it, d ->
                d.value = false
                paletteStyle = it
            }
        )
    }
}