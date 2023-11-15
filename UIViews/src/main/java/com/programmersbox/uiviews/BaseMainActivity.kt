package com.programmersbox.uiviews

import android.Manifest
import android.app.assist.AssistContent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.all.AllView
import com.programmersbox.uiviews.details.DetailsScreen
import com.programmersbox.uiviews.favorite.FavoriteUi
import com.programmersbox.uiviews.globalsearch.GlobalSearchView
import com.programmersbox.uiviews.history.HistoryUi
import com.programmersbox.uiviews.lists.ImportListScreen
import com.programmersbox.uiviews.lists.OtakuCustomListScreen
import com.programmersbox.uiviews.lists.OtakuListScreen
import com.programmersbox.uiviews.notifications.NotificationsScreen
import com.programmersbox.uiviews.notifications.cancelNotification
import com.programmersbox.uiviews.recent.RecentView
import com.programmersbox.uiviews.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.settings.ExtensionList
import com.programmersbox.uiviews.settings.GeneralSettings
import com.programmersbox.uiviews.settings.InfoSettings
import com.programmersbox.uiviews.settings.NotificationSettings
import com.programmersbox.uiviews.settings.PlaySettings
import com.programmersbox.uiviews.settings.SettingScreen
import com.programmersbox.uiviews.utils.ChromeCustomTabsNavigator
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.OtakuMaterialTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.chromeCustomTabs
import com.programmersbox.uiviews.utils.components.GlassScaffold
import com.programmersbox.uiviews.utils.currentDetailsUrl
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.dispatchIo
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : AppCompatActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val customPreferences = ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences())
    private val appLogo: AppLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    protected lateinit var navController: NavHostController

    protected fun isNavInitialized() = ::navController.isInitialized

    private val settingsHandling: SettingsHandling by inject()

    private val sourceRepository by inject<SourceRepository>()
    private val currentSourceRepository by inject<CurrentSourceRepository>()

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    private var notificationCount by mutableIntStateOf(0)

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
        onCreate()

        enableEdgeToEdge()

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        changingSettingsRepository
            .showNavBar
            .onEach {
                if (it) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            .launchIn(lifecycleScope)

        setContent {
            navController = rememberNavController(
                remember { ChromeCustomTabsNavigator(this) }
            )

            val windowSize = calculateWindowSizeClass(activity = this@BaseMainActivity)

            OtakuMaterialTheme(navController, genericInfo) {
                AskForNotificationPermissions()

                val showAllItem by settingsHandling.showAll.collectAsStateWithLifecycle(false)

                val navType = when (windowSize.widthSizeClass) {
                    WindowWidthSizeClass.Expanded -> NavigationBarType.Rail
                    else -> NavigationBarType.Bottom
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val showNavBar by changingSettingsRepository.showNavBar.collectAsStateWithLifecycle(true)

                genericInfo.DialogSetups()

                Row(Modifier.fillMaxSize()) {
                    Rail(
                        navController = navController,
                        showNavBar = showNavBar,
                        navType = navType,
                        showAllItem = showAllItem,
                        currentDestination = currentDestination
                    )
                    GlassScaffold(
                        bottomBar = {
                            BottomNav(
                                navController = navController,
                                showNavBar = showNavBar,
                                navType = navType,
                                showAllItem = showAllItem,
                                currentDestination = currentDestination
                            )
                        },
                        contentWindowInsets = WindowInsets(0.dp),
                        blurBottomBar = true
                    ) { innerPadding ->
                        CompositionLocalProvider(
                            LocalNavHostPadding provides innerPadding
                        ) {
                            NavHost(
                                navController = navController,
                                //startDestination = Screen.RecentScreen.route,
                                startDestination = Screen.Settings.route
                            ) { navGraph(customPreferences, windowSize) }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BottomNav(
        navController: NavHostController,
        showNavBar: Boolean,
        navType: NavigationBarType,
        showAllItem: Boolean,
        currentDestination: NavDestination?,
    ) {
        Column {
            BottomBarAdditions()
            AnimatedVisibility(
                visible = showNavBar && navType == NavigationBarType.Bottom,
                enter = slideInVertically { it / 2 } + expandVertically() + fadeIn(),
                exit = slideOutVertically { it / 2 } + shrinkVertically() + fadeOut(),
            ) {
                NavigationBar(
                    containerColor = Color.Transparent
                ) {

                    @Composable
                    fun ScreenBottomItem(
                        screen: Screen,
                        icon: ImageVector,
                        label: Int,
                        badge: @Composable BoxScope.() -> Unit = {},
                    ) {
                        NavigationBarItem(
                            icon = { BadgedBox(badge = badge) { Icon(icon, null) } },
                            label = { Text(stringResource(label)) },
                            selected = currentDestination.isTopLevelDestinationInHierarchy(screen),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    ScreenBottomItem(
                        screen = Screen.RecentScreen,
                        icon = Icons.Default.History,
                        label = R.string.recent
                    )
                    if (showAllItem) {
                        ScreenBottomItem(
                            screen = Screen.AllScreen,
                            icon = Icons.Default.BrowseGallery,
                            label = R.string.all
                        )
                    }
                    ScreenBottomItem(
                        screen = Screen.Settings,
                        icon = Icons.Default.Settings,
                        label = R.string.settings,
                        badge = { if (updateCheck()) Badge { Text("") } }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Rail(
        navController: NavHostController,
        showNavBar: Boolean,
        navType: NavigationBarType,
        showAllItem: Boolean,
        currentDestination: NavDestination?,
    ) {
        AnimatedVisibility(
            visible = showNavBar && navType == NavigationBarType.Rail,
            enter = slideInHorizontally { it / 2 } + expandHorizontally() + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + shrinkHorizontally() + fadeOut(),
        ) {
            NavigationRail(
                header = {
                    Image(
                        rememberDrawablePainter(drawable = appLogo.logo),
                        null,
                    )
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                NavigationRailItem(
                    imageVector = Icons.Default.History,
                    label = stringResource(R.string.recent),
                    screen = Screen.RecentScreen,
                    currentDestination = currentDestination,
                    navController = navController
                )

                AnimatedVisibility(visible = showAllItem) {
                    NavigationRailItem(
                        imageVector = Icons.Default.BrowseGallery,
                        label = stringResource(R.string.all),
                        screen = Screen.AllScreen,
                        currentDestination = currentDestination,
                        navController = navController
                    )
                }

                AnimatedVisibility(visible = notificationCount > 0) {
                    NavigationRailItem(
                        imageVector = Icons.Default.Notifications,
                        label = stringResource(R.string.notifications),
                        screen = Screen.NotificationScreen,
                        currentDestination = currentDestination,
                        navController = navController,
                        customRoute = "_home"
                    )
                }

                NavigationRailItem(
                    imageVector = Icons.AutoMirrored.Default.List,
                    label = stringResource(R.string.custom_lists_title),
                    screen = Screen.CustomListScreen,
                    currentDestination = currentDestination,
                    navController = navController,
                    customRoute = "_home"
                )

                NavigationRailItem(
                    imageVector = Icons.Default.Search,
                    label = stringResource(R.string.global_search),
                    screen = Screen.GlobalSearchScreen,
                    currentDestination = currentDestination,
                    navController = navController,
                    customRoute = "_home"
                )

                NavigationRailItem(
                    imageVector = Icons.Default.Star,
                    label = stringResource(R.string.viewFavoritesMenu),
                    screen = Screen.FavoriteScreen,
                    currentDestination = currentDestination,
                    navController = navController,
                    customRoute = "_home"
                )

                NavigationRailItem(
                    imageVector = Icons.Default.Extension,
                    label = stringResource(R.string.extensions),
                    screen = Screen.ExtensionListScreen,
                    currentDestination = currentDestination,
                    navController = navController,
                    customRoute = "_home"
                )

                NavigationRailItem(
                    icon = {
                        BadgedBox(
                            badge = { if (updateCheck()) Badge { Text("") } }
                        ) { Icon(Icons.Default.Settings, null) }
                    },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentDestination.isTopLevelDestinationInHierarchy(Screen.Settings),
                    onClick = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun NavigationRailItem(
        imageVector: ImageVector,
        label: String,
        screen: Screen,
        currentDestination: NavDestination?,
        navController: NavHostController,
        customRoute: String = "",
        onClick: () -> Unit = {
            navController.navigate(screen.route + customRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) {
        NavigationRailItem(
            icon = { Icon(imageVector, null) },
            label = { Text(label) },
            selected = currentDestination.isTopLevelDestinationInHierarchy(screen.route + customRoute),
            onClick = onClick
        )
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class, ExperimentalFoundationApi::class
    )
    private fun NavGraphBuilder.navGraph(
        customPreferences: ComposeSettingsDsl,
        windowSize: WindowSizeClass,
    ) {
        composable(Screen.RecentScreen.route) { RecentView() }
        composable(Screen.AllScreen.route) { AllView() }
        settings(customPreferences, windowSize) { with(genericInfo) { settingsNavSetup() } }

        composable(
            Screen.DetailsScreen.route + "/{model}",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = genericInfo.deepLinkUri + "${Screen.DetailsScreen.route}/{model}"
                }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            DetailsScreen(
                logo = notificationLogo,
                windowSize = windowSize
            )
        }

        chromeCustomTabs()

        with(genericInfo) { globalNavSetup() }
    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class
    )
    private fun NavGraphBuilder.settings(
        customPreferences: ComposeSettingsDsl,
        windowSize: WindowSizeClass,
        additionalSettings: NavGraphBuilder.() -> Unit,
    ) {
        navigation(
            route = Screen.Settings.route,
            startDestination = Screen.SettingsScreen.route,
        ) {
            composable(
                Screen.SettingsScreen.route,
                deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.SettingsScreen.route }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) {
                SettingScreen(
                    composeSettingsDsl = customPreferences,
                    notificationClick = { navController.navigate(Screen.NotificationScreen.route) { launchSingleTop = true } },
                    favoritesClick = { navController.navigate(Screen.FavoriteScreen.route) { launchSingleTop = true } },
                    historyClick = { navController.navigate(Screen.HistoryScreen.route) { launchSingleTop = true } },
                    globalSearchClick = { navController.navigate(Screen.GlobalSearchScreen.route) { launchSingleTop = true } },
                    listClick = { navController.navigate(Screen.CustomListScreen.route) { launchSingleTop = true } },
                    debugMenuClick = { navController.navigate(Screen.DebugScreen.route) { launchSingleTop = true } }
                )
            }

            composable(
                Screen.NotificationsSettings.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { NotificationSettings() }

            composable(
                Screen.GeneralSettings.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { GeneralSettings(customPreferences.generalSettings) }

            composable(
                Screen.MoreInfoSettings.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) {
                InfoSettings(
                    usedLibraryClick = { navController.navigate(Screen.AboutScreen.route) { launchSingleTop = true } }
                )
            }

            composable(
                Screen.OtherSettings.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { PlaySettings(customPreferences.playerSettings) }

            composable(
                Screen.HistoryScreen.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { HistoryUi() }

            composable(
                Screen.FavoriteScreen.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) {
                FavoriteUi(
                    isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                )
            }

            composable(
                Screen.AboutScreen.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { AboutLibrariesScreen() }

            composable(
                Screen.GlobalSearchScreen.route + "?searchFor={searchFor}",
                arguments = listOf(navArgument("searchFor") { nullable = true }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) {
                GlobalSearchView(
                    notificationLogo = notificationLogo,
                    isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                )
            }

            composable(Screen.CustomListScreen.route) { OtakuListScreen() }
            composable(
                Screen.CustomListItemScreen.route + "/{uuid}"
            ) {
                OtakuCustomListScreen(
                    isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                )
            }

            composable(
                Screen.ImportListScreen.route + "?uri={uri}"
            ) { ImportListScreen() }

            composable(
                Screen.NotificationScreen.route,
                deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.NotificationScreen.route }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) {
                val notificationManager = remember { this@BaseMainActivity.notificationManager }
                NotificationsScreen(
                    notificationLogo = notificationLogo,
                    cancelNotificationById = notificationManager::cancel,
                    cancelNotification = notificationManager::cancelNotification
                )
            }

            composable(
                Screen.ExtensionListScreen.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { ExtensionList() }

            additionalSettings()

            if (BuildConfig.DEBUG) {
                composable(Screen.DebugScreen.route) {
                    DebugView()
                }
            }
        }

        //These few are specifically so Settings won't get highlighted when selecting these screens from navigation
        composable(
            Screen.GlobalSearchScreen.route + "_home" + "?searchFor={searchFor}",
            arguments = listOf(navArgument("searchFor") { nullable = true }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            GlobalSearchView(
                notificationLogo = notificationLogo,
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        composable(Screen.CustomListScreen.route + "_home") { OtakuListScreen() }

        composable(
            Screen.NotificationScreen.route + "_home",
            deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.NotificationScreen.route }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            val notificationManager = remember { this@BaseMainActivity.notificationManager }
            NotificationsScreen(
                notificationLogo = notificationLogo,
                cancelNotificationById = notificationManager::cancel,
                cancelNotification = notificationManager::cancelNotification
            )
        }

        composable(
            Screen.FavoriteScreen.route + "_home",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            FavoriteUi(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        composable(
            Screen.ExtensionListScreen.route + "_home",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) { ExtensionList() }
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
            appVersion(),
            appUpdate?.update_real_version.orEmpty()
        )
    }

    private fun setup() {
        lifecycleScope.launch {
            if (currentService == null) {
                val s = sourceRepository.list.randomOrNull()?.apiService
                currentSourceRepository.emit(s)
                currentService = s?.serviceName
            } else {
                sourceRepository.toSourceByApiServiceName(currentService.orEmpty())?.let { currentSourceRepository.emit(it.apiService) }
            }
        }

        when (runBlocking { settingsHandling.systemThemeMode.firstOrNull() }) {
            SystemThemeMode.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            SystemThemeMode.Day -> AppCompatDelegate.MODE_NIGHT_NO
            SystemThemeMode.Night -> AppCompatDelegate.MODE_NIGHT_YES
            else -> null
        }?.let(AppCompatDelegate::setDefaultNightMode)

        flow { emit(AppUpdate.getUpdate()) }
            .catch { emit(null) }
            .dispatchIo()
            .onEach(updateAppCheck::emit)
            .launchIn(lifecycleScope)

        ItemDatabase.getInstance(this)
            .itemDao()
            .getAllNotificationCount()
            .onEach { notificationCount = it }
            .launchIn(lifecycleScope)
    }

    private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: Screen) = this?.hierarchy?.any {
        it.route?.contains(destination.route, true) ?: false
    } ?: false

    private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: String) = this?.hierarchy?.any {
        it.route?.contains(destination, true) ?: false
    } ?: false

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = Uri.parse(currentDetailsUrl)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    enum class NavigationBarType { Rail, Bottom }
}