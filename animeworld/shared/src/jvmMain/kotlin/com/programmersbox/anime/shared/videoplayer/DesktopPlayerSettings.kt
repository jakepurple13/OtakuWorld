package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRoute
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import org.koin.compose.koinInject

/** Desktop's Player group on the Play Settings screen: style picker with live preview, subtitles, blur. */
@Composable
fun DesktopPlayerSettings() {
    val navigationActions = koinInject<NavigationActions>()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CategoryGroup {
            item { PlayerStyleInlinePicker(PlayerPreviewDefaults.Desktop) }
        }
        CategoryGroup {
            item {
                PreferenceSetting(
                    settingTitle = { Text("Subtitles") },
                    summaryValue = { Text("Size, color and outline") },
                    settingIcon = { Icon(Icons.Default.Subtitles, null, modifier = Modifier.fillMaxSize()) },
                    onClick = { navigationActions.navigate(SubtitleSettingsRoute) },
                )
            }
            item {
                var blur by videoPlayerBlur.asState()
                SwitchSetting(
                    settingTitle = { Text("Video player blur") },
                    summaryValue = { Text("Blur the player's bars with your blur kind") },
                    settingIcon = { Icon(Icons.Default.BlurOn, null, modifier = Modifier.fillMaxSize()) },
                    value = blur,
                    updateValue = { blur = it },
                )
            }
        }
    }
}

/** Desktop's onboarding page for the player. */
@Composable
fun DesktopPlayerOnboarding() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Set up the player") },
            supportingContent = { Text("There are more player settings in Settings.") },
        )
        DesktopPlayerSettings()
    }
}
