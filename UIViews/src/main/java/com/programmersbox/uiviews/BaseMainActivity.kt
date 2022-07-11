package com.programmersbox.uiviews

import android.app.assist.AssistContent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.utils.*
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import com.programmersbox.uiviews.utils.Screen as SScreen

abstract class BaseMainActivity : AppCompatActivity() {

    protected val disposable = CompositeDisposable()

    //protected var currentNavController: LiveData<NavController>? = null

    protected val genericInfo: GenericInfo by inject()
    private val logo: MainLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    private val dao by lazy { ItemDatabase.getInstance(this).itemDao() }
    private val historyDao by lazy { HistoryDatabase.getInstance(this).historyDao() }
    protected lateinit var navController: NavHostController

    protected fun isNavInitialized() = ::navController.isInitialized

    protected abstract fun onCreate()

    companion object {
        var showNavBar by mutableStateOf(true)
    }

    @OptIn(
        ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        genericInfo.toSource(currentService.orEmpty())?.let { sourcePublish.onNext(it) }

        when (runBlocking { themeSetting.first() }) {
            "System" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "Light" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> null
        }?.let(AppCompatDelegate::setDefaultNightMode)

        onCreate()

        Single.create<AppUpdate.AppUpdates> { AppUpdate.getUpdate()?.let(it::onSuccess) ?: it.onError(Throwable("Something went wrong")) }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnError {}
            .subscribe(appUpdateCheck::onNext)
            .addTo(disposable)

        setContent {
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            navController = rememberAnimatedNavController(bottomSheetNavigator)

            if (showNavBar) {
                showSystemBars()
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } else {
                hideSystemBars()
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }

            OtakuMaterialTheme(navController, genericInfo) {
                val showAllItem by showAll.collectAsState(false)

                com.google.accompanist.navigation.material.ModalBottomSheetLayout(
                    bottomSheetNavigator,
                    sheetBackgroundColor = MaterialTheme.colorScheme.surface,
                    sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                ) {
                    Scaffold(
                        bottomBar = {
                            Column {
                                genericInfo.BottomBarAdditions()
                                AnimatedVisibility(
                                    visible = showNavBar,
                                    enter = slideInVertically { it / 2 },
                                    exit = slideOutVertically { it / 2 }
                                ) {
                                    NavigationBar {
                                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                                        val currentDestination = navBackStackEntry?.destination
                                        SScreen.bottomItems.forEach { screen ->
                                            if (screen !is SScreen.AllScreen || showAllItem) {
                                                NavigationBarItem(
                                                    icon = {
                                                        BadgedBox(
                                                            badge = {
                                                                if (screen is SScreen.SettingsScreen) {
                                                                    val updateAvailable = updateCheck()
                                                                    if (updateAvailable) {
                                                                        Badge { Text("") }
                                                                    }
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                when (screen) {
                                                                    SScreen.RecentScreen -> Icons.Default.History
                                                                    SScreen.AllScreen -> Icons.Default.BrowseGallery
                                                                    SScreen.SettingsScreen -> Icons.Default.Settings
                                                                    else -> Icons.Default.BrokenImage
                                                                },
                                                                null
                                                            )
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            when (screen) {
                                                                SScreen.AllScreen -> stringResource(R.string.all)
                                                                SScreen.RecentScreen -> stringResource(R.string.recent)
                                                                SScreen.SettingsScreen -> stringResource(R.string.settings)
                                                                else -> ""
                                                            }
                                                        )
                                                    },
                                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                                    onClick = {
                                                        navController.navigate(screen.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = SScreen.RecentScreen.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(
                                SScreen.RecentScreen.route
                            ) {
                                val context = LocalContext.current
                                RecentView(
                                    recentVm = viewModel { RecentViewModel(dao, context) },
                                    info = genericInfo,
                                    navController = navController,
                                    logo = logo
                                )
                            }

                            composable(
                                SScreen.AllScreen.route
                            ) {
                                val context = LocalContext.current
                                AllView(
                                    allVm = viewModel { AllViewModel(dao, context) },
                                    info = genericInfo,
                                    navController = navController,
                                    logo = logo
                                )
                            }

                            composable(
                                SScreen.SettingsScreen.route,
                                //enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Start) },
                                //exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.End) },
                            ) {
                                SettingScreen(
                                    navController = navController,
                                    logo = logo,
                                    genericInfo = genericInfo,
                                    activity = this@BaseMainActivity,
                                    notificationClick = { navController.navigate(SScreen.NotificationScreen.route) { launchSingleTop = true } },
                                    globalSearchClick = { navController.navigate(SScreen.GlobalSearchScreen.route) { launchSingleTop = true } },
                                    favoritesClick = { navController.navigate(SScreen.FavoriteScreen.route) { launchSingleTop = true } },
                                    historyClick = { navController.navigate(SScreen.HistoryScreen.route) { launchSingleTop = true } },
                                    usedLibraryClick = { navController.navigate(SScreen.AboutScreen.route) { launchSingleTop = true } }
                                )
                            }

                            composable(
                                SScreen.NotificationScreen.route,
                                deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + SScreen.NotificationScreen.route }),
                                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) }
                            ) {
                                NotificationsScreen(
                                    navController = navController,
                                    genericInfo = genericInfo,
                                    db = dao,
                                    notificationManager = LocalContext.current.notificationManager,
                                    logo = logo,
                                    notificationLogo = notificationLogo,
                                    fragmentManager = supportFragmentManager
                                )
                            }

                            composable(
                                SScreen.GlobalSearchScreen.route + "?searchFor={searchFor}",
                                arguments = listOf(navArgument("searchFor") { nullable = true }),
                                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) }
                            ) { GlobalSearchView(mainLogo = logo, notificationLogo = notificationLogo) }

                            composable(
                                SScreen.FavoriteScreen.route,
                                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) }
                            ) { FavoriteUi(logo) }

                            composable(
                                SScreen.HistoryScreen.route,
                                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) }
                            ) { HistoryUi(dao = historyDao, logo = logo) }

                            composable(
                                SScreen.AboutScreen.route,
                                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) }
                            ) { AboutLibrariesScreen(logo) }

                            composable(
                                SScreen.DetailsScreen.route + "/{model}",
                                deepLinks = listOf(navDeepLink {
                                    uriPattern = genericInfo.deepLinkUri + "${SScreen.DetailsScreen.route}/{model}"
                                }),
                                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) }
                            ) {
                                DetailsScreen(
                                    navController = navController,
                                    genericInfo = genericInfo,
                                    logo = notificationLogo,
                                    dao = dao,
                                    historyDao = historyDao,
                                    windowSize = rememberWindowSizeClass()
                                )
                            }

                            with(genericInfo) { navSetup() }
                        }
                    }
                }
            }
        }

        /*intent.data?.let {
            if (URLUtil.isValidUrl(it.toString())) {
                currentService?.let { it1 ->
                    genericInfo.toSource(it1)?.getSourceByUrl(it.toString())
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeBy { it2 ->
                            currentNavController?.value?.navigate(RecentFragmentDirections.actionRecentFragment2ToDetailsFragment2(it2))
                        }
                        ?.addTo(disposable)
                }
            }
        }*/

    }

    @Composable
    fun updateCheck(): Boolean {
        val appUpdate by appUpdateCheck.subscribeAsState(null)

        return AppUpdate.checkForUpdate(
            remember { packageManager.getPackageInfo(packageName, 0)?.versionName.orEmpty() },
            appUpdate?.update_real_version.orEmpty()
        )
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    class AssetParamType(val info: GenericInfo) : NavType<ItemModel>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): ItemModel? {
            return bundle.getSerializable(key) as? ItemModel
        }

        override fun parseValue(value: String): ItemModel {
            return value.fromJson<ItemModel>(ApiService::class.java to ApiServiceDeserializer(info))!!
        }

        override fun put(bundle: Bundle, key: String, value: ItemModel) {
            bundle.putSerializable(key, value)
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = Uri.parse(currentDetailsUrl)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        //setupBottomNavBar()
    }

    enum class Screen(val route: SScreen) { RECENT(SScreen.RecentScreen), ALL(SScreen.AllScreen), SETTINGS(SScreen.SettingsScreen) }

    fun goToScreen(screen: Screen) {
        if (::navController.isInitialized) {
            navController.navigate(screen.route.route)
        }
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (isNavInitialized()) {
            navController.handleDeepLink(intent)
            newIntent(intent)
        }
    }

    protected fun newIntent(intent: Intent?) = Unit

}