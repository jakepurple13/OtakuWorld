package com.programmersbox.otakuworld.info

import android.accounts.AccountManager
import android.content.ContentResolver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TwoRowsTopAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.otakuworld.BuildConfig
import com.programmersbox.otakuworld.DbModel
import com.programmersbox.otakuworld.MultiprocessDataStoreHandler
import com.programmersbox.otakuworld.Navigation
import com.programmersbox.otakuworld.OtakuSettings
import com.programmersbox.otakuworld.ShareViaQrCode
import com.programmersbox.otakuworld.TopLevelBackStack
import com.programmersbox.otakuworld.optionsKmpSheet
import com.programmersbox.otakuworld.providers.App
import com.programmersbox.otakuworld.rememberBiometricPrompting
import com.programmersbox.otakuworld.repository.OtakuInfo
import com.programmersbox.otakuworld.syncadapters.FavoritesSyncAdapter
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InfoScreen(
    viewModel: InfoViewModel = koinViewModel(),
) {
    val backStack = remember {
        TopLevelBackStack<NavKey>(SelectionScreen(App.MangaWorld))
    }

    LaunchedEffect(Unit) {
        viewModel.checkForApps()
    }

    var state by remember { mutableIntStateOf(0) }

    val tabsState by remember {
        derivedStateOf {
            listOf(
                OtakuItemState(
                    appName = "MangaWorld",
                    otakuItem = viewModel.mangaWorld,
                    otakuInfo = viewModel.hasApps.hasMangaWorld,
                    app = App.MangaWorld
                ),
                OtakuItemState(
                    appName = "AnimeWorld",
                    otakuItem = viewModel.animeWorld,
                    otakuInfo = viewModel.hasApps.hasAnimeWorld,
                    app = App.AnimeWorld
                ),
                OtakuItemState(
                    appName = "NovelWorld",
                    otakuItem = viewModel.novelWorld,
                    otakuInfo = viewModel.hasApps.hasNovelWorld,
                    app = App.NovelWorld
                ),
            )
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { backStack.topLevelKey }
            .collect {
                state = when (it) {
                    is SelectionScreen -> it.app
                    is FavScreen -> it.app
                    is ListScreen -> it.app
                    else -> App.MangaWorld
                }.ordinal
            }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TwoRowsTopAppBar(
                title = { Text("OtakuWorld") },
                actions = {
                    IconButton(
                        onClick = viewModel::checkForApps,
                        shapes = IconButtonDefaults.shapes()
                    ) { Icon(Icons.Default.Refresh, null) }
                },
                subtitle = { expanded ->
                    if (expanded) {
                        PrimaryTabRow(
                            selectedTabIndex = state,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabsState.forEachIndexed { index, title ->
                                OtakuTab(
                                    state = state,
                                    onStateUpdate = { state = it },
                                    index = index,
                                    backStack = backStack,
                                    title = title
                                )
                            }
                        }
                    } else {
                        Text(
                            text = tabsState[state].appName,
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Navigation(
            backStack = backStack.backStack,
            onBack = { backStack.removeLast() },
            modifier = Modifier.padding(padding)
        ) {
            fun getApp(app: App) = when (app) {
                App.MangaWorld -> tabsState[0]
                App.AnimeWorld -> tabsState[1]
                App.NovelWorld -> tabsState[2]
            }

            entry<SelectionScreen> {
                SelectionScreen(
                    item = getApp(it.app),
                    onShowingType = { showing ->
                        backStack.add(
                            when (showing) {
                                ShowingType.Selection -> SelectionScreen(it.app)
                                ShowingType.Favorites -> FavScreen(it.app)
                                ShowingType.Lists -> ListScreen(it.app)
                                ShowingType.Incognito -> IncognitoScreen(it.app)
                            }
                        )
                    },
                )
            }

            entry<FavScreen> {
                FavoritesScreen(
                    item = getApp(it.app),
                    onBack = { backStack.removeLast() }
                )
            }

            entry<ListScreen> {
                ListsScreen(
                    item = getApp(it.app),
                    onBack = { backStack.removeLast() }
                )
            }

            entry<IncognitoScreen> {
                IncognitoScreen(
                    item = getApp(it.app),
                    onBack = { backStack.removeLast() }
                )
            }
        }
    }
}

@Composable
private fun OtakuTab(
    state: Int,
    onStateUpdate: (Int) -> Unit,
    index: Int,
    backStack: TopLevelBackStack<NavKey>,
    title: OtakuItemState,
) {
    Tab(
        selected = state == index,
        onClick = {
            onStateUpdate(index)
            backStack.addTopLevel(SelectionScreen(title.app))
        },
        text = { Text(text = title.appName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        icon = {
            title
                .otakuInfo
                ?.let {
                    Image(
                        painter = rememberDrawablePainter(it.drawable),
                        contentDescription = null
                    )
                } ?: Icon(Icons.Default.NotInterested, null)
        }
    )
}

@Serializable
data class SelectionScreen(val app: App) : NavKey

@Serializable
data class FavScreen(val app: App) : NavKey

@Serializable
data class ListScreen(val app: App) : NavKey

@Serializable
data class IncognitoScreen(val app: App) : NavKey

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionGetter(
    item: OtakuItemState,
    type: String,
    onPermissionRequest: () -> Unit,
    hasPermission: Boolean,
    onPermissionGranted: @Composable () -> Unit,
) {
    Crossfade(hasPermission) { target ->
        if (target) {
            onPermissionGranted()
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onPermissionRequest,
                    shapes = ButtonDefaults.shapes()
                ) { Text("Allow Access to $type ${item.appName}") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionScreen(
    item: OtakuItemState,
    onShowingType: (ShowingType) -> Unit,
) {
    Crossfade(item.otakuInfo) { target ->
        if (target != null) {
            var hasListPermission by rememberSaveable { mutableStateOf(false) }
            val listLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { hasListPermission = it }
            var hasFavoritePermission by rememberSaveable { mutableStateOf(false) }
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                hasFavoritePermission = it
                listLauncher.launch(item.otakuItem.listsPermission)
            }

            SideEffect { launcher.launch(item.otakuItem.favoritePermission) }

            val multiprocessDataStoreHandler by koinInject<MultiprocessDataStoreHandler>()
                .getFlow()
                .collectAsStateWithLifecycle(OtakuSettings())

            val format = remember { SimpleDateFormat.getDateTimeInstance() }

            val formattedFavoritesSync by remember {
                derivedStateOf {
                    format.format(
                        when (item.app) {
                            App.MangaWorld -> multiprocessDataStoreHandler.lastFavoritesSyncManga
                            App.AnimeWorld -> multiprocessDataStoreHandler.lastFavoritesSyncAnime
                            App.NovelWorld -> multiprocessDataStoreHandler.lastFavoritesSyncNovel
                        }
                    )
                }
            }

            val formattedListSync by remember {
                derivedStateOf {
                    format.format(
                        when (item.app) {
                            App.MangaWorld -> multiprocessDataStoreHandler.lastListsSyncManga
                            App.AnimeWorld -> multiprocessDataStoreHandler.lastListsSyncAnime
                            App.NovelWorld -> multiprocessDataStoreHandler.lastListsSyncNovel
                        }
                    )
                }
            }

            val formattedIncognitoSync by remember {
                derivedStateOf {
                    format.format(
                        when (item.app) {
                            App.MangaWorld -> multiprocessDataStoreHandler.lastIncognitoSyncManga
                            App.AnimeWorld -> multiprocessDataStoreHandler.lastIncognitoSyncAnime
                            App.NovelWorld -> multiprocessDataStoreHandler.lastIncognitoSyncNovel
                        }
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                PermissionGetter(
                    item = item,
                    type = "favorites",
                    onPermissionRequest = { launcher.launch(item.otakuItem.favoritePermission) },
                    hasPermission = hasFavoritePermission
                ) {
                    Card(
                        onClick = { onShowingType(ShowingType.Favorites) }
                    ) {
                        ListItem(
                            headlineContent = { Text("Favorites") },
                            trailingContent = { Text(item.otakuItem.favorites.size.toString()) },
                            overlineContent = { Text("Last Synced: $formattedFavoritesSync") },
                            supportingContent = { HorizontalDivider() },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                PermissionGetter(
                    item = item,
                    type = "lists",
                    onPermissionRequest = { listLauncher.launch(item.otakuItem.listsPermission) },
                    hasPermission = hasListPermission
                ) {
                    Card(
                        onClick = { onShowingType(ShowingType.Lists) }
                    ) {
                        ListItem(
                            headlineContent = { Text("Lists") },
                            trailingContent = { Text(item.otakuItem.list.size.toString()) },
                            overlineContent = { Text("Last Synced: $formattedListSync") },
                            supportingContent = { HorizontalDivider() },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                AnimatedVisibility(item.otakuItem.incognitoSources.isNotEmpty()) {
                    val biometric = rememberBiometricPrompting()

                    Card(
                        onClick = {
                            biometric.authenticate(
                                onAuthenticationSucceeded = { onShowingType(ShowingType.Incognito) },
                                onAuthenticationFailed = {},
                                title = "Security required to view",
                                subtitle = "Please authenticate to view incognito",
                                negativeButtonText = "Never Mind"
                            )
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text("Incognito") },
                            trailingContent = { Text(item.otakuItem.incognitoSources.size.toString()) },
                            overlineContent = { Text("Last Synced: $formattedIncognitoSync") },
                            supportingContent = { HorizontalDivider() },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                Column {
                    ListItem(
                        headlineContent = { Text(item.appName) },
                        trailingContent = { Text(item.otakuInfo?.version.orEmpty()) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                    HorizontalDivider()
                }

                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SelectionSettings(
                        item = item,
                        onDismiss = { showSettings = false }
                    )
                }

                Button(
                    onClick = { showSettings = true },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Settings") }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("No App Found")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionSettings(
    item: OtakuItemState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        val context = LocalContext.current

        Button(
            onClick = {
                AccountManager.get(context)
                    .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                    .forEach { account ->
                        ContentResolver.requestSync(
                            account,
                            item.otakuItem.listsUri,
                            bundleOf()
                        )
                    }
            },
            shapes = ButtonDefaults.shapes()
        ) { Text("Sync Lists Now") }

        FlowRow {
            Button(
                onClick = {
                    AccountManager.get(context)
                        .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                        .forEach { account ->
                            FavoritesSyncAdapter.syncToRemote(
                                item.otakuItem.favoritesUri,
                                account
                            )
                        }
                },
                shapes = ButtonDefaults.shapes()
            ) { Text("Sync Favorites to Cloud Now") }

            Button(
                onClick = {
                    AccountManager.get(context)
                        .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                        .forEach { account ->
                            FavoritesSyncAdapter.syncToLocal(
                                item.otakuItem.favoritesUri,
                                account
                            )
                        }
                },
                shapes = ButtonDefaults.shapes()
            ) { Text("Sync Favorites to Local Now") }

            Button(
                onClick = {
                    AccountManager.get(context)
                        .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                        .forEach { account ->
                            FavoritesSyncAdapter.sync(
                                item.otakuItem.favoritesUri,
                                account
                            )
                        }
                },
                shapes = ButtonDefaults.shapes()
            ) { Text("Sync Favorites Now") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListsScreen(
    item: OtakuItemState,
    onBack: () -> Unit,
) {
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
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IncognitoScreen(
    item: OtakuItemState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incognito Sources") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes()
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                windowInsets = WindowInsets(0.dp),
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(item.otakuItem.incognitoSources) {
                Card(
                    onClick = { },
                ) {
                    ListItem(
                        headlineContent = { Text(it.name) },
                        supportingContent = { Text(it.source) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FavoritesScreen(
    item: OtakuItemState,
    onBack: () -> Unit,
) {
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
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes()
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                windowInsets = WindowInsets(0.dp),
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent
                ),
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
    Lists,
    Incognito
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
        modifier = modifier.size(
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
    val otakuInfo: OtakuInfo?,
    val app: App,
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
    actions: @Composable (RowScope.() -> Unit)? = null,
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
        navigationIcon = leadingIcon,
        actions = actions,
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