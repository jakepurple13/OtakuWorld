package com.programmersbox.uiviews

import android.app.assist.AssistContent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import com.programmersbox.uiviews.utils.Screen as SScreen

abstract class BaseMainActivity : AppCompatActivity() {

    protected val disposable = CompositeDisposable()

    protected var currentNavController: LiveData<NavController>? = null

    protected val genericInfo: GenericInfo by inject()
    private val logo: MainLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    private val dao by lazy { ItemDatabase.getInstance(this).itemDao() }
    private val historyDao by lazy { HistoryDatabase.getInstance(this).historyDao() }

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

        setContent {
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navController = rememberAnimatedNavController(bottomSheetNavigator)

            if (showNavBar) {
                showSystemBars()
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } else {
                hideSystemBars()
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }

            OtakuMaterialTheme(navController, genericInfo) {
                val showAllItem by showAll.collectAsState(false)

                com.google.accompanist.navigation.material.ModalBottomSheetLayout(bottomSheetNavigator) {
                    androidx.compose.material3.Scaffold(
                        bottomBar = {
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
                                                    androidx.compose.material3.Icon(
                                                        when (screen) {
                                                            SScreen.RecentScreen -> Icons.Default.Favorite
                                                            SScreen.AllScreen -> Icons.Default.Settings
                                                            SScreen.SettingsScreen -> Icons.Default.Settings
                                                            else -> Icons.Default.BrokenImage
                                                        },
                                                        null
                                                    )
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
                                    notificationClick = { navController.navigate(SScreen.NotificationScreen.route) },
                                    globalSearchClick = { navController.navigate(SScreen.GlobalSearchScreen.route) },
                                    favoritesClick = { navController.navigate(SScreen.FavoriteScreen.route) },
                                    historyClick = { navController.navigate(SScreen.HistoryScreen.route) },
                                    usedLibraryClick = { navController.navigate(SScreen.AboutScreen.route) }
                                )
                            }

                            composable(SScreen.NotificationScreen.route) {
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
                                arguments = listOf(navArgument("searchFor") { nullable = true })
                            ) { GlobalSearchView(mainLogo = logo, notificationLogo = notificationLogo) }

                            composable(SScreen.FavoriteScreen.route) { FavoriteUi(logo) }
                            composable(SScreen.HistoryScreen.route) { HistoryUi(dao = historyDao, logo = logo) }
                            composable(SScreen.AboutScreen.route) { AboutLibrariesScreen(logo) }

                            composable(
                                SScreen.DetailsScreen.route + "/{model}",
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

    private fun hideSystemBars() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
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

    enum class Screen(val id: Int) { RECENT(R.id.recent_nav), ALL(R.id.all_nav), SETTINGS(R.id.setting_nav) }

    fun goToScreen(screen: Screen) {
        findViewById<BottomNavigationView>(R.id.navLayout2)?.selectedItemId = screen.id
    }

    /*private fun setupBottomNavBar() {
        //TODO: Look into doing a recreate and if for the all_nav when showAll is changed
        val navGraphIds = listOf(R.navigation.recent_nav, R.navigation.all_nav, R.navigation.setting_nav)
        currentScreen.value = R.id.recent_nav
        val controller = findViewById<BottomNavigationView>(R.id.navLayout2)
            .also { b ->
                lifecycleScope.launch { showAll.collect { runOnUiThread { b.menu.findItem(R.id.all_nav)?.isVisible = it } } }
                appUpdateCheck
                    .filter {
                        AppUpdate.checkForUpdate(
                            packageManager?.getPackageInfo(packageName, 0)?.versionName.orEmpty(),
                            it.update_real_version.orEmpty()
                        )
                    }
                    .subscribe { b.getOrCreateBadge(R.id.setting_nav).number = 1 }
                    .addTo(disposable)
            }
            .setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.mainShows,
                intent = intent,
                dynamicDestinations = mapOf(
                    R.navigation.recent_nav to { f, n -> genericInfo.recentNavSetup(f, n) },
                    R.navigation.all_nav to { f, n -> genericInfo.allNavSetup(f, n) },
                    R.navigation.setting_nav to { f, n -> genericInfo.settingNavSetup(f, n) }
                )
            )

        currentNavController = controller

        Single.create<AppUpdate.AppUpdates> {
            AppUpdate.getUpdate()?.let { d -> it.onSuccess(d) } ?: it.onError(Exception("Something went wrong"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { }
            .subscribeBy { appUpdateCheck.onNext(it) }
            .addTo(disposable)
    }

    override fun onSupportNavigateUp(): Boolean = currentNavController?.value?.navigateUp() ?: false*/

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

}