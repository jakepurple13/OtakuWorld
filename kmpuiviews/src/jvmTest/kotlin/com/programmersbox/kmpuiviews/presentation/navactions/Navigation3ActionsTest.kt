package com.programmersbox.kmpuiviews.presentation.navactions

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Navigation3ActionsTest {

    @Test fun `popBackStack to the root keeps the root`() {
        val actions = Navigation3Actions()
        actions.favorites()
        actions.debug()

        actions.popBackStack(Screen.RecentScreen, false)

        assertEquals(listOf(Screen.RecentScreen), actions.navBackStack.backStack.toList())
    }

    @Test fun `popBackStack inclusive never removes the root`() {
        val actions = Navigation3Actions()
        actions.favorites()

        actions.popBackStack(Screen.RecentScreen, true)

        assertEquals(listOf(Screen.RecentScreen), actions.navBackStack.backStack.toList())
    }

    @Test fun `popBackStack honours inclusive`() {
        val actions = Navigation3Actions()
        actions.favorites()
        actions.debug()
        actions.history()

        actions.popBackStack(Screen.DebugScreen, false)
        assertEquals(
            listOf(Screen.RecentScreen, Screen.FavoriteScreen, Screen.DebugScreen),
            actions.navBackStack.backStack.toList(),
        )

        actions.popBackStack(Screen.FavoriteScreen, true)
        assertEquals(listOf(Screen.RecentScreen), actions.navBackStack.backStack.toList())
    }

    @Test fun `popBackStack to a missing route does nothing`() {
        val actions = Navigation3Actions()
        actions.favorites()

        actions.popBackStack(Screen.DebugScreen, false)

        assertEquals(listOf(Screen.RecentScreen, Screen.FavoriteScreen), actions.navBackStack.backStack.toList())
    }

    @Test fun `popBackStack to the root drops other top level stacks and survives later navigation`() {
        val actions = Navigation3Actions()
        actions.settings()
        actions.debug()

        actions.popBackStack(Screen.RecentScreen, false)
        assertEquals(listOf(Screen.RecentScreen), actions.navBackStack.backStack.toList())
        assertEquals(Screen.RecentScreen, actions.navBackStack.topLevelKey)

        // The change lives in the top level stacks, so the next navigation doesn't bring entries back
        actions.favorites()
        assertEquals(listOf(Screen.RecentScreen, Screen.FavoriteScreen), actions.navBackStack.backStack.toList())
    }

    @Test fun `interceptorTrueBlocksPush`() {
        val actions = Navigation3Actions()
        val seen = mutableListOf<NavKey>()
        actions.interceptor = NavInterceptor { _, key -> seen += key; true }

        actions.details("t", "d", "u", "i", "s")

        assertEquals(listOf<NavKey>(Screen.RecentScreen), actions.backStack.toList())
        assertTrue(seen.single() is Screen.DetailsScreen.Details)
    }

    @Test fun `interceptorFalseStillPushes`() {
        val actions = Navigation3Actions()
        actions.interceptor = NavInterceptor { _, _ -> false }

        actions.details("t", "d", "u", "i", "s")

        assertTrue(actions.backStack.last() is Screen.DetailsScreen.Details)
    }

    @Test fun `topLevelNotIntercepted`() {
        val actions = Navigation3Actions()
        actions.interceptor = NavInterceptor { _, _ -> true }

        actions.homeScreenNavigate(Screen.AllScreen)

        assertEquals(Screen.AllScreen, actions.navBackStack.topLevelKey)
    }

    @Test fun `startKeyIsRoot`() {
        assertEquals(listOf<NavKey>(Screen.Settings), Navigation3Actions(Screen.Settings).backStack.toList())
    }
}
