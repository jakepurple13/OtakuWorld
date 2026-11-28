package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.di.navigationModule
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.NavInterceptor
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TabNavigation3ActionsTest {
    private val r = Screen.RecentScreen
    private val nav = TabNavigation3Actions(r)

    private fun title(name: String) = Screen.DetailsScreen.Details(
        title = name,
        description = "d",
        url = "url-$name",
        imageUrl = "img-$name",
        source = "src",
    )

    @Test
    fun pushRecordsAndRebuildsBackStack() {
        nav.navigate(Screen.HistoryScreen)
        assertEquals(listOf(r, Screen.HistoryScreen), nav.history.entries)
        assertEquals(listOf(r, Screen.HistoryScreen), nav.backStack.toList())
    }

    @Test
    fun popBackStackStepsBackAndKeepsForward() {
        nav.navigate(Screen.HistoryScreen)
        nav.popBackStack()
        assertEquals(listOf(r), nav.backStack.toList())
        assertTrue(nav.history.canGoForward)
        nav.forward()
        assertEquals(listOf(r, Screen.HistoryScreen), nav.backStack.toList())
    }

    @Test
    fun settingsAndRecentNavigateInPlace() {
        nav.settings()
        nav.recent()
        assertEquals(listOf(r, Screen.Settings, r), nav.history.entries)
        nav.popBackStack()
        assertEquals(Screen.Settings, nav.history.current)
    }

    @Test
    fun navigatingBackToEarlierPageDoesNotDuplicateStackKeys() {
        nav.settings()
        nav.recent()
        assertEquals(listOf(Screen.Settings, r), nav.backStack.toList())
    }

    @Test
    fun interceptedPushRecordsNothing() {
        nav.interceptor = NavInterceptor { _, _ -> true }
        nav.navigate(Screen.FavoriteScreen)
        assertEquals(listOf(r), nav.history.entries)
    }

    @Test
    fun popToRouteJumps() {
        val a = title("A")
        val b = title("B")
        nav.navigate(a)
        nav.navigate(b)
        nav.popBackStack(a, inclusive = false)
        assertEquals(a, nav.history.current)
    }

    @Test
    fun homeRailSwitchesAreHistoryAndHighlightLatestRailPage() = runComposeUiTest {
        nav.homeScreenNavigate(r)
        nav.homeScreenNavigate(Screen.FavoriteScreen)
        nav.navigate(title("X"))
        assertEquals(listOf(r, Screen.FavoriteScreen, title("X")), nav.history.entries)
        var favorite: Boolean? = null
        var recent: Boolean? = null
        setContent {
            favorite = nav.currentDestination(Screen.FavoriteScreen)
            recent = nav.currentDestination(r)
        }
        waitForIdle()
        assertEquals(true, favorite)
        assertEquals(false, recent)
    }

    @Test
    fun onboardingToRecentResetsHistory() {
        nav.toOnboarding()
        nav.onboardingToRecent()
        assertEquals(listOf(r), nav.history.entries)
        assertFalse(nav.history.canGoBack)
    }

    @Test
    fun koinHomeNavIsTabNavigation() {
        val koin = koinApplication { modules(navigationModule, tabNavigationModule) }.koin
        assertTrue(koin.get<Navigation3Actions>() is TabNavigation3Actions)
        assertSame(koin.get<Navigation3Actions>(), koin.get<NavigationActions>())
    }
}
