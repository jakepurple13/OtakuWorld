package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
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