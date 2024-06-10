package com.programmersbox.mangaworld.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.programmersbox.mangasettings.PlayingMiddleAction
import com.programmersbox.mangasettings.PlayingStartAction
import com.programmersbox.mangasettings.ReaderType
import com.programmersbox.mangaworld.MangaSettingsHandling
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.reader.compose.ReaderTopBar
import com.programmersbox.uiviews.utils.CategorySetting
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.SliderSetting
import com.programmersbox.uiviews.utils.SwitchSetting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerSettings(
    mangaSettingsHandling: MangaSettingsHandling,
    onImageLoaderClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var padding by remember {
        mutableFloatStateOf(
            runBlocking { mangaSettingsHandling.pagePadding.flow.first().toFloat() }
        )
    }

    SliderSetting(
        sliderValue = padding,
        settingTitle = { Text(stringResource(R.string.reader_padding_between_pages)) },
        settingSummary = { Text(stringResource(R.string.default_padding_summary)) },
        settingIcon = { Icon(Icons.Default.FormatLineSpacing, null) },
        range = 0f..10f,
        updateValue = { padding = it },
        onValueChangedFinished = { scope.launch { mangaSettingsHandling.pagePadding.updateSetting(padding.toInt()) } }
    )

    var reader by mangaSettingsHandling.rememberUseNewReader()

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.useNewReader)) },
        summaryValue = { Text(stringResource(R.string.reader_summary_setting)) },
        settingIcon = { Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, null, modifier = Modifier.fillMaxSize()) },
        value = reader,
        updateValue = { reader = it }
    )

    var readerType by mangaSettingsHandling.rememberReaderType()

    ShowWhen(reader) {
        Column {
            var showReaderTypeDropdown by remember { mutableStateOf(false) }

            PreferenceSetting(
                settingTitle = { Text("Reader Type") },
                endIcon = {
                    DropdownMenu(
                        expanded = showReaderTypeDropdown,
                        onDismissRequest = { showReaderTypeDropdown = false }
                    ) {
                        ReaderType.entries
                            .filter { it != ReaderType.UNRECOGNIZED }
                            .forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    leadingIcon = {
                                        if (it == readerType) {
                                            Icon(Icons.Default.Check, null)
                                        }
                                    },
                                    onClick = {
                                        readerType = it
                                        showReaderTypeDropdown = false
                                    }
                                )
                            }
                    }
                    Text(readerType.name)
                },
                modifier = Modifier.clickable { showReaderTypeDropdown = true }
            )

            HorizontalDivider()

            PreferenceSetting(
                settingTitle = { Text("Image Loader Settings") },
                settingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = onImageLoaderClick
                )
            )

            HorizontalDivider()

            CategorySetting { Text("Top Bar Settings") }

            var startAction by mangaSettingsHandling.rememberPlayingStartAction()
            var middleAction by mangaSettingsHandling.rememberPlayingMiddleAction()

            var showStartDropdown by remember { mutableStateOf(false) }

            PreferenceSetting(
                settingTitle = { Text("Start Option") },
                endIcon = {
                    DropdownMenu(
                        expanded = showStartDropdown,
                        onDismissRequest = { showStartDropdown = false }
                    ) {
                        PlayingStartAction.entries
                            .filter { it != PlayingStartAction.UNRECOGNIZED }
                            .forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    leadingIcon = {
                                        if (it == startAction) {
                                            Icon(Icons.Default.Check, null)
                                        }
                                    },
                                    onClick = {
                                        startAction = it
                                        showStartDropdown = false
                                    }
                                )
                            }
                    }
                    Text(startAction.name)
                },
                modifier = Modifier.clickable { showStartDropdown = true }
            )

            var showMiddleDropdown by remember { mutableStateOf(false) }

            PreferenceSetting(
                settingTitle = { Text("Middle Option") },
                endIcon = {
                    DropdownMenu(
                        expanded = showMiddleDropdown,
                        onDismissRequest = { showMiddleDropdown = false }
                    ) {
                        PlayingMiddleAction.entries
                            .filter { it != PlayingMiddleAction.UNRECOGNIZED }
                            .forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    leadingIcon = {
                                        if (it == middleAction) {
                                            Icon(Icons.Default.Check, null)
                                        }
                                    },
                                    onClick = {
                                        middleAction = it
                                        showMiddleDropdown = false
                                    }
                                )
                            }
                    }
                    Text(middleAction.name)
                },
                modifier = Modifier.clickable { showMiddleDropdown = true }
            )

            ReaderTopBar(
                pages = List(10) { "" },
                currentPage = 5,
                currentChapter = "Ch 10",
                playingStartAction = startAction,
                playingMiddleAction = middleAction,
                showBlur = false,
                modifier = Modifier.border(
                    2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}