package com.programmersbox.uiviews.favorite

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ReadMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.M3CoverCard
import com.programmersbox.uiviews.utils.MockAppIcon
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.components.GroupButton
import com.programmersbox.uiviews.utils.components.GroupButtonModel
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.navigateToDetails
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun FavoriteUi(
    logo: MainLogo,
    dao: ItemDao = LocalItemDao.current,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    viewModel: FavoriteViewModel = viewModel { FavoriteViewModel(dao, sourceRepository) },
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var showBanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    OtakuBannerBox(
        showBanner = showBanner,
        placeholder = logo.logoId,
        modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        OtakuScaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        InsetSmallTopAppBar(
                            scrollBehavior = scrollBehavior,
                            navigationIcon = { BackButton() },
                            title = { Text(stringResource(R.string.viewFavoritesMenu)) },
                            actions = {

                                val rotateIcon: @Composable (SortFavoritesBy<*>) -> Float = {
                                    animateFloatAsState(if (it == viewModel.sortedBy && viewModel.reverse) 180f else 0f, label = "").value
                                }

                                GroupButton(
                                    selected = viewModel.sortedBy,
                                    options = listOf(
                                        GroupButtonModel(SortFavoritesBy.TITLE) {
                                            Icon(
                                                Icons.Default.SortByAlpha,
                                                null,
                                                modifier = Modifier.rotate(rotateIcon(SortFavoritesBy.TITLE))
                                            )
                                        },
                                        GroupButtonModel(SortFavoritesBy.COUNT) {
                                            Icon(
                                                Icons.Default.Sort,
                                                null,
                                                modifier = Modifier.rotate(rotateIcon(SortFavoritesBy.COUNT))
                                            )
                                        },
                                        GroupButtonModel(SortFavoritesBy.CHAPTERS) {
                                            Icon(
                                                Icons.Default.ReadMore,
                                                null,
                                                modifier = Modifier.rotate(rotateIcon(SortFavoritesBy.CHAPTERS))
                                            )
                                        }
                                    )
                                ) { if (viewModel.sortedBy != it) viewModel.sortedBy = it else viewModel.reverse = !viewModel.reverse }
                            }
                        )

                        var active by rememberSaveable { mutableStateOf(false) }

                        fun closeSearchBar() {
                            focusManager.clearFocus()
                            active = false
                        }
                        SearchBar(
                            modifier = Modifier.fillMaxWidth(),
                            windowInsets = WindowInsets(0.dp),
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.searchText = "" }) {
                                    Icon(Icons.Default.Cancel, null)
                                }
                            },
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
                                        Divider()
                                    }
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = true,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { viewModel.resetSources() },
                                        onLongClick = { viewModel.selectedSources.clear() }
                                    ),
                                    label = { Text("ALL") },
                                    onClick = { viewModel.allClick() }
                                )
                            }

                            items(viewModel.allSources) {
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
                LazyVerticalGrid(
                    columns = adaptiveGridCell(),
                    state = rememberLazyGridState(),
                    contentPadding = p,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        viewModel.groupedSources,
                        key = { it.key }
                    ) { info ->
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
                                showBanner = c == ComponentState.Pressed
                            },
                            imageUrl = remember { info.value.randomOrNull()?.imageUrl.orEmpty() },
                            name = info.key,
                            placeHolder = logo.logoId,
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
                                    }
                                    ?.let(navController::navigateToDetails) ?: scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        "Something went wrong. Source might not be installed",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                Screen.FavoriteChoiceScreen.navigate(navController, info.value)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteChoiceScreen(vm: FavoriteChoiceViewModel = viewModel { FavoriteChoiceViewModel(createSavedStateHandle()) }) {
    val sourceRepository = LocalSourcesRepository.current
    val navController = LocalNavController.current
    val context = LocalContext.current
    ListBottomScreen(
        includeInsetPadding = false,
        title = stringResource(R.string.chooseASource),
        list = vm.items,
        onClick = { item ->
            item
                .let {
                    sourceRepository
                        .toSourceByApiServiceName(it.source)
                        ?.apiService
                        ?.let { it1 -> it.toItemModel(it1) }
                }
                ?.let(navController::navigateToDetails) ?: Toast.makeText(
                context,
                "Something went wrong. Source might not be installed",
                Toast.LENGTH_SHORT
            ).show()
        }
    ) {
        ListBottomSheetItemModel(
            primaryText = it.title,
            overlineText = it.source
        )
    }
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@LightAndDarkPreviews
@Composable
private fun FavoriteScreenPreview() {
    PreviewTheme {
        FavoriteUi(logo = MockAppIcon)
    }
}