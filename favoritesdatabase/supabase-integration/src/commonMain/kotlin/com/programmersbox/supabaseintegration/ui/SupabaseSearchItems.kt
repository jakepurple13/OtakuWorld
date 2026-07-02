package com.programmersbox.supabaseintegration.ui

import com.programmersbox.sharedtools.SearchRegistryItem
import com.programmersbox.sharedtools.SettingSearchItem

class SupabaseSearchItems : SearchRegistryItem {
    override fun addSearchItems(): List<SettingSearchItem> {
        return listOf(
            SettingSearchItem(
                displayName = "Supabase",
                keywords = listOf("supabase", "database", "cloud"),
                breadcrumb = listOf(SupabaseRoutes),
                targetScreen = SupabaseRoutes,
                highlightKey = "supabase",
            ),
            SettingSearchItem(
                displayName = "Supabase Config",
                keywords = listOf("supabase", "config", "configuration"),
                breadcrumb = listOf(SupabaseRoutes, SupabaseConfigRoute),
                targetScreen = SupabaseConfigRoute,
                highlightKey = "config",
            ),
            SettingSearchItem(
                displayName = "Supabase Authentication",
                keywords = listOf("supabase", "auth", "authentication"),
                breadcrumb = listOf(SupabaseRoutes, AuthRoute),
                targetScreen = AuthRoute,
                highlightKey = "auth",
            ),
            SettingSearchItem(
                displayName = "Supabase Sync Status",
                keywords = listOf("supabase", "sync", "status"),
                breadcrumb = listOf(SupabaseRoutes, SyncStatusRoute),
                targetScreen = SyncStatusRoute,
                highlightKey = "sync",
            ),
            SettingSearchItem(
                displayName = "Supabase Backup/Restore",
                keywords = listOf("supabase", "backup", "restore"),
                breadcrumb = listOf(SupabaseRoutes, BackupRestoreRoute),
                targetScreen = BackupRestoreRoute,
                highlightKey = "backup",
            )
        )
    }
}