package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.backupproccesor.BackupProcessor
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
import com.programmersbox.kmpuiviews.utils.backupproccesor.SourceOrderBackupProcessor
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.new
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

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
}

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
}

inline fun <reified T : BackupProcessor> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: () -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

inline fun <reified T : BackupProcessor, reified T1> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: (T1) -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

inline fun <reified T : BackupProcessor, reified T1, reified T2> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: (T1, T2) -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

expect fun platformModule(): Module