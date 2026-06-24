package com.programmersbox.supabaseintegration.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.programmersbox.supabaseintegration.di.SupabaseActions
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object SupabaseConfigRoute : NavKey

@Serializable
data object AuthRoute : NavKey

@Serializable
data object SyncStatusRoute : NavKey

@Serializable
data object BackupRestoreRoute : NavKey

@Serializable
data object SupabaseRoutes : NavKey

fun EntryProviderScope<NavKey>.supabaseRoutes(
    hideComposable: @Composable () -> Unit,
) {
    entry<SupabaseConfigRoute> {
        hideComposable()
        SupabaseConfigScreen()
    }
    entry<AuthRoute> {
        hideComposable()
        AuthScreen()
    }
    entry<SyncStatusRoute> {
        hideComposable()
        SyncStatusScreen()
    }
    entry<BackupRestoreRoute> {
        hideComposable()
        BackupRestoreScreen(
            getLocalDbPath = { "" }
        )
    }
    entry<SupabaseRoutes> {
        val actions = koinInject<SupabaseActions>()
        SupabaseSettingsScreen(
            onNavigate = actions.onNavigate
        )
    }
}

@Composable
fun BackButton() {
    val navEvent = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher

    val navInput = remember { DirectNavigationEventInput() }

    DisposableEffect(Unit) {
        navEvent?.addInput(navInput)
        onDispose { navEvent?.removeInput(navInput) }
    }

    IconButton(
        onClick = { navInput.backCompleted() }
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
}