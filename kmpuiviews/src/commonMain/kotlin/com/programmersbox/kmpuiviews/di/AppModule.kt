package com.programmersbox.kmpuiviews.di

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.backupproccesor.BackupSettingsProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.BookmarksBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.ChaptersWatchedBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.FavoriteBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.HeatMapBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.HistoryBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.IncognitoBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.ListBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.NewSettingsBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.NotesBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.NotificationsBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.RecommendationsBackupProcessor
import com.programmersbox.kmpuiviews.utils.backupproccesor.SourceOrderBackupProcessor
import com.programmersbox.sharedtools.backupProcessor
import com.programmersbox.supabaseintegration.di.SupabaseActions
import com.programmersbox.supabaseintegration.di.supabaseModule
import com.programmersbox.supabaseintegration.sync.SyncConfigDataStore
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

val appModule = module {
    singleOf(::AppUpdateCheck)
    single {
        OtakuWorldCatalog(
            get<KmpGenericInfo>().sourceType
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        )
    }

    singleOf(::DataStoreHandling)
    singleOf(::MediaUpdateChecker)
    factoryOf(::Backup)
    backupProcessors()
    includes(platformModule())

    single {
        val pollDataStore = DataStoreHandler(
            longPreferencesKey("pollInterval"),
            5.minutes.inWholeMilliseconds
        )
        val maxRetriesDataStore = DataStoreHandler(
            intPreferencesKey("maxRetries"),
            3
        )
        val initialBackoffDataStore = DataStoreHandler(
            longPreferencesKey("initialBackoff"),
            10.minutes.inWholeMilliseconds
        )
        val maxBackOffDataStore = DataStoreHandler(
            longPreferencesKey("maxBackoff"),
            1.hours.inWholeMilliseconds
        )

        SyncConfigDataStore(
            pollIntervalMs = pollDataStore.asFlow(),
            setPollIntervalMs = { pollDataStore.set(it) },
            maxRetries = maxRetriesDataStore.asFlow(),
            setMaxRetries = { maxRetriesDataStore.set(it) },
            initialBackoffMs = initialBackoffDataStore.asFlow(),
            setInitialBackoffMs = { initialBackoffDataStore.set(it) },
            maxBackoffMs = maxBackOffDataStore.asFlow(),
            setMaxBackoffMs = { maxBackOffDataStore.set(it) },
        )
    }

    single {
        val navHandler = get<NavigationActions>()
        SupabaseActions(
            onNavigate = { navHandler.navigate(it) }
        )
    }

    includes(supabaseModule)
}

//TODO: Move BackupProcessor to a separate shared like module
private fun Module.backupProcessors() {
    backupProcessor("backupSettings", ::BackupSettingsProcessor)
    backupProcessor("bookmarks", ::BookmarksBackupProcessor)
    backupProcessor("chaptersWatched", ::ChaptersWatchedBackupProcessor)
    backupProcessor("favorite", ::FavoriteBackupProcessor)
    backupProcessor("heatMap", ::HeatMapBackupProcessor)
    backupProcessor("history", ::HistoryBackupProcessor)
    backupProcessor("incognito", ::IncognitoBackupProcessor)
    backupProcessor("list", ::ListBackupProcessor)
    backupProcessor("newSettings", ::NewSettingsBackupProcessor)
    backupProcessor("notifications", ::NotificationsBackupProcessor)
    backupProcessor("sourceOrder", ::SourceOrderBackupProcessor)
    backupProcessor("notes", ::NotesBackupProcessor)
    backupProcessor("recommendations", ::RecommendationsBackupProcessor)
}

expect fun platformModule(): Module