package com.programmersbox.uiviews.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SendTimeExtension
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.OtakuWorldCatalog
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.all.pagerTabIndicatorOffset
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.DownloadAndInstaller
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtensionList(
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    otakuWorldCatalog: OtakuWorldCatalog = koinInject(),
    viewModel: ExtensionListViewModel = viewModel { ExtensionListViewModel(sourceRepository, otakuWorldCatalog) },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 2 }

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

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                InsetSmallTopAppBar(
                    title = { Text(stringResource(R.string.extensions)) },
                    navigationIcon = { BackButton() },
                    actions = { IconButton(onClick = ::updateCheck) { Icon(Icons.Default.Update, null) } },
                    scrollBehavior = scrollBehavior,
                )
                TabRow(
                    // Our selected tab is our current page
                    selectedTabIndex = pagerState.currentPage,
                    // Override the indicator, using the provided pagerTabIndicatorOffset modifier
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    // Add tabs for all of our pages
                    LeadingIconTab(
                        text = { Text(stringResource(R.string.installed)) },
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(Icons.Default.Extension, null) }
                    )

                    LeadingIconTab(
                        text = { Text(stringResource(R.string.extensions)) },
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(Icons.Default.SendTimeExtension, null) }
                    )
                }
            }
        },
    ) { paddingValues ->
        Crossfade(
            targetState = viewModel.installed.isEmpty(),
            label = "",
        ) { target ->
            when (target) {
                true -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        RemoteExtensionItems(
                            remoteSources = viewModel.remoteSources,
                            onDownloadAndInstall = { downloadLink, destinationPath ->
                                downloadAndInstall.downloadAndInstall(downloadLink, destinationPath)
                            }
                        )
                    }
                }

                false -> {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = paddingValues,
                    ) { page ->
                        when (page) {
                            0 -> InstalledExtensionItems(
                                installedSources = viewModel.installed,
                                sourcesList = viewModel.remoteSourcesVersions,
                                onDownloadAndInstall = { downloadLink, destinationPath ->
                                    downloadAndInstall.downloadAndInstall(downloadLink, destinationPath)
                                }
                            )

                            1 -> RemoteExtensionItems(
                                remoteSources = viewModel.remoteSources,
                                onDownloadAndInstall = { downloadLink, destinationPath ->
                                    downloadAndInstall.downloadAndInstall(downloadLink, destinationPath)
                                }
                            )
                        }
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstalledExtensionItems(
    installedSources: Map<ApiServicesCatalog?, InstalledViewState>,
    sourcesList: List<RemoteSources>,
    onDownloadAndInstall: (String, String) -> Unit,
) {
    val currentSourceRepository = LocalCurrentSource.current
    val context = LocalContext.current
    fun uninstall(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val uninstall = Intent(Intent.ACTION_DELETE, uri)
        context.startActivity(uninstall)
    }
    Column {
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.packageManager.getPackageInfo(
                                    source.packageName,
                                    PackageManager.PackageInfoFlags.of(0L)
                                )
                            } else {
                                context.packageManager.getPackageInfo(source.packageName, 0)
                            }
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RemoteExtensionItems(
    remoteSources: Map<String, RemoteViewState>,
    onDownloadAndInstall: (String, String) -> Unit,
) {
    Column {
        var search by remember { mutableStateOf("") }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search Remote Extensions") },
            trailingIcon = {
                IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, null) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            remoteSources.forEach { (t, u) ->
                stickyHeader {
                    InsetSmallTopAppBar(
                        title = { Text(t) },
                        insetPadding = WindowInsets(0.dp),
                        navigationIcon = { Text("(${u.sources.size})") },
                        actions = {
                            IconButton(
                                onClick = { u.showItems = !u.showItems }
                            ) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    null,
                                    modifier = Modifier.rotateWithBoolean(u.showItems)
                                )
                            }
                        }
                    )
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
                        println(remoteSource.downloadLink)
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
                            Divider()
                        }
                    }
                }
            },
            trailingContent = {
                Row {
                    IconButton(
                        onClick = { showSources = !showSources }
                    ) { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.rotateWithBoolean(showSources)) }

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

@LightAndDarkPreviews
@Composable
private fun ExtensionListPreview() {
    PreviewTheme {
        ExtensionList()
    }
}