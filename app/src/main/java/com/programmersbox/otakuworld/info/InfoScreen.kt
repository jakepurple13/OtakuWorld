package com.programmersbox.otakuworld.info

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.otakuworld.App
import com.programmersbox.otakuworld.Navigation2
import com.programmersbox.otakuworld.ShareViaQrCode
import com.programmersbox.otakuworld.TopLevelBackStack
import com.programmersbox.otakuworld.optionsKmpSheet
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(viewModel: InfoViewModel = koinViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.checkForApps()
    }

    var state by remember(viewModel.hasApps) { mutableIntStateOf(0) }

    val tabsState by remember {
        derivedStateOf {
            listOfNotNull(
                OtakuItemState(
                    "MangaWorld",
                    viewModel.mangaWorld
                ).takeIf { viewModel.hasApps.hasMangaWorld != null },
                OtakuItemState(
                    "AnimeWorld",
                    viewModel.animeWorld
                ).takeIf { viewModel.hasApps.hasAnimeWorld != null },
                OtakuItemState(
                    "NovelWorld",
                    viewModel.novelWorld
                ).takeIf { viewModel.hasApps.hasNovelWorld != null },
            )
        }
    }

    val scrollState = rememberScrollState()

    val pagerState = rememberPagerState { tabsState.size }

    LaunchedEffect(pagerState.currentPage) {
        state = pagerState.currentPage
        scrollState.animateScrollTo(pagerState.currentPage)
    }

    LaunchedEffect(scrollState, state) {
        pagerState.animateScrollToPage(state)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OtakuWorld") },
                actions = {
                    IconButton(
                        onClick = viewModel::checkForApps
                    ) { Icon(Icons.Default.Refresh, null) }

                    IconButton(
                        onClick = {}
                    ) { Icon(Icons.Default.Settings, null) }
                }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding)
        ) {
            if (viewModel.hasApps.let { it.hasMangaWorld != null || it.hasAnimeWorld != null || it.hasNovelWorld != null }) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = state,
                    scrollState = scrollState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabsState.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = { Text(text = title.appName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }

                val stateHolder = rememberSaveableStateHolder()

                HorizontalPager(
                    pagerState,
                    //modifier = Modifier.fillMaxSize()
                ) {
                    stateHolder.SaveableStateProvider(it) {
                        OtakuItemScreen(viewModel, tabsState[it])
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("No Apps Found")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen2(
    viewModel: InfoViewModel = koinViewModel(),
) {
    val backStack = remember { TopLevelBackStack<NavKey>(SelectionScreen(App.MangaWorld)) }

    LaunchedEffect(Unit) {
        viewModel.checkForApps()
    }

    //TODO: Need to make sure this changes on backstack changes
    var state by remember(viewModel.hasApps) { mutableIntStateOf(0) }

    val tabsState by remember {
        derivedStateOf {
            listOfNotNull(
                OtakuItemState(
                    "MangaWorld",
                    viewModel.mangaWorld
                ),
                OtakuItemState(
                    "AnimeWorld",
                    viewModel.animeWorld
                ),
                OtakuItemState(
                    "NovelWorld",
                    viewModel.novelWorld
                ),
            )
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { backStack.topLevelKey }
            .collect {
                println(it)
                println(state)

                state = when (it) {
                    is SelectionScreen -> it.app
                    is FavScreen -> it.app
                    is ListScreen -> it.app
                    else -> App.MangaWorld
                }
                    .ordinal
                    .also { println(it) }

                println(state)
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("OtakuWorld") },
                    actions = {
                        IconButton(
                            onClick = viewModel::checkForApps
                        ) { Icon(Icons.Default.Refresh, null) }

                        IconButton(
                            onClick = {}
                        ) { Icon(Icons.Default.Settings, null) }
                    }
                )

                if (viewModel.hasApps.let { it.hasMangaWorld != null || it.hasAnimeWorld != null || it.hasNovelWorld != null }) {
                    PrimaryTabRow(
                        selectedTabIndex = state,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabsState.forEachIndexed { index, title ->
                            Tab(
                                selected = state == index,
                                onClick = {
                                    state = index
                                    backStack.addTopLevel(
                                        when (index) {
                                            0 -> SelectionScreen(App.MangaWorld)
                                            1 -> SelectionScreen(App.AnimeWorld)
                                            2 -> SelectionScreen(App.NovelWorld)
                                            else -> SelectionScreen(App.MangaWorld)
                                        }
                                    )
                                },
                                text = { Text(text = title.appName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                icon = {
                                    when (index) {
                                        0 -> viewModel.hasApps.hasMangaWorld
                                        1 -> viewModel.hasApps.hasAnimeWorld
                                        2 -> viewModel.hasApps.hasNovelWorld
                                        else -> null
                                    }?.let {
                                        Image(
                                            painter = rememberDrawablePainter(it.drawable),
                                            contentDescription = null
                                        )
                                    } ?: Icon(Icons.Default.NotInterested, null)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding)
        ) {
            Navigation2(
                backStack = backStack.backStack,
                onBack = { backStack.removeLast() },
            ) {
                entry<SelectionScreen> {
                    SelectionScreen(
                        item = when (it.app) {
                            App.MangaWorld -> tabsState[0]
                            App.AnimeWorld -> tabsState[1]
                            App.NovelWorld -> tabsState[2]
                        },
                        onShowingType = { showing ->
                            backStack.add(
                                when (showing) {
                                    ShowingType.Selection -> SelectionScreen(it.app)
                                    ShowingType.Favorites -> FavScreen(it.app)
                                    ShowingType.Lists -> ListScreen(it.app)
                                }
                            )
                        },
                    )
                }

                entry<FavScreen> {
                    FavoritesScreen(
                        item = when (it.app) {
                            App.MangaWorld -> tabsState[0]
                            App.AnimeWorld -> tabsState[1]
                            App.NovelWorld -> tabsState[2]
                        },
                        onBack = {}
                    )
                }

                entry<ListScreen> {
                    ListsScreen(
                        item = when (it.app) {
                            App.MangaWorld -> tabsState[0]
                            App.AnimeWorld -> tabsState[1]
                            App.NovelWorld -> tabsState[2]
                        },
                        onBack = {}
                    )
                }
            }
        }
    }
}

@Serializable
data class SelectionScreen(val app: App) : NavKey

@Serializable
data class FavScreen(val app: App) : NavKey

@Serializable
data class ListScreen(val app: App) : NavKey

@Composable
private fun OtakuItemScreen(
    viewModel: InfoViewModel,
    item: OtakuItemState,
) {
    var hasFavoritePermission by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasFavoritePermission = it.all { it.value } }

    SideEffect { launcher.launch(arrayOf(item.otakuItem.favoritePermission, item.otakuItem.listsPermission)) }

    Crossfade(hasFavoritePermission) { target ->
        if (target) {
            /*
            //TODO: This all will go into a settings screen
                    val context = LocalContext.current

                    Button(
                        onClick = {
                            AccountManager.get(context)
                                .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                                .forEach { account ->
                                    println(account)
                                    ContentResolver.setIsSyncable(
                                        account,
                                        item.otakuItem.favoritesUri,
                                        1
                                    )
                                    ContentResolver.setIsSyncable(
                                        account,
                                        item.otakuItem.listsUri,
                                        1
                                    )
                                    ContentResolver.setSyncAutomatically(
                                        account,
                                        item.otakuItem.favoritesUri,
                                        true
                                    )
                                    ContentResolver.requestSync(
                                        SyncRequest.Builder()
                                            .setDisallowMetered(true)
                                            .setSyncAdapter(
                                                account,
                                                item.otakuItem.favoritesUri
                                            )
                                            .setExtras(
                                                bundleOf(
                                                    "type" to item.otakuItem.app.name
                                                )
                                            )
                                            .syncPeriodic(
                                                1.days.inWholeSeconds,
                                                1.hours.inWholeSeconds
                                            )
                                            .build()
                                    )

                                    ContentResolver.setSyncAutomatically(
                                        account,
                                        item.otakuItem.listsUri,
                                        true
                                    )
                                    ContentResolver.requestSync(
                                        SyncRequest.Builder()
                                            .setDisallowMetered(true)
                                            .setSyncAdapter(
                                                account,
                                                item.otakuItem.listsUri
                                            )
                                            .setExtras(
                                                bundleOf(
                                                    "type" to item.otakuItem.app.name
                                                )
                                            )
                                            .syncPeriodic(
                                                1.days.inWholeSeconds,
                                                1.hours.inWholeSeconds
                                            )
                                            .build()
                                    )
                                }
                        },
                    ) { Text("Setup Syncs") }
             */

            //ShowingSelectionScreen(item)
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Button(
                    onClick = {
                        println(item.otakuItem.favoritePermission)
                        launcher.launch(arrayOf(item.otakuItem.favoritePermission, item.otakuItem.listsPermission))
                    },
                ) { Text("Allow Access to favorites ${item.appName}") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionGetter(
    permission: String,
    item: OtakuItemState,
    type: String,
    onPermissionGranted: @Composable () -> Unit,
) {
    var hasFavoritePermission by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasFavoritePermission = it }

    Crossfade(hasFavoritePermission) { target ->
        if (target) {
            onPermissionGranted()
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { launcher.launch(permission) },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Allow Access to $type ${item.appName}") }
            }
        }
    }
}

@Composable
private fun ShowingSelectionScreen(
    item: OtakuItemState,
) {
    var selectionType by rememberSaveable { mutableStateOf(ShowingType.Selection) }

    Crossfade(selectionType) { target ->
        when (target) {
            ShowingType.Selection -> SelectionScreen(
                item = item,
                onShowingType = { selectionType = it }
            )

            ShowingType.Favorites -> FavoritesScreen(
                item = item,
                onBack = { selectionType = ShowingType.Selection }
            )

            ShowingType.Lists -> ListsScreen(
                item = item,
                onBack = { selectionType = ShowingType.Selection }
            )
        }
    }
}

@Composable
private fun SelectionScreen(
    item: OtakuItemState,
    onShowingType: (ShowingType) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        PermissionGetter(
            permission = item.otakuItem.listsPermission,
            item = item,
            type = "lists"
        ) {
            Card(
                onClick = { onShowingType(ShowingType.Lists) }
            ) {
                ListItem(
                    headlineContent = { Text("Lists") },
                    trailingContent = { Text(item.otakuItem.list.size.toString()) },
                    supportingContent = { HorizontalDivider() },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

        PermissionGetter(
            permission = item.otakuItem.favoritePermission,
            item = item,
            type = "favorites"
        ) {
            Card(
                onClick = { onShowingType(ShowingType.Favorites) }
            ) {
                ListItem(
                    headlineContent = { Text("Favorites") },
                    trailingContent = { Text(item.otakuItem.favorites.size.toString()) },
                    supportingContent = { HorizontalDivider() },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListsScreen(
    item: OtakuItemState,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()

    val customListsShowing by remember {
        derivedStateOf {
            item
                .otakuItem
                .list
                .associateWith { mutableStateOf(false) }
        }
    }

    Scaffold(
        topBar = {
            DynamicSearchBar(
                textFieldState = textFieldState,
                onSearch = { },
                searchBarState = searchBarState,
                placeholder = { Text("Search") },
                leadingIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                windowInsets = WindowInsets(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                /*searchValues
                    .take(10)
                    .forEach {
                        ListItem(
                            headlineContent = { Text(it.title) },
                            modifier = Modifier.clickable {
                                textFieldState.setTextAndPlaceCursorAtEnd(it.title)
                                scope.launch { searchBarState.animateToCollapsed() }
                            }
                        )
                    }*/
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            //TODO: Bring OptionsSheet over to handle removing, toggling notifying, biometrics, etc
            // Maybe put OptionsSheet into its own module? Maybe a components module?
            item.otakuItem.list.forEach { list ->
                stickyHeader {
                    Card(
                        onClick = { customListsShowing[list]?.value = customListsShowing[list]?.value?.not() ?: false },
                    ) {
                        ListItem(
                            headlineContent = { Text(list.item.name) },
                            trailingContent = { Text(list.list.size.toString()) },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                if (customListsShowing[list]?.value == true) {
                    items(list.list) {
                        M3CoverCard(
                            imageUrl = it.imageUrl,
                            name = it.title,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesScreen(
    item: OtakuItemState,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()

    val searchValues by remember {
        derivedStateOf {
            item
                .otakuItem
                .favorites
                .distinctBy { it.title }
                .filter { it.title.contains(textFieldState.text, true) }
        }
    }

    val list by remember {
        derivedStateOf {
            item
                .otakuItem
                .favorites
                .groupBy { it.source }
                .mapValues { favorites ->
                    favorites.value.filter { it.title.contains(textFieldState.text, true) }
                }
        }
    }

    val showing by remember {
        derivedStateOf {
            list.mapValues { mutableStateOf(textFieldState.text.isNotEmpty()) }
        }
    }

    Scaffold(
        topBar = {
            DynamicSearchBar(
                textFieldState = textFieldState,
                onSearch = { },
                searchBarState = searchBarState,
                placeholder = { Text("Search") },
                leadingIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                windowInsets = WindowInsets(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                searchValues
                    .take(10)
                    .forEach {
                        ListItem(
                            headlineContent = { Text(it.title) },
                            modifier = Modifier.clickable {
                                textFieldState.setTextAndPlaceCursorAtEnd(it.title)
                                scope.launch { searchBarState.animateToCollapsed() }
                            }
                        )
                    }
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            list.forEach { (source, favorites) ->
                stickyHeader {
                    Card(
                        onClick = { showing[source]?.value = showing[source]?.value?.not() ?: false },
                    ) {
                        ListItem(
                            headlineContent = { Text(source) },
                            trailingContent = { Text(favorites.size.toString()) },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                if (showing[source]?.value == true) {
                    items(favorites) {
                        var favoritesInfo by favoritesSheet(
                            onRemoveClick = item.otakuItem::deleteFavorite,
                            onToggleNotifyClick = item.otakuItem::toggleNotify
                        )
                        M3CoverCard(
                            imageUrl = it.imageUrl,
                            name = it.title,
                            onClick = { favoritesInfo = it }
                        )
                    }
                }
            }
        }
    }
}

enum class ShowingType {
    Selection,
    Favorites,
    Lists
}

@Composable
fun M3CoverCard(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlideImage(
                imageModel = { imageUrl },
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    name,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

data class OtakuItemState(
    val appName: String,
    val otakuItem: OtakuItem,
)

object ComposableUtils {
    const val IMAGE_WIDTH_PX = 360
    const val IMAGE_HEIGHT_PX = 480
    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { IMAGE_WIDTH_PX.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { IMAGE_HEIGHT_PX.toDp() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun favoritesSheet(
    onRemoveClick: (DbModel) -> Unit,
    onToggleNotifyClick: (DbModel) -> Unit,
) = optionsKmpSheet {
    val dbModel = it.itemModel

    if (dbModel.shouldCheckForUpdate) {
        OptionsItem(
            title = "Don't check for update",
            onClick = { onToggleNotifyClick(dbModel) }
        )
    } else {
        OptionsItem(
            title = "Check for update",
            onClick = { onToggleNotifyClick(dbModel) }
        )
    }

    var showQr by remember { mutableStateOf(false) }
    if (showQr) {
        ShareViaQrCode(
            url = dbModel.url,
            title = dbModel.title,
            imageUrl = dbModel.imageUrl,
            apiService = dbModel.source,
            onClose = { showQr = false }
        )
    }

    OptionsItem(
        title = "Share via QR Code",
        onClick = { showQr = true }
    )

    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove from favorites") },
            text = { Text("Are you sure you want to remove this item from favorites?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveClick(dbModel)
                        showRemoveDialog = false
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    OptionsItem(
        title = "Remove from favorites",
        onClick = { showRemoveDialog = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState = rememberSearchBarState(),
    scrollBehavior: SearchBarScrollBehavior? = null,
    enabled: Boolean = true,
    isDocked: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = if (isDocked) SearchBarDefaults.dockedShape else SearchBarDefaults.inputFieldShape,
    colors: AppBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearch,
            enabled = enabled,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            interactionSource = interactionSource,
            colors = colors.searchBarColors.inputFieldColors
        )
    }

    AppBarWithSearch(
        state = searchBarState,
        inputField = inputField,
        colors = colors,
        shape = shape,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )

    if (isDocked) {
        ExpandedDockedSearchBar(
            inputField = inputField,
            state = searchBarState,
            content = content,
            colors = colors.searchBarColors,
            shape = shape,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = modifier,
        )
    } else {
        ExpandedFullScreenSearchBar(
            inputField = inputField,
            state = searchBarState,
            content = content,
            colors = colors.searchBarColors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = modifier,
        )
    }
}