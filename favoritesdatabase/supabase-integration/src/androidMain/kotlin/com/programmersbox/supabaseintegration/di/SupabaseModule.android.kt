package com.programmersbox.supabaseintegration.di

import com.programmersbox.supabaseintegration.backup.BackupWorker
import com.programmersbox.supabaseintegration.client.SupabaseClientEngine
import com.programmersbox.supabaseintegration.credentials.AndroidCredentialManager
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.migration.AndroidMigrationPrefs
import com.programmersbox.supabaseintegration.migration.MigrationPrefs
import com.programmersbox.supabaseintegration.sync.AndroidConnectivityMonitor
import com.programmersbox.supabaseintegration.sync.ConnectivityMonitor
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<CredentialManager> { AndroidCredentialManager(get()) }
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(get()) }
    single<MigrationPrefs> { AndroidMigrationPrefs(get()) }
    workerOf(::BackupWorker)
    single { SupabaseClientEngine(OkHttp.create()) }
}