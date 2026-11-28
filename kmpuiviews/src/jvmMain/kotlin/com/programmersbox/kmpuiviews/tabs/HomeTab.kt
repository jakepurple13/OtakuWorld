package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.sharedcomponents.repository.ChangingSettingsRepository
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
 * The pinned Home tab: the Home sidebar, then the Home pages. The sidebar's Home section (Recent, All,
 * Favorites, Extensions) navigates inside Home; its Open in tab section (Notifications, Lists, Search,
 * Settings) opens each in its own tab.
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
    itemDao: ItemDao = koinInject(),
) {
    val showNavBar by changingSettingsRepository.showNavBar.collectAsStateWithLifecycle(true)
    val showAllItem by settingsHandling.rememberShowAll()
    val notificationCount by itemDao.getAllNotificationCount().collectAsStateWithLifecycle(0)
    var collapsed by homeSidebarCollapsedPref.asState()
    val homeItems = remember(showAllItem) { homeSidebarItems(showAllItem) }
    // currentDestination is composable, so the highlighted page is worked out here, not in the sidebar.
    val selectedKey = homeItems.firstOrNull { tab.nav.currentDestination(it.key as Screen) }?.key

    Row(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showNavBar,
            enter = slideInHorizontally { it / 2 } + expandHorizontally() + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + shrinkHorizontally() + fadeOut(),
        ) {
            HomeSidebar(
                homeItems = homeItems,
                tabItems = TabSidebarItems,
                selected = { key -> key == selectedKey },
                notificationCount = notificationCount,
                collapsed = collapsed,
                onToggleCollapsed = { collapsed = !collapsed },
                onHome = { key -> tab.nav.homeScreenNavigate(key) },
                onOpenTab = { key -> controller.open(key, TabOpenMode.Foreground, tab.id) },
            )
        }

        // Clipped: a page sliding out during a switch would otherwise overlap the sidebar and take its clicks.
        Box(Modifier.weight(1f).clipToBounds()) {
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

private fun homeSidebarItems(showAllItem: Boolean): List<SidebarItem> = listOfNotNull(
    SidebarItem(Screen.RecentScreen, Res.string.recent, Icons.Default.History),
    SidebarItem(Screen.AllScreen, Res.string.all, Icons.Default.BrowseGallery).takeIf { showAllItem },
    SidebarItem(Screen.FavoriteScreen, Res.string.viewFavoritesMenu, Icons.Default.Star),
    SidebarItem(Screen.ExtensionListScreen, Res.string.extensions, Icons.Default.Extension),
)

// The update badge lives on the toolbar's Settings button.
private val TabSidebarItems = listOf(
    SidebarItem(Screen.NotificationScreen, Res.string.notifications, Icons.Default.Notifications),
    SidebarItem(Screen.CustomListScreen, Res.string.custom_lists_title, Icons.AutoMirrored.Default.List),
    SidebarItem(Screen.GlobalSearchScreen(), Res.string.global_search, Icons.Default.Search),
    SidebarItem(Screen.Settings, Res.string.settings, Icons.Default.Settings),
)
