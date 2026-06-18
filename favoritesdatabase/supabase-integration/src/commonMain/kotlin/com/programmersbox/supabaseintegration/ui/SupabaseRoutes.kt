package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SupabaseConfigRoute : NavKey

@Serializable
data object AuthRoute : NavKey

@Serializable
data object SyncStatusRoute : NavKey

@Serializable
data object BackupRestoreRoute : NavKey

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
}