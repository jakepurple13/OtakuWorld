package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions

/**
 * A desktop tab's navigation. Every navigation goes into [history], and the back stack shown is
 * rebuilt from it, so back and forward step through visited pages like a browser.
 */
class TabNavigation3Actions(start: NavKey, railKeys: Set<NavKey> = setOf(start)) : Navigation3Actions(start) {
    val history = TabHistory(start)

    // Pages opened from the home rail, for the rail highlight. Seeded so the start page highlights at launch.
    private val railKeys = railKeys.toMutableSet()

    private fun sync() = navBackStack.replaceStack(history.backStack())

    override fun push(key: NavKey) {
        record(key)
    }

    /** Returns false when the interceptor handled [key] and nothing was recorded. */
    private fun record(key: NavKey): Boolean {
        if (interceptor?.intercept(this, key) == true) return false
        history.push(key)
        sync()
        return true
    }

    fun forward() {
        history.forward()
        sync()
    }

    fun jumpTo(index: Int) {
        history.jumpTo(index)
        sync()
    }

    fun restoreHistory(entries: List<NavKey>, index: Int) {
        history.restore(entries, index)
        sync()
    }

    override fun recent() = push(Screen.RecentScreen)

    override fun settings() = push(Screen.Settings)

    override fun <T : Any> homeScreenNavigate(nav: T) {
        if (nav is NavKey && record(nav)) railKeys += nav
    }

    override fun toOnboarding() = push(Screen.OnboardingScreen)

    override fun <T : Any> clearBackStack(nav: T) {
        if (nav is NavKey) push(nav)
    }

    override fun onboardingToRecent() {
        history.reset(Screen.RecentScreen)
        sync()
    }

    override fun popBackStack() {
        history.back()
        sync()
    }

    override fun popBackStack(route: Any, inclusive: Boolean) {
        if (route !is NavKey) return
        history.popTo(route, inclusive)
        sync()
    }

    override fun remove(route: NavKey) {
        history.remove(route)
        sync()
    }

    @Composable
    override fun currentDestination(screen: Screen): Boolean =
        screen == history.entries.take(history.index + 1).lastOrNull { it in railKeys }
}
