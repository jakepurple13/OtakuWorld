package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.ScreensaverType
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ScreensaverTypeSettings(handling: NewSettingsHandling) {
    var screensaverType by handling.rememberScreensaverType()

    ListSetting(
        settingTitle = { Text("Screensaver") },
        settingIcon = { Icon(Icons.Default.Slideshow, null, modifier = Modifier.fillMaxSize()) },
        value = screensaverType,
        updateValue = { it, d ->
            d.value = false
            screensaverType = it
        },
        options = ScreensaverType.entries,
        summaryValue = {
            Text(
                when (screensaverType) {
                    ScreensaverType.Dashboard -> "Dashboard: Clock, battery, activity, and saved items."
                    ScreensaverType.CoverCarousel -> "Cover Carousel: Full-screen rotating cover art."
                    ScreensaverType.HistoryFeed -> "History Feed: Recently viewed items."
                    ScreensaverType.StatsDashboard -> "Stats: Reading/watching activity totals."
                    ScreensaverType.CustomListRotation -> "Custom Lists: Cycles through your custom lists."
                }
            )
        },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
        dialogTitle = { Text("Screensaver") },
        dialogIcon = { Icon(Icons.Default.Slideshow, null) },
    )
}