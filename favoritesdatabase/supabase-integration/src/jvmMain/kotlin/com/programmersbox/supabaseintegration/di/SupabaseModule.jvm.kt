package com.programmersbox.supabaseintegration.di

import com.programmersbox.supabaseintegration.client.SupabaseClientEngine
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import com.programmersbox.supabaseintegration.credentials.JvmCredentialManager
import com.programmersbox.supabaseintegration.credentials.createCredentialSignIn
import com.programmersbox.supabaseintegration.migration.JvmMigrationPrefs
import com.programmersbox.supabaseintegration.migration.MigrationPrefs
import com.programmersbox.supabaseintegration.sync.ConnectivityMonitor
import com.programmersbox.supabaseintegration.sync.FullSyncHandler
import com.programmersbox.supabaseintegration.sync.JvmConnectivityMonitor
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<CredentialManager> { JvmCredentialManager(get()) }
    single<CredentialSignIn> { createCredentialSignIn() }
    single<ConnectivityMonitor> { JvmConnectivityMonitor() }
    single<MigrationPrefs> { JvmMigrationPrefs() }
    single { SupabaseClientEngine() }
    singleOf(::FullSyncHandler)
}