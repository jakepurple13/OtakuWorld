package com.programmersbox.supabaseintegration.di

import com.programmersbox.supabaseintegration.client.SupabaseClientEngine
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import com.programmersbox.supabaseintegration.credentials.IosCredentialManager
import com.programmersbox.supabaseintegration.credentials.createCredentialSignIn
import com.programmersbox.supabaseintegration.migration.IosMigrationPrefs
import com.programmersbox.supabaseintegration.migration.MigrationPrefs
import com.programmersbox.supabaseintegration.sync.ConnectivityMonitor
import com.programmersbox.supabaseintegration.sync.IosConnectivityMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<CredentialManager> { IosCredentialManager() }
    single<CredentialSignIn> { createCredentialSignIn() }
    single<ConnectivityMonitor> { IosConnectivityMonitor() }
    single<MigrationPrefs> { IosMigrationPrefs() }
    single { SupabaseClientEngine() }
}