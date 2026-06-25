package com.programmersbox.kmpuiviews.presentation.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PaletteSwatchType
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ColorsScreen() {
    val handling: NewSettingsHandling = koinInject()

    SettingsScaffold(
        title = "Colors",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroup {
            item {
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
        }

        CategoryGroup {
            item {
                var usePalette by handling.rememberUsePalette()
                SwitchSetting(
                    settingTitle = { Text("Use Palette") },
                    summaryValue = { Text("Color the details screen using image palette") },
                    settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                    value = usePalette,
                    updateValue = { usePalette = it },
                )
                CategoryGroupDefaults.Divider()
                ShowWhen(usePalette) {
                    var paletteSwatchType by rememberSwatchType()
                    ListSetting(
                        settingTitle = { Text("Swatch Type") },
                        dialogIcon = { Icon(Icons.Default.Palette, null) },
                        settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                        dialogTitle = { Text("Choose a Swatch Type") },
                        summaryValue = { Text(paletteSwatchType.name) },
                        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
                        value = paletteSwatchType,
                        options = PaletteSwatchType.entries,
                        updateValue = { it, d -> d.value = false; paletteSwatchType = it },
                    )
                    CategoryGroupDefaults.Divider()
                    var paletteStyle by rememberSwatchStyle()
                    ListSetting(
                        settingTitle = { Text("Swatch Style") },
                        dialogIcon = { Icon(Icons.Default.Palette, null) },
                        settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                        dialogTitle = { Text("Choose a Swatch Style") },
                        summaryValue = { Text(paletteStyle.name) },
                        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
                        value = paletteStyle,
                        options = PaletteStyle.entries,
                        updateValue = { it, d -> d.value = false; paletteStyle = it },
                    )
                }
            }
        }
    }
}
