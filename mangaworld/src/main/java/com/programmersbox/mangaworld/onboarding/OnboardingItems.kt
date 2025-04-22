package com.programmersbox.mangaworld.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.programmersbox.kmpuiviews.presentation.components.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.SwitchSetting
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangaworld.settings.ImageLoaderSettings
import com.programmersbox.mangaworld.settings.ReaderTypeSetting
import com.programmersbox.mangaworld.settings.SpacingSetting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderOnboarding(
    mangaSettingsHandling: MangaNewSettingsHandling,
) {
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Customize your reading experience") },
            supportingContent = { Text("There are more settings that can be set in the settings menu") },
        )

        HorizontalDivider()

        var readerType by mangaSettingsHandling.rememberReaderType()

        ReaderTypeSetting(
            readerType = readerType,
            onReaderTypeChange = { readerType = it }
        )

        val imageLoaderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        var showImageLoaderSettings by remember { mutableStateOf(false) }

        if (showImageLoaderSettings) {
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

        val imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()

        PreferenceSetting(
            settingTitle = { Text("Image Loader") },
            endIcon = { Text(imageLoaderType.name) },
            settingIcon = { Icon(Icons.Default.Image, null) },
            modifier = Modifier.clickable { showImageLoaderSettings = true }
        )

        var padding by remember {
            mutableFloatStateOf(
                runBlocking { mangaSettingsHandling.pagePadding.flow.first().toFloat() }
            )
        }

        SpacingSetting(
            readerPadding = padding,
            onPaddingChange = { padding = it },
            onPaddingChangeFinished = { scope.launch { mangaSettingsHandling.pagePadding.updateSetting(padding.toInt()) } },
        )

        HorizontalDivider()

        var includeInsets by mangaSettingsHandling.rememberIncludeInsetsForReader()

        SwitchSetting(
            value = includeInsets,
            updateValue = { includeInsets = it },
            settingTitle = { Text("Include insets in Reader") },
            settingIcon = { Icon(Icons.Default.BorderOuter, null, modifier = Modifier.fillMaxSize()) }
        )
    }
}