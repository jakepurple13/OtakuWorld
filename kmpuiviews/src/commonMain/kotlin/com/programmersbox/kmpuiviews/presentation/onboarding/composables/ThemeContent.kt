package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.kmpuiviews.presentation.components.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.ThemeItem
import com.programmersbox.kmpuiviews.presentation.components.seedColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.amoled_mode
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.choose_a_theme
import otakuworld.kmpuiviews.generated.resources.theme_choice_title

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeContent(
    handling: NewSettingsHandling = koinInject(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Pick your theme!") },
        )

        HorizontalDivider()

        var themeSetting by handling.rememberSystemThemeMode()

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

        var isAmoledMode by handling.rememberIsAmoledMode()

        AnimatedVisibility(
            visible = themeSetting != SystemThemeMode.Day
        ) {
            SwitchSetting(
                settingTitle = { Text(stringResource(Res.string.amoled_mode)) },
                settingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.fillMaxSize()) },
                value = isAmoledMode,
                updateValue = { isAmoledMode = it }
            )
        }

        var themeColor by handling.rememberThemeColor()

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeColor.entries
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
                                    else -> isSystemInDarkTheme()
                                },
                                isAmoled = isAmoledMode
                            )
                    )
                }
        }
    }
}
