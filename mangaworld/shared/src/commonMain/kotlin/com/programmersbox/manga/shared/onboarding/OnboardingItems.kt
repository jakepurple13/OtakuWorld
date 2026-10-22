package com.programmersbox.manga.shared.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.manga.shared.settings.ImageLoaderSettings
import com.programmersbox.manga.shared.settings.SpacingSetting
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderOnboarding(
    mangaSettingsHandling: MangaNewSettingsHandling,
) {
    val scope = rememberCoroutineScope()

    var readerType by mangaSettingsHandling.rememberReaderType()
    val imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()
    var includeInsets by mangaSettingsHandling.rememberIncludeInsetsForReader()
    var showImageLoaderSettings by remember { mutableStateOf(false) }

    var padding by remember {
        mutableFloatStateOf(
            runBlocking { mangaSettingsHandling.pagePadding.flow.first().toFloat() }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Reader") },
            supportingContent = { Text("Customize your reading experience. There are more options in Settings.") },
        )

        CategoryGroup {
            categorySetting { Text("Reader type") }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ReaderType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = readerType == type,
                                onClick = { readerType = type },
                                shape = SegmentedButtonDefaults.itemShape(index, ReaderType.entries.size),
                                label = { Text(type.label) },
                            )
                        }
                    }
                    Text(
                        readerType.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        CategoryGroup {
            categorySetting { Text("Pages") }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Image Loader") },
                    summaryValue = { Text(imageLoaderType.name) },
                    settingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.fillMaxSize()) },
                    onClick = { showImageLoaderSettings = true },
                )
            }

            item {
                SpacingSetting(
                    readerPadding = padding,
                    onPaddingChange = { padding = it },
                    onPaddingChangeFinished = { scope.launch { mangaSettingsHandling.pagePadding.updateSetting(padding.toInt()) } },
                )
            }

            item {
                SwitchSetting(
                    value = includeInsets,
                    updateValue = { includeInsets = it },
                    settingTitle = { Text("Include insets in Reader") },
                    summaryValue = { Text("Keep pages clear of the status and navigation bars") },
                    settingIcon = { Icon(Icons.Default.BorderOuter, null, modifier = Modifier.fillMaxSize()) }
                )
            }
        }
    }

    if (showImageLoaderSettings) {
        val imageLoaderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showImageLoaderSettings = false },
            sheetState = imageLoaderSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CategorySetting(
                settingIcon = {
                    IconButton(
                        onClick = {
                            scope.launch { imageLoaderSheetState.hide() }
                                .invokeOnCompletion { showImageLoaderSettings = false }
                        }
                    ) { Icon(Icons.Default.Close, null) }
                },
                settingTitle = { Text("Image Loader Settings") }
            )
            ImageLoaderSettings(
                mangaSettingsHandling = mangaSettingsHandling,
                windowInsets = WindowInsets(0.dp),
                navigationButton = {}
            )
        }
    }
}

private val ReaderType.label: String
    get() = when (this) {
        ReaderType.List -> "List"
        ReaderType.Pager -> "Pager"
        ReaderType.FlipPager -> "Flip"
        ReaderType.CurlPager -> "Curl"
    }

private val ReaderType.description: String
    get() = when (this) {
        ReaderType.List -> "Scroll down through every page."
        ReaderType.Pager -> "Swipe sideways, one page at a time."
        ReaderType.FlipPager -> "Pages flip over like a book."
        ReaderType.CurlPager -> "Pages curl as you turn them."
    }

/** Placeholder manga page for the reader tip demo: a few panel outlines. */
@Composable
fun BoxScope.SampleMangaPage() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .matchParentSize()
            .padding(10.dp)
    ) {
        listOf(0.35f, 0.25f, 0.4f).forEach { weight ->
            Box(
                Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            )
        }
    }
}
