package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.koogintegration.Koog
import com.programmersbox.koogintegration.KoogSettings
import com.programmersbox.koogintegration.screens.chatscreen.ChatScreen
import com.programmersbox.koogintegration.screens.chatscreen.KoogNavigation
import com.programmersbox.koogintegration.screens.settings.KoogSettingsScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

actual fun EntryProviderScope<NavKey>.buildPlatformPaths() {
    entry<KoogSettings> {
        val navigationActions = LocalNavActions.current
        KoogSettingsScreen(
            onBack = { navigationActions.popBackStack() }
        )
    }
    entry<Koog> {
        val navigationActions = LocalNavActions.current
        HideNavBarWhileOnScreen()
        ChatScreen(
            viewModel = koinViewModel { parametersOf("otaku_agent") },
            koogNavigation = KoogNavigation(
                onBack = { navigationActions.popBackStack() },
                onKoogSettingsClick = { navigationActions.navigate(KoogSettings) },
                onSearchClick = { navigationActions.globalSearch(it) },
                onListClick = { navigationActions.customList() }
            )
        )
    }
}