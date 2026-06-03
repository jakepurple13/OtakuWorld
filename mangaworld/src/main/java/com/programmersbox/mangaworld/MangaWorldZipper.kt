package com.programmersbox.mangaworld

import android.content.Context
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.kmpuiviews.utils.backupproccesor.BackupProcessor
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class MangaWorldZipper(
    context: Context,
    backupProcessors: List<BackupProcessor>,
    exceptionDao: ExceptionDao,
) : Zipper(context, backupProcessors, exceptionDao)

class MangaNewSettingsBackupProcessor(
    private val mangaNewSettingsHandling: MangaNewSettingsHandling,
) : BackupProcessor() {
    override val fileName: String
        get() = "manga_settings"

    override suspend fun backup(sink: BufferedSink) {
        mangaNewSettingsHandling
            .preferences
            .data
            .firstOrNull()
            ?.encode(sink)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        mangaNewSettingsHandling
            .preferences
            .updateData { MangaSettings.ADAPTER.decode(bufferedSource) }
    }
}