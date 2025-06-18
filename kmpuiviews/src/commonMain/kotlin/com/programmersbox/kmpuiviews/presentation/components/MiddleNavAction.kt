package com.programmersbox.kmpuiviews.presentation.components

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
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.all_kmp
import otakuworld.kmpuiviews.generated.resources.custom_lists_title_kmp
import otakuworld.kmpuiviews.generated.resources.global_search_kmp
import otakuworld.kmpuiviews.generated.resources.more_kmp
import otakuworld.kmpuiviews.generated.resources.notifications_kmp
import otakuworld.kmpuiviews.generated.resources.viewFavoritesMenu_kmp

data class MiddleNavigationItem(
    val screen: Screen,
    val icon: (Boolean) -> ImageVector,
    val label: StringResource,
)

val MiddleNavigationAction.visibleName get() = name

val MiddleNavigationAction.item: MiddleNavigationItem?
    get() = when (this) {
        MiddleNavigationAction.All -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.BrowseGallery else Icons.Outlined.BrowseGallery },
            label = Res.string.all_kmp,
            screen = Screen.AllScreen,
        )

        MiddleNavigationAction.Notifications -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.Notifications else Icons.Outlined.Notifications },
            label = Res.string.notifications_kmp,
            screen = Screen.NotificationScreen,
        )

        MiddleNavigationAction.Lists -> MiddleNavigationItem(
            icon = { if (it) Icons.AutoMirrored.Default.List else Icons.AutoMirrored.Outlined.List },
            label = Res.string.custom_lists_title_kmp,
            screen = Screen.CustomListScreen,
        )

        MiddleNavigationAction.Favorites -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.Star else Icons.Outlined.StarOutline },
            label = Res.string.viewFavoritesMenu_kmp,
            screen = Screen.FavoriteScreen,
        )

        MiddleNavigationAction.Search -> MiddleNavigationItem(
            icon = { if (it) Icons.Default.Search else Icons.Outlined.Search },
            label = Res.string.global_search_kmp,
            screen = Screen.GlobalSearchScreen(),
        )

        MiddleNavigationAction.Multiple -> MiddleNavigationItem(
            icon = { Icons.Default.UnfoldMore },
            label = Res.string.more_kmp,
            screen = Screen.MoreSettings,
        )

        else -> null
    }

@Composable
fun MiddleNavigationItem.ScreenBottomItem(
    rowScope: RowScope,
    onClick: (MiddleNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
) {
    val navActions = LocalNavActions.current
    with(rowScope) {
        NavigationBarItem(
            icon = { Icon(icon(navActions.currentDestination(screen)), null) },
            label = { Text(stringResource(label)) },
            selected = navActions.currentDestination(screen),
            colors = colors,
            onClick = { onClick(this@ScreenBottomItem) },
            modifier = modifier
        )
    }
}

@Composable
fun MiddleNavigationItem.ScreenBottomItem(
    onClick: (MiddleNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navActions = LocalNavActions.current
    IconButton(
        onClick = { onClick(this) },
        modifier = modifier
    ) { Icon(icon(navActions.currentDestination(screen)), null) }
}

@Composable
fun MiddleNavigationAction.ScreenBottomItem(
    rowScope: RowScope,
    modifier: Modifier = Modifier,
    onClick: (MiddleNavigationItem) -> Unit,
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
        item?.ScreenBottomItem(
            rowScope = rowScope,
            onClick = onClick,
            modifier = modifier,
            colors = colors
        )
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: Screen) = isTopLevelDestinationInHierarchy(destination.route)

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: String) = this?.hierarchy?.any {
    it.route?.contains(destination, true) ?: false
} ?: false
