package com.programmersbox.uiviews.utils.customsettings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.programmersbox.uiviews.MiddleNavigationAction
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.Screen

data class MiddleNavigationItem(
    val screen: Screen,
    val icon: (Boolean) -> ImageVector,
    val label: Int,
)

val MiddleNavigationAction.visibleName get() = if (this == MiddleNavigationAction.UNRECOGNIZED) "None" else name

val MiddleNavigationAction.item: MiddleNavigationItem?
    get() = when (this) {
        MiddleNavigationAction.All -> MiddleNavigationItem(
            screen = Screen.AllScreen,
            icon = { if (it) Icons.Default.BrowseGallery else Icons.Outlined.BrowseGallery },
            label = R.string.all
        )

        MiddleNavigationAction.Notifications -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.Notifications else Icons.Outlined.Notifications },
            label = R.string.notifications,
            screen = Screen.NotificationScreen.Home,
        )

        MiddleNavigationAction.Lists -> MiddleNavigationItem(
            icon = { if (it) Icons.AutoMirrored.Default.List else Icons.AutoMirrored.Outlined.List },
            label = R.string.custom_lists_title,
            screen = Screen.CustomListScreen.Home,
        )

        MiddleNavigationAction.Favorites -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.Star else Icons.Outlined.StarOutline },
            label = R.string.viewFavoritesMenu,
            screen = Screen.FavoriteScreen.Home,
        )

        MiddleNavigationAction.Search -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.Search else Icons.Outlined.Search },
            label = R.string.global_search,
            screen = Screen.GlobalSearchScreen.Home(),
        )

        MiddleNavigationAction.Multiple -> MiddleNavigationItem(
            icon = { Icons.Default.UnfoldMore },
            label = R.string.more,
            screen = Screen.MoreSettings,
        )

        else -> null
    }

@Composable
fun MiddleNavigationItem.ScreenBottomItem(
    rowScope: RowScope,
    currentDestination: NavDestination?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
) {
    with(rowScope) {
        NavigationBarItem(
            icon = { Icon(icon(currentDestination.isTopLevelDestinationInHierarchy(screen)), null) },
            label = { Text(stringResource(label)) },
            selected = currentDestination.isTopLevelDestinationInHierarchy(screen),
            colors = colors,
            onClick = {
                navController.navigate(screen) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = modifier
        )
    }
}

@Composable
fun MiddleNavigationItem.ScreenBottomItem(
    currentDestination: NavDestination?,
    navController: NavHostController,
    additionalOnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = {
            additionalOnClick()
            navController.navigate(screen) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        modifier = modifier
    ) { Icon(icon(currentDestination.isTopLevelDestinationInHierarchy(screen)), null) }
}

@Composable
fun MiddleNavigationAction.ScreenBottomItem(
    rowScope: RowScope,
    currentDestination: NavDestination?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
    multipleClick: () -> Unit,
) {
    if (this == MiddleNavigationAction.Multiple) {
        with(rowScope) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.UnfoldMore, null) },
                label = { Text("More") },
                selected = false,
                colors = colors,
                onClick = multipleClick
            )
        }
    } else {
        item?.ScreenBottomItem(rowScope, currentDestination, navController, modifier, colors)
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: Screen) = isTopLevelDestinationInHierarchy(destination.route)

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: String) = this?.hierarchy?.any {
    it.route?.contains(destination, true) ?: false
} ?: false
