package com.programmersbox.kmpuiviews.presentation.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DetailsChapterSwipeBehavior
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSettingsScreen() {
    val handling: NewSettingsHandling = koinInject()

    SettingsScaffold(
        title = "Details",
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailsChapterSwipeBehaviorSettings(handling)

        CategoryGroup {
            item { ShareChapterSettings(handling = handling) }
            item { ShowDownloadSettings(handling = handling) }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DetailsChapterSwipeBehaviorSettings(handling: NewSettingsHandling) {
    var detailsChapterSwipeBehavior by handling.detailsChapterSwipeBehavior.rememberPreference()
    CategoryGroup {
        categorySetting { Text("Details Chapter Swipe Behavior") }

        item {
            ListSetting(
                settingTitle = { Text("Start to End") },
                dialogIcon = { Icon(Icons.Default.SwipeRight, null) },
                settingIcon = { Icon(Icons.Default.SwipeRight, null, modifier = Modifier.fillMaxSize()) },
                dialogTitle = { Text("Choose the behavior for the start to end swipe") },
                summaryValue = { Text(detailsChapterSwipeBehavior.detailsChapterSwipeBehaviorStartToEnd.name) },
                confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
                value = detailsChapterSwipeBehavior.detailsChapterSwipeBehaviorStartToEnd,
                options = DetailsChapterSwipeBehavior.entries,
                updateValue = { it, d ->
                    d.value = false
                    detailsChapterSwipeBehavior = detailsChapterSwipeBehavior.copy(
                        detailsChapterSwipeBehaviorStartToEnd = it
                    )
                }
            )
        }

        item {
            ListSetting(
                settingTitle = { Text("End to Start") },
                dialogIcon = { Icon(Icons.Default.SwipeLeft, null) },
                settingIcon = { Icon(Icons.Default.SwipeLeft, null, modifier = Modifier.fillMaxSize()) },
                dialogTitle = { Text("Choose the behavior for the end to start swipe") },
                summaryValue = { Text(detailsChapterSwipeBehavior.detailsChapterSwipeBehaviorEndToStart.name) },
                confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
                value = detailsChapterSwipeBehavior.detailsChapterSwipeBehaviorEndToStart,
                options = DetailsChapterSwipeBehavior.entries,
                updateValue = { it, d ->
                    d.value = false
                    detailsChapterSwipeBehavior = detailsChapterSwipeBehavior.copy(
                        detailsChapterSwipeBehaviorEndToStart = it
                    )
                }
            )
        }
    }
}