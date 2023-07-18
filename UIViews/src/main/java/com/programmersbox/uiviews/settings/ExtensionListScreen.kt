package com.programmersbox.uiviews.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SendTimeExtension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.sourceFlow
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.all.pagerTabIndicatorOffset
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtensionList(
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    viewModel: ExtensionListViewModel = viewModel { ExtensionListViewModel(sourceRepository) }
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 2 }

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                InsetSmallTopAppBar(
                    title = { Text(stringResource(R.string.extensions)) },
                    navigationIcon = { BackButton() },
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
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    ) { p ->
        HorizontalPager(
            state = pagerState,
            contentPadding = p
        ) { page ->
            when (page) {
                0 -> InstalledExtensionItems(
                    installedSources = viewModel.installedSources
                )

                1 -> RemoteExtensionItems(
                    remoteSources = viewModel.remoteSources
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstalledExtensionItems(
    installedSources: List<SourceInformation>
) {
    val context = LocalContext.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        installedSources.groupBy { it.catalog }.forEach { (t, u) ->
            stickyHeader {
                Surface(
                    shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(0f), topEnd = CornerSize(0f)),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        modifier = Modifier.padding(4.dp),
                        headlineContent = { Text(u.firstOrNull()?.name?.takeIf { t != null } ?: "Single Source") },
                        leadingContent = { Text("(${u.size})") },
                        trailingContent = t?.let {
                            {
                                IconButton(
                                    onClick = {
                                        val uri = Uri.fromParts("package", u.random().packageName, null)
                                        val uninstall = Intent(Intent.ACTION_DELETE, uri)
                                        context.startActivity(uninstall)
                                    }
                                ) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    )
                }
            }

            items(u) {
                ExtensionItem(
                    sourceInformation = it,
                    onClick = { sourceFlow.tryEmit(it.apiService) },
                    trailingIcon = if (t == null) {
                        {
                            IconButton(
                                onClick = {
                                    val uri = Uri.fromParts("package", it.packageName, null)
                                    val uninstall = Intent(Intent.ACTION_DELETE, uri)
                                    context.startActivity(uninstall)
                                }
                            ) { Icon(Icons.Default.Delete, null) }
                        }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RemoteExtensionItems(
    remoteSources: Map<String, List<RemoteSources>>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        remoteSources.forEach { (t, u) ->
            stickyHeader { TopAppBar(title = { Text(t) }) }
            items(u) {
                RemoteItem(
                    remoteSource = it,
                    onClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionItem(
    sourceInformation: SourceInformation,
    onClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)?
) {
    OutlinedCard(
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(sourceInformation.apiService.serviceName) },
            leadingContent = { Icon(rememberDrawablePainter(drawable = sourceInformation.icon), null) },
            trailingContent = trailingIcon
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteItem(
    remoteSource: RemoteSources,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(remoteSource.name) },
            leadingContent = { AsyncImage(model = remoteSource.iconUrl, contentDescription = null) },
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun ExtensionListPreview() {
    PreviewTheme {
        ExtensionList()
    }
}