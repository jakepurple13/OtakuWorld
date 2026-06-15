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
