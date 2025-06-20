package com.programmersbox.manga.shared.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.mangasettings.MangaNewSettingsHandling

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerSettings(
    mangaSettingsHandling: MangaNewSettingsHandling,
) {
    CategoryGroup {
        item {
            var reader by mangaSettingsHandling.rememberUseNewReader()
            SwitchSetting(
                settingTitle = { Text("Use New Reader") },
                summaryValue = { Text("If the new reader crashes, please use the old reader. The new reader is still being worked on.") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, null, modifier = Modifier.fillMaxSize()) },
                value = reader,
                updateValue = { reader = it }
            )
        }
    }
}