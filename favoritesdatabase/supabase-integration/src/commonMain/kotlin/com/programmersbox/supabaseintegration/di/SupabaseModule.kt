package com.programmersbox.supabaseintegration.di

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthManagerImpl
import com.programmersbox.supabaseintegration.backup.BackupManager
import com.programmersbox.supabaseintegration.backup.BackupManagerImpl
import com.programmersbox.supabaseintegration.backup.RestoreManager
import com.programmersbox.supabaseintegration.backup.RestoreManagerImpl
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.migration.MigrationManager
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncEngine
import com.programmersbox.supabaseintegration.sync.SyncEngineImpl
import com.programmersbox.supabaseintegration.sync.SyncManager
import com.programmersbox.supabaseintegration.ui.viewmodel.AuthViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupRestoreViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.SyncViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val supabaseModule = module {
    singleOf(::SupabaseClientProvider)
    single<AuthManager> { AuthManagerImpl(get()) }
    singleOf(::SyncConfig)
    single<SyncEngine> {
        SyncEngineImpl(
            clientProvider = get(),
            authManager = get(),
            itemDao = get(),
            connectivityMonitor = get(),
            historyDao = getOrNull(),
            bookmarkDao = getOrNull(),
            notesDao = getOrNull(),
            listDao = getOrNull(),
            heatMapDao = getOrNull(),
        )
    }
    singleOf(::SyncManager)
    single<BackupManager> { BackupManagerImpl(get(), get()) }
    single<RestoreManager> { RestoreManagerImpl(get(), get()) }
    singleOf(::MigrationManager)

    viewModelOf(::SupabaseConfigViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::SyncViewModel)
    viewModelOf(::BackupRestoreViewModel)

    includes(platformModule())
}

expect fun platformModule(): Module