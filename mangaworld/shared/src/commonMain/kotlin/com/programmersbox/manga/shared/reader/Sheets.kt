package com.programmersbox.manga.shared.reader

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderBottom
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.presentation.components.settings.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.manga.shared.settings.ImageLoaderSettings
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(
    onDismiss: () -> Unit,
    mangaSettingsHandling: MangaNewSettingsHandling,
    readerType: ReaderType,
    readerTypeChange: (ReaderType) -> Unit,
    modifier: Modifier = Modifier,
    settingsHandling: NewSettingsHandling = LocalSettingsHandling.current,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        content = {
            CategorySetting(
                settingIcon = {
                    IconButton(
                        onClick = {
                            scope.launch { sheetState.hide() }
                                .invokeOnCompletion { onDismiss() }
                        }
                    ) { Icon(Icons.Default.Close, null) }
                },
                settingTitle = { Text("Settings") }
            )
            SliderSetting(
                scope = scope,
                settingIcon = Icons.Default.FormatLineSpacing,
                settingTitle = "Reader Padding Between Pages",
                settingSummary = "Default is 4",
                preferenceUpdate = { mangaSettingsHandling.pagePadding.updateSetting(it) },
                initialValue = runBlocking { mangaSettingsHandling.pagePadding.flow.firstOrNull() ?: 4 },
                range = 0f..10f
            )
            HorizontalDivider()

            var userGestureAllowed by mangaSettingsHandling.rememberUserGestureEnabled()
            SwitchSetting(
                value = userGestureAllowed,
                updateValue = { userGestureAllowed = it },
                settingTitle = { Text("Allow User Gestures for Chapter List in Reader") },
                settingIcon = { Icon(Icons.Default.Gesture, null, modifier = Modifier.fillMaxSize()) }
            )

            var useFloatingBottomBar by mangaSettingsHandling.rememberUseFloatingReaderBottomBar()
            SwitchSetting(
                value = useFloatingBottomBar,
                updateValue = { useFloatingBottomBar = it },
                settingTitle = { Text("Use a Floating Bottom Bar in Reader") },
                settingIcon = { Icon(Icons.Default.BorderBottom, null, modifier = Modifier.fillMaxSize()) }
            )

            HorizontalDivider()

            var showReaderTypeDropdown by remember { mutableStateOf(false) }

            PreferenceSetting(
                settingTitle = { Text("Reader Type") },
                endIcon = {
                    DropdownMenu(
                        expanded = showReaderTypeDropdown,
                        onDismissRequest = { showReaderTypeDropdown = false }
                    ) {
                        ReaderType.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.name) },
                                leadingIcon = {
                                    if (it == readerType) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                },
                                onClick = {
                                    readerTypeChange(it)
                                    showReaderTypeDropdown = false
                                }
                            )
                        }
                    }
                    Text(readerType.name)
                },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable { showReaderTypeDropdown = true }
            )

            HorizontalDivider()

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

            PreferenceSetting(
                settingTitle = { Text("Image Loader Settings") },
                settingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = { showImageLoaderSettings = true }
                )
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SliderSetting(
    scope: CoroutineScope,
    settingIcon: ImageVector,
    settingTitle: String,
    settingSummary: String,
    preferenceUpdate: suspend (Int) -> Unit,
    initialValue: Int,
    range: ClosedFloatingPointRange<Float>,
) {
    var sliderValue by remember { mutableFloatStateOf(initialValue.toFloat()) }

    SliderSetting(
        sliderValue = sliderValue,
        settingTitle = { Text(settingTitle) },
        settingSummary = { Text(settingSummary) },
        range = range,
        updateValue = {
            sliderValue = it
            scope.launch { preferenceUpdate(sliderValue.toInt()) }
        },
        settingIcon = { Icon(settingIcon, null) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun SheetView(
    readVm: ReadViewModel,
    onSheetHide: () -> Unit,
    currentPage: Int,
    pages: List<String>,
    onPageChange: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(sheetScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = sheetScrollBehavior,
                title = { Text(readVm.list.getOrNull(readVm.currentChapter)?.name.orEmpty()) },
                actions = { PageIndicator(currentPage + 1, pages.size) },
                navigationIcon = {
                    IconButton(onClick = onSheetHide) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            )
        }
    ) { p ->
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(pages) { i, it ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        .border(
                            animateDpAsState(if (currentPage == i) 4.dp else 0.dp, label = "").value,
                            color = animateColorAsState(
                                if (currentPage == i) MaterialTheme.colorScheme.primary
                                else Color.Transparent, label = ""
                            ).value
                        )
                        .clickable {
                            scope.launch {
                                if (currentPage == i) onSheetHide()
                                onPageChange(i)
                            }
                        }
                ) {
                    KamelImage(
                        resource = { asyncPainterResource(it) },
                        onLoading = { CircularProgressIndicator(progress = { it }) },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black
                                    ),
                                    startY = 50f
                                ),
                                alpha = .5f
                            )
                    ) {
                        Text(
                            (i + 1).toString(),
                            style = MaterialTheme
                                .typography
                                .bodyLarge
                                .copy(textAlign = TextAlign.Center, color = Color.White),
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddToFavoritesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onAddToFavorites: () -> Unit,
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to Favorites?") },
            text = {
                Text("You have read a few chapters and seem to have some interest in this manga. Would you like to add it to your favorites?")
            },
            confirmButton = {
                TextButton(
                    onClick = onAddToFavorites
                ) {
                    Icon(Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.size(4.dp))
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) { Text("Cancel") }
            }
        )
    }
}