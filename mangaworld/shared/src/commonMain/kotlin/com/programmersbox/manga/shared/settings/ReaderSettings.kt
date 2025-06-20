package com.programmersbox.manga.shared.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BorderBottom
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.settings.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.manga.shared.reader.FloatingBottomBar
import com.programmersbox.manga.shared.reader.ReaderTopBar
import com.programmersbox.manga.shared.reader.dpToPx
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data object ReaderSettingsScreen : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettings(
    mangaSettingsHandling: MangaNewSettingsHandling,
    settingsHandling: NewSettingsHandling,
) {
    HideNavBarWhileOnScreen()

    val scope = rememberCoroutineScope()

    var includeInsets by mangaSettingsHandling.rememberIncludeInsetsForReader()

    var useFloatingBar by mangaSettingsHandling.rememberUseFloatingReaderBottomBar()

    val imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()

    var padding by remember {
        mutableFloatStateOf(
            runBlocking { mangaSettingsHandling.pagePadding.flow.first().toFloat() }
        )
    }

    var allowUserGesture by mangaSettingsHandling.rememberUserGestureEnabled()

    var readerType by mangaSettingsHandling.rememberReaderType()

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

    ReaderSettings(
        imageLoaderType = imageLoaderType,
        onImageLoaderClick = { showImageLoaderSettings = true },
        readerType = readerType,
        onReaderTypeChange = { readerType = it },
        useFloatingBar = useFloatingBar,
        onUseFloatingBarChange = { useFloatingBar = it },
        allowUserGesture = allowUserGesture,
        onAllowUserGestureChange = { allowUserGesture = it },
        readerPadding = padding,
        includeInsets = includeInsets,
        onIncludeInsetsChange = { includeInsets = it },
        onPaddingChange = { padding = it },
        onPaddingChangeFinished = { scope.launch { mangaSettingsHandling.pagePadding.updateSetting(padding.toInt()) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
private fun ReaderSettings(
    imageLoaderType: ImageLoaderType,
    useFloatingBar: Boolean,
    readerPadding: Float,
    allowUserGesture: Boolean,
    readerType: ReaderType = ReaderType.List,
    onReaderTypeChange: (ReaderType) -> Unit = {},
    includeInsets: Boolean,
    onIncludeInsetsChange: (Boolean) -> Unit = {},
    onAllowUserGestureChange: (Boolean) -> Unit = {},
    onPaddingChange: (Float) -> Unit = {},
    onPaddingChangeFinished: () -> Unit = {},
    onImageLoaderClick: () -> Unit = {},
    onUseFloatingBarChange: (Boolean) -> Unit = {},
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Reader Settings") },
                    navigationIcon = { BackButton() }
                )

                ReaderTopBar(
                    currentChapter = "13",
                    onSettingsClick = {},
                    showBlur = false
                )
            }
        },
        bottomBar = {
            if (!useFloatingBar) {
                //BottomBar()
                FloatingBottomBar(
                    onChapterShow = {},
                    onPageSelectClick = {},
                    onNextChapter = {},
                    onPreviousChapter = {},
                    showBlur = false,
                    isAmoledMode = false,
                    currentPage = 0,
                    pages = 13,
                    chapterNumber = "13",
                    chapterCount = "13",
                    previousButtonEnabled = true,
                    nextButtonEnabled = true,
                    modifier = Modifier
                        .padding(16.dp)
                        .windowInsetsPadding(if (includeInsets) NavigationBarDefaults.windowInsets else WindowInsets(0.dp))
                )
            }
        },
        floatingActionButton = {
            if (useFloatingBar) {
                FloatingBottomBar()
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ImageLoaderSetting(
                    imageLoaderType = imageLoaderType,
                    onImageLoaderChange = onImageLoaderClick
                )
            }

            item { HorizontalDivider() }

            item {
                SpacingSetting(
                    readerPadding = readerPadding,
                    onPaddingChange = onPaddingChange,
                    onPaddingChangeFinished = onPaddingChangeFinished
                )
            }

            item { HorizontalDivider() }

            item {
                SwitchSetting(
                    value = allowUserGesture,
                    updateValue = onAllowUserGestureChange,
                    settingTitle = { Text("Allow User Gestures for Chapter List in Reader") },
                    settingIcon = { Icon(Icons.Default.Gesture, null, modifier = Modifier.fillMaxSize()) }
                )
            }

            item { HorizontalDivider() }

            item {
                ReaderTypeSetting(
                    readerType = readerType,
                    onReaderTypeChange = onReaderTypeChange
                )
            }

            item { HorizontalDivider() }

            item {
                SwitchSetting(
                    value = useFloatingBar,
                    updateValue = onUseFloatingBarChange,
                    settingTitle = { Text("Use a Floating Bottom Bar in Reader") },
                    settingIcon = { Icon(Icons.Default.BorderBottom, null, modifier = Modifier.fillMaxSize()) }
                )
            }

            item {
                SwitchSetting(
                    value = includeInsets,
                    updateValue = onIncludeInsetsChange,
                    settingTitle = { Text("Include insets in Reader") },
                    settingIcon = { Icon(Icons.Default.BorderOuter, null, modifier = Modifier.fillMaxSize()) }
                )
            }
        }
    }
}

@Composable
fun ReaderTypeSetting(
    readerType: ReaderType,
    onReaderTypeChange: (ReaderType) -> Unit = {},
) {
    var showReaderTypeDropdown by remember { mutableStateOf(false) }

    PreferenceSetting(
        settingTitle = { Text("Reader Type") },
        settingIcon = { Icon(Icons.AutoMirrored.Default.ChromeReaderMode, null) },
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
                            onReaderTypeChange(it)
                            showReaderTypeDropdown = false
                        }
                    )
                }
            }
            Text(readerType.name)
        },
        modifier = Modifier.clickable { showReaderTypeDropdown = true }
    )
}

@Composable
fun ImageLoaderSetting(
    imageLoaderType: ImageLoaderType,
    onImageLoaderChange: () -> Unit = {},
) {
    Surface(
        onClick = onImageLoaderChange,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Image Loader")
            Text(imageLoaderType.name)
            imageLoaderType.Composed(
                url = "https://picsum.photos/480/360",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    .clip(MaterialTheme.shapes.medium)
            )
        }
    }
}


@Composable
fun SpacingSetting(
    readerPadding: Float,
    onPaddingChange: (Float) -> Unit = {},
    onPaddingChangeFinished: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SliderSetting(
            sliderValue = readerPadding,
            settingTitle = { Text("Reader Padding Between Pages") },
            settingSummary = { Text("Default is 4") },
            settingIcon = { Icon(Icons.Default.FormatLineSpacing, null) },
            range = 0f..10f,
            updateValue = onPaddingChange,
            onValueChangedFinished = onPaddingChangeFinished
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    MaterialTheme.shapes.small.copy(
                        topStart = CornerSize(0f),
                        topEnd = CornerSize(0f)
                    )
                )
                .height(16.dp)
                .background(Color.White)
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(animateDpAsState(dpToPx(readerPadding.toInt()).dp).value)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    MaterialTheme.shapes.small.copy(
                        bottomStart = CornerSize(0f),
                        bottomEnd = CornerSize(0f)
                    )
                )
                .height(16.dp)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
) {
    var showFloatBar by remember { mutableStateOf(false) }
    val onShowFloatBarChange: (Boolean) -> Unit = { showFloatBar = it }
    VerticalFloatingToolbar(
        expanded = showFloatBar,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = { onShowFloatBarChange(!showFloatBar) },
            ) {
                Icon(
                    if (showFloatBar) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = "Localized description"
                )
            }
        },
        modifier = modifier
    ) {
        IconButton(
            onClick = {},
        ) { Icon(Icons.Default.ArrowBack, null) }

        OutlinedIconButton(
            onClick = {},
            border = BorderStroke(ButtonDefaults.outlinedButtonBorder(true).width, MaterialTheme.colorScheme.primary)
        ) { Icon(Icons.Default.Home, null) }

        FilledIconButton(
            onClick = {},
        ) { Icon(Icons.Default.ArrowForward, null) }

        IconButton(
            onClick = {},
        ) { Icon(Icons.Default.GridOn, null) }

        IconButton(
            onClick = {},
        ) { Icon(Icons.Default.Numbers, null) }

        IconButton(
            onClick = {},
        ) { Icon(Icons.Default.Settings, null) }
    }
}
