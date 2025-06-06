package com.programmersbox.uiviews.presentation.navigation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.datastore.MiddleMultipleActions
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.HazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.ScreenBottomItem
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.navactions.TopLevelBackStack
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import com.programmersbox.kmpuiviews.utils.ChromeCustomTabsNavigator
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.MultipleActions
import com.programmersbox.uiviews.presentation.components.rememberMultipleBarState
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun HomeNav(
    activity: Activity,
    startDestination: Screen,
    customPreferences: ComposeSettingsDsl,
    bottomBarAdditions: @Composable () -> Unit,
    navController: NavHostController = rememberNavController(
        remember { ChromeCustomTabsNavigator(activity) }
    ),
    genericInfo: GenericInfo = koinInject(),
    settingsHandling: NewSettingsHandling = koinInject(),
    notificationLogo: NotificationLogo = koinInject(),
    appLogo: AppLogo = koinInject(),
    changingSettingsRepository: ChangingSettingsRepository = koinInject(),
) {
    val windowSize = calculateWindowSizeClass(activity = activity)

    val isAmoledMode by settingsHandling.rememberIsAmoledMode()

    val showBlur by settingsHandling.rememberShowBlur()

    //val backStack = rememberNavBackStack(startDestination)

    val backStack = remember { TopLevelBackStack<NavKey>(startDestination) }

    CompositionLocalProvider(
        LocalWindowSizeClass provides windowSize,
        LocalGenericInfo provides genericInfo
    ) {
        OtakuMaterialTheme(
            navController = navController,
            navBackStack = backStack,
            settingsHandling = settingsHandling,
        ) {
            AskForNotificationPermissions()

            val showAllItem by settingsHandling.rememberShowAll()
            val middleNavItem by settingsHandling.rememberMiddleNavigationAction()
            val multipleActions by settingsHandling.rememberMiddleMultipleActions()

            val navType = when (windowSize.widthSizeClass) {
                WindowWidthSizeClass.Expanded -> NavigationBarType.Rail
                else -> NavigationBarType.Bottom
            }

            val showNavBar by changingSettingsRepository.showNavBar.collectAsStateWithLifecycle(true)
            val floatingNavigation by rememberFloatingNavigation()
            val hazeState = remember { HazeState() }

            val navigationActions = LocalNavActions.current

            //val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

            genericInfo.DialogSetups()

            SharedTransitionLayout {
                Row(Modifier.fillMaxSize()) {
                    Rail(
                        navActions = navigationActions,
                        showNavBar = showNavBar,
                        navType = navType,
                        showAllItem = showAllItem,
                        appLogo = appLogo
                    )

                    HazeScaffold(
                        hazeState = hazeState,
                        bottomBar = {
                            if (!floatingNavigation) {
                                BottomNav(
                                    navActions = navigationActions,
                                    showNavBar = showNavBar,
                                    navType = navType,
                                    showBlur = showBlur,
                                    isAmoledMode = isAmoledMode,
                                    middleNavItem = middleNavItem,
                                    multipleActions = multipleActions,
                                    bottomBarAdditions = bottomBarAdditions,
                                    modifier = Modifier.renderInSharedTransitionScopeOverlay()
                                )
                            } else {
                                HomeNavigationBar(
                                    showNavBar = showNavBar,
                                    navType = navType,
                                    showBlur = showBlur,
                                    isAmoledMode = isAmoledMode,
                                    middleNavItem = middleNavItem,
                                    //scrollBehavior = null,
                                    multipleActions = multipleActions,
                                    navActions = navigationActions,
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
                            NavigationGraph(
                                backStack = backStack.backStack,
                                navigationActions = navigationActions,
                                genericInfo = genericInfo,
                                windowSize = windowSize,
                                customPreferences = customPreferences,
                                notificationLogo = notificationLogo,
                                startDestination = startDestination,
                                navController = navController
                            )
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
    navActions: NavigationActions,
    navType: NavigationBarType,
    showBlur: Boolean,
    isAmoledMode: Boolean,
    middleNavItem: MiddleNavigationAction,
    modifier: Modifier = Modifier,
    scrollBehavior: BottomAppBarScrollBehavior? = null,
    multipleActions: MiddleMultipleActions?,
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
                        selected = navActions.currentDestination(screen),
                        colors = colors,
                        onClick = { navActions.homeScreenNavigate(screen) }
                    )
                }

                ScreenBottomItem(
                    screen = Screen.RecentScreen,
                    icon = if (navActions.currentDestination(Screen.RecentScreen)) Icons.Default.History else Icons.Outlined.History,
                    label = R.string.recent
                )

                middleNavItem.ScreenBottomItem(
                    rowScope = this,
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
                    onClick = { navActions.homeScreenNavigate(it.screen) }
                )

                ScreenBottomItem(
                    screen = Screen.Settings,
                    icon = if (navActions.currentDestination(Screen.Settings)) Icons.Default.Settings else Icons.Outlined.Settings,
                    label = R.string.settings,
                    badge = { if (updateCheck()) Badge { Text("") } }
                )
            }

            multipleActions?.let {
                MultipleActions(
                    state = multipleBarState,
                    middleNavItem = middleNavItem,
                    multipleActions = it,
                    navigationActions = navActions
                )
            }
        }
    }
}

@Composable
private fun BottomNav(
    navActions: NavigationActions,
    showNavBar: Boolean,
    navType: NavigationBarType,
    showBlur: Boolean,
    isAmoledMode: Boolean,
    middleNavItem: MiddleNavigationAction,
    multipleActions: MiddleMultipleActions?,
    bottomBarAdditions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val multipleBarState = rememberMultipleBarState()
    Box(modifier = modifier) {
        Column {
            bottomBarAdditions()
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
                            selected = navActions.currentDestination(screen),
                            onClick = { navActions.homeScreenNavigate(screen) }
                        )
                    }

                    ScreenBottomItem(
                        screen = Screen.RecentScreen,
                        icon = if (navActions.currentDestination(Screen.RecentScreen)) Icons.Default.History else Icons.Outlined.History,
                        label = R.string.recent
                    )

                    middleNavItem.ScreenBottomItem(
                        rowScope = this,
                        multipleClick = {
                            scope.launch {
                                if (multipleBarState.showHorizontalBar) {
                                    multipleBarState.hide()
                                } else {
                                    multipleBarState.show()
                                }
                            }
                        },
                        onClick = { navActions.homeScreenNavigate(it.screen) }
                    )

                    ScreenBottomItem(
                        screen = Screen.Settings,
                        icon = if (navActions.currentDestination(Screen.Settings)) Icons.Default.Settings else Icons.Outlined.Settings,
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
                navigationActions = navActions
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Rail(
    navActions: NavigationActions,
    showNavBar: Boolean,
    navType: NavigationBarType,
    showAllItem: Boolean,
    appLogo: AppLogo,
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
                navigationActions = navActions
            )

            AnimatedVisibility(visible = showAllItem) {
                NavigationRailItem(
                    imageVector = Icons.Default.BrowseGallery,
                    label = stringResource(R.string.all),
                    screen = Screen.AllScreen,
                    navigationActions = navActions
                )
            }

            NavigationRailItem(
                imageVector = Icons.Default.Notifications,
                label = stringResource(R.string.notifications),
                screen = Screen.NotificationScreen,
                navigationActions = navActions
            )

            NavigationRailItem(
                imageVector = Icons.AutoMirrored.Default.List,
                label = stringResource(R.string.custom_lists_title),
                screen = Screen.CustomListScreen,
                navigationActions = navActions
            )

            NavigationRailItem(
                imageVector = Icons.Default.Search,
                label = stringResource(R.string.global_search),
                screen = Screen.GlobalSearchScreen(),
                navigationActions = navActions
            )

            NavigationRailItem(
                imageVector = Icons.Default.Star,
                label = stringResource(R.string.viewFavoritesMenu),
                screen = Screen.FavoriteScreen,
                navigationActions = navActions
            )

            NavigationRailItem(
                imageVector = Icons.Default.Extension,
                label = stringResource(R.string.extensions),
                screen = Screen.ExtensionListScreen,
                navigationActions = navActions
            )

            NavigationRailItem(
                icon = {
                    BadgedBox(
                        badge = { if (updateCheck()) Badge { Text("") } }
                    ) { Icon(Icons.Default.Settings, null) }
                },
                label = { Text(stringResource(R.string.settings)) },
                selected = navActions.currentDestination(Screen.Settings),
                onClick = { navActions.homeScreenNavigate(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun NavigationRailItem(
    imageVector: ImageVector,
    label: String,
    screen: Screen,
    navigationActions: NavigationActions,
    onClick: () -> Unit = { navigationActions.homeScreenNavigate(screen) },
) {
    NavigationRailItem(
        icon = { Icon(imageVector, null) },
        label = { Text(label) },
        selected = navigationActions.currentDestination(screen),
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
fun updateCheck(
    appUpdateCheck: AppUpdateCheck = koinInject(),
): Boolean {
    val appUpdate by appUpdateCheck
        .updateAppCheck
        .collectAsStateWithLifecycle(null)
    return AppUpdate.checkForUpdate(
        appVersion(),
        appUpdate?.updateRealVersion.orEmpty()
    )
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: Screen) = isTopLevelDestinationInHierarchy(destination.route)

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: String) = this?.hierarchy?.any {
    it.route?.contains(destination, true) == true
} == true


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