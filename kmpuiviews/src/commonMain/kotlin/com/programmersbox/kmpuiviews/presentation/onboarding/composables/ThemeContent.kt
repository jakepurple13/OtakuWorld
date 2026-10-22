package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.material3.rememberDynamicColorScheme
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.kmpuiviews.presentation.components.ThemeItem
import com.programmersbox.kmpuiviews.presentation.components.seedColor
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.AmoledModeSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.ColorBlindTypeSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ExpressivenessSetting
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeContent(
    handling: NewSettingsHandling = koinInject(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Pick your look") },
            supportingContent = { Text("Changes show up right away.") },
        )

        ThemePreview(modifier = Modifier.padding(horizontal = 16.dp))

        var themeSetting by handling.rememberSystemThemeMode()

        val themeModes = listOf(
            SystemThemeMode.FollowSystem to "System",
            SystemThemeMode.Night to "Dark",
            SystemThemeMode.Day to "Light",
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            themeModes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = themeSetting == mode,
                    onClick = { themeSetting = mode },
                    shape = SegmentedButtonDefaults.itemShape(index, themeModes.size),
                    label = { Text(label) },
                )
            }
        }

        var isAmoledMode by handling.rememberIsAmoledMode()
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

        CategoryGroup {
            item {
                AnimatedVisibility(
                    visible = themeSetting != SystemThemeMode.Day
                ) {
                    AmoledModeSetting(
                        isAmoledMode = isAmoledMode,
                        onAmoledModeChange = { isAmoledMode = it }
                    )
                }
            }

            item { ExpressivenessSetting(handling = handling) }

            item {
                var usePalette by handling.rememberUsePalette()
                SwitchSetting(
                    settingTitle = { Text("Color details from cover") },
                    summaryValue = { Text("Tint the details screen with colors from the cover art") },
                    settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                    value = usePalette,
                    updateValue = { usePalette = it },
                )
            }

            item {
                var gridChoice by handling.rememberGridChoice()
                ListSetting(
                    settingTitle = { Text("Grid") },
                    settingIcon = { Icon(Icons.Default.GridView, null, modifier = Modifier.fillMaxSize()) },
                    dialogIcon = { Icon(Icons.Default.GridView, null) },
                    dialogTitle = { Text("Grid type") },
                    summaryValue = {
                        Text(
                            when (gridChoice) {
                                GridChoice.FullAdaptive -> "Full adaptive: as many columns as fit"
                                GridChoice.Adaptive -> "Adaptive: adjusts as best it can"
                                GridChoice.Fixed -> "Fixed: 3, 5 or 6 columns by screen size"
                            }
                        )
                    },
                    confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
                    value = gridChoice,
                    options = listOf(GridChoice.FullAdaptive, GridChoice.Adaptive, GridChoice.Fixed),
                    updateValue = { it, d ->
                        d.value = false
                        gridChoice = it
                    }
                )
            }

            item { ColorBlindTypeSettings(handling = handling) }
        }
    }
}

/** A tiny details row so theme changes have something to show on. */
@Composable
private fun ThemePreview(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(width = 48.dp, height = 68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
            Column {
                Text(
                    "Preview title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Chapter 12 · 3 min ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
