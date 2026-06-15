package com.programmersbox.mangaworld

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.gsonutils.toJson
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.di.backupProcessor
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.kmpuiviews.utils.backupproccesor.BackupProcessor
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.GenericSharedManga
import com.programmersbox.manga.shared.downloads.DownloadChapterWorker
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import com.programmersbox.mangaworld.reader.ReadActivity
import com.programmersbox.source_utilities.NetworkHelper
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.bindsGenericInfo
import kotlinx.coroutines.flow.first
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::GenericManga) { bindsGenericInfo() }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }
    singleOf(::NetworkHelper)
    single { NotificationLogo(R.drawable.manga_world_round_logo) }
    singleOf(::ChapterHolder)
    single {
        MangaNewSettingsHandling(
            createProtobuf(
                context = get(),
                serializer = MangaNewSettingsSerializer,
                fileName = "MangaSettings.preferences_pb"
            )
        )
    }
    viewModelOf(::ReadViewModel)
    factoryOf(::DownloadedMediaHandler)
    viewModelOf(::DownloadViewModel)
    backupProcessor("manga_settings", ::MangaNewSettingsBackupProcessor)
    factory { Zipper(get(), getAll<BackupProcessor>(), get()) }
    singleOf(::MangaDownloadManager)
    workerOf(::DownloadChapterWorker)
}

//TODO: For multiplatform, maybe this becomes an open class that then the Android version overrides
// while ios and desktop just use the open class?
class GenericManga(
    val context: Context,
    val chapterHolder: ChapterHolder,
    mangaSettingsHandling: MangaNewSettingsHandling,
    settingsHandling: NewSettingsHandling,
    appConfig: AppConfig,
    navigationActions: NavigationActions,
    mangaDownloadManager: MangaDownloadManager,
) : GenericSharedManga(
    mangaSettingsHandling = mangaSettingsHandling,
    settingsHandling = settingsHandling,
    appConfig = appConfig,
    navigationActions = navigationActions,
    mangaDownloadManager = mangaDownloadManager,
), GenericInfo {

    override val deepLinkUri: String get() = "mangaworld://"

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        chapterHolder.chapters = allChapters
        if (mangaSettingsHandling.useNewReader.flow.first()) {
            chapterHolder.chapterModel = model
            ReadViewModel.navigateToMangaReader(
                navController,
                infoModel.title,
                model.url,
                model.sourceUrl
            )
        } else {
            context.startActivity(
                Intent(context, ReadActivity::class.java).apply {
                    putExtra("currentChapter", model.toJson(KmpChapterModel::class.java to ChapterModelSerializer()))
                    putExtra("allChapters", allChapters.toJson(KmpChapterModel::class.java to ChapterModelSerializer()))
                    putExtra("mangaTitle", infoModel.title)
                    putExtra("mangaUrl", model.url)
                    putExtra("mangaInfoUrl", model.sourceUrl)
                }
            )
        }
    }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkDetailsUri(itemModel),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(itemModel?.hashCode() ?: 0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkSettingsUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(13, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}