package com.programmersbox.mangaworld

import android.content.Context
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.sharedtools.BackupProcessor

class MangaWorldZipper(
    context: Context,
    backupProcessors: List<BackupProcessor>,
    exceptionDao: ExceptionDao,
) : Zipper(context, backupProcessors, exceptionDao)
