package com.programmersbox.uiviews.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SendTimeExtension
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.extensionloader.SourceLoader
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.OtakuWorldCatalog
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.lists.calculateStandardPaneScaffoldDirective
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.DownloadAndInstaller
import com.programmersbox.uiviews.utils.InsetCenterAlignedTopAppBar
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.components.ToolTipWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExtensionList(
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    otakuWorldCatalog: OtakuWorldCatalog = koinInject(),
    sourceLoader: SourceLoader = koinInject(),
    settingsHandling: SettingsHandling = LocalSettingsHandling.current,
    viewModel: ExtensionListViewModel = viewModel {
        ExtensionListViewModel(
            sourceRepository = sourceRepository,
            sourceLoader = sourceLoader,
            otakuWorldCatalog = otakuWorldCatalog,
            settingsHandling = settingsHandling
        )
    },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val downloadAndInstall = remember { DownloadAndInstaller(context) }

    fun updateCheck() {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "sourceCheck",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SourceUpdateChecker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(false)
                            .setRequiresCharging(false)
                            .setRequiresDeviceIdle(false)
                            .setRequiresStorageNotLow(false)
                            .build()
                    )
                    .build()
            )
    }

    var showUrlDialog by remember { mutableStateOf(false) }
    if (showUrlDialog) {
        CustomUrlDialog(
            onDismissRequest = { showUrlDialog = false },
            onAddUrl = { scope.launch { settingsHandling.addCustomUrl(it) } },
            onRemoveUrl = { scope.launch { settingsHandling.removeCustomUrl(it) } },
            urls = settingsHandling.customUrls.collectAsStateWithLifecycle(initialValue = emptyList()).value
        )
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<Int>(
        scaffoldDirective = calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo())
    )

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(R.string.extensions)) },
                navigationIcon = { BackButton() },
                actions = {
                    var showDropDown by remember { mutableStateOf(false) }
                    var checked by remember { mutableStateOf(false) }

                    ToolTipWrapper(info = { Text("View Remote Extensions") }) {
                        IconButton(onClick = { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }) {
                            Icon(Icons.Default.SendTimeExtension, null)
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
                                if (!checked) updateCheck()
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
            windowInsets = WindowInsets(0.dp),
            listPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    InstalledExtensionItems(
                        installedSources = viewModel.installed,
                        sourcesList = viewModel.remoteSourcesVersions,
                        onDownloadAndInstall = { downloadLink, destinationPath ->
                            downloadAndInstall.downloadAndInstall(downloadLink, destinationPath)
                        }
                    )
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    RemoteExtensionItems(
                        remoteSources = viewModel.remoteSources,
                        onDownloadAndInstall = { downloadLink, destinationPath ->
                            downloadAndInstall.downloadAndInstall(downloadLink, destinationPath)
                        },
                    )
                }
            },
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun InstalledExtensionItems(
    installedSources: Map<ApiServicesCatalog?, InstalledViewState>,
    sourcesList: List<RemoteSources>,
    onDownloadAndInstall: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSourceRepository = LocalCurrentSource.current
    val context = LocalContext.current
    fun uninstall(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val uninstall = Intent(Intent.ACTION_DELETE, uri)
        context.startActivity(uninstall)
    }
    Column(
        modifier = modifier
    ) {
        InsetCenterAlignedTopAppBar(
            title = { Text("Installed Extensions") },
            insetPadding = WindowInsets(0.dp)
        )
        ListItem(
            headlineContent = {
                val source by LocalCurrentSource.current.asFlow().collectAsState(initial = null)
                Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty()))
            }
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            installedSources.forEach { (t, u) ->
                stickyHeader {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp,
                        onClick = { u.showItems = !u.showItems },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            modifier = Modifier.padding(4.dp),
                            headlineContent = {
                                Text(t?.name ?: u.sourceInformation.firstOrNull()?.name?.takeIf { t != null } ?: "Single Source")
                            },
                            leadingContent = { Text("(${u.sourceInformation.size})") },
                            trailingContent = t?.let {
                                {
                                    IconButton(
                                        onClick = { uninstall(u.sourceInformation.random().packageName) }
                                    ) { Icon(Icons.Default.Delete, null) }
                                }
                            }
                        )
                    }
                }

                if (u.showItems) {
                    items(
                        u.sourceInformation,
                        key = { it.apiService.serviceName },
                        contentType = { it }
                    ) { source ->
                        val version = remember(context) {
                            runCatching {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    context.packageManager.getPackageInfo(
                                        source.packageName,
                                        PackageManager.PackageInfoFlags.of(0L)
                                    )
                                } else {
                                    context.packageManager.getPackageInfo(source.packageName, 0)
                                }
                            }
                                .getOrNull()
                                ?.versionName
                                .orEmpty()
                        }
                        ExtensionItem(
                            sourceInformation = source,
                            version = version,
                            onClick = { currentSourceRepository.tryEmit(source.apiService) },
                            trailingIcon = {
                                Row {
                                    sourcesList.find {
                                        it.sources.any { s -> s.baseUrl == source.apiService.baseUrl }
                                    }
                                        ?.let { r ->
                                            if (AppUpdate.checkForUpdate(version, r.version)) {
                                                IconButton(
                                                    onClick = {
                                                        onDownloadAndInstall(
                                                            r.downloadLink,
                                                            r.downloadLink.toUri().lastPathSegment ?: "${r.name}.apk"
                                                        )
                                                    }
                                                ) { Icon(Icons.Default.Update, null) }
                                            }
                                        }
                                    IconButton(
                                        onClick = { uninstall(source.packageName) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteExtensionItems(
    remoteSources: Map<String, RemoteState>,
    onDownloadAndInstall: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            remoteSources.forEach { (t, u) ->
                when (u) {
                    is RemoteViewState -> {
                        stickyHeader {
                            ElevatedCard(
                                onClick = { u.showItems = !u.showItems }
                            ) {
                                ListItem(
                                    headlineContent = { Text(t) },
                                    leadingContent = { Text("(${u.sources.size})") },
                                    trailingContent = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            null,
                                            modifier = Modifier.rotateWithBoolean(u.showItems)
                                        )
                                    }
                                )
                            }
                        }

                        if (u.showItems) {
                            items(u.sources.filter { it.name.contains(search, true) }) {
                                RemoteItem(
                                    remoteSource = it,
                                    onDownloadAndInstall = {
                                        onDownloadAndInstall(it.downloadLink, it.downloadLink.toUri().lastPathSegment ?: "${it.name}.apk")
                                    },
                                    modifier = Modifier.animateItemPlacement()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionItem(
    sourceInformation: SourceInformation,
    version: String,
    onClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        ListItem(
            overlineContent = { Text("Version: $version") },
            headlineContent = { Text(sourceInformation.apiService.serviceName) },
            leadingContent = { Image(rememberDrawablePainter(drawable = sourceInformation.icon), null) },
            trailingContent = trailingIcon
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteItem(
    remoteSource: RemoteSources,
    onDownloadAndInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Download and Install ${remoteSource.name}?") },
            //icon = { AsyncImage(model = remoteSource.iconUrl, contentDescription = null) },
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
    var showSources by remember { mutableStateOf(false) }
    OutlinedCard(
        onClick = { showSources = !showSources },
        modifier = modifier.animateContentSize()
    ) {
        ListItem(
            headlineContent = { Text(remoteSource.name) },
            leadingContent = { AsyncImage(model = remoteSource.iconUrl, contentDescription = null) },
            overlineContent = { Text("Version: ${remoteSource.version}") },
            supportingContent = {
                AnimatedVisibility(visible = showSources) {
                    Column {
                        remoteSource.sources.forEach {
                            ListItem(
                                headlineContent = { Text(it.name) },
                                overlineContent = { Text("Version: ${it.version}") }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            trailingContent = {
                Row {
                    IconButton(
                        onClick = { showSources = !showSources }
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            modifier = Modifier.rotateWithBoolean(showSources)
                        )
                    }

                    IconButton(
                        onClick = { showDialog = true }
                    ) { Icon(Icons.Default.InstallMobile, null) }
                }
            },
            modifier = Modifier.animateContentSize()
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
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(true)
    ) {
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
                var customUrl by rememberSaveable { mutableStateOf("") }

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
                    val clipboard = LocalClipboardManager.current
                    OutlinedCard(
                        onClick = { clipboard.setText(buildAnnotatedString { append(it) }) }
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

@LightAndDarkPreviews
@Composable
private fun ExtensionListPreview() {
    PreviewTheme {
        ExtensionList()
    }
}