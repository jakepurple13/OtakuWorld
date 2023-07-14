package com.programmersbox.uiviews.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.extensionloader.SourceInformation
import com.programmersbox.extensionloader.SourceRepository
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
                        text = { Text("Installed") },
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(Icons.Default.BrowseGallery, null) }
                    )

                    LeadingIconTab(
                        text = { Text("Extensions") },
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(Icons.Default.Search, null) }
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

@Composable
private fun InstalledExtensionItems(
    installedSources: List<SourceInformation>
) {
    val context = LocalContext.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(installedSources) {
            ExtensionItem(
                sourceInformation = it,
                onClick = { sourceFlow.tryEmit(it.apiService) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val uri = Uri.fromParts("package", it.packageName, null)
                            val uninstall = Intent(Intent.ACTION_DELETE, uri)
                            context.startActivity(uninstall)
                        }
                    ) { Icon(Icons.Default.Delete, null) }
                }
            )
        }
    }
}

@Composable
private fun RemoteExtensionItems(
    remoteSources: List<SourceInformation>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(remoteSources) {
            ExtensionItem(
                sourceInformation = it,
                onClick = {},
                trailingIcon = {
                    TextButton(onClick = {}) {
                        Text("Install")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionItem(
    sourceInformation: SourceInformation,
    onClick: () -> Unit,
    trailingIcon: @Composable () -> Unit
) {
    OutlinedCard(
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(sourceInformation.name) },
            leadingContent = { Icon(rememberDrawablePainter(drawable = sourceInformation.icon), null) },
            trailingContent = trailingIcon
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