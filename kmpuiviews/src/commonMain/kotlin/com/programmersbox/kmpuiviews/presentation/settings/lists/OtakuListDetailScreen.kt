package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.components.OptionsSheetValues
import com.programmersbox.kmpuiviews.presentation.components.SecureScreenWrapper
import com.programmersbox.kmpuiviews.presentation.components.UtilityCard
import com.programmersbox.kmpuiviews.presentation.components.blurkind.rememberBlurKindState
import com.programmersbox.kmpuiviews.presentation.components.blurkind.setBlurKind
import com.programmersbox.kmpuiviews.presentation.components.blurkind.setBlurKindSource
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.optionsSheetList
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.kmpuiviews.presentation.settings.lists.addtolist.ListChoiceScreen
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.loadItem
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import com.programmersbox.sharedcomponents.qrcode.QrCodeRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.areYouSureRemove
import otakuworld.kmpuiviews.generated.resources.are_you_sure_delete_list
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.chooseASource
import otakuworld.kmpuiviews.generated.resources.confirm
import otakuworld.kmpuiviews.generated.resources.delete_list_title
import otakuworld.kmpuiviews.generated.resources.delete_multiple
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.remove
import otakuworld.kmpuiviews.generated.resources.remove_items
import otakuworld.kmpuiviews.generated.resources.search
import otakuworld.kmpuiviews.generated.resources.yes

@Composable
fun OtakuCustomListScreenStandAlone(
    screen: Screen.CustomListScreen.CustomListItem,
    viewModel: OtakuCustomListViewModel = koinViewModel { parametersOf(screen) },
    isHorizontal: Boolean = false,
    dao: ListRepository = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val navController = LocalNavActions.current
    // Key on loaded/not loaded only. Keying on the list itself rebuilt the whole screen on every
    // database update, resetting scroll and selection and re-showing the hidden nav bar.
    Crossfade(
        targetState = viewModel.customList != null,
        label = "",
    ) { loaded ->
        val target = viewModel.customList
        if (loaded && target != null) {
            SecureScreenWrapper(target.item.useBiometric) {
                OtakuCustomListScreen(
                    viewModel = viewModel,
                    customItem = target,
                    writeToFile = viewModel::writeToFile,
                    isHorizontal = isHorizontal,
                    deleteAll = viewModel::deleteAll,
                    updateDetails = viewModel::updateDetails,
                    searchQuery = viewModel.searchQuery,
                    setQuery = viewModel::setQuery,
                    navigateBack = { navController.popBackStack() },
                    addSecurityItem = {
                        scope.launch { dao.updateBiometric(it, true) }
                    },
                    removeSecurityItem = {
                        scope.launch { dao.updateBiometric(it, false) }
                    },
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalComposeUiApi::class,
)
@Composable
fun OtakuCustomListScreen(
    viewModel: OtakuCustomListViewModel,
    customItem: CustomList,
    writeToFile: (PlatformFile) -> Unit,
    deleteAll: suspend () -> Unit,
    updateDetails: (name: String, description: String) -> Unit,
    searchQuery: TextFieldState,
    setQuery: (String) -> Unit,
    navigateBack: () -> Unit,
    isHorizontal: Boolean = false,
    addSecurityItem: (String) -> Unit,
    removeSecurityItem: (String) -> Unit,
    dao: ListRepository = koinInject(),
) {
    val blurKindState = rememberBlurKindState()
    val navController = LocalNavActions.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    val sourceRepository = LocalSourcesRepository.current

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

    val pickDocumentLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { document -> document?.let { writeToFile(it) } }

    val qrCodeRepository = koinInject<QrCodeRepository>()

    HideNavBarWhileOnScreen()

    // Phones hide the toolbar while scrolling down. Wider windows keep it visible.
    val isCompact = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact && !isHorizontal
    var toolbarScrolledAway by remember { mutableStateOf(false) }
    val toolbarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y < -1f) toolbarScrolledAway = true
                else if (consumed.y > 1f) toolbarScrolledAway = false
                return Offset.Zero
            }
        }
    }

    var searching by remember { mutableStateOf(false) }
    val toolbarMode = when {
        viewModel.inSelectionMode -> ListToolbarMode.Select
        searching -> ListToolbarMode.Search
        else -> ListToolbarMode.Browse
    }

    BackHandler(toolbarMode != ListToolbarMode.Browse) {
        if (viewModel.inSelectionMode) {
            viewModel.clearSelection()
        } else {
            searching = false
            setQuery("")
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    var deleteList by remember { mutableStateOf(false) }

    if (deleteList) {
        var listName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { deleteList = false },
            title = { Text(stringResource(Res.string.delete_list_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.are_you_sure_delete_list))
                    Text(customItem.item.name)
                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { deleteAll() }
                            deleteList = false
                            navigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = listName == customItem.item.name
                ) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteList = false }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    var optionsSheet by optionsSheetList<CustomListItemOptionSheet>(
        onOpen = {
            sourceRepository
                .toSourceByApiServiceName(it.serviceName)
                ?.apiService
                ?.let { source ->
                    flow {
                        Cached.cache[it.url]?.let {
                            emit(
                                it
                                    .toDbModel()
                                    .toItemModel(source)
                            )
                        } ?: emitAll(source.getSourceByUrlFlow(it.url))
                    }
                }
                ?.dispatchIo()
                ?.onStart { showLoadingDialog = true }
                ?.onEach { item ->
                    showLoadingDialog = false
                    navController.details(item)
                }
                ?.onCompletion { showLoadingDialog = false }
                ?.launchIn(scope)
        }
    ) { model, optionsSheetScope ->
        var showDeleteDialog by remember { mutableStateOf(false) }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Remove item?") },
                text = { Text("Are you sure you want to remove ${model.title} from ${customItem.item.name}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                dao.removeItem(model.info)
                                viewModel.customList?.item?.let { dao.updateFullList(it) }
                            }.invokeOnCompletion {
                                showDeleteDialog = false
                                // The item is gone, so the sheet has nothing left to show.
                                optionsSheetScope.dismiss()
                            }
                        }
                    ) { Text(stringResource(Res.string.confirm)) }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.cancel)) } }
            )
        }

        UtilityCard(
            onClick = { showDeleteDialog = true },
            icon = Icons.Default.Delete,
            title = "Remove",
            modifier = Modifier.weight(1f)
        )
    }

    var showCoverPicker by remember { mutableStateOf(false) }

    if (showCoverPicker) {
        CoverPickerSheet(
            items = customItem.list,
            currentCoverUrl = viewModel.coverImageUrl,
            colorFilter = colorFilter,
            onSelect = {
                viewModel.updateCoverImage(it)
                showCoverPicker = false
            },
            onDismiss = { showCoverPicker = false },
        )
    }

    var showEditSheet by remember { mutableStateOf(false) }

    if (showEditSheet) {
        EditListSheet(
            customList = customItem,
            coverUrl = viewModel.coverImageUrl,
            colorFilter = colorFilter,
            onChangeCover = { showCoverPicker = true },
            onSave = { name, description ->
                updateDetails(name, description)
                showEditSheet = false
            },
            addSecurityItem = addSecurityItem,
            removeSecurityItem = removeSecurityItem,
            onDismiss = { showEditSheet = false },
        )
    }

    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        SortAndViewSheet(
            sort = viewModel.sort,
            viewMode = viewModel.viewMode,
            onSort = { viewModel.sort = it },
            onViewMode = { viewModel.viewMode = it },
            onDismiss = { showSortSheet = false },
        )
    }

    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        val count = viewModel.selectedItems.size
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(Res.string.remove_items)) },
            text = { Text(pluralStringResource(Res.plurals.areYouSureRemove, count, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.removeSelected()
                                .onFailure { showMessage("Couldn't remove those titles") }
                            showRemoveDialog = false
                        }
                    }
                ) { Text(stringResource(Res.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    var showAddToListSheet by remember { mutableStateOf(false) }

    if (showAddToListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddToListSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ListChoiceScreen(
                navigationIcon = {
                    IconButton(onClick = { showAddToListSheet = false }) { Icon(Icons.Default.Close, null) }
                },
                onClick = { target ->
                    if (target.item.uuid == customItem.item.uuid) {
                        showMessage("Already in ${target.item.name}")
                    } else {
                        scope.launch {
                            viewModel.addSelectedTo(target)
                                .onSuccess { added ->
                                    showMessage(
                                        if (added == 0) "Already in ${target.item.name}"
                                        else "Added $added to ${target.item.name}"
                                    )
                                }
                                .onFailure { showMessage("Couldn't add those titles") }
                        }
                    }
                    showAddToListSheet = false
                }
            )
        }
    }

    val visibleCount = when (val state = viewModel.items) {
        is OtakuListState.ByTitle -> state.items.size
        is OtakuListState.BySource -> state.items.sumOf { it.value.size }
        OtakuListState.Empty -> 0
    }
    val countLabel = if (viewModel.allSourcesShown && searchQuery.text.isEmpty()) {
        "${customItem.list.size} titles"
    } else {
        "$visibleCount of ${customItem.list.size}"
    }

    val sourceChips: @Composable () -> Unit = {
        SourceChipsRow(
            sources = viewModel.sources,
            total = customItem.list.size,
            filtered = viewModel.filtered,
            allSourcesShown = viewModel.allSourcesShown,
            onSelectSource = viewModel::selectSource,
            onShowAll = viewModel::clearFilter,
        )
    }

    // The app's bottom nav is hidden here, so only the system bars and keyboard matter.
    // Works for both 3-button and gesture navigation.
    val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
    // Room for the toolbar so the last row and the snackbar aren't covered.
    val toolbarClearance = FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset * 2

    val gridState = rememberLazyGridState()
    // Swap to the slim bar once the header's back/edit row has scrolled under the status bar.
    val collapseThreshold = with(LocalDensity.current) { 96.dp.roundToPx() }
    val collapsed by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > collapseThreshold
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            // Smaller covers than other grids so more of the list fits on screen.
            columns = adaptiveGridCell(ComposableUtils.IMAGE_WIDTH * 2 / 3),
            contentPadding = WindowInsets.navigationBars.asPaddingValues() + PaddingValues(bottom = toolbarClearance),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(toolbarScrollConnection)
                .setBlurKindSource(blurKindState)
        ) {
            item(
                key = "hero",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ListHeroHeader(
                    customList = customItem,
                    coverUrl = viewModel.coverImageUrl,
                    sources = viewModel.sources,
                    colorFilter = colorFilter,
                    onBack = navigateBack,
                    onEdit = { showEditSheet = true },
                )
            }

            item(
                key = "chips",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Box(modifier = Modifier.padding(bottom = 4.dp)) { sourceChips() }
            }

            when (val state = viewModel.items) {
                is OtakuListState.BySource if state.items.isNotEmpty() -> {
                    state.items.forEach { (source, sourceItems) ->
                        val showSource = state.sourceShower[source]?.value == true

                        //TODO: Try stickyHeader
                        item(
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 4.dp,
                                onClick = {
                                    state.sourceShower[source]?.value = state.sourceShower[source]?.value?.not() == true
                                },
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                ListItem(
                                    modifier = Modifier.padding(4.dp),
                                    headlineContent = { Text(LocalSourcesRepository.current.displayNameFor(source)) },
                                    leadingContent = { Text(sourceItems.size.toString()) },
                                    trailingContent = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            null,
                                            modifier = Modifier.rotate(animateFloatAsState(if (showSource) 180f else 0f, label = "").value)
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                    )
                                )
                            }
                        }

                        if (showSource) {
                            items(
                                items = sourceItems,
                                key = { it.title + it.source + it.uniqueId },
                                contentType = { it }
                            ) { item ->
                                CustomItemVertical(
                                    items = listOf(item),
                                    title = item.title,
                                    showLoadingDialog = { showLoadingDialog = it },
                                    onError = { showMessage("Something went wrong. Source might not be installed") },
                                    selectionMode = viewModel.inSelectionMode,
                                    selected = viewModel.isSelected(listOf(item)),
                                    onToggleSelection = { viewModel.toggleSelection(listOf(item)) },
                                    colorFilter = colorFilter,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }

                is OtakuListState.ByTitle if state.items.isNotEmpty() -> {
                    items(
                        items = state.items,
                        key = { it.uniqueId },
                        contentType = { it }
                    ) { item ->
                        CustomItemVertical(
                            items = listOf(item),
                            title = item.title,
                            showLoadingDialog = { showLoadingDialog = it },
                            onError = { showMessage("Something went wrong. Source might not be installed") },
                            selectionMode = viewModel.inSelectionMode,
                            selected = viewModel.isSelected(listOf(item)),
                            onToggleSelection = { viewModel.toggleSelection(listOf(item)) },
                            colorFilter = colorFilter,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                else -> {
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        ) {
                            Text(
                                if (customItem.list.isEmpty()) "You haven't added anything to this list yet!"
                                else "No titles match."
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = collapsed,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CollapsedListBar(
                customList = customItem,
                coverUrl = viewModel.coverImageUrl,
                countLabel = countLabel,
                colorFilter = colorFilter,
                onBack = navigateBack,
                containerColor = if (showBlur) Color.Transparent else MaterialTheme.colorScheme.surface,
                modifier = Modifier.setBlurKind(blurKindState),
                chips = sourceChips,
            )
        }

        AnimatedVisibility(
            visible = !isCompact || toolbarMode != ListToolbarMode.Browse || !toolbarScrolledAway,
            enter = slideInVertically { it * 2 } + fadeIn(),
            exit = slideOutVertically { it * 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(bottomInsets)
                .padding(bottom = FloatingToolbarDefaults.ScreenOffset)
        ) {
            ListFloatingToolbar(
                mode = toolbarMode,
                searchQuery = searchQuery,
                searchPlaceholder = stringResource(Res.string.search) + " " + customItem.item.name,
                selectedCount = viewModel.selectedItems.size,
                canDeleteList = customItem.item.uuid != AppConfig.forLaterUuid,
                onSearch = { searching = true },
                onCloseSearch = {
                    searching = false
                    setQuery("")
                },
                onSort = { showSortSheet = true },
                onShare = {
                    scope.launch {
                        qrCodeRepository.shareUrl(
                            customItem.list.joinToString("\n") { "${it.title} - ${it.url}" },
                            customItem.item.name
                        )
                    }
                },
                onEdit = { showEditSheet = true },
                onExport = { pickDocumentLauncher.launch(customItem.item.name, "json") },
                onDeleteList = { deleteList = true },
                onRandom = {
                    viewModel.randomPick()
                        ?.let { pick -> optionsSheet = pick.map { it.toOptionsSheetValues() } }
                        ?: showMessage("Nothing to pick from")
                },
                onClearSelection = viewModel::clearSelection,
                onSelectAll = viewModel::selectAllVisible,
                onInfo = {
                    viewModel
                        .selectedItems
                        .firstOrNull()
                        ?.let { optionsSheet = listOf(it.toOptionsSheetValues()) }
                },
                onAddToList = { showAddToListSheet = true },
                onRemove = { showRemoveDialog = true },
            )
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(bottomInsets)
                .padding(bottom = toolbarClearance)
        )
    }
}

data class CustomListItemOptionSheet(
    override val imageUrl: String,
    override val title: String,
    override val description: String,
    override val serviceName: String,
    override val url: String,
    val info: CustomListInfo,
) : OptionsSheetValues

private fun CustomListInfo.toOptionsSheetValues() = CustomListItemOptionSheet(
    imageUrl = imageUrl,
    title = title,
    description = description,
    serviceName = source,
    url = url,
    info = this
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomItemVertical(
    items: List<CustomListInfo>,
    title: String,
    showLoadingDialog: (Boolean) -> Unit,
    onError: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
) {
    val biometrics = rememberBiometricOpening()
    val scope = rememberCoroutineScope()
    val sourceRepository = LocalSourcesRepository.current
    val navController = LocalNavActions.current

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
                title = stringResource(Res.string.chooseASource),
                list = items,
                onClick = { item ->
                    showBottomSheet = false
                    sourceRepository
                        .toSourceByApiServiceName(item.source)
                        ?.apiService
                        ?.let { source ->
                            flow {
                                Cached.cache[item.url]?.let {
                                    emit(
                                        it
                                            .toDbModel()
                                            .toItemModel(source)
                                    )
                                } ?: emitAll(source.getSourceByUrlFlow(item.url))
                            }
                        }
                        ?.dispatchIo()
                        ?.onStart { showLoadingDialog(true) }
                        ?.onEach {
                            showLoadingDialog(false)
                            navController.details(it)
                        }
                        ?.onCompletion { showLoadingDialog(false) }
                        ?.launchIn(scope) ?: onError()
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
        onLongPress = { c -> if (c == ComponentState.Pressed) onToggleSelection() },
        imageUrl = remember(items) { items.firstOrNull()?.imageUrl.orEmpty() },
        name = title,
        //placeHolder = { painterLogo() },
        placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
        favoriteIcon = {
            if (selected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )
            }
            if (selectionMode) {
                Icon(
                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
                            CircleShape
                        )
                )
            }
            if (items.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        items.size.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        onClick = {
            if (selectionMode) {
                onToggleSelection()
                return@M3CoverCard
            }
            scope.launch {
                biometrics.openIfNotIncognito(*items.toTypedArray()) {
                    if (items.size == 1) {
                        runCatching {
                            val listItem = items.first()
                            sourceRepository
                                .loadItem(listItem.source, listItem.url)
                                ?.onStart { showLoadingDialog(true) }
                                ?.onEach {
                                    showLoadingDialog(false)
                                    navController.details(it)
                                }
                                ?.onCompletion { showLoadingDialog(false) }
                                ?.launchIn(scope) ?: error("Nothing")
                        }.onFailure {
                            it.printStackTrace()
                            onError()
                        }
                    } else {
                        showBottomSheet = true
                    }
                }
            }
        },
        colorFilter = colorFilter,
        modifier = modifier
            .aspectRatio(2f / 3f)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                else Modifier
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteItemsModal(
    list: Map<String, List<CustomListInfo>>,
    onRemove: suspend (List<CustomListInfo>) -> Result<Boolean>,
    onDismiss: () -> Unit,
    state: SheetState,
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = state
    ) {
        val itemsToDelete = remember { mutableStateListOf<CustomListInfo>() }
        var showPopup by remember { mutableStateOf(false) }
        var removing by remember { mutableStateOf(false) }

        if (showPopup) {
            val onPopupDismiss = { showPopup = false }

            AlertDialog(
                onDismissRequest = if (removing) {
                    {}
                } else onPopupDismiss,
                title = { Text("Delete") },
                text = {
                    Text(
                        pluralStringResource(
                            Res.plurals.areYouSureRemove,
                            itemsToDelete.size,
                            itemsToDelete.size
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            removing = true
                            scope.launch {
                                onRemove(itemsToDelete)
                                    .onSuccess {
                                        removing = false
                                        itemsToDelete.clear()
                                        onPopupDismiss()
                                        onDismiss()
                                    }
                            }
                        },
                        enabled = !removing
                    ) { Text(stringResource(Res.string.yes)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } },
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.delete_multiple)) },
                    windowInsets = WindowInsets(0.dp),
                )
            },
            bottomBar = {
                BottomAppBar(
                    contentPadding = PaddingValues(0.dp),
                    windowInsets = WindowInsets(0.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(Res.string.cancel)) }

                    Button(
                        onClick = { showPopup = true },
                        enabled = itemsToDelete.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(Res.string.remove)) }
                }
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = adaptiveGridCell(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = padding,
                modifier = Modifier.padding(4.dp),
            ) {
                list.forEach { (t, u) ->
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Column {
                            HorizontalDivider()
                            CenterAlignedTopAppBar(
                                title = { Text(t) },
                                windowInsets = WindowInsets(0.dp),
                            )
                        }
                    }
                    items(u) { item ->
                        val transition = updateTransition(targetState = item in itemsToDelete, label = "")
                        val outlineColor = MaterialTheme.colorScheme.outline
                        M3CoverCard(
                            imageUrl = item.imageUrl,
                            name = item.title,
                            //placeHolder = { painterLogo() },
                            placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
                            onClick = {
                                if (item in itemsToDelete) itemsToDelete.remove(item) else itemsToDelete.add(item)
                            },
                            modifier = Modifier
                                .animateItem()
                                .border(
                                    border = BorderStroke(
                                        transition.animateDp(label = "border_width") { target -> if (target) 4.dp else 1.dp }.value,
                                        transition.animateColor(label = "border_color") { target -> if (target) Color(0xfff44336) else outlineColor }.value
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverPickerSheet(
    items: List<CustomListInfo>,
    currentCoverUrl: String,
    colorFilter: ColorFilter?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        ) {
            items(items, key = { it.uniqueId }) { info ->
                val isSelected = info.imageUrl == currentCoverUrl
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(info.imageUrl) }
                ) {
                    Box {
                        ImageLoaderChoice(
                            imageUrl = info.imageUrl,
                            name = info.title,
                            placeHolder = { painterLogo() },
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.small)
                                .let {
                                    if (isSelected)
                                        it.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                    else
                                        it
                                }
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            )
                        }
                    }
                    Text(
                        info.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
