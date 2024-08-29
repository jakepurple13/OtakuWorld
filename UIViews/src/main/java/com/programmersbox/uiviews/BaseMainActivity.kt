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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
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
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
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
import com.programmersbox.uiviews.lists.OtakuListScreen
import com.programmersbox.uiviews.notifications.NotificationsScreen
import com.programmersbox.uiviews.notifications.cancelNotification
import com.programmersbox.uiviews.recent.RecentView
import com.programmersbox.uiviews.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.settings.ExtensionList
import com.programmersbox.uiviews.settings.GeneralSettings
import com.programmersbox.uiviews.settings.InfoSettings
import com.programmersbox.uiviews.settings.MoreSettingsScreen
import com.programmersbox.uiviews.settings.NotificationSettings
import com.programmersbox.uiviews.settings.PlaySettings
import com.programmersbox.uiviews.settings.SettingScreen
import com.programmersbox.uiviews.utils.ChromeCustomTabsNavigator
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.LocalWindowSizeClass
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.OtakuMaterialTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.chromeCustomTabs
import com.programmersbox.uiviews.utils.components.HazeScaffold
import com.programmersbox.uiviews.utils.currentDetailsUrl
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.sharedelements.LocalSharedElementScope
import com.programmersbox.uiviews.utils.sharedelements.animatedScopeComposable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : AppCompatActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val customPreferences = ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences())
    private val appLogo: AppLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    private val itemDatabase: ItemDatabase by inject()
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
        ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class
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

            val themeSetting by settingsHandling.rememberSystemThemeMode()

            val isAmoledMode by settingsHandling.rememberIsAmoledMode()

            val showBlur by settingsHandling.rememberShowBlur()

            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSize
            ) {
                OtakuMaterialTheme(
                    navController = navController,
                    genericInfo = genericInfo,
                    themeSetting = themeSetting,
                    isAmoledMode = isAmoledMode,
                ) {
                    AskForNotificationPermissions()

                    val showAllItem by settingsHandling.rememberShowAll()

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
                        HazeScaffold(
                            bottomBar = {
                                BottomNav(
                                    navController = navController,
                                    showNavBar = showNavBar,
                                    navType = navType,
                                    showAllItem = showAllItem,
                                    currentDestination = currentDestination,
                                    showBlur = showBlur,
                                    isAmoledMode = isAmoledMode
                                )
                            },
                            contentWindowInsets = WindowInsets(0.dp),
                            blurBottomBar = showBlur
                        ) { innerPadding ->
                            SharedTransitionLayout {
                                CompositionLocalProvider(
                                    LocalNavHostPadding provides innerPadding,
                                    LocalSharedElementScope provides this@SharedTransitionLayout
                                ) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = Screen.RecentScreen,
                                        modifier = Modifier.fillMaxSize()
                                    ) { navGraph(customPreferences, windowSize) }
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
    private fun BottomNav(
        navController: NavHostController,
        showNavBar: Boolean,
        navType: NavigationBarType,
        showAllItem: Boolean,
        currentDestination: NavDestination?,
        showBlur: Boolean,
        isAmoledMode: Boolean,
    ) {
        Column {
            BottomBarAdditions()
            AnimatedVisibility(
                visible = showNavBar && navType == NavigationBarType.Bottom,
                enter = slideInVertically { it / 2 } + expandVertically() + fadeIn(),
                exit = slideOutVertically { it / 2 } + shrinkVertically() + fadeOut(),
            ) {
                NavigationBar(
                    containerColor = when {
                        showBlur -> Color.Transparent
                        isAmoledMode -> MaterialTheme.colorScheme.surface
                        else -> NavigationBarDefaults.containerColor
                    }
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
                                navController.navigate(screen) {
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
                        screen = Screen.NotificationScreen.Home,
                        currentDestination = currentDestination,
                        navController = navController,
                    )
                }

                NavigationRailItem(
                    imageVector = Icons.AutoMirrored.Default.List,
                    label = stringResource(R.string.custom_lists_title),
                    screen = Screen.CustomListScreen.Home,
                    currentDestination = currentDestination,
                    navController = navController,
                )

                NavigationRailItem(
                    imageVector = Icons.Default.Search,
                    label = stringResource(R.string.global_search),
                    screen = Screen.GlobalSearchScreen.Home(),
                    currentDestination = currentDestination,
                    navController = navController,
                )

                NavigationRailItem(
                    imageVector = Icons.Default.Star,
                    label = stringResource(R.string.viewFavoritesMenu),
                    screen = Screen.FavoriteScreen.Home,
                    currentDestination = currentDestination,
                    navController = navController,
                )

                NavigationRailItem(
                    imageVector = Icons.Default.Extension,
                    label = stringResource(R.string.extensions),
                    screen = Screen.ExtensionListScreen.Home,
                    currentDestination = currentDestination,
                    navController = navController,
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
                        navController.navigate(Screen.Settings) {
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
        onClick: () -> Unit = {
            navController.navigate(screen) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) {
        NavigationRailItem(
            icon = { Icon(imageVector, null) },
            label = { Text(label) },
            selected = currentDestination.isTopLevelDestinationInHierarchy(screen.route),
            onClick = onClick
        )
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class
    )
    private fun NavGraphBuilder.navGraph(
        customPreferences: ComposeSettingsDsl,
        windowSize: WindowSizeClass,
    ) {
        animatedScopeComposable<Screen.RecentScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) }
        ) { RecentView() }

        animatedScopeComposable<Screen.AllScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            AllView(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        settings(customPreferences, windowSize) { with(genericInfo) { settingsNavSetup() } }

        animatedScopeComposable<Screen.DetailsScreen.Details>(
            deepLinks = listOf(
                navDeepLink<Screen.DetailsScreen.Details>(
                    basePath = genericInfo.deepLinkUri + Screen.DetailsScreen.Details::class.qualifiedName
                )
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            DetailsScreen(
                detailInfo = it.toRoute(),
                logo = notificationLogo,
                windowSize = windowSize
            )
        }

        composable<Screen.GeminiRecommendationScreen> {
            GeminiRecommendationScreen()
        }

        chromeCustomTabs()

        with(genericInfo) { globalNavSetup() }
    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class
    )
    private fun NavGraphBuilder.settings(
        customPreferences: ComposeSettingsDsl,
        windowSize: WindowSizeClass,
        additionalSettings: NavGraphBuilder.() -> Unit,
    ) {
        navigation<Screen.Settings>(
            startDestination = Screen.SettingsScreen,
        ) {
            composable<Screen.SettingsScreen>(
                deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.SettingsScreen.route }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) {
                SettingScreen(
                    composeSettingsDsl = customPreferences,
                    notificationClick = { navController.navigate(Screen.NotificationScreen) { launchSingleTop = true } },
                    favoritesClick = { navController.navigate(Screen.FavoriteScreen) { launchSingleTop = true } },
                    historyClick = { navController.navigate(Screen.HistoryScreen) { launchSingleTop = true } },
                    globalSearchClick = { navController.navigate(Screen.GlobalSearchScreen()) { launchSingleTop = true } },
                    listClick = { navController.navigate(Screen.CustomListScreen) { launchSingleTop = true } },
                    debugMenuClick = { navController.navigate(Screen.DebugScreen) { launchSingleTop = true } },
                    extensionClick = { navController.navigate(Screen.ExtensionListScreen) { launchSingleTop = true } },
                    notificationSettingsClick = { navController.navigate(Screen.NotificationsSettings) },
                    generalClick = { navController.navigate(Screen.GeneralSettings) },
                    otherClick = { navController.navigate(Screen.OtherSettings) },
                    moreInfoClick = { navController.navigate(Screen.MoreInfoSettings) },
                    moreSettingsClick = { navController.navigate(Screen.MoreSettings) },
                    recommendationClick = { navController.navigate(Screen.GeminiRecommendationScreen) }
                )
            }

            composable<Screen.NotificationsSettings>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { NotificationSettings() }

            composable<Screen.GeneralSettings>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { GeneralSettings(customPreferences.generalSettings) }

            composable<Screen.MoreInfoSettings>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) {
                InfoSettings(
                    usedLibraryClick = { navController.navigate(Screen.AboutScreen) { launchSingleTop = true } }
                )
            }

            composable<Screen.OtherSettings>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { PlaySettings(customPreferences.playerSettings) }

            composable<Screen.MoreSettings>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { MoreSettingsScreen() }

            animatedScopeComposable<Screen.HistoryScreen>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { HistoryUi() }

            animatedScopeComposable<Screen.FavoriteScreen>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) {
                FavoriteUi(
                    isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                )
            }

            composable<Screen.AboutScreen>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { AboutLibrariesScreen() }

            composable<Screen.GlobalSearchScreen>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) {
                GlobalSearchView(
                    globalSearchScreen = it.toRoute<Screen.GlobalSearchScreen>(),
                    notificationLogo = notificationLogo,
                    isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                )
            }

            animatedScopeComposable<Screen.CustomListScreen>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
            ) {
                OtakuListScreen(
                    isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                )
            }

            composable<Screen.ImportListScreen> { ImportListScreen(it.toRoute()) }

            animatedScopeComposable<Screen.NotificationScreen>(
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

            composable<Screen.ExtensionListScreen>(
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
            ) { ExtensionList() }

            additionalSettings()

            if (BuildConfig.DEBUG) {
                composable<Screen.DebugScreen> {
                    DebugView()
                }
            }
        }

        //These few are specifically so Settings won't get highlighted when selecting these screens from navigation
        composable<Screen.GlobalSearchScreen.Home>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            GlobalSearchView(
                globalSearchScreen = it.toRoute<Screen.GlobalSearchScreen>(),
                notificationLogo = notificationLogo,
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        animatedScopeComposable<Screen.CustomListScreen.Home>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            OtakuListScreen(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        animatedScopeComposable<Screen.NotificationScreen.Home>(
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

        animatedScopeComposable<Screen.FavoriteScreen.Home>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            FavoriteUi(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        composable<Screen.ExtensionListScreen.Home>(
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

        flow { emit(AppUpdate.getUpdate()) }
            .catch { emit(null) }
            .dispatchIo()
            .onEach(updateAppCheck::emit)
            .launchIn(lifecycleScope)

        itemDatabase
            .itemDao()
            .getAllNotificationCount()
            .onEach { notificationCount = it }
            .launchIn(lifecycleScope)
    }

    private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: Screen) = isTopLevelDestinationInHierarchy(destination.route)

    private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: String) = this?.hierarchy?.any {
        it.route?.contains(destination, true) ?: false
    } ?: false

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = Uri.parse(currentDetailsUrl)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    enum class NavigationBarType { Rail, Bottom }
}