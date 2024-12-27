package com.programmersbox.uiviews.presentation.lists

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.DataStoreHandler
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.CoilGradientImage
import com.programmersbox.uiviews.presentation.components.DynamicSearchBar
import com.programmersbox.uiviews.presentation.components.ListBottomScreen
import com.programmersbox.uiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.presentation.components.plus
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.theme.LocalCustomListDao
import com.programmersbox.uiviews.theme.LocalSourcesRepository
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CustomBannerBox
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.M3CoverCard
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.biometricPrompting
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.loadItem
import com.programmersbox.uiviews.utils.rememberBiometricPrompt
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
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
    customItem: CustomList?,
    writeToFile: (Uri, Context) -> Unit,
    deleteAll: suspend () -> Unit,
    rename: suspend (String) -> Unit,
    searchQuery: String,
    setQuery: (String) -> Unit,
    searchBarActive: Boolean,
    onSearchBarActiveChange: (Boolean) -> Unit,
    navigateBack: () -> Unit,
    isHorizontal: Boolean = false,
    addSecurityItem: (UUID) -> Unit,
    removeSecurityItem: (UUID) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

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
                    Text(customItem?.item?.name.orEmpty())
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
                    enabled = listName == customItem?.item?.name
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

    var showInfoSheet by rememberSaveable { mutableStateOf(false) }
    val infoSheetState = rememberModalBottomSheetState()

    if (showInfoSheet) {
        customItem?.let {
            InfoSheet(
                onDismiss = { showInfoSheet = false },
                sheetState = infoSheetState,
                customItem = it,
                rename = { name -> scope.launch { rename(name) } },
                addSecurityItem = addSecurityItem,
                removeSecurityItem = removeSecurityItem,
                logo = logoDrawable,
                onDeleteListAction = { deleteList = true },
                onRemoveItemsAction = {
                    it
                        .item
                        .uuid
                        .toString()
                        .let { navController.navigate(Screen.CustomListScreen.DeleteFromList(it)) }
                },
                onExportAction = { pickDocumentLauncher.launch("${it.item.name}.json") },
                filtered = viewModel.filtered,
                onFilterAction = viewModel::filter,
                onClearFilterAction = viewModel::clearFilter,
                showBySource = viewModel.showBySource,
                onShowBySource = { viewModel.toggleShowSource(context, it) },
            )
        }
    }

    var showBanner by remember { mutableStateOf(false) }

    CustomBannerBox<CustomListInfo>(
        showBanner = showBanner,
        bannerContent = {
            ListItem(
                leadingContent = {
                    CoilGradientImage(
                        model = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(it?.imageUrl)
                                .lifecycle(LocalLifecycleOwner.current)
                                .crossfade(true)
                                .placeholder(logoDrawable.logoId)
                                .error(logoDrawable.logoId)
                                .build()
                        ),
                        modifier = Modifier
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            .clip(MaterialTheme.shapes.small)
                    )
                },
                overlineContent = { Text(it?.source.orEmpty()) },
                headlineContent = { Text(it?.title.orEmpty()) },
                supportingContent = {
                    Text(
                        it?.description.orEmpty(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 5
                    )
                },
                modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
            )
        },
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.padding(LocalNavHostPadding.current)
                )
            },
            topBar = {
                DynamicSearchBar(
                    query = searchQuery,
                    onQueryChange = setQuery,
                    isDocked = isHorizontal,
                    onSearch = { onSearchBarActiveChange(false) },
                    active = searchBarActive,
                    onActiveChange = { onSearchBarActiveChange(it) },
                    placeholder = { Text(stringResource(id = R.string.search) + " " + customItem?.item?.name.orEmpty()) },
                    leadingIcon = {
                        IconButton(onClick = navigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = animateColorAsState(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (searchBarActive) {
                                    1f
                                } else {
                                    if (showBlur) 0f else 1f
                                }
                            ),
                            label = ""
                        ).value,
                    ),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(searchQuery.isNotEmpty()) {
                                IconButton(onClick = { setQuery("") }) {
                                    Icon(Icons.Default.Cancel, null)
                                }
                            }

                            Text("(${customItem?.list.orEmpty().size})")

                            AnimatedVisibility(!searchBarActive) {
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
                                                            customItem?.list.orEmpty().joinToString("\n") { "${it.title} - ${it.url}" }
                                                        )
                                                        putExtra(Intent.EXTRA_TITLE, customItem?.item?.name.orEmpty())
                                                    },
                                                    context.getString(R.string.share_item, customItem?.item?.name.orEmpty())
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            if (showBlur)
                                it.hazeChild(
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
                            ListItem(
                                headlineContent = { Text(item.title) },
                                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                modifier = Modifier
                                    .clickable {
                                        setQuery(item.title)
                                        onSearchBarActiveChange(false)
                                    }
                                    .animateItem()
                            )
                            if (index != 0) {
                                HorizontalDivider()
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
                    .haze(state = hazeState)
            ) {
                when (val state = viewModel.items) {
                    is OtakuListState.BySource if state.items.isNotEmpty() -> {
                        state.items.forEach { (source, sourceItems) ->
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                CenterAlignedTopAppBar(
                                    title = { Text(source) },
                                    windowInsets = WindowInsets(0.dp),
                                )
                            }

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
                                    onShowBanner = {
                                        newItem(if (it) item else null)
                                        showBanner = it
                                    },
                                    modifier = Modifier.animateItem()
                                )
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
                                onShowBanner = {
                                    newItem(if (it) item.value.firstOrNull() else null)
                                    showBanner = it
                                },
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
}

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
                    context = context,
                    defaultValue = false,
                    key = booleanPreferencesKey("asdf")
                )
            )
        }
        OtakuCustomListScreen(
            viewModel = viewModel,
            customItem = null,
            writeToFile = viewModel::writeToFile,
            navigateBack = {},
            isHorizontal = false,
            deleteAll = viewModel::deleteAll,
            rename = viewModel::rename,
            searchQuery = viewModel.searchQuery,
            setQuery = viewModel::setQuery,
            searchBarActive = viewModel.searchBarActive,
            onSearchBarActiveChange = { viewModel.searchBarActive = it },
            addSecurityItem = {},
            removeSecurityItem = {},
        )
    }
}

//TODO: Add a bottom sheet for some info about the list.
// This includes being able to maybe changing the cover and anything else
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalLayoutApi::class)
@Composable
private fun InfoSheet(
    customItem: CustomList,
    sheetState: SheetState,
    addSecurityItem: (UUID) -> Unit,
    removeSecurityItem: (UUID) -> Unit,
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
        sheetState = sheetState
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
                        Text(stringResource(R.string.delete_list_title))
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