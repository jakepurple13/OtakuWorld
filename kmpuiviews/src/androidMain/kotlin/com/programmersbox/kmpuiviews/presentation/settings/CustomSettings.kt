package com.programmersbox.kmpuiviews.presentation.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.ScreensaverType
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreensaverTypeSettings(handling: NewSettingsHandling) {
    var screensaverType by handling.rememberScreensaverType()
    var showScreensaverSettings by remember { mutableStateOf(false) }

    if (showScreensaverSettings) {
        AlertDialog(
            onDismissRequest = { showScreensaverSettings = false },
            confirmButton = {
                TextButton(onClick = { showScreensaverSettings = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            title = { Text("Screensaver") },
            text = {
                LazyColumn {
                    items(ScreensaverType.entries) {
                        Surface(
                            onClick = {
                                screensaverType = it
                                showScreensaverSettings = false
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = it == screensaverType,
                                    onClick = {
                                        screensaverType = it
                                        showScreensaverSettings = false
                                    },
                                    modifier = Modifier.padding(8.dp),
                                )
                                Text(
                                    it.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    val context = LocalContext.current

    CategoryGroupListItem {
        segmentedListItem(
            onClick = { showScreensaverSettings = true },
            content = { Text("Screensaver type") },
            supportingContent = {
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
            leadingContent = { Icon(Icons.Default.Slideshow, null) }
        )

        segmentedListItem(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
                }
            },
            leadingContent = { Icon(Icons.Default.SettingsSystemDaydream, null) },
            content = { Text("View Screensaver Settings") }
        )
    }
}