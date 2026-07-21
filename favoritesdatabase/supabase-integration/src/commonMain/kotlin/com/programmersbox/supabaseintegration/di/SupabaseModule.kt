package com.programmersbox.supabaseintegration.di

import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.BackupPreferenceDao
import com.programmersbox.favoritesdatabase.SyncPreferences
import com.programmersbox.sharedcomponents.Navigator
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import com.programmersbox.sharedtools.SearchRegistryItem
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthManagerImpl
import com.programmersbox.supabaseintegration.backup.BackupManager
import com.programmersbox.supabaseintegration.backup.BackupManagerImpl
import com.programmersbox.supabaseintegration.backup.RestoreManager
import com.programmersbox.supabaseintegration.backup.RestoreManagerImpl
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.database.DatabaseRepository
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.migration.MigrationManager
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncConfigRepository
import com.programmersbox.supabaseintegration.sync.SyncEngine
import com.programmersbox.supabaseintegration.sync.SyncEngineImpl
import com.programmersbox.supabaseintegration.sync.SyncManager
import com.programmersbox.supabaseintegration.sync.syncprocessor.BookmarksSyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.ChaptersWatchedSyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.CustomListInfoSyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.CustomListItemSyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.FavoritesSyncer
import com.programmersbox.supabaseintegration.sync.syncprocessor.HeatMapSyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.HistorySyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.NotesSyncProcessor
import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import com.programmersbox.supabaseintegration.ui.AuthRoute
import com.programmersbox.supabaseintegration.ui.AuthScreen
import com.programmersbox.supabaseintegration.ui.BackupPreferencesRoute
import com.programmersbox.supabaseintegration.ui.BackupPreferencesScreen
import com.programmersbox.supabaseintegration.ui.BackupRestoreRoute
import com.programmersbox.supabaseintegration.ui.BackupRestoreScreen
import com.programmersbox.supabaseintegration.ui.SupabaseConfigRoute
import com.programmersbox.supabaseintegration.ui.SupabaseConfigScreen
import com.programmersbox.supabaseintegration.ui.SupabaseRoutes
import com.programmersbox.supabaseintegration.ui.SupabaseSearchItems
import com.programmersbox.supabaseintegration.ui.SupabaseSettingsScreen
import com.programmersbox.supabaseintegration.ui.SyncStatusRoute
import com.programmersbox.supabaseintegration.ui.SyncStatusScreen
import com.programmersbox.supabaseintegration.ui.viewmodel.AuthViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupRestoreViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.SyncViewModel
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

fun supabaseModule() = module {
    singleOf(::SupabaseClientProvider)
    single<AuthManager> { AuthManagerImpl(get(), get()) }
    single { SyncConfigRepository(get()) }
    single {
        SyncEngineImpl(
            clientProvider = get(),
            authManager = get(),
            connectivityMonitor = get(),
            syncProcessors = getAll()
        )
    } bind SyncEngine::class
    single {
        SyncManager(
            syncEngine = get(),
            authManager = get(),
            connectivityMonitor = get(),
            fullSyncHandler = get(),
            configFlow = getOrNull<SyncConfigRepository>()?.listenForChanges() ?: flowOf(SyncConfig())
        )
    }
    single<BackupManager> { BackupManagerImpl(get(), get()) }
    single<RestoreManager> { RestoreManagerImpl(get(), get()) }
    singleOf(::MigrationManager)
    single { DatabaseRepository(getAll()) }

    single<SyncPreferences> { SyncPreferences.getInstance(get()) }
    single<BackupPreferenceDao> { get<SyncPreferences>().backupPreferenceDao() }
    single { BackupPreferenceRepository(get()) }

    viewModelOf(::SupabaseConfigViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::SyncViewModel)
    viewModelOf(::BackupRestoreViewModel)
    viewModel { BackupPreferencesViewModel(get(), getAll(), get()) }
    singleOf(::SupabaseSearchItems) bind SearchRegistryItem::class

    syncProcessorModule()

    single {
        val navHandler = get<Navigator>()
        SupabaseActions(
            onNavigate = { navHandler.navigateTo(it) }
        )
    }

    includes(platformModule())

    navigation<SupabaseConfigRoute> {
        HideNavBarWhileOnScreen()
        SupabaseConfigScreen()
    }
    navigation<AuthRoute> {
        HideNavBarWhileOnScreen()
        AuthScreen()
    }
    navigation<SyncStatusRoute> {
        HideNavBarWhileOnScreen()
        SyncStatusScreen()
    }
    navigation<BackupRestoreRoute> {
        HideNavBarWhileOnScreen()
        BackupRestoreScreen(
            getLocalDbPath = { "" }
        )
    }
    navigation<BackupPreferencesRoute> {
        HideNavBarWhileOnScreen()
        BackupPreferencesScreen()
    }
    navigation<SupabaseRoutes> {
        val actions = koinInject<SupabaseActions>()
        SupabaseSettingsScreen(
            onNavigate = actions.onNavigate
        )
    }
}

private fun Module.syncProcessorModule() {
    singleOf(::FavoritesSyncer) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::ChaptersWatchedSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::BookmarksSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::NotesSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::HistorySyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::CustomListItemSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::CustomListInfoSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::HeatMapSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
}

expect fun platformModule(): Module

data class SupabaseActions(
    val onNavigate: (NavKey) -> Unit,
)