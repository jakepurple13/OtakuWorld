package com.programmersbox.novelworld

import android.app.PendingIntent
import android.content.Context
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.novel.shared.GenericSharedNovel
import com.programmersbox.novel.shared.novelSharedModule
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::GenericNovel) { bindsGenericInfo() }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }
    singleOf(::Backup)
    factory { Zipper(get(), getAll<BackupProcessor>(), get()) }

    includes(novelSharedModule())
}

class GenericNovel(
    val context: Context,
    val appConfig: AppConfig,
    chapterHolder: ChapterHolder,
) : GenericSharedNovel(chapterHolder = chapterHolder), GenericInfo {

    override val deepLinkUri: String get() = "novelworld://"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> novelNoFirebaseFile
                BuildType.Full -> novelFile
            }
        }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkDetailsUri(itemModel),
            activity = MainActivity::class.java
        )
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkSettingsUri(),
            activity = MainActivity::class.java
        )
    }
}