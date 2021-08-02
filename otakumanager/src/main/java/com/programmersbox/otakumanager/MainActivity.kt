package com.programmersbox.otakumanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.otakumanager.ui.theme.OtakuWorldTheme
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CoverCard
import com.programmersbox.uiviews.utils.CustomChip
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import com.programmersbox.anime_sources.Sources as ASources
import com.programmersbox.manga_sources.Sources as MSources
import com.programmersbox.novel_sources.Sources as NSources

class MainActivity : ComponentActivity() {

    private val animeFire = FirebaseDb2("favoriteShows", "episodesWatched", "animeworld", "showUrl", "numEpisodes")
    private val mangaFire = FirebaseDb2("favoriteManga", "chaptersRead", "mangaworld", "mangaUrl", "chapterCount")
    private val novelFire = FirebaseDb2("favoriteNovels", "novelsChaptersRead", "novelworld", "novelUrl", "novelNumChapters")

    private val animeListener = animeFire.FirebaseListener()
    private val mangaListener = mangaFire.FirebaseListener()
    private val novelListener = novelFire.FirebaseListener()

    private val genericInfo by inject<GenericInfo>()

    private val allSources: List<ApiService> by lazy {
        genericInfo
            .sourceList()
            .sortedBy { it.serviceName }
    }

    private val animeSources = ASources.values().map { it.serviceName }
    private val mangaSources = MSources.values().map { it.serviceName }
    private val novelSources = NSources.values().map { it.serviceName }

    sealed class Screen(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
        object Favorites : Screen("mainUi", R.string.viewFavoritesMenu, Icons.Filled.Favorite)
        object Settings : Screen("settings", R.string.settings, Icons.Filled.Menu)
        object Details : Screen("details?info={item}", R.string.app_name, Icons.Filled.Book)
    }

    private val disposable = CompositeDisposable()

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Single.create<AppUpdate.AppUpdates> {
            AppUpdate.getUpdate()?.let { d -> it.onSuccess(d) } ?: it.onError(Exception("Something went wrong"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { }
            .subscribe(appUpdateCheck::onNext)
            .addTo(disposable)

        val screenList = listOf(
            Screen.Favorites,
            Screen.Settings
        )

        setContent {
            MdcTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val navController = rememberNavController()

                    Scaffold(
                        bottomBar = {
                            BottomNavigation {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination
                                screenList.forEach { screen ->
                                    BottomNavigationItem(
                                        icon = { Icon(screen.icon, contentDescription = null) },
                                        label = { Text(stringResource(id = screen.resourceId)) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                // Pop up to the start destination of the graph to
                                                // avoid building up a large stack of destinations
                                                // on the back stack as users select items
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                // Avoid multiple copies of the same destination when
                                                // reselecting the same item
                                                launchSingleTop = true
                                                // Restore state when reselecting a previously selected item
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { p ->

                        NavHost(navController = navController, startDestination = Screen.Favorites.route, modifier = Modifier.padding(p)) {
                            composable(Screen.Favorites.route) { MainUi(navController) }
                            composable(Screen.Settings.route) { OtakuSettings(this@MainActivity, genericInfo) }
                            composable(
                                Screen.Details.route,
                                arguments = listOf(navArgument("item") { type = NavType.StringType })
                            ) {
                                val i = it.arguments?.getString("item")?.fromJson<DbModel>()
                                    ?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                DetailsScreen(
                                    info = i!!,
                                    logoId = when (i.source.serviceName) {
                                        in animeSources -> R.drawable.animeworld_logo
                                        in mangaSources -> R.drawable.mangaworld_logo
                                        in novelSources -> R.drawable.novelworld_logo
                                        else -> R.drawable.otakumanager_logo
                                    },
                                    firebase = when (i.source.serviceName) {
                                        in animeSources -> animeFire
                                        in mangaSources -> mangaFire
                                        in novelSources -> novelFire
                                        else -> null
                                    },
                                    itemListener = when (i.source.serviceName) {
                                        in animeSources -> animeFire
                                        in mangaSources -> mangaFire
                                        in novelSources -> novelFire
                                        else -> null
                                    }?.copy()?.FirebaseListener(),
                                    chapterListener = when (i.source.serviceName) {
                                        in animeSources -> animeFire
                                        in mangaSources -> mangaFire
                                        in novelSources -> novelFire
                                        else -> null
                                    }?.copy()?.FirebaseListener(),
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    fun MainUi(navController: NavController) {

        val systemUi = rememberSystemUiController()
        systemUi.setStatusBarColor(animateColorAsState(MaterialTheme.colors.primaryVariant).value)

        val focusManager = LocalFocusManager.current

        var searchText by rememberSaveable { mutableStateOf("") }

        val favorites by Flowable.combineLatest(
            animeListener.getAllShowsFlowable(),
            mangaListener.getAllShowsFlowable(),
            novelListener.getAllShowsFlowable()
        ) { a, m, n -> (a + m + n).sortedBy { it.title } }
            .map { it.filter { it.source in allSources.map(ApiService::serviceName) } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = emptyList())

        val selectedSources = remember { allSources.map { it.serviceName }.toMutableStateList() }

        val showing = favorites
            .filter { it.title.contains(searchText, true) && it.source in selectedSources }

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier.padding(5.dp)
                ) {

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text(resources.getQuantityString(R.plurals.numFavorites, showing.size, showing.size)) },
                        trailingIcon = { IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Cancel, null) } },
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    Row(
                        modifier = Modifier.padding(top = 5.dp)
                    ) {

                        LazyRow {

                            item {
                                CustomChip(
                                    "ALL",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                selectedSources.clear()
                                                selectedSources.addAll(allSources.map { it.serviceName })
                                            },
                                            onLongClick = {
                                                selectedSources.clear()
                                            }
                                        ),
                                    backgroundColor = MaterialTheme.colors.primary,
                                    textColor = MaterialTheme.colors.onPrimary
                                )
                            }

                            items(
                                (allSources.map { it.serviceName } + showing.map { it.source })
                                    .groupBy { it }
                                    .toList()
                                    .sortedBy { it.first }
                            ) {

                                CustomChip(
                                    "${it.first}: ${it.second.size - 1}",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (it.first in selectedSources) selectedSources.remove(it.first)
                                                else selectedSources.add(it.first)
                                            },
                                            onLongClick = {
                                                selectedSources.clear()
                                                selectedSources.add(it.first)
                                            }
                                        ),
                                    backgroundColor = animateColorAsState(if (it.first in selectedSources) MaterialTheme.colors.primary else MaterialTheme.colors.surface).value,
                                    textColor = animateColorAsState(if (it.first in selectedSources) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface).value
                                )
                            }
                        }

                    }
                }
            },
            bottomBar = {
                //TODO: if not logged in, show empty state for needing to login
            }
        ) {

            LazyVerticalGrid(
                cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                contentPadding = it,
                state = rememberLazyListState()
            ) {
                items(
                    showing
                        .groupBy(DbModel::title)
                        .entries
                        .toTypedArray()
                ) { info ->
                    CoverCard(
                        imageUrl = info.value.random().imageUrl,
                        name = info.key,
                        placeHolder = when (info.value.first().source) {
                            in animeSources -> R.drawable.animeworld_logo
                            in mangaSources -> R.drawable.mangaworld_logo
                            in novelSources -> R.drawable.novelworld_logo
                            else -> R.drawable.otakumanager_logo
                        }
                    ) {

                        if (info.value.size == 1) {
                            val item = info.value.firstOrNull()
                            navController.navigate(Screen.Details.route.replace("{item}", item.toJson()))
                        } else {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(R.string.chooseASource)
                                .setItems(info.value.map { "${it.source} - ${it.title}" }.toTypedArray()) { d, i ->
                                    val item = info.value[i]
                                    d.dismiss()
                                    navController.navigate(Screen.Details.route.replace("{item}", item.toJson()))
                                }
                                .show()
                        }

                    }
                }
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        animeListener.unregister()
        mangaListener.unregister()
        novelListener.unregister()
        disposable.dispose()
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OtakuWorldTheme {
        Greeting("Android")
    }
}