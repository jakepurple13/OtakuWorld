package com.programmersbox.uiviews

import android.Manifest
import android.app.assist.AssistContent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.all.AllView
import com.programmersbox.uiviews.all.AllViewModel
import com.programmersbox.uiviews.details.DetailsScreen
import com.programmersbox.uiviews.favorite.FavoriteUi
import com.programmersbox.uiviews.globalsearch.GlobalSearchView
import com.programmersbox.uiviews.history.HistoryUi
import com.programmersbox.uiviews.notifications.NotificationsScreen
import com.programmersbox.uiviews.recent.RecentView
import com.programmersbox.uiviews.recent.RecentViewModel
import com.programmersbox.uiviews.settings.SettingScreen
import com.programmersbox.uiviews.settings.TranslationScreen
import com.programmersbox.uiviews.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import com.programmersbox.uiviews.utils.Screen as SScreen

abstract class BaseMainActivity : AppCompatActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val logo: MainLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    private val dao by lazy { ItemDatabase.getInstance(this).itemDao() }
    private val historyDao by lazy { HistoryDatabase.getInstance(this).historyDao() }
    protected lateinit var navController: NavHostController

    protected fun isNavInitialized() = ::navController.isInitialized

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    companion object {
        var showNavBar by mutableStateOf(true)
    }

    @OptIn(
        ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            genericInfo.toSource(currentService.orEmpty())?.let { sourceFlow.emit(it) }
        }

        when (runBlocking { themeSetting.first() }) {
            "System" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "Light" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> null
        }?.let(AppCompatDelegate::setDefaultNightMode)

        onCreate()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            flow { emit(AppUpdate.getUpdate()) }
                .catch { emit(null) }
                .dispatchIo()
                .onEach(updateAppCheck::emit)
                .collect()
        }

        setContent {
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            navController = rememberAnimatedNavController(bottomSheetNavigator)

            val systemUiController = rememberSystemUiController()

            if (showNavBar) {
                systemUiController.isSystemBarsVisible = true
                systemUiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                systemUiController.isSystemBarsVisible = false
                systemUiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            OtakuMaterialTheme(navController, genericInfo) {
                AskForNotificationPermissions()

                val showAllItem by showAll.collectAsState(false)

                com.google.accompanist.navigation.material.ModalBottomSheetLayout(
                    bottomSheetNavigator,
                    sheetBackgroundColor = MaterialTheme.colorScheme.surface,
                    sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                ) {
                    Scaffold(
                        modifier = Modifier.navigationBarsPadding(),
                        bottomBar = {
                            Column {
                                BottomBarAdditions()
                                AnimatedVisibility(
                                    visible = showNavBar,
                                    enter = slideInVertically { it / 2 } + expandVertically() + fadeIn(),
                                    exit = slideOutVertically { it / 2 } + shrinkVertically() + fadeOut(),
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
                                deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + SScreen.SettingsScreen.route }),
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

                            bottomSheet(SScreen.TranslationScreen.route) { TranslationScreen() }

                            with(genericInfo) { navSetup() }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun AskForNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            val permissions = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
            LaunchedEffect(Unit) { permissions.launchPermissionRequest() }
        }
    }

    @Composable
    fun updateCheck(): Boolean {
        val appUpdate by updateAppCheck.collectAsState(null)

        return AppUpdate.checkForUpdate(
            remember { packageManager.getPackageInfo(packageName, 0)?.versionName.orEmpty() },
            appUpdate?.update_real_version.orEmpty()
        )
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = Uri.parse(currentDetailsUrl)
    }

    enum class Screen(val route: SScreen) { RECENT(SScreen.RecentScreen), ALL(SScreen.AllScreen), SETTINGS(SScreen.SettingsScreen) }

    fun goToScreen(screen: Screen) {
        if (::navController.isInitialized) {
            navController.navigate(screen.route.route)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

}