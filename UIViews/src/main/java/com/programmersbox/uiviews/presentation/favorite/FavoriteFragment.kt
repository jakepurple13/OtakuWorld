package com.programmersbox.uiviews.presentation.favorite

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
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
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.optionsKmpSheetList
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.kmpuiviews.presentation.favorite.SortFavoritesBy
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.DynamicSearchBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
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
    favoritesRepository: FavoritesRepository = koinInject(),
    viewModel: FavoriteViewModel = koinViewModel(),
) {
    val navController = LocalNavActions.current
    val context = LocalContext.current

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

    var optionsSheet by optionsKmpSheetList()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    var showDbModel by remember { mutableStateOf<DbModel?>(null) }

    var showSort by remember { mutableStateOf(false) }

    val logo = koinInject<AppLogo>()

    SourceNotInstalledModal(
        showItem = showDbModel?.title,
        onShowItemDismiss = { showDbModel = null },
        source = showDbModel?.source,
        url = showDbModel?.url
    ) {
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.removeFromFavorites)) },
            leadingContent = { Icon(Icons.Default.FavoriteBorder, null) },
            modifier = Modifier.clickable {
                showDbModel?.let {
                    scope.launch {
                        favoritesRepository.removeFavorite(it)
                    }
                }
                showDbModel = null
            }
        )
    }

    if (showSort) {
        ModalBottomSheet(
            onDismissRequest = { showSort = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CenterAlignedTopAppBar(
                title = { Text("Sort By") },
                windowInsets = WindowInsets(0.dp)
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

    OtakuHazeScaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        },
        topBar = {
            Surface(
                color = if (showBlur) Color.Transparent else MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    val searchBarState = rememberSearchBarState()

                    fun closeSearchBar() {
                        scope.launch { searchBarState.animateToCollapsed() }
                    }

                    DynamicSearchBar(
                        isDocked = isHorizontal,
                        textFieldState = viewModel.searchText,
                        searchBarState = searchBarState,
                        onSearch = { closeSearchBar() },
                        placeholder = {
                            Text(
                                pluralStringResource(
                                    R.plurals.numFavorites,
                                    viewModel.listSources.size,
                                    viewModel.listSources.size
                                )
                            )
                        },
                        leadingIcon = {
                            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                                IconButton(
                                    onClick = { closeSearchBar() }
                                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }

                            } else {
                                BackButton()
                            }
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedVisibility(viewModel.searchText.text.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.searchText = TextFieldState() }
                                    ) { Icon(Icons.Default.Cancel, null) }
                                }

                                AnimatedVisibility(searchBarState.currentValue == SearchBarValue.Collapsed) {
                                    IconButton(onClick = { showSort = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, null)
                                    }
                                }
                            }
                        },
                        colors = SearchBarDefaults.colors(
                            inputFieldColors = if (showBlur)
                                SearchBarDefaults.inputFieldColors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                )
                            else
                                SearchBarDefaults.inputFieldColors()
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            viewModel.listSources.take(4).forEachIndexed { index, dbModel ->
                                Card(
                                    onClick = {
                                        viewModel.searchText = TextFieldState(dbModel.title)
                                        closeSearchBar()
                                    },
                                ) {
                                    ListItem(
                                        headlineContent = { Text(dbModel.title) },
                                        supportingContent = { Text(dbModel.source) },
                                        leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                                        colors = ListItemDefaults.colors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    var showFilterBySourceModal by remember { mutableStateOf(false) }

                    if (showFilterBySourceModal) {
                        BackHandler { showFilterBySourceModal = false }

                        ModalBottomSheet(
                            onDismissRequest = { showFilterBySourceModal = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentWindowInsets = { WindowInsets.systemBars.only(WindowInsetsSides.Top) },
                        ) {
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
        },
        blurTopBar = showBlur,
        topBarBlur = {
            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
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
                            onClick = { navController.popBackStack(Screen.RecentScreen, false) },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp)
                        ) { Text(text = stringResource(R.string.add_a_favorite)) }
                    }
                }
            }
        } else {
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
                onLongPress = { optionsSheet = it },
                logo = logo.logo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FavoritesGrid(
    paddingValues: PaddingValues,
    groupedSources: List<Map.Entry<String, List<DbModel>>>,
    sourceRepository: SourceRepository,
    navController: NavigationActions,
    moreInfoClick: (DbModel) -> Unit,
    onLongPress: (List<KmpItemModel>) -> Unit,
    logo: Drawable,
    modifier: Modifier = Modifier,
) {
    val biometric = rememberBiometricOpening()
    val scope = rememberCoroutineScope()
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
                    onDismissRequest = { showBottomSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
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
                            item.let {
                                sourceRepository
                                    .toSourceByApiServiceName(it.source)
                                    ?.apiService
                                    ?.let { it1 -> it.toItemModel(it1) }
                            }?.let(navController::details) ?: moreInfoClick(item)
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
                    onLongPress(
                        info.value.mapNotNull {
                            sourceRepository
                                .toSourceByApiServiceName(it.source)
                                ?.apiService
                                ?.let { it1 -> it.toItemModel(it1) }
                        }
                    )
                },
                imageUrl = remember { info.value.randomOrNull()?.imageUrl.orEmpty() },
                name = info.key,
                placeHolder = { painterLogo() },
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
                modifier = Modifier.animateItem()
            ) {
                scope.launch {
                    biometric.openIfNotIncognito(*info.value.toTypedArray()) {
                        if (info.value.size == 1) {
                            info.value
                                .firstOrNull()
                                ?.let {
                                    sourceRepository
                                        .toSourceByApiServiceName(it.source)
                                        ?.apiService
                                        ?.let { it1 -> it.toItemModel(it1) }
                                        ?.let(navController::details) ?: moreInfoClick(it)
                                }
                        } else {
                            showBottomSheet = true
                        }
                    }
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