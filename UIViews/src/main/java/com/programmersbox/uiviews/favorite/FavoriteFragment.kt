@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.uiviews.favorite

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.BannerScope
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.InsetCenterAlignedTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.M3CoverCard
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.SourceNotInstalledModal
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.components.DynamicSearchBar
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.navigateToDetails
import com.programmersbox.uiviews.utils.topBounds
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FavoriteUi(
    isHorizontal: Boolean = false,
    dao: ItemDao = LocalItemDao.current,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    viewModel: FavoriteViewModel = viewModel { FavoriteViewModel(dao, sourceRepository) },
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current

    var showBanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    var showDbModel by remember { mutableStateOf<DbModel?>(null) }

    var showSort by remember { mutableStateOf(false) }

    val logo = koinInject<AppLogo>().logoId

    SourceNotInstalledModal(
        showItem = showDbModel?.title,
        onShowItemDismiss = { showDbModel = null },
        source = showDbModel?.source,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.removeFromFavorites)) },
            leadingContent = { Icon(Icons.Default.FavoriteBorder, null) },
            modifier = Modifier.clickable {
                showDbModel?.let {
                    scope.launch {
                        dao.deleteFavorite(it)
                        FirebaseDb.removeShowFlow(it).collect()
                    }
                }
                showDbModel = null
            }
        )
    }

    if (showSort) {
        ModalBottomSheet(
            onDismissRequest = { showSort = false }
        ) {
            InsetCenterAlignedTopAppBar(
                title = { Text("Sort By") },
                insetPadding = WindowInsets(0.dp)
            )

            val rotateIcon: @Composable (SortFavoritesBy<*>) -> Float = {
                animateFloatAsState(if (it == viewModel.sortedBy && viewModel.reverse) 180f else 0f, label = "").value
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                @Composable
                fun SegmentedButtonItem(
                    sortFavoritesBy: SortFavoritesBy<*>,
                    label: String,
                    index: Int,
                    icon: ImageVector,
                ) {
                    val isSelected = viewModel.sortedBy == sortFavoritesBy
                    SegmentedButton(
                        selected = isSelected,
                        onClick = {
                            if (viewModel.sortedBy != sortFavoritesBy)
                                viewModel.sortedBy = sortFavoritesBy
                            else
                                viewModel.reverse = !viewModel.reverse
                        },
                        label = { Text(label) },
                        icon = {
                            SegmentedButtonDefaults.Icon(
                                active = isSelected,
                                activeContent = {
                                    Icon(
                                        icon,
                                        null,
                                        modifier = Modifier.rotate(rotateIcon(sortFavoritesBy))
                                    )
                                }
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, 3)
                    )
                }

                SegmentedButtonItem(
                    sortFavoritesBy = SortFavoritesBy.TITLE,
                    label = "Title",
                    index = 0,
                    icon = Icons.Default.SortByAlpha
                )

                SegmentedButtonItem(
                    sortFavoritesBy = SortFavoritesBy.COUNT,
                    label = "Count",
                    index = 1,
                    icon = Icons.AutoMirrored.Filled.Sort
                )

                SegmentedButtonItem(
                    sortFavoritesBy = SortFavoritesBy.CHAPTERS,
                    label = "Chapters",
                    index = 2,
                    icon = Icons.AutoMirrored.Filled.ReadMore
                )
            }
        }
    }

    OtakuBannerBox(
        showBanner = showBanner,
        placeholder = logo,
        modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        OtakuScaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    var active by rememberSaveable { mutableStateOf(false) }

                    fun closeSearchBar() {
                        focusManager.clearFocus()
                        active = false
                    }
                    DynamicSearchBar(
                        isDocked = isHorizontal,
                        query = viewModel.searchText,
                        onQueryChange = { viewModel.searchText = it },
                        onSearch = { closeSearchBar() },
                        active = active,
                        onActiveChange = {
                            active = it
                            if (!active) focusManager.clearFocus()
                        },
                        placeholder = {
                            Text(
                                context.resources.getQuantityString(
                                    R.plurals.numFavorites,
                                    viewModel.listSources.size,
                                    viewModel.listSources.size
                                )
                            )
                        },
                        leadingIcon = { BackButton() },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedVisibility(viewModel.searchText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchText = "" }) {
                                        Icon(Icons.Default.Cancel, null)
                                    }
                                }

                                AnimatedVisibility(!active) {
                                    IconButton(onClick = { showSort = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, null)
                                    }
                                }
                            }
                        },
                        colors = SearchBarDefaults.colors(
                            containerColor = animateColorAsState(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = if (active) 1f else 0f
                                ),
                                label = ""
                            ).value,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            viewModel.listSources.take(4).forEachIndexed { index, dbModel ->
                                ListItem(
                                    headlineContent = { Text(dbModel.title) },
                                    supportingContent = { Text(dbModel.source) },
                                    leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        viewModel.searchText = dbModel.title
                                        closeSearchBar()
                                    }
                                )
                                if (index != 3) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    var showFilterBySourceModal by remember { mutableStateOf(false) }

                    if (showFilterBySourceModal) {
                        BackHandler { showFilterBySourceModal = false }

                        ModalBottomSheet(onDismissRequest = { showFilterBySourceModal = false }) {
                            CenterAlignedTopAppBar(title = { Text("Filter by Source") })
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                FilterChip(
                                    selected = true,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { viewModel.resetSources() },
                                        onLongClick = { viewModel.selectedSources.clear() }
                                    ),
                                    label = { Text("ALL") },
                                    onClick = { viewModel.allClick() }
                                )

                                viewModel.allSources.forEach {
                                    FilterChip(
                                        selected = it.first in viewModel.selectedSources,
                                        onClick = { viewModel.newSource(it.first) },
                                        label = { Text(it.first) },
                                        leadingIcon = { Text("${it.second.size - 1}") },
                                        modifier = Modifier.combinedClickable(
                                            onClick = { viewModel.newSource(it.first) },
                                            onLongClick = { viewModel.singleSource(it.first) }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        SuggestionChip(
                            onClick = { showFilterBySourceModal = true },
                            label = { Text("Filter By Source") }
                        )

                        SuggestionChip(
                            label = { Text("ALL") },
                            onClick = { viewModel.resetSources() }
                        )
                    }
                }
            }
        ) { p ->
            if (viewModel.listSources.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(p)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        tonalElevation = 4.dp,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Column(modifier = Modifier) {
                            Text(
                                text = stringResource(id = R.string.get_started),
                                style = M3MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Text(
                                text = stringResource(R.string.get_started_info),
                                style = M3MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Button(
                                onClick = { navController.popBackStack(Screen.RecentScreen.route, false) },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 4.dp)
                            ) { Text(text = stringResource(R.string.add_a_favorite)) }
                        }
                    }
                }
            } else {
                BoxWithConstraints {
                    FavoritesGrid(
                        paddingValues = p,
                        groupedSources = viewModel.groupedSources,
                        sourceRepository = sourceRepository,
                        navController = navController,
                        moreInfoClick = {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    "Something went wrong. Source might not be installed",
                                    duration = SnackbarDuration.Long,
                                    actionLabel = "More Options",
                                    withDismissAction = true
                                )
                                showDbModel = when (result) {
                                    SnackbarResult.Dismissed -> null
                                    SnackbarResult.ActionPerformed -> it
                                }
                            }
                        },
                        onShowBanner = { showBanner = it },
                        logo = logo,
                        modifier = Modifier.haze(
                            topBounds(p),
                            backgroundColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun BannerScope.FavoritesGrid(
    paddingValues: PaddingValues,
    groupedSources: List<Map.Entry<String, List<DbModel>>>,
    sourceRepository: SourceRepository,
    navController: NavController,
    moreInfoClick: (DbModel) -> Unit,
    onShowBanner: (Boolean) -> Unit,
    logo: Int,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = adaptiveGridCell(),
        state = rememberLazyGridState(),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            groupedSources,
            key = { it.key }
        ) { info ->
            var showBottomSheet by remember { mutableStateOf(false) }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false }
                ) {
                    ListBottomScreen(
                        navigationIcon = {
                            IconButton(onClick = { showBottomSheet = false }) { Icon(Icons.Default.Close, null) }
                        },
                        includeInsetPadding = false,
                        title = stringResource(R.string.chooseASource),
                        list = info.value,
                        onClick = { item ->
                            showBottomSheet = false
                            item
                                .let {
                                    sourceRepository
                                        .toSourceByApiServiceName(it.source)
                                        ?.apiService
                                        ?.let { it1 -> it.toItemModel(it1) }
                                }
                                ?.let(navController::navigateToDetails) ?: moreInfoClick(item)
                        }
                    ) {
                        ListBottomSheetItemModel(
                            primaryText = it.title,
                            overlineText = it.source
                        )
                    }
                }
            }

            M3CoverCard(
                onLongPress = { c ->
                    newItemModel(
                        if (c == ComponentState.Pressed) {
                            info.value.randomOrNull()?.let {
                                sourceRepository
                                    .toSourceByApiServiceName(it.source)
                                    ?.apiService
                                    ?.let { it1 -> it.toItemModel(it1) }
                            }
                        } else null
                    )
                    onShowBanner(c == ComponentState.Pressed)
                },
                imageUrl = remember { info.value.randomOrNull()?.imageUrl.orEmpty() },
                name = info.key,
                placeHolder = logo,
                favoriteIcon = {
                    if (info.value.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = null,
                                tint = M3MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Text(
                                info.value.size.toString(),
                                color = M3MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                },
                modifier = Modifier.animateItemPlacement()
            ) {
                if (info.value.size == 1) {
                    info.value
                        .firstOrNull()
                        ?.let {
                            sourceRepository
                                .toSourceByApiServiceName(it.source)
                                ?.apiService
                                ?.let { it1 -> it.toItemModel(it1) }
                                ?.let(navController::navigateToDetails) ?: moreInfoClick(it)
                        }
                } else {
                    showBottomSheet = true
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@LightAndDarkPreviews
@Composable
private fun FavoriteScreenPreview() {
    PreviewTheme {
        FavoriteUi()
    }
}