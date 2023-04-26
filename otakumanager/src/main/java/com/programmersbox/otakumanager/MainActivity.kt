package com.programmersbox.otakumanager

//import com.programmersbox.sharedutils.appUpdateCheck
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.otakumanager.ui.theme.OtakuWorldTheme
import com.programmersbox.sharedutils.FirebaseAuthentication
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CoverCard
import com.programmersbox.uiviews.utils.components.CustomChip2
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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
        object Settings : Screen("settings", R.string.settings, Icons.Filled.Settings)
        object Details : Screen("details?info={item}", R.string.app_name, Icons.Filled.Book)
    }

    private val disposable = CompositeDisposable()

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*Single.create<AppUpdate.AppUpdates> {
            AppUpdate.getUpdate()?.let { d -> it.onSuccess(d) } ?: it.onError(Exception("Something went wrong"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { }
            .subscribe(appUpdateCheck::onNext)
            .addTo(disposable)*/

        val screenList = listOf(
            Screen.Favorites,
            Screen.Settings
        )

        setContent {
            MdcTheme {

                val favorites by Flowable.combineLatest(
                    animeListener.getAllShowsFlowable(),
                    mangaListener.getAllShowsFlowable(),
                    novelListener.getAllShowsFlowable()
                ) { a, m, n -> (a + m + n).sortedBy { it.title } }
                    .map { it.filter { it.source in allSources.map(ApiService::serviceName) } }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAsState(initial = emptyList())

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
                            composable(Screen.Favorites.route) { MainUi(navController, favorites) }
                            composable(Screen.Settings.route) { OtakuSettings(this@MainActivity, genericInfo) }
                            composable(
                                Screen.Details.route,
                                //arguments = listOf(navArgument("item") { type = NavType.StringType })
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
    fun MainUi(navController: NavController, favorites: List<DbModel>) {

        val systemUi = rememberSystemUiController()
        systemUi.setStatusBarColor(animateColorAsState(MaterialTheme.colors.primaryVariant).value)

        val focusManager = LocalFocusManager.current

        var searchText by rememberSaveable { mutableStateOf("") }

        val selectedSources = remember { allSources.map { it.serviceName }.toMutableStateList() }

        val showing = favorites
            .filter { it.title.contains(searchText, true) && it.source in selectedSources }

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text(resources.getQuantityString(R.plurals.numFavorites, showing.size, showing.size)) },
                        trailingIcon = { IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Cancel, null) } },
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp)
                    ) {

                        LazyRow {

                            item {
                                CustomChip2(
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

                                CustomChip2(
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
            }
        ) {

            when {
                //FirebaseAuthentication.currentUser == null -> NotLoggedInState(it)
                showing.isEmpty() -> EmptyState(navController = navController, paddingValues = it)
                else -> {

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                        contentPadding = it,
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
        }

    }

    @Composable
    private fun NotLoggedInState(paddingValues: PaddingValues) {
        InvalidState(R.string.login_info, R.string.login_button_text, paddingValues) { FirebaseAuthentication.signIn(this@MainActivity) }
    }

    @Composable
    private fun EmptyState(navController: NavController, paddingValues: PaddingValues) {
        InvalidState(R.string.no_favorites, R.string.goto_app_for_fav, paddingValues) { navController.navigate(Screen.Settings.route) }
    }

    @Composable
    private fun InvalidState(bodyText: Int, buttonText: Int, paddingValues: PaddingValues, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(4.dp)
            ) {

                Column(modifier = Modifier) {

                    Text(
                        text = stringResource(id = R.string.get_started),
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(bodyText),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(buttonText),
                            style = MaterialTheme.typography.button
                        )
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