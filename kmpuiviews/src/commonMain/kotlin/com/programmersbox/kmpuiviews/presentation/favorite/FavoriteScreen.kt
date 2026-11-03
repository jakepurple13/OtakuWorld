package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.optionsKmpSheetList
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.add_a_favorite
import otakuworld.kmpuiviews.generated.resources.chooseASource
import otakuworld.kmpuiviews.generated.resources.fav_clear_search
import otakuworld.kmpuiviews.generated.resources.fav_filter
import otakuworld.kmpuiviews.generated.resources.fav_search_placeholder
import otakuworld.kmpuiviews.generated.resources.fav_tab_library
import otakuworld.kmpuiviews.generated.resources.fav_tab_overview
import otakuworld.kmpuiviews.generated.resources.get_started
import otakuworld.kmpuiviews.generated.resources.get_started_info
import otakuworld.kmpuiviews.generated.resources.removeFromFavorites

@OptIn(ExperimentalSharedTransitionApi::class)
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FavoriteScreen(
    isHorizontal: Boolean = false,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    favoritesRepository: FavoritesRepository = koinInject(),
    viewModel: FavoriteViewModel = koinViewModel(),
) {
    val navController = LocalNavActions.current
    val settings = koinInject<NewSettingsHandling>()

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()
    var gridView by settings.rememberFavoritesGridView()
    var openLibrary by settings.rememberFavoritesOpenLibrary()

    val colorBlindness: ColorBlindnessType by settings.rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    var optionsSheet by optionsKmpSheetList()
    val scope = rememberCoroutineScope()
    val biometric = rememberBiometricOpening()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDbModel by remember { mutableStateOf<DbModel?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var chooser by remember { mutableStateOf<FavoriteGroup?>(null) }

    val displayName: (String) -> String = remember(sourceRepository) { { sourceRepository.displayNameFor(it) } }

    // Typing from Overview moves to Library, where results are shown
    LaunchedEffect(viewModel) {
        snapshotFlow { viewModel.searchText.text.isNotEmpty() }
            .collect { if (it) openLibrary = true }
    }

    fun moreInfoClick(dbModel: DbModel) {
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
                SnackbarResult.ActionPerformed -> dbModel
            }
        }
    }

    fun openDetails(dbModel: DbModel) {
        sourceRepository
            .toSourceByApiServiceName(dbModel.source)
            ?.apiService
            ?.let { dbModel.toItemModel(it) }
            ?.let(navController::details)
            ?: moreInfoClick(dbModel)
    }

    fun openGroup(group: FavoriteGroup) {
        scope.launch {
            biometric.openIfNotIncognito(*group.items.toTypedArray()) {
                if (group.items.size == 1) openDetails(group.items.first()) else chooser = group
            }
        }
    }

    fun longPress(group: FavoriteGroup) {
        optionsSheet = group.items.mapNotNull { item ->
            sourceRepository
                .toSourceByApiServiceName(item.source)
                ?.apiService
                ?.let { item.toItemModel(it) }
        }
    }

    SourceNotInstalledModal(
        showItem = showDbModel?.title,
        onShowItemDismiss = { showDbModel = null },
        source = showDbModel?.source,
        url = showDbModel?.url
    ) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.removeFromFavorites)) },
            leadingContent = { Icon(Icons.Default.FavoriteBorder, null) },
            modifier = Modifier.clickable {
                showDbModel?.let { scope.launch { favoritesRepository.removeFavorite(it) } }
                showDbModel = null
            }
        )
    }

    chooser?.let { group ->
        ModalBottomSheet(
            onDismissRequest = { chooser = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ListBottomScreen(
                navigationIcon = {
                    IconButton(onClick = { chooser = null }) { Icon(Icons.Default.Close, null) }
                },
                includeInsetPadding = false,
                title = stringResource(Res.string.chooseASource),
                list = group.items,
                onClick = { item ->
                    chooser = null
                    openDetails(item)
                }
            ) {
                ListBottomSheetItemModel(
                    primaryText = it.title,
                    overlineText = displayName(it.source)
                )
            }
        }
    }

    if (showFilters) {
        FavoritesFilterSheet(
            quickFilters = viewModel.quickFilters,
            quickCounts = viewModel.quickCounts,
            onToggleQuick = viewModel::toggleQuickFilter,
            sourceCounts = viewModel.sourceCounts,
            selectedSources = viewModel.selectedSources,
            displayName = displayName,
            onToggleSource = viewModel::toggleSource,
            onOnlySource = viewModel::onlySource,
            onSelectAll = viewModel::selectAll,
            onSelectNone = viewModel::selectNone,
            activeFilterCount = viewModel.activeFilterCount,
            onReset = viewModel::resetFilters,
            resultCount = viewModel.resultCount,
            onApply = {
                showFilters = false
                if (viewModel.activeFilterCount > 0) openLibrary = true
            },
            onDismiss = { showFilters = false },
        )
    }

    val navHostPadding = LocalNavHostPadding.current

    OtakuHazeScaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(navHostPadding)
            )
        },
        topBar = {
            Surface(color = if (showBlur) Color.Transparent else MaterialTheme.colorScheme.surface) {
                Column(Modifier.statusBarsPadding()) {
                    FavoriteSearchBar(
                        viewModel = viewModel,
                        onOpenFilters = { showFilters = true },
                    )
                    FavoriteTabs(
                        library = openLibrary,
                        onSelect = { openLibrary = it },
                    )
                }
            }
        },
        blurTopBar = showBlur,
        topBarBlur = {
            progressive(
                HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
            )
        }
    ) { p ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            top = p.calculateTopPadding(),
            bottom = p.calculateBottomPadding() + navHostPadding.calculateBottomPadding(),
        )

        if (viewModel.totalCount == 0) {
            GetStarted(
                onAdd = { navController.popBackStack(Screen.RecentScreen, false) },
                modifier = Modifier.padding(top = p.calculateTopPadding(), start = p.calculateStartPadding(layoutDirection)),
            )
            return@OtakuHazeScaffold
        }

        CompositionLocalProvider(LocalFavoritesColorFilter provides colorFilter) {
            val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
            val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            SharedTransitionLayout {
                CompositionLocalProvider(LocalFavoritesSharedScope provides this) {
                    AnimatedContent(
                        targetState = openLibrary,
                        transitionSpec = {
                            val direction = if (targetState) 1 else -1
                            (slideInHorizontally(slideSpec) { it / 3 * direction } + fadeIn(fadeInSpec)) togetherWith
                                (slideOutHorizontally(slideSpec) { -it / 3 * direction } + fadeOut(fadeOutSpec))
                        },
                        label = "favoritesTab",
                    ) { library ->
                        CompositionLocalProvider(LocalFavoritesTabScope provides this) {
                            if (library) {
                                FavoritesLibrary(
                                    sections = viewModel.sections,
                                    isSearching = viewModel.isSearching,
                                    query = viewModel.searchQuery,
                                    resultCount = viewModel.resultCount,
                                    totalCount = viewModel.totalCount,
                                    gridView = gridView,
                                    onGridViewChange = { gridView = it },
                                    sort = viewModel.sort,
                                    onSortChange = { viewModel.sort = it },
                                    quickFilters = viewModel.quickFilters,
                                    selectedSources = viewModel.selectedSources,
                                    displayName = displayName,
                                    onRemoveQuick = viewModel::removeQuickFilter,
                                    onClearSources = viewModel::clearSources,
                                    onOpenFilters = { showFilters = true },
                                    contentPadding = contentPadding,
                                    onGroupClick = ::openGroup,
                                    onGroupLongPress = ::longPress,
                                )
                            } else {
                                FavoritesOverview(
                                    overview = viewModel.overview,
                                    contentPadding = PaddingValues(
                                        top = contentPadding.calculateTopPadding() + 4.dp,
                                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                                    ),
                                    onSeeAll = {
                                        viewModel.seeAll(it)
                                        openLibrary = true
                                    },
                                    onOpenFilters = { showFilters = true },
                                    onGroupClick = ::openGroup,
                                    onGroupLongPress = ::longPress,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSearchBar(
    viewModel: FavoriteViewModel,
    onOpenFilters: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp),
    ) {
        BackButton()
        TextField(
            state = viewModel.searchText,
            placeholder = { Text(stringResource(Res.string.fav_search_placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (viewModel.searchText.text.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchText.clearText() }) {
                        Icon(Icons.Default.Close, stringResource(Res.string.fav_clear_search))
                    }
                }
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { focusManager.clearFocus() },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
        )
        val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        val shrinkSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        BadgedBox(
            badge = {
                AnimatedContent(
                    targetState = viewModel.activeFilterCount,
                    transitionSpec = { scaleIn(popSpec) togetherWith scaleOut(shrinkSpec) },
                    label = "filterBadge",
                ) { count ->
                    if (count > 0) Badge { Text(count.toString()) }
                }
            },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            // Drop focus first so the keyboard doesn't come back when the sheet closes
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    onOpenFilters()
                }
            ) {
                Icon(Icons.Default.FilterList, stringResource(Res.string.fav_filter))
            }
        }
    }
}

@Composable
private fun FavoriteTabs(
    library: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = if (library) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tabIndicator",
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .graphicsLayer { translationX = fraction * size.width * if (rtl) -1 else 1 }
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        )
        Row(Modifier.fillMaxSize()) {
            listOf(false to Res.string.fav_tab_overview, true to Res.string.fav_tab_library).forEach { (isLibrary, label) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                        ) { onSelect(isLibrary) },
                ) {
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (library == isLibrary) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GetStarted(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(stringResource(Res.string.get_started), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(Res.string.get_started_info), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onAdd, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(Res.string.add_a_favorite))
            }
        }
    }
}
