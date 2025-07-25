package com.programmersbox.kmpuiviews.presentation.settings.extensions


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SendTimeExtension
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.pathSegments
import coil3.toUri
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.SourceIcon
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.ToolTipWrapper
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.SourceInfoRepository
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentSource
import kotlin.time.Duration.Companion.minutes

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ExtensionList(
    settingsHandling: NewSettingsHandling = koinInject(),
    backgroundWorkHandler: BackgroundWorkHandler = koinInject(),
    viewModel: ExtensionListViewModel = koinViewModel(),
) {
    val sourceInfoRepository = koinInject<SourceInfoRepository>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val iconLoader = koinInject<IconLoader>()

    val navController = LocalNavActions.current

    var showUrlDialog by remember { mutableStateOf(false) }
    if (showUrlDialog) {
        CustomUrlDialog(
            onDismissRequest = { showUrlDialog = false },
            onAddUrl = { scope.launch { settingsHandling.addCustomUrl(it) } },
            onRemoveUrl = { scope.launch { settingsHandling.removeCustomUrl(it) } },
            urls = settingsHandling.customUrls.collectAsStateWithLifecycle(initialValue = emptyList()).value
        )
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()

    BackHandler(navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (navigator.currentDestination?.contentKey == 1) {
                            "Remote Extensions"
                        } else {
                            "Installed Extensions"
                        }
                    )
                },
                navigationIcon = { BackButton() },
                actions = {
                    var showDropDown by remember { mutableStateOf(false) }
                    var checked by remember { mutableStateOf(false) }

                    if (navigator.currentDestination?.contentKey == 1) {
                        ToolTipWrapper(info = { Text("View Installed Extensions") }) {
                            IconButton(onClick = { scope.launch { navigator.navigateBack() } }) {
                                Icon(
                                    Icons.Default.SendTimeExtension, null,
                                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                                )
                            }
                        }
                    } else {
                        ToolTipWrapper(info = { Text("View Remote Extensions") }) {
                            IconButton(onClick = { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 1) } }) {
                                Icon(Icons.Default.SendTimeExtension, null)
                            }
                        }
                    }

                    IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }

                    DropdownMenu(
                        expanded = showDropDown,
                        onDismissRequest = { showDropDown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Check for Source Updates") },
                            onClick = {
                                showDropDown = false
                                if (!checked) backgroundWorkHandler.sourceUpdate()
                                scope.launch {
                                    checked = true
                                    delay(10.minutes)
                                    checked = false
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Update, null) },
                            enabled = !checked
                        )

                        DropdownMenuItem(
                            text = { Text("Refresh Extensions") },
                            onClick = {
                                showDropDown = false
                                viewModel.refreshExtensions()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Enable Incognito") },
                            onClick = {
                                showDropDown = false
                                navController.incognito()
                            },
                            leadingIcon = { Icon(Icons.Default.HideSource, null) }
                        )

                        if (viewModel.hasCustomBridge) {
                            DropdownMenuItem(
                                text = { Text("Add Custom Tachiyomi Bridge") },
                                onClick = {
                                    showUrlDialog = true
                                    showDropDown = false
                                },
                                leadingIcon = { Icon(Icons.Default.AddCircleOutline, null) },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            paneExpansionState = rememberPaneExpansionState(keyProvider = navigator.scaffoldValue),
            paneExpansionDragHandle = { state ->
                val interactionSource = remember { MutableInteractionSource() }
                VerticalDragHandle(
                    interactionSource = interactionSource,
                    modifier = Modifier.paneExpansionDraggable(
                        state = state,
                        minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                        interactionSource = interactionSource,
                        semanticsProperties = state.defaultDragHandleSemantics()
                    )
                )
            },
            listPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    InstalledExtensionItems(
                        installedSources = viewModel.installed,
                        sourcesList = viewModel.remoteSourcesVersions,
                        iconLoader = iconLoader,
                        onDownloadAndInstall = { downloadLink, destinationPath ->
                            //TODO: Need to show some ui for download progress and installing progress
                            viewModel.downloadAndInstall(downloadLink, destinationPath)
                        },
                        onUninstall = { sourceInfoRepository.uninstall(it) }
                    )
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    RemoteExtensionItems(
                        remoteSources = viewModel.remoteSources,
                        remoteSourcesShowing = viewModel.remoteSourcesShowing,
                        onDownloadAndInstall = { downloadLink, destinationPath ->
                            viewModel.downloadAndInstall(downloadLink, destinationPath)
                        },
                    )
                }
            },
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun InstalledExtensionItems(
    installedSources: Map<String?, InstalledViewState>,
    sourcesList: List<KmpRemoteSources>,
    iconLoader: IconLoader,
    onDownloadAndInstall: (String, String) -> Unit,
    onUninstall: (KmpSourceInformation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceInfoRepository = koinInject<SourceInfoRepository>()
    val currentSourceRepository = LocalCurrentSource.current
    Surface {
        Column(
            modifier = modifier
        ) {
            ListItem(
                headlineContent = {
                    val source by LocalCurrentSource.current.asFlow().collectAsStateWithLifecycle(initialValue = null)
                    Text(stringResource(Res.string.currentSource, source?.serviceName.orEmpty()))
                }
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                installedSources.forEach { (t, u) ->

                    val itemList = u
                        .sourceInformation
                        .groupBy { it.packageName }
                        .values
                        .map { it.first() }

                    item {
                        Card(
                            onClick = { u.showItems = !u.showItems },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(t ?: u.sourceInformation.firstOrNull()?.name ?: "Single Source")
                                },
                                leadingContent = { Text("(${itemList.size})") },
                                trailingContent = t?.let {
                                    {
                                        IconButton(
                                            onClick = { onUninstall(u.sourceInformation.random()) }
                                        ) { Icon(Icons.Default.Delete, null) }
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }

                    if (u.showItems) {
                        itemsIndexed(
                            itemList,
                            key = { i, it -> it.apiService.serviceName + i },
                            contentType = { _, it -> it }
                        ) { index, source ->
                            val version = remember { sourceInfoRepository.versionName(source) }
                            ExtensionItem(
                                sourceInformation = source,
                                version = version,
                                onClick = { currentSourceRepository.tryEmit(source.apiService) },
                                iconLoader = iconLoader,
                                trailingIcon = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        sourcesList.find {
                                            it.sources.any { s -> s.baseUrl == source.apiService.baseUrl }
                                        }?.let { r ->
                                            if (AppUpdate.checkForUpdate(version, r.version)) {
                                                IconButton(
                                                    onClick = {
                                                        onDownloadAndInstall(
                                                            r.downloadLink,
                                                            r.downloadLink
                                                                .toUri()
                                                                .pathSegments
                                                                .lastOrNull()
                                                                ?: "${r.name}.apk"
                                                        )
                                                    }
                                                ) { Icon(Icons.Default.Update, null) }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onUninstall(source) }
                                        ) { Icon(Icons.Default.Delete, null) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteExtensionItems(
    remoteSources: Map<String, RemoteState>,
    remoteSourcesShowing: SnapshotStateMap<String, Boolean>,
    onDownloadAndInstall: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface {
        Column(
            modifier = modifier
        ) {
            var search by remember { mutableStateOf("") }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search Remote Extensions") },
                trailingIcon = {
                    IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                remoteSources.forEach { (t, u) ->
                    val showing by derivedStateOf {
                        runCatching { remoteSourcesShowing[t]!! }.getOrDefault(false)
                    }

                    when (u) {
                        is RemoteViewState -> {
                            item {
                                ElevatedCard(
                                    onClick = { remoteSourcesShowing[t] = !showing },
                                    modifier = Modifier.animateItem()
                                ) {
                                    ListItem(
                                        headlineContent = { Text(t) },
                                        leadingContent = { Text("(${u.sources.size})") },
                                        trailingContent = {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                null,
                                                modifier = Modifier.rotateWithBoolean(showing)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }

                            if (showing) {
                                items(u.sources.filter { it.name.contains(search, true) }) {
                                    RemoteItem(
                                        remoteSource = it,
                                        onDownloadAndInstall = {
                                            onDownloadAndInstall(
                                                it.downloadLink,
                                                it.downloadLink
                                                    .toUri()
                                                    .pathSegments
                                                    .lastOrNull()
                                                    ?: "${it.name}.apk"
                                            )
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }

                        is RemoteErrorState -> {
                            item {
                                ElevatedCard {
                                    ListItem(
                                        headlineContent = { Text(t) },
                                        supportingContent = { Text("Something went wrong") },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Warning,
                                                null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionItem(
    sourceInformation: KmpSourceInformation,
    version: String,
    onClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)?,
    iconLoader: IconLoader,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        ListItem(
            overlineContent = { Text(sourceInformation.packageName) },
            headlineContent = { Text(sourceInformation.name) },
            supportingContent = { Text("Version: $version") },
            leadingContent = {
                //TODO: Need to deal with this
                SourceIcon(iconLoader, sourceInformation)
            },
            trailingContent = trailingIcon
        )
    }
}

@Composable
private fun RemoteItem(
    remoteSource: KmpRemoteSources,
    onDownloadAndInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Download and Install ${remoteSource.name}?") },
            icon = { Icon(Icons.Default.InstallMobile, null) },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDownloadAndInstall()
                        showDialog = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { Text("No") }
            }
        )
    }

    RemoteSourceItem(
        remoteSource = remoteSource,
        onExtensionClick = { showDialog = true },
        modifier = modifier
    )
}

@Composable
private fun RemoteSourceItem(
    remoteSource: KmpRemoteSources,
    onExtensionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onExtensionClick,
        modifier = modifier
    ) {
        ListItem(
            overlineContent = { Text(remoteSource.packageName) },
            headlineContent = { Text(remoteSource.name) },
            trailingContent = { Text(remoteSource.version) },
            leadingContent = {
                AsyncImage(
                    model = remoteSource.iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(32.dp)
                )
            },
        )
    }
}

private fun Modifier.rotateWithBoolean(shouldRotate: Boolean) = composed {
    rotate(animateFloatAsState(targetValue = if (shouldRotate) 180f else 0f, label = "").value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomUrlDialog(
    onDismissRequest: () -> Unit,
    onAddUrl: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    urls: List<String>,
) {
    val sourceInfoRepository = koinInject<SourceInfoRepository>()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val scope = rememberCoroutineScope()
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = onDismissRequest
                        ) { Icon(Icons.Default.Close, null) }
                    },
                    title = { Text("Custom Tachiyomi Urls") },
                    actions = { Text(urls.size.toString()) }
                )
            },
            bottomBar = {
                var customUrl by remember { mutableStateOf("") }

                ElevatedCard(
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customUrl,
                            label = { Text("Custom Url") },
                            onValueChange = { customUrl = it },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .fillMaxWidth()
                                .weight(0.85f)
                        )
                        IconButton(
                            onClick = {
                                if (customUrl.isNotBlank()) {
                                    onAddUrl(customUrl)
                                    customUrl = ""
                                }
                            },
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .align(Alignment.CenterVertically)
                                .fillMaxWidth()
                                .weight(0.15f)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(urls) {
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Are you sure you want to delete this?") },
                            text = { Text(it) },
                            confirmButton = {
                                TextButton(
                                    onClick = { onRemoveUrl(it) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDeleteDialog = false },
                                ) { Text("No") }
                            }
                        )
                    }
                    val clipboard = LocalClipboard.current
                    OutlinedCard(
                        onClick = {
                            scope.launch { sourceInfoRepository.copyUrl(clipboard, it) }
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text(it) },
                            trailingContent = {
                                IconButton(
                                    onClick = { showDeleteDialog = true }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
