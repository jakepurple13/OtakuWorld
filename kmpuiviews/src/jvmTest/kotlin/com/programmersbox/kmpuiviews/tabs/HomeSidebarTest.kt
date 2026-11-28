package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.extensions
import otakuworld.kmpuiviews.generated.resources.global_search
import otakuworld.kmpuiviews.generated.resources.notifications
import otakuworld.kmpuiviews.generated.resources.recent
import otakuworld.kmpuiviews.generated.resources.settings
import otakuworld.kmpuiviews.generated.resources.viewFavoritesMenu
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HomeSidebarTest {
    private val homeItems = listOf(
        SidebarItem(Screen.RecentScreen, Res.string.recent, Icons.Default.History),
        SidebarItem(Screen.FavoriteScreen, Res.string.viewFavoritesMenu, Icons.Default.Star),
        SidebarItem(Screen.ExtensionListScreen, Res.string.extensions, Icons.Default.Extension),
    )
    private val tabItems = listOf(
        SidebarItem(Screen.NotificationScreen, Res.string.notifications, Icons.Default.Notifications),
        SidebarItem(Screen.CustomListScreen, Res.string.custom_lists_title, Icons.AutoMirrored.Default.List),
        SidebarItem(Screen.GlobalSearchScreen(), Res.string.global_search, Icons.Default.Search),
        SidebarItem(Screen.Settings, Res.string.settings, Icons.Default.Settings),
    )

    private val homeCalls = mutableStateListOf<NavKey>()
    private val tabCalls = mutableStateListOf<NavKey>()

    private fun ComposeUiTest.sidebar(
        collapsed: Boolean = false,
        notificationCount: Int = 0,
        home: List<SidebarItem> = homeItems,
        tabs: List<SidebarItem> = tabItems,
        modifier: Modifier = Modifier,
    ) = setContent {
        HomeSidebar(
            homeItems = home,
            tabItems = tabs,
            selected = { it == Screen.RecentScreen },
            notificationCount = notificationCount,
            collapsed = collapsed,
            onToggleCollapsed = {},
            onHome = { homeCalls += it },
            onOpenTab = { tabCalls += it },
            modifier = modifier,
        )
    }

    @Test
    fun homeRowNavigatesInHome() = runComposeUiTest {
        sidebar()
        onNodeWithText("Favorites").performClick()
        assertEquals(listOf<NavKey>(Screen.FavoriteScreen), homeCalls.toList())
        assertEquals(emptyList(), tabCalls.toList())
    }

    @Test
    fun tabRowOpensTab() = runComposeUiTest {
        sidebar()
        onNodeWithText("Lists").performClick()
        assertEquals(listOf<NavKey>(Screen.CustomListScreen), tabCalls.toList())
        assertEquals(emptyList(), homeCalls.toList())
    }

    @Test
    fun homeRowNewTabButtonShowsOnHoverWhenExpanded() = runComposeUiTest {
        sidebar()
        onNodeWithContentDescription("Open Recent in new tab").assertDoesNotExist()
        onNodeWithText("Recent").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithContentDescription("Open Recent in new tab").assertIsDisplayed()
            .performMouseInput { click() }
        waitForIdle()
        assertEquals(listOf<NavKey>(Screen.RecentScreen), tabCalls.toList())
        assertEquals(emptyList(), homeCalls.toList())
    }

    @Test
    fun homeRowNewTabButtonHiddenWhenCollapsed() = runComposeUiTest {
        sidebar(collapsed = true)
        onNodeWithContentDescription("Recent").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithContentDescription("Open Recent in new tab").assertDoesNotExist()
    }

    @Test
    fun badgeHiddenAtZero() = runComposeUiTest {
        sidebar(notificationCount = 0)
        onNodeWithText("0").assertDoesNotExist()
        onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun badgeShowsCountExpanded() = runComposeUiTest {
        sidebar(notificationCount = 3)
        onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun badgeShowsCountCollapsed() = runComposeUiTest {
        sidebar(collapsed = true, notificationCount = 3)
        onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun badgeCapsAt99PlusExpanded() = runComposeUiTest {
        sidebar(notificationCount = 120)
        onNodeWithText("99+").assertIsDisplayed()
    }

    @Test
    fun badgeCapsAt9PlusCollapsed() = runComposeUiTest {
        sidebar(collapsed = true, notificationCount = 12)
        onNodeWithText("9+").assertIsDisplayed()
        onNodeWithText("12").assertDoesNotExist()
    }

    @Test
    fun collapsedHidesLabelsAndSectionTitles() = runComposeUiTest {
        sidebar(collapsed = true)
        onNodeWithText("Open in tab").assertDoesNotExist()
        onNodeWithText("Home").assertDoesNotExist()
        onNodeWithText("Lists").assertDoesNotExist()
        onNodeWithContentDescription("Expand").assertIsDisplayed()
    }

    @Test
    fun expandedShowsSectionTitlesAndCollapse() = runComposeUiTest {
        sidebar()
        onNodeWithText("Home").assertIsDisplayed()
        onNodeWithText("Open in tab").assertIsDisplayed()
        onNodeWithText("Collapse").assertIsDisplayed()
    }

    @Test
    fun listScrollsWhenItemsOverflow() = runComposeUiTest {
        val many = List(19) { SidebarItem(Screen.GlobalSearchScreen("q$it"), Res.string.recent, Icons.Default.History) } +
            SidebarItem(Screen.Settings, Res.string.settings, Icons.Default.Settings)
        sidebar(home = many, tabs = emptyList(), modifier = Modifier.height(300.dp))
        // Off screen, the lazy list has not composed it.
        onNodeWithText("Settings").assertDoesNotExist()
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Settings"))
        onNodeWithText("Settings").assertIsDisplayed()
    }
}
