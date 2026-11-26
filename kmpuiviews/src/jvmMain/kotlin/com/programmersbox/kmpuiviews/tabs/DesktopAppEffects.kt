package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.InitialSetup
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.notifier.InAppBannerHost
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsOverlay
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope
import com.programmersbox.supabaseintegration.sync.SyncManager
import org.koin.compose.koinInject

/**
 * What `HomeNav` ran once, for the desktop tab shell. Composed by the main window only, so it runs once per app
 * however many windows are open: setup, sync and the app's dialogs.
 */
@Composable
fun DesktopAppOnce(genericInfo: KmpGenericInfo = koinInject()) {
    InitialSetup()

    val supabaseSync = koinInject<SyncManager>()
    LaunchedEffect(Unit) { supabaseSync.start() }

    genericInfo.DialogSetups()
}

/**
 * One window's share of what `HomeNav` provided: the window-size and shared-element locals, and, in the window that
 * [hostsOverlays] (the focused one), the quick-actions overlay and in-app banners, so they show once, where the user
 * is.
 *
 * The app theme comes from the window (`OtakuDecoratedWindow`). AMOLED and blur only styled
 * `HomeNav`'s bottom bar, which desktop never showed.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DesktopWindowEffects(
    windowSize: WindowSizeClass,
    hostsOverlays: Boolean,
    content: @Composable SharedTransitionScope.() -> Unit,
) {
    CompositionLocalProvider(LocalWindowSizeClass provides windowSize) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedElementScope provides this) {
                Box(Modifier.fillMaxSize()) {
                    content()

                    if (hostsOverlays) {
                        CompositionLocalProvider(LocalNavHostPadding provides PaddingValues()) {
                            QuickActionsOverlay()
                        }

                        InAppBannerHost()
                    }
                }
            }
        }
    }
}
