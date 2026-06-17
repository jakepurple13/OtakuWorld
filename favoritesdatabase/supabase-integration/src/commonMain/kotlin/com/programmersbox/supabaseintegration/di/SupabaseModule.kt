package com.programmersbox.supabaseintegration.di

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthManagerImpl
import com.programmersbox.supabaseintegration.backup.BackupManager
import com.programmersbox.supabaseintegration.backup.BackupManagerImpl
import com.programmersbox.supabaseintegration.backup.RestoreManager
import com.programmersbox.supabaseintegration.backup.RestoreManagerImpl
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.createCredentialManager
import com.programmersbox.supabaseintegration.migration.MigrationManager
import com.programmersbox.supabaseintegration.migration.MigrationPrefs
import com.programmersbox.supabaseintegration.migration.createMigrationPrefs
import com.programmersbox.supabaseintegration.sync.ConnectivityMonitor
import com.programmersbox.supabaseintegration.sync.createConnectivityMonitor
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncEngine
import com.programmersbox.supabaseintegration.sync.SyncEngineImpl
import com.programmersbox.supabaseintegration.sync.SyncManager
import com.programmersbox.supabaseintegration.ui.viewmodel.AuthViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupRestoreViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.SyncViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val supabaseModule = module {
    single<CredentialManager> { createCredentialManager(getOrNull()) }
    single { SupabaseClientProvider(get()) }
    single<AuthManager> { AuthManagerImpl(get()) }
    single<ConnectivityMonitor> { createConnectivityMonitor(getOrNull()) }
    single { SyncConfig() }
    single<SyncEngine> { SyncEngineImpl(get(), get(), get(), get()) }
    single { SyncManager(get(), get(), get(), get()) }
    single<BackupManager> { BackupManagerImpl(get(), get()) }
    single<RestoreManager> { RestoreManagerImpl(get(), get()) }
    single<MigrationPrefs> { createMigrationPrefs(getOrNull()) }
    single { MigrationManager(get(), get()) }

    viewModelOf(::SupabaseConfigViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::SyncViewModel)
    viewModelOf(::BackupRestoreViewModel)
}
