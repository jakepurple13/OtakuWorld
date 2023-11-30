@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.uiviews.lists

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.AnimatedPane
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingePolicy
import androidx.compose.material3.adaptive.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.PaneAdaptedValue
import androidx.compose.material3.adaptive.PaneScaffoldDirective
import androidx.compose.material3.adaptive.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.Alizarin
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CustomBannerBox
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.uiviews.utils.M3CoverCard
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.components.CoilGradientImage
import com.programmersbox.uiviews.utils.components.DynamicSearchBar
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.components.thenIf
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.loadItem
import com.programmersbox.uiviews.utils.navigateToDetails
import com.programmersbox.uiviews.utils.topBounds
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

//TODO: Rename this to something that makes sense
// if I like it, modify the other files to accept what is needed for reusability
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun OtakuListStuff(
    listDao: ListDao = LocalCustomListDao.current,
    viewModel: OtakuListStuffViewModel = viewModel { OtakuListStuffViewModel(listDao) },
) {
    val showListDetail by LocalSettingsHandling.current
        .showListDetail
        .collectAsStateWithLifecycle(true)

    val state = rememberListDetailPaneScaffoldNavigator(
        scaffoldDirective = calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo())
    )

    val details: @Composable ThreePaneScaffoldScope.() -> Unit = {
        AnimatedPane(modifier = Modifier) { pane ->
            AnimatedContent(
                targetState = viewModel.customItem,
                label = "",
                transitionSpec = {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (fadeOut() + slideOutHorizontally { -it })
                }
            ) { targetState ->
                if (targetState != null) {
                    DetailPart(
                        viewModel = viewModel,
                        navigateBack = {
                            viewModel.customItem = null
                            state.navigateBack()
                        },
                        isHorizontal = pane == PaneAdaptedValue.Expanded
                    )
                    BackHandler {
                        viewModel.customItem = null
                        state.navigateBack()
                    }
                } else {
                    NoDetailSelected()
                }
            }
        }
    }

    ListDetailPaneScaffold(
        scaffoldState = state.scaffoldState,
        listPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                ListPart(
                    viewModel = viewModel,
                    navigateDetail = {
                        if (showListDetail)
                            state.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        else
                            state.navigateTo(ListDetailPaneScaffoldRole.Extra)
                    }
                )
            }
        },
        detailPane = { if (showListDetail) details() },
        extraPane = if (!showListDetail) {
            { details() }
        } else null
    )
}

@ExperimentalMaterial3AdaptiveApi
fun calculateStandardPaneScaffoldDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating,
): PaneScaffoldDirective {
    val maxHorizontalPartitions: Int
    val contentPadding: PaddingValues
    val verticalSpacerSize: Dp// = 0.dp
    when (windowAdaptiveInfo.windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            maxHorizontalPartitions = 1
            contentPadding = PaddingValues(0.dp)
            verticalSpacerSize = 0.dp
        }

        WindowWidthSizeClass.Medium -> {
            maxHorizontalPartitions = 1
            contentPadding = PaddingValues(horizontal = 0.dp)
            verticalSpacerSize = 0.dp
        }

        else -> {
            maxHorizontalPartitions = 2
            contentPadding = PaddingValues(horizontal = 0.dp)
            verticalSpacerSize = 24.dp
        }
    }
    val maxVerticalPartitions: Int
    val horizontalSpacerSize: Dp = 0.dp

    // TODO(conradchen): Confirm the table top mode settings
    if (windowAdaptiveInfo.windowPosture.isTabletop) {
        maxVerticalPartitions = 2
        //horizontalSpacerSize = 24.dp
    } else {
        maxVerticalPartitions = 1
        //horizontalSpacerSize = 0.dp
    }

    val posture = windowAdaptiveInfo.windowPosture

    return PaneScaffoldDirective(
        maxHorizontalPartitions = maxHorizontalPartitions,
        contentPadding = contentPadding,
        verticalPartitionSpacerSize = verticalSpacerSize,
        horizontalPartitionSpacerSize = horizontalSpacerSize,
        maxVerticalPartitions = maxVerticalPartitions,
        excludedBounds = when (verticalHingePolicy) {
            HingePolicy.AvoidSeparating -> posture.separatingVerticalHingeBounds
            HingePolicy.AvoidOccluding -> posture.occludingVerticalHingeBounds
            HingePolicy.AlwaysAvoid -> posture.allVerticalHingeBounds
            else -> emptyList()
        }
    )
}

@Composable
private fun NoDetailSelected() {
    Surface {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
                Text("Select a list to view!")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ListPart(
    viewModel: OtakuListStuffViewModel,
    navigateDetail: () -> Unit,
) {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val dateTimeFormatter = LocalSystemDateTimeFormat.current
    val dao = LocalCustomListDao.current
    val scope = rememberCoroutineScope()

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { document -> document?.let { Screen.ImportListScreen.navigate(navController, it) } }

    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.create_new_list)) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.list_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dao.create(name)
                            showAdd = false
                        }
                    },
                    enabled = name.isNotEmpty()
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(id = R.string.cancel)) } }
        )
    }

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(R.string.custom_lists_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = { pickDocumentLauncher.launch(arrayOf("application/json")) }
                    ) { Icon(Icons.Default.FileDownload, null) }

                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.customLists) {
                ElevatedCard(
                    onClick = {
                        viewModel.customItem = it
                        navigateDetail()
                    },
                    modifier = Modifier
                        .animateItemPlacement()
                        .padding(horizontal = 4.dp)
                        .thenIf(viewModel.customItem == it) {
                            border(
                                2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CardDefaults.elevatedShape
                            )
                        }
                ) {
                    val time = remember { dateTimeFormatter.format(it.item.time) }
                    ListItem(
                        overlineContent = { Text(stringResource(id = R.string.custom_list_updated_at, time)) },
                        trailingContent = { Text("(${it.list.size})") },
                        headlineContent = { Text(it.item.name) },
                        supportingContent = {
                            Column {
                                it.list.take(3).forEach { info ->
                                    Text(info.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    )
                }
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailPart(
    viewModel: OtakuListStuffViewModel,
    navigateBack: () -> Unit,
    isHorizontal: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customItem = viewModel.customItem
    val snackbarHostState = remember { SnackbarHostState() }

    val logoDrawable = koinInject<AppLogo>()

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { document -> document?.let { viewModel.writeToFile(it, context) } }

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
                            withContext(Dispatchers.IO) { viewModel.deleteAll() }
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

    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        var name by remember { mutableStateOf(customItem?.item?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.update_list_name_title)) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.rename(name)
                            showAdd = false
                        }
                    },
                    enabled = name.isNotEmpty()
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    var showDeleteModal by remember { mutableStateOf(false) }

    if (showDeleteModal) {
        DeleteItemsModal(
            list = viewModel.listBySource,
            onRemove = viewModel::removeItems,
            onDismiss = { showDeleteModal = false },
            drawable = logoDrawable.logo
        )
    }

    var showBanner by remember { mutableStateOf(false) }

    CustomBannerBox(
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
        OtakuScaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                DynamicSearchBar(
                    query = viewModel.searchQuery,
                    onQueryChange = viewModel::setQuery,
                    isDocked = isHorizontal,
                    onSearch = { viewModel.searchBarActive = false },
                    active = viewModel.searchBarActive,
                    onActiveChange = { viewModel.searchBarActive = it },
                    placeholder = { Text(stringResource(id = R.string.search) + " " + customItem?.item?.name.orEmpty()) },
                    leadingIcon = {
                        IconButton(onClick = navigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = animateColorAsState(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (viewModel.searchBarActive) 1f else 0f
                            ),
                            label = ""
                        ).value,
                    ),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(viewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Default.Cancel, null)
                                }
                            }

                            Text("(${customItem?.list.orEmpty().size})")

                            var showMenu by remember { mutableStateOf(false) }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_list)) },
                                    onClick = {
                                        showMenu = false
                                        pickDocumentLauncher.launch("${customItem?.item?.name}.json")
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_import_list)) },
                                    onClick = {
                                        showMenu = false
                                        showAdd = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.remove_items)) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteModal = true
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = Alizarin//MaterialTheme.colorScheme.error,
                                    ),
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_list_title)) },
                                    onClick = {
                                        showMenu = false
                                        deleteList = true
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer)
                                )
                            }

                            AnimatedVisibility(!viewModel.searchBarActive) {
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
                            }
                            AnimatedVisibility(!viewModel.searchBarActive) {
                                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) {
                        itemsIndexed(items = viewModel.searchItems) { index, item ->
                            ListItem(
                                headlineContent = { Text(item.title) },
                                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                modifier = Modifier
                                    .clickable {
                                        viewModel.setQuery(item.title)
                                        viewModel.searchBarActive = false
                                    }
                                    .animateItemPlacement()
                            )
                            if (index != 0) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        ) { padding ->
            BoxWithConstraints {
                LazyVerticalGrid(
                    columns = adaptiveGridCell(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .haze(
                            topBounds(padding),
                            backgroundColor = MaterialTheme.colorScheme.surface
                        )
                ) {
                    items(
                        items = viewModel.items,
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
            onDismissRequest = { showBottomSheet = false }
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss
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
                            modifier = Modifier.border(
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

class OtakuListStuffViewModel(
    private val listDao: ListDao,
) : ViewModel() {
    val customLists = mutableStateListOf<CustomList>()

    var customItem: CustomList? by mutableStateOf(null)

    var searchBarActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    val listBySource by derivedStateOf {
        customItem?.list
            .orEmpty()
            .groupBy { it.source }
    }

    val searchItems by derivedStateOf {
        customItem?.list
            .orEmpty()
            .distinctBy { it.title }
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val items by derivedStateOf {
        customItem?.list
            .orEmpty()
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.title }
            .entries
            .toList()
    }

    init {
        listDao.getAllLists()
            .onEach {
                customLists.clear()
                customLists.addAll(it)
            }
            .launchIn(viewModelScope)

        /*snapshotFlow {  }

        uuid?.let(listDao::getCustomListItemFlow)
            ?.onEach { customItem = it }
            ?.launchIn(viewModelScope)*/
    }

    fun removeItem(item: CustomListInfo) {
        viewModelScope.launch {
            listDao.removeItem(item)
            viewModelScope.launch { customItem?.item?.let { listDao.updateFullList(it) } }
        }
    }

    suspend fun removeItems(items: List<CustomListInfo>): Result<Boolean> = runCatching {
        items.forEach { item -> listDao.removeItem(item) }
        customItem?.item?.let { listDao.updateFullList(it) }
        true
    }

    fun rename(newName: String) {
        viewModelScope.launch { customItem?.item?.copy(name = newName)?.let { listDao.updateFullList(it) } }
    }

    fun deleteAll() {
        viewModelScope.launch { customItem?.let { item -> listDao.removeList(item) } }
    }

    fun setQuery(query: String) {
        searchQuery = query
    }

    fun writeToFile(document: Uri, context: Context) {
        runCatching {
            viewModelScope.launch {
                try {
                    context.contentResolver.openFileDescriptor(document, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { f ->
                            f.write(customItem?.toJson()?.toByteArray())
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
            .onSuccess { println("Written!") }
            .onFailure { it.printStackTrace() }
    }
}
