package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.sharedcomponents.repository.ChangingSettingsRepository
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.all
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.extensions
import otakuworld.kmpuiviews.generated.resources.global_search
import otakuworld.kmpuiviews.generated.resources.notifications
import otakuworld.kmpuiviews.generated.resources.recent
import otakuworld.kmpuiviews.generated.resources.settings
import otakuworld.kmpuiviews.generated.resources.viewFavoritesMenu

/**
 * The pinned Home tab: today's desktop rail, then the Home pages. Recent, All, Favorites and
 * Extensions navigate inside Home; Notifications, Lists, Search and Settings open their own tabs.
 */
@Composable
fun HomeTab(
    tab: TabState,
    controller: TabsController,
    stateHolder: SaveableStateHolder,
    genericInfo: KmpGenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
    settingsHandling: NewSettingsHandling = koinInject(),
    changingSettingsRepository: ChangingSettingsRepository = koinInject(),
) {
    val showNavBar by changingSettingsRepository.showNavBar.collectAsStateWithLifecycle(true)
    val showAllItem by settingsHandling.rememberShowAll()

    Row(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showNavBar,
            enter = slideInHorizontally { it / 2 } + expandHorizontally() + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + shrinkHorizontally() + fadeOut(),
        ) {
            HomeSideNav(
                tab = tab,
                showAllItem = showAllItem,
                openTab = { key -> controller.open(key, TabOpenMode.Foreground, tab.id) },
            )
        }

        Box(Modifier.weight(1f)) {
            TabContent(
                tab = tab,
                stateHolder = stateHolder,
                genericInfo = genericInfo,
                windowSize = windowSize,
                customPreferences = customPreferences,
                motion = controller.motion,
            )
        }
    }
}

@Composable
private fun HomeSideNav(
    tab: TabState,
    showAllItem: Boolean,
    openTab: (NavKey) -> Unit,
) {
    val navActions = tab.nav
    NavigationRail(
        header = { Image(painterLogo(), null, modifier = Modifier.size(64.dp)) },
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeItem(Icons.Default.History, stringResource(Res.string.recent), navActions.currentDestination(Screen.RecentScreen)) {
            navActions.homeScreenNavigate(Screen.RecentScreen)
        }

        AnimatedVisibility(visible = showAllItem) {
            HomeItem(Icons.Default.BrowseGallery, stringResource(Res.string.all), navActions.currentDestination(Screen.AllScreen)) {
                navActions.homeScreenNavigate(Screen.AllScreen)
            }
        }

        HomeItem(Icons.Default.Notifications, stringResource(Res.string.notifications), false) {
            openTab(Screen.NotificationScreen)
        }

        HomeItem(Icons.AutoMirrored.Default.List, stringResource(Res.string.custom_lists_title), false) {
            openTab(Screen.CustomListScreen)
        }

        HomeItem(Icons.Default.Search, stringResource(Res.string.global_search), false) {
            openTab(Screen.GlobalSearchScreen())
        }

        HomeItem(
            Icons.Default.Star,
            stringResource(Res.string.viewFavoritesMenu),
            navActions.currentDestination(Screen.FavoriteScreen),
        ) {
            navActions.homeScreenNavigate(Screen.FavoriteScreen)
        }

        HomeItem(
            Icons.Default.Extension,
            stringResource(Res.string.extensions),
            navActions.currentDestination(Screen.ExtensionListScreen),
        ) {
            navActions.homeScreenNavigate(Screen.ExtensionListScreen)
        }

        // The update badge lives on the toolbar's Settings button.
        HomeItem(Icons.Default.Settings, stringResource(Res.string.settings), false) {
            openTab(Screen.Settings)
        }
    }
}

@Composable
private fun HomeItem(
    imageVector: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationRailItem(
        icon = { Icon(imageVector, null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
    )
}
