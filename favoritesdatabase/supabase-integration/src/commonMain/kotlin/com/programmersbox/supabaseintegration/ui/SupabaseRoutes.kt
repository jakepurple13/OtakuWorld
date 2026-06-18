package com.programmersbox.supabaseintegration.ui

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

fun EntryProviderScope<NavKey>.supabaseRoutes() {
    entry<SupabaseConfigRoute> { SupabaseConfigScreen() }
    entry<AuthRoute> { AuthScreen() }
    entry<SyncStatusRoute> { SyncStatusScreen() }
    entry<BackupRestoreRoute> {
        BackupRestoreScreen(
            getLocalDbPath = { "" }
        )
    }
}