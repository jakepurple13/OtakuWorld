package com.programmersbox.uiviews.presentation.lists

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.DynamicSearchBar
import com.programmersbox.uiviews.presentation.components.ListBottomScreen
import com.programmersbox.uiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.presentation.components.M3CoverCard
import com.programmersbox.uiviews.presentation.components.OptionsSheetValues
import com.programmersbox.uiviews.presentation.components.optionsSheetList
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.theme.LocalCustomListDao
import com.programmersbox.uiviews.theme.LocalSourcesRepository
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.biometricPrompting
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.loadItem
import com.programmersbox.uiviews.utils.rememberBiometricPrompt
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OtakuCustomListScreen(
    viewModel: OtakuCustomListViewModel,
    customItem: CustomList,
    writeToFile: (Uri, Context) -> Unit,
    deleteAll: suspend () -> Unit,
    rename: suspend (String) -> Unit,
    searchQuery: TextFieldState,
    setQuery: (String) -> Unit,
    navigateBack: () -> Unit,
    isHorizontal: Boolean = false,
    addSecurityItem: (String) -> Unit,
    removeSecurityItem: (String) -> Unit,
    dao: ListDao = koinInject(),
) {
    val hazeState = remember { HazeState() }
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sourceRepository = LocalSourcesRepository.current

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

    val window = LocalActivity.current

    DisposableEffect(customItem.item.useBiometric) {
        if (customItem.item.useBiometric) {
            window?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    val logoDrawable = koinInject<AppLogo>()

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { document -> document?.let { writeToFile(it, context) } }

    val shareItem = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    var deleteList by remember { mutableStateOf(false) }

    if (deleteList) {
        var listName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { deleteList = false },
            title = { Text(stringResource(R.string.delete_list_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.are_you_sure_delete_list))
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
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteList = false }) { Text(stringResource(id = R.string.cancel)) } }
        )
    }

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    /*val modalSheetState = rememberModalBottomSheetState()
    var showDeleteModal by remember { mutableStateOf(false) }

    if (showDeleteModal) {
        DeleteItemsModal(
            list = listBySource,
            onRemove = removeItems,
            onDismiss = {
                scope.launch { modalSheetState.hide() }
                    .invokeOnCompletion { showDeleteModal = false }
            },
            drawable = logoDrawable.logo,
            state = modalSheetState
        )
    }*/

    var optionsSheet by optionsSheetList<CustomListItemOptionSheet>(
        onOpen = {
            sourceRepository
                .toSourceByApiServiceName(it.serviceName)
                ?.apiService
                ?.let { source ->
                    Cached.cache[it.url]?.let { model ->
                        flow {
                            emit(
                                model
                                    .toDbModel()
                                    .toItemModel(source)
                            )
                        }
                    } ?: source.getSourceByUrlFlow(it.url)
                }
                ?.dispatchIo()
                ?.onStart { showLoadingDialog = true }
                ?.onEach { item ->
                    showLoadingDialog = false
                    navController.navigateToDetails(item)
                }
                ?.onCompletion { showLoadingDialog = false }
                ?.launchIn(scope)
        }
    ) { model ->
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
                            }.invokeOnCompletion { showDeleteDialog = false }
                        }
                    ) { Text(stringResource(id = R.string.confirm)) }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(id = R.string.cancel)) } }
            )
        }

        OptionsItem(
            "Remove",
            onClick = { showDeleteDialog = true }
        )
    }

    var showInfoSheet by rememberSaveable { mutableStateOf(false) }
    val infoSheetState = rememberModalBottomSheetState()

    if (showInfoSheet) {
        InfoSheet(
            onDismiss = { showInfoSheet = false },
            sheetState = infoSheetState,
            customItem = customItem,
            rename = { name -> scope.launch { rename(name) } },
            addSecurityItem = addSecurityItem,
            removeSecurityItem = removeSecurityItem,
            logo = logoDrawable,
            onDeleteListAction = { deleteList = true },
            onRemoveItemsAction = {
                customItem
                    .item
                    .uuid
                    .toString()
                    .let { navController.navigate(Screen.CustomListScreen.DeleteFromList(it)) }
            },
            onExportAction = { pickDocumentLauncher.launch("${customItem.item.name}.json") },
            filtered = viewModel.filtered,
            onFilterAction = viewModel::filter,
            onClearFilterAction = viewModel::clearFilter,
            showBySource = viewModel.showBySource,
            onShowBySource = { viewModel.toggleShowSource(context, it) },
        )
    }


    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        },
        topBar = {
            val searchBarState = rememberSearchBarState()

            DynamicSearchBar(
                textFieldState = searchQuery,
                searchBarState = searchBarState,
                isDocked = isHorizontal,
                onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                placeholder = { Text(stringResource(id = R.string.search) + " " + customItem.item.name) },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }

                    } else {
                        IconButton(onClick = navigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    }
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(searchQuery.text.isNotEmpty()) {
                            IconButton(
                                onClick = { setQuery("") }
                            ) { Icon(Icons.Default.Cancel, null) }
                        }

                        Text("(${customItem.list.size})")

                        AnimatedVisibility(searchBarState.currentValue == SearchBarValue.Collapsed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        shareItem.launchCatching(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        customItem.list.joinToString("\n") { "${it.title} - ${it.url}" }
                                                    )
                                                    putExtra(Intent.EXTRA_TITLE, customItem.item.name)
                                                },
                                                context.getString(R.string.share_item, customItem.item.name)
                                            )
                                        )
                                    }
                                ) { Icon(Icons.Default.Share, null) }

                                IconButton(
                                    onClick = { showInfoSheet = true }
                                ) { Icon(Icons.Default.Info, null) }
                            }
                        }
                    }
                },
                modifier = Modifier.let {
                    if (showBlur)
                        it.hazeEffect(
                            hazeState,
                            HazeMaterials.regular(MaterialTheme.colorScheme.surface)
                        ) {
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                        } else it
                },
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(items = viewModel.searchItems) { index, item ->
                        Card(
                            onClick = {
                                setQuery(item.title)
                                scope.launch { searchBarState.animateToCollapsed() }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            ListItem(
                                headlineContent = { Text(item.title) },
                                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            contentPadding = padding + LocalNavHostPadding.current,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .hazeSource(state = hazeState)
        ) {
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
                                    headlineContent = { Text(source) },
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
                                    logo = logoDrawable.logo,
                                    showLoadingDialog = { showLoadingDialog = it },
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                "Something went wrong. Source might not be installed",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onShowBanner = { optionsSheet = listOf(item.toOptionsSheetValues()) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }

                is OtakuListState.ByTitle if state.items.isNotEmpty() -> {
                    items(
                        items = state.items,
                        key = { it.key },
                        contentType = { it }
                    ) { item ->
                        CustomItemVertical(
                            items = item.value,
                            title = item.key,
                            logo = logoDrawable.logo,
                            showLoadingDialog = { showLoadingDialog = it },
                            onError = {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        "Something went wrong. Source might not be installed",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onShowBanner = { optionsSheet = item.value.map { it.toOptionsSheetValues() } },
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
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("You haven't added anything to this list yet!")
                        }
                    }
                }
            }
        }
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
    logo: Drawable?,
    showLoadingDialog: (Boolean) -> Unit,
    onError: () -> Unit,
    onShowBanner: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val sourceRepository = LocalSourcesRepository.current
    val navController = LocalNavController.current

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
                list = items,
                onClick = { item ->
                    showBottomSheet = false
                    sourceRepository
                        .toSourceByApiServiceName(item.source)
                        ?.apiService
                        ?.let { source ->
                            Cached.cache[item.url]?.let {
                                flow {
                                    emit(
                                        it
                                            .toDbModel()
                                            .toItemModel(source)
                                    )
                                }
                            } ?: source.getSourceByUrlFlow(item.url)
                        }
                        ?.dispatchIo()
                        ?.onStart { showLoadingDialog(true) }
                        ?.onEach {
                            showLoadingDialog(false)
                            navController.navigateToDetails(it)
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
        onLongPress = { c -> onShowBanner(c == ComponentState.Pressed) },
        imageUrl = remember(items) { items.firstOrNull()?.imageUrl.orEmpty() },
        name = title,
        placeHolder = logo,
        favoriteIcon = {
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
            if (items.size == 1) {
                runCatching {
                    val listItem = items.first()
                    sourceRepository.loadItem(listItem.source, listItem.url)
                        ?.onStart { showLoadingDialog(true) }
                        ?.onEach {
                            showLoadingDialog(false)
                            navController.navigateToDetails(it)
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
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteItemsModal(
    list: Map<String, List<CustomListInfo>>,
    onRemove: suspend (List<CustomListInfo>) -> Result<Boolean>,
    onDismiss: () -> Unit,
    drawable: Drawable,
    state: SheetState,
) {
    val context = LocalContext.current
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
                        context.resources.getQuantityString(
                            R.plurals.areYouSureRemove,
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
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } },
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.delete_multiple)) },
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
                    ) { Text(stringResource(id = R.string.cancel)) }

                    Button(
                        onClick = { showPopup = true },
                        enabled = itemsToDelete.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(id = R.string.remove)) }
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
                            placeHolder = drawable,
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

@LightAndDarkPreviews
@Composable
private fun CustomListScreenPreview() {
    PreviewTheme {
        val listDao: ListDao = LocalCustomListDao.current
        val context = LocalContext.current
        val viewModel: OtakuCustomListViewModel = viewModel {
            OtakuCustomListViewModel(
                listDao,
                DataStoreHandler(
                    defaultValue = false,
                    key = booleanPreferencesKey("asdf")
                )
            )
        }
        OtakuCustomListScreen(
            viewModel = viewModel,
            customItem = CustomList(
                item = CustomListItem(
                    uuid = UUID.randomUUID().toString(),
                    name = "Hello",
                ),
                list = emptyList()
            ),
            writeToFile = viewModel::writeToFile,
            navigateBack = {},
            isHorizontal = false,
            deleteAll = viewModel::deleteAll,
            rename = viewModel::rename,
            searchQuery = viewModel.searchQuery,
            setQuery = viewModel::setQuery,
            addSecurityItem = {},
            removeSecurityItem = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalLayoutApi::class)
@Composable
private fun InfoSheet(
    customItem: CustomList,
    sheetState: SheetState,
    addSecurityItem: (String) -> Unit,
    removeSecurityItem: (String) -> Unit,
    rename: (String) -> Unit,
    onDismiss: () -> Unit,
    logo: AppLogo,
    onDeleteListAction: () -> Unit,
    onRemoveItemsAction: () -> Unit,
    onExportAction: () -> Unit,
    filtered: List<String>,
    onFilterAction: (String) -> Unit,
    onClearFilterAction: () -> Unit,
    showBySource: Boolean,
    onShowBySource: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val biometricPrompt = rememberBiometricPrompt(
        onAuthenticationSucceeded = { addSecurityItem(customItem.item.uuid) },
    )

    val removeBiometricPrompt = rememberBiometricPrompt(
        onAuthenticationSucceeded = { removeSecurityItem(customItem.item.uuid) },
    )

    var showSecurityDialog by remember { mutableStateOf(false) }

    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text("Require Authentication to View this list?") },
            text = { Text("This will require phone authentication to view this list") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSecurityDialog = false
                        biometricPrompting(
                            context,
                            biometricPrompt
                        ).authenticate(
                            BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Add Authentication for ${customItem.item.name}")
                                .setSubtitle("Enter Authentication to add")
                                .setNegativeButtonText("Never Mind")
                                .build()
                        )
                    }
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSecurityDialog = false }
                ) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    var showRemoveSecurityDialog by remember { mutableStateOf(false) }

    if (showRemoveSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveSecurityDialog = false },
            title = { Text("Remove Authentication?") },
            text = { Text("This will remove the phone authentication to view this list") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveSecurityDialog = false
                        biometricPrompting(
                            context,
                            removeBiometricPrompt
                        ).authenticate(
                            BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Remove Authentication for ${customItem.item.name}")
                                .setSubtitle("Enter Authentication to remove")
                                .setNegativeButtonText("Never Mind")
                                .build()
                        )
                    }
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveSecurityDialog = false }
                ) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    var currentName by remember { mutableStateOf(customItem.item.name) }

    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.update_list_name_title)) },
            text = { Text("Are you sure you want to change the name?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        rename(currentName)
                        showAdd = false
                    }
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                currentName,
                onValueChange = { currentName = it },
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    IconButton(
                        onClick = { showAdd = true },
                        enabled = currentName != customItem.item.name
                    ) { Icon(Icons.Default.Check, null) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ListItem(
                headlineContent = {},
                leadingContent = {
                    GlideImage(
                        model = customItem.list.firstOrNull()?.imageUrl,
                        failure = placeholder(logo.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            .clip(MaterialTheme.shapes.small)
                    )
                },
                supportingContent = {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Require Biometrics?")
                            Checkbox(
                                checked = customItem.item.useBiometric,
                                onCheckedChange = {
                                    if (it) {
                                        showSecurityDialog = true
                                    } else {
                                        showRemoveSecurityDialog = true
                                    }
                                }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Show by Source?")
                            Checkbox(
                                checked = showBySource,
                                onCheckedChange = onShowBySource
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                )
            )

            HorizontalDivider()

            Text("List Count: ${customItem.list.size}")

            FlowRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                customItem.list
                    .groupBy { it.source }
                    .forEach { (t, u) ->
                        FilterChip(
                            selected = filtered.contains(t),
                            onClick = { onFilterAction(t) },
                            label = { Text(t) },
                            trailingIcon = { Text(u.size.toString()) }
                        )
                    }

                FilledTonalButton(
                    onClick = onClearFilterAction
                ) { Text("Clear Filter") }
            }

            HorizontalDivider()

            FlowRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionItem(
                    onClick = {
                        scope.launch { sheetState.hide() }
                            .invokeOnCompletion {
                                onDismiss()
                                onExportAction()
                            }
                    }
                ) {
                    Icon(Icons.Default.ImportExport, null)
                    Text(stringResource(R.string.export_list))
                }

                ActionItem(
                    onClick = {
                        scope.launch { sheetState.hide() }
                            .invokeOnCompletion {
                                onDismiss()
                                onRemoveItemsAction()
                            }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                ) {
                    Icon(Icons.Default.RemoveCircle, null)
                    Text(stringResource(R.string.remove_items))
                }

                if (customItem.item.uuid != OtakuApp.forLaterUuid) {
                    ActionItem(
                        onClick = {
                            scope.launch { sheetState.hide() }
                                .invokeOnCompletion {
                                    onDismiss()
                                    onDeleteListAction()
                                }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        colors = colors,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
            content = content
        )
    }
}