package com.programmersbox.uiviews

import android.Manifest
import android.app.assist.AssistContent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.BottomAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.HazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.ScreenBottomItem
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.utils.ChromeCustomTabsNavigator
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.presentation.components.MultipleActions
import com.programmersbox.uiviews.presentation.components.rememberMultipleBarState
import com.programmersbox.uiviews.presentation.navGraph
import com.programmersbox.uiviews.theme.OtakuMaterialTheme
import com.programmersbox.uiviews.utils.LocalWindowSizeClass
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.currentDetailsUrl
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.sharedelements.LocalSharedElementScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class BaseMainActivity : AppCompatActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val customPreferences = ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences())
    private val appLogo: AppLogo by inject()
    private val notificationLogo: NotificationLogo by inject()
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    private val itemDatabase: ItemDatabase by inject()
    private val dataStoreHandling: DataStoreHandling by inject()
    protected lateinit var navController: NavHostController

    protected fun isNavInitialized() = ::navController.isInitialized

    private val settingsHandling: NewSettingsHandling by inject()

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
            .showInsets
            .onEach {
                if (it) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            .launchIn(lifecycleScope)

        val startDestination = if (runBlocking { dataStoreHandling.hasGoneThroughOnboarding.getOrNull() } == false) {
            Screen.OnboardingScreen
        } else {
            Screen.RecentScreen
        }

        setContent {
            navController = rememberNavController(
                remember { ChromeCustomTabsNavigator(this) }
            )

            val windowSize = calculateWindowSizeClass(activity = this@BaseMainActivity)

            val isAmoledMode by settingsHandling.rememberIsAmoledMode()

            val showBlur by settingsHandling.rememberShowBlur()

            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSize
            ) {
                OtakuMaterialTheme(
                    navController = navController,
                    genericInfo = genericInfo,
                    settingsHandling = settingsHandling
                ) {
                    AskForNotificationPermissions()

                    val showAllItem by settingsHandling.rememberShowAll()
                    val middleNavItem by settingsHandling.rememberMiddleNavigationAction()
                    val multipleActions by settingsHandling.rememberMiddleMultipleActions()

                    val navType = when (windowSize.widthSizeClass) {
                        WindowWidthSizeClass.Expanded -> NavigationBarType.Rail
                        else -> NavigationBarType.Bottom
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val showNavBar by changingSettingsRepository.showNavBar.collectAsStateWithLifecycle(true)
                    val floatingNavigation by rememberFloatingNavigation()
                    val hazeState = remember { HazeState() }

                    //val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

                    genericInfo.DialogSetups()

                    SharedTransitionLayout {
                        Row(Modifier.fillMaxSize()) {
                            Rail(
                                navController = navController,
                                showNavBar = showNavBar,
                                navType = navType,
                                showAllItem = showAllItem,
                                currentDestination = currentDestination
                            )

                            HazeScaffold(
                                hazeState = hazeState,
                                bottomBar = {
                                    if (!floatingNavigation) {
                                        BottomNav(
                                            navController = navController,
                                            showNavBar = showNavBar,
                                            navType = navType,
                                            currentDestination = currentDestination,
                                            showBlur = showBlur,
                                            isAmoledMode = isAmoledMode,
                                            middleNavItem = middleNavItem,
                                            multipleActions = multipleActions,
                                            modifier = Modifier.renderInSharedTransitionScopeOverlay()
                                        )
                                    } else {
                                        HomeNavigationBar(
                                            showNavBar = showNavBar,
                                            navType = navType,
                                            currentDestination = currentDestination,
                                            showBlur = showBlur,
                                            isAmoledMode = isAmoledMode,
                                            middleNavItem = middleNavItem,
                                            //scrollBehavior = null,
                                            multipleActions = multipleActions,
                                            modifier = Modifier
                                                .padding(horizontal = 24.dp)
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .clip(MaterialTheme.shapes.extraLarge)
                                                .hazeEffect(
                                                    state = hazeState,
                                                    style = HazeMaterials.ultraThin(),
                                                )
                                                .fillMaxWidth()
                                                .renderInSharedTransitionScopeOverlay()
                                        )
                                    }
                                },
                                contentWindowInsets = WindowInsets(0.dp),
                                //blurBottomBar = showBlur && !floatingNavigation,
                            ) { innerPadding ->
                                CompositionLocalProvider(
                                    LocalNavHostPadding provides innerPadding,
                                    LocalSharedElementScope provides this@SharedTransitionLayout,
                                    //LocalHazeState provides hazeState,
                                    //For later maybe
                                    //LocalBottomAppBarScrollBehavior provides bottomAppBarScrollBehavior
                                ) {
                                    NavHost(
                                        navController = navController,
                                        //startDestination = Screen.RecentScreen,
                                        startDestination = startDestination,
                                        modifier = Modifier.fillMaxSize()
                                    ) { navGraph(customPreferences, windowSize, genericInfo, navController, notificationLogo) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FloatingNavigationBar(
        modifier: Modifier = Modifier,
        shape: Shape = MaterialTheme.shapes.extraLarge,
        containerColor: Color = NavigationBarDefaults.containerColor,
        contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
        tonalElevation: Dp = NavigationBarDefaults.Elevation,
        content: @Composable RowScope.() -> Unit,
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shape = shape,
            border = BorderStroke(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                ),
            ),
            modifier = modifier,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .height(80.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun HomeNavigationBar(
        showNavBar: Boolean,
        navType: NavigationBarType,
        currentDestination: NavDestination?,
        showBlur: Boolean,
        isAmoledMode: Boolean,
        middleNavItem: com.programmersbox.datastore.MiddleNavigationAction,
        modifier: Modifier = Modifier,
        scrollBehavior: BottomAppBarScrollBehavior? = null,
        multipleActions: com.programmersbox.datastore.MiddleMultipleActions?,
    ) {
        val scope = rememberCoroutineScope()
        AnimatedVisibility(
            visible = showNavBar && navType == NavigationBarType.Bottom,
            enter = slideInVertically { it / 2 } + expandVertically() + fadeIn(),
            exit = slideOutVertically { it / 2 } + shrinkVertically() + fadeOut(),
        ) {
            val appBarDragModifier = if (scrollBehavior != null && !scrollBehavior.isPinned) {
                Modifier.draggable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberDraggableState { delta -> scrollBehavior.state.heightOffset -= delta },
                    onDragStopped = { velocity ->
                        settleAppBarBottom(
                            scrollBehavior.state,
                            velocity,
                            scrollBehavior.flingAnimationSpec,
                            scrollBehavior.snapAnimationSpec
                        )
                    }
                )
            } else {
                Modifier
            }

            val multipleBarState = rememberMultipleBarState()

            Box(modifier = Modifier.fillMaxWidth()) {
                FloatingNavigationBar(
                    containerColor = when {
                        showBlur -> Color.Transparent
                        isAmoledMode -> MaterialTheme.colorScheme.surface
                        else -> NavigationBarDefaults.containerColor
                    },
                    modifier = modifier
                        .layout { measurable, constraints ->
                            // Sets the app bar's height offset to collapse the entire bar's height when
                            // content
                            // is scrolled.
                            scrollBehavior?.state?.heightOffsetLimit = -80.dp.toPx()

                            val placeable = measurable.measure(constraints)
                            val height = placeable.height + (scrollBehavior?.state?.heightOffset ?: 0f)
                            layout(placeable.width, height.roundToInt()) { placeable.place(0, 0) }
                        }
                        .then(appBarDragModifier),
                ) {
                    val colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )

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
                            colors = colors,
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
                        icon = if (currentDestination.isTopLevelDestinationInHierarchy(Screen.RecentScreen)) Icons.Default.History else Icons.Outlined.History,
                        label = R.string.recent
                    )

                    middleNavItem.ScreenBottomItem(
                        rowScope = this,
                        currentDestination = currentDestination,
                        colors = colors,
                        multipleClick = {
                            scope.launch {
                                if (multipleBarState.showHorizontalBar) {
                                    multipleBarState.hide()
                                } else {
                                    multipleBarState.show()
                                }
                            }
                        },
                        onClick = {
                            navController.navigate(it.screen) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    ScreenBottomItem(
                        screen = Screen.Settings,
                        icon = if (currentDestination.isTopLevelDestinationInHierarchy(Screen.Settings)) Icons.Default.Settings else Icons.Outlined.Settings,
                        label = R.string.settings,
                        badge = { if (updateCheck()) Badge { Text("") } }
                    )
                }

                multipleActions?.let {
                    MultipleActions(
                        state = multipleBarState,
                        middleNavItem = middleNavItem,
                        multipleActions = it,
                        currentDestination = currentDestination,
                        navController = navController
                    )
                }
            }
        }
    }

    @Composable
    private fun BottomNav(
        navController: NavHostController,
        showNavBar: Boolean,
        navType: NavigationBarType,
        currentDestination: NavDestination?,
        showBlur: Boolean,
        isAmoledMode: Boolean,
        middleNavItem: com.programmersbox.datastore.MiddleNavigationAction,
        multipleActions: com.programmersbox.datastore.MiddleMultipleActions?,
        modifier: Modifier = Modifier,
    ) {
        val scope = rememberCoroutineScope()
        val multipleBarState = rememberMultipleBarState()
        Box(modifier = modifier) {
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
                            icon = if (currentDestination.isTopLevelDestinationInHierarchy(Screen.RecentScreen)) Icons.Default.History else Icons.Outlined.History,
                            label = R.string.recent
                        )

                        middleNavItem.ScreenBottomItem(
                            rowScope = this,
                            currentDestination = currentDestination,
                            multipleClick = {
                                scope.launch {
                                    if (multipleBarState.showHorizontalBar) {
                                        multipleBarState.hide()
                                    } else {
                                        multipleBarState.show()
                                    }
                                }
                            },
                            onClick = {
                                navController.navigate(it.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        ScreenBottomItem(
                            screen = Screen.Settings,
                            icon = if (currentDestination.isTopLevelDestinationInHierarchy(Screen.Settings)) Icons.Default.Settings else Icons.Outlined.Settings,
                            label = R.string.settings,
                            badge = { if (updateCheck()) Badge { Text("") } }
                        )
                    }
                }
            }

            multipleActions?.let {
                MultipleActions(
                    state = multipleBarState,
                    middleNavItem = middleNavItem,
                    multipleActions = it,
                    currentDestination = currentDestination,
                    navController = navController
                )
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

    @Composable
    fun AskForNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    @Composable
    fun updateCheck(): Boolean {
        val appUpdate by updateAppCheck.collectAsStateWithLifecycle(null)
        return AppUpdate.checkForUpdate(
            appVersion(),
            appUpdate?.updateRealVersion.orEmpty()
        )
    }

    private fun setup() {
        sourceRepository.sources
            .onEach {
                val itemDao = itemDatabase.itemDao()
                it.forEachIndexed { index, sourceInformation ->
                    itemDao.insertSourceOrder(
                        SourceOrder(
                            source = sourceInformation.packageName,
                            name = sourceInformation.apiService.serviceName,
                            order = index
                        )
                    )
                }
            }
            .launchIn(lifecycleScope)

        dataStoreHandling
            .currentService
            .asFlow()
            .mapNotNull {
                if (it == null) {
                    sourceRepository
                        .list
                        .filter { it.catalog == null }
                        .randomOrNull()
                } else {
                    sourceRepository.toSourceByApiServiceName(it)
                }
            }
            .onEach { currentSourceRepository.emit(it.apiService) }
            .launchIn(lifecycleScope)

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
        it.route?.contains(destination, true) == true
    } == true

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = currentDetailsUrl.toUri()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    enum class NavigationBarType { Rail, Bottom }

    @OptIn(ExperimentalMaterial3Api::class)
    private suspend fun settleAppBarBottom(
        state: BottomAppBarState,
        velocity: Float,
        flingAnimationSpec: DecayAnimationSpec<Float>?,
        snapAnimationSpec: AnimationSpec<Float>?,
    ): Velocity {
        // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
        // and just return Zero Velocity.
        // Note that we don't check for 0f due to float precision with the collapsedFraction
        // calculation.
        if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
            return Velocity.Zero
        }
        var remainingVelocity = velocity
        // In case there is an initial velocity that was left after a previous user fling, animate to
        // continue the motion to expand or collapse the app bar.
        if (flingAnimationSpec != null && abs(velocity) > 1f) {
            var lastValue = 0f
            AnimationState(
                initialValue = 0f,
                initialVelocity = velocity,
            )
                .animateDecay(flingAnimationSpec) {
                    val delta = value - lastValue
                    val initialHeightOffset = state.heightOffset
                    state.heightOffset = initialHeightOffset + delta
                    val consumed = abs(initialHeightOffset - state.heightOffset)
                    lastValue = value
                    remainingVelocity = this.velocity
                    // avoid rounding errors and stop if anything is unconsumed
                    if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                }
        }
        // Snap if animation specs were provided.
        if (snapAnimationSpec != null) {
            if (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit) {
                AnimationState(initialValue = state.heightOffset).animateTo(
                    if (state.collapsedFraction < 0.5f) {
                        0f
                    } else {
                        state.heightOffsetLimit
                    },
                    animationSpec = snapAnimationSpec
                ) {
                    state.heightOffset = value
                }
            }
        }

        return Velocity(0f, remainingVelocity)
    }
}