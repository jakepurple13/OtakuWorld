package com.programmersbox.kmpuiviews.presentation.navactions

import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
