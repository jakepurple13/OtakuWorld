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
import androidx.appcompat.content.res.AppCompatResources
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.all.AllView
import com.programmersbox.uiviews.details.DetailsScreen
import com.programmersbox.uiviews.favorite.FavoriteChoiceScreen
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
import com.programmersbox.uiviews.settings.GeneralSettings
import com.programmersbox.uiviews.settings.InfoSettings
import com.programmersbox.uiviews.settings.NotificationSettings
import com.programmersbox.uiviews.settings.PlaySettings
import com.programmersbox.uiviews.settings.SettingScreen
import com.programmersbox.uiviews.settings.SourceChooserScreen
import com.programmersbox.uiviews.settings.TranslationScreen
import com.programmersbox.uiviews.utils.ChromeCustomTabsNavigator
import com.programmersbox.uiviews.utils.ModalBottomSheetLayout
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.OtakuMaterialTheme
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.bottomSheet
import com.programmersbox.uiviews.utils.chromeCustomTabs
import com.programmersbox.uiviews.utils.currentDetailsUrl
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.rememberBottomSheetNavigator
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : AppCompatActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val logo: MainLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    protected lateinit var navController: NavHostController

    protected fun isNavInitialized() = ::navController.isInitialized

    private val settingsHandling: SettingsHandling by inject()

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    companion object {
        var showNavBar by mutableStateOf(true)
    }

    @OptIn(
        ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class, ExperimentalFoundationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            genericInfo.toSource(currentService.orEmpty())?.let { sourceFlow.emit(it) }
        }

        when (runBlocking { settingsHandling.systemThemeMode.firstOrNull() }) {
            SystemThemeMode.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            SystemThemeMode.Day -> AppCompatDelegate.MODE_NIGHT_NO
            SystemThemeMode.Night -> AppCompatDelegate.MODE_NIGHT_YES
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
            val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
            navController = rememberNavController(
                bottomSheetNavigator,
                remember { ChromeCustomTabsNavigator(this) }
            )

            val systemUiController = rememberSystemUiController()
            val customPreferences = remember { ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences(navController)) }

            if (showNavBar) {
                systemUiController.isSystemBarsVisible = true
                systemUiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                systemUiController.isSystemBarsVisible = false
                systemUiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            OtakuMaterialTheme(navController, genericInfo) {
                AskForNotificationPermissions()

                val showAllItem by settingsHandling.showAll.collectAsState(false)

                ModalBottomSheetLayout(
                    bottomSheetNavigator = bottomSheetNavigator,
                    sheetBackgroundColor = MaterialTheme.colorScheme.surface,
                    sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                ) {
                    val windowSize = calculateWindowSizeClass(activity = this@BaseMainActivity)
                    val navType = when (windowSize.widthSizeClass) {
                        WindowWidthSizeClass.Expanded -> NavigationBarType.Rail
                        else -> NavigationBarType.Bottom
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    Row(Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = showNavBar && navType == NavigationBarType.Rail,
                            enter = slideInHorizontally { it / 2 } + expandHorizontally() + fadeIn(),
                            exit = slideOutHorizontally { it / 2 } + shrinkHorizontally() + fadeOut(),
                        ) {
                            NavigationRail(
                                header = {
                                    Image(
                                        AppCompatResources.getDrawable(this@BaseMainActivity, logo.logoId)!!.toBitmap().asImageBitmap(),
                                        null,
                                    )
                                }
                            ) {
                                Screen.bottomItems.forEach { screen ->
                                    if (screen !is Screen.AllScreen || showAllItem) {
                                        NavigationRailItem(
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (screen is Screen.Settings) {
                                                            val updateAvailable = updateCheck()
                                                            if (updateAvailable) {
                                                                Badge { Text("") }
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        when (screen) {
                                                            Screen.RecentScreen -> Icons.Default.History
                                                            Screen.AllScreen -> Icons.Default.BrowseGallery
                                                            Screen.Settings -> Icons.Default.Settings
                                                            else -> Icons.Default.BrokenImage
                                                        },
                                                        null
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    when (screen) {
                                                        Screen.AllScreen -> stringResource(R.string.all)
                                                        Screen.RecentScreen -> stringResource(R.string.recent)
                                                        Screen.Settings -> stringResource(R.string.settings)
                                                        else -> ""
                                                    }
                                                )
                                            },
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
                                }
                            }
                        }
                        OtakuScaffold(
                            bottomBar = {
                                Column {
                                    BottomBarAdditions()
                                    AnimatedVisibility(
                                        visible = showNavBar && navType == NavigationBarType.Bottom,
                                        enter = slideInVertically { it / 2 } + expandVertically() + fadeIn(),
                                        exit = slideOutVertically { it / 2 } + shrinkVertically() + fadeOut(),
                                    ) {
                                        NavigationBar {
                                            Screen.bottomItems.forEach { screen ->
                                                if (screen !is Screen.AllScreen || showAllItem) {
                                                    NavigationBarItem(
                                                        icon = {
                                                            BadgedBox(
                                                                badge = {
                                                                    if (screen is Screen.Settings) {
                                                                        val updateAvailable = updateCheck()
                                                                        if (updateAvailable) {
                                                                            Badge { Text("") }
                                                                        }
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(
                                                                    when (screen) {
                                                                        Screen.RecentScreen -> Icons.Default.History
                                                                        Screen.AllScreen -> Icons.Default.BrowseGallery
                                                                        Screen.Settings -> Icons.Default.Settings
                                                                        else -> Icons.Default.BrokenImage
                                                                    },
                                                                    null
                                                                )
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                when (screen) {
                                                                    Screen.AllScreen -> stringResource(R.string.all)
                                                                    Screen.RecentScreen -> stringResource(R.string.recent)
                                                                    Screen.Settings -> stringResource(R.string.settings)
                                                                    else -> ""
                                                                }
                                                            )
                                                        },
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
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Screen.RecentScreen.route,
                                modifier = Modifier.padding(innerPadding)
                            ) { navGraph(customPreferences, windowSize) }
                        }
                    }
                }
            }
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class, ExperimentalFoundationApi::class
    )
    private fun NavGraphBuilder.navGraph(
        customPreferences: ComposeSettingsDsl,
        windowSize: WindowSizeClass
    ) {
        composable(Screen.RecentScreen.route) { RecentView(logo = logo) }
        composable(Screen.AllScreen.route) { AllView(logo = logo) }
        settings(customPreferences) { with(genericInfo) { settingsNavSetup() } }

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

        bottomSheet(Screen.TranslationScreen.route) { TranslationScreen() }

        bottomSheet(
            Screen.FavoriteChoiceScreen.route + "/{${Screen.FavoriteChoiceScreen.dbitemsArgument}}",
        ) { FavoriteChoiceScreen() }

        bottomSheet(Screen.SourceChooserScreen.route) { SourceChooserScreen() }

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
        additionalSettings: NavGraphBuilder.() -> Unit
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
                    logo = logo,
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
            ) { HistoryUi(logo = logo) }

            composable(
                Screen.FavoriteScreen.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { FavoriteUi(logo) }

            composable(
                Screen.AboutScreen.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { AboutLibrariesScreen(logo) }

            composable(
                Screen.GlobalSearchScreen.route + "?searchFor={searchFor}",
                arguments = listOf(navArgument("searchFor") { nullable = true }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) { GlobalSearchView(mainLogo = logo, notificationLogo = notificationLogo) }

            composable(Screen.CustomListScreen.route) { OtakuListScreen() }
            composable(
                Screen.CustomListItemScreen.route + "/{uuid}"
            ) { OtakuCustomListScreen(logo) }

            composable(
                Screen.ImportListScreen.route + "?uri={uri}"
            ) { ImportListScreen(logo) }

            composable(
                Screen.NotificationScreen.route,
                deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.NotificationScreen.route }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
            ) {
                val notificationManager = remember { this@BaseMainActivity.notificationManager }
                NotificationsScreen(
                    logo = logo,
                    notificationLogo = notificationLogo,
                    cancelNotificationById = notificationManager::cancel,
                    cancelNotification = notificationManager::cancelNotification
                )
            }

            additionalSettings()

            if (BuildConfig.DEBUG) {
                composable(Screen.DebugScreen.route) {
                    DebugView()
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
            appVersion(),
            appUpdate?.update_real_version.orEmpty()
        )
    }

    private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: Screen) = this?.hierarchy?.any {
        it.route?.contains(destination.route, true) ?: false
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