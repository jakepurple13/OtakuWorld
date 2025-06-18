package com.programmersbox.mangaworld.settings

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
import androidx.compose.ui.res.stringResource
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangaworld.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerSettings(
    mangaSettingsHandling: MangaNewSettingsHandling,
) {
    CategoryGroup {
        item {
            var reader by mangaSettingsHandling.rememberUseNewReader()
            SwitchSetting(
                settingTitle = { Text(stringResource(R.string.useNewReader)) },
                summaryValue = { Text(stringResource(R.string.reader_summary_setting)) },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, null, modifier = Modifier.fillMaxSize()) },
                value = reader,
                updateValue = { reader = it }
            )
        }
    }
}