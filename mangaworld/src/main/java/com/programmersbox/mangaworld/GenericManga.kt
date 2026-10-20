package com.programmersbox.mangaworld

import android.app.PendingIntent
import android.content.Context
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.kmpuiviews.utils.plus
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.GenericSharedManga
import com.programmersbox.manga.shared.downloads.DownloadChapterWorker
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import com.programmersbox.manga.shared.mangaSharedModule
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import com.programmersbox.mihon.mihonModule
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.source_utilities.NetworkHelper
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val appModule = module {
    singleOf(::GenericManga) { bindsGenericInfo() }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }
    singleOf(::NetworkHelper)
    single { NotificationLogo(R.drawable.manga_world_round_logo) }
    single {
        MangaNewSettingsHandling(
            createProtobuf(
                context = get(),
                serializer = MangaNewSettingsSerializer,
                fileName = "MangaSettings.preferences_pb"
            )
        )
    }
    factoryOf(::DownloadedMediaHandler)
    factory { Zipper(get(), getAll<BackupProcessor>(), get()) }
    singleOf(::MangaDownloadManager)
    workerOf(::DownloadChapterWorker)

    navigation<Screen.SourcePreferences> { MihonPreferencesScreen(it.serviceName) }

    includes(mangaSharedModule(), mihonModule)
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
        chapterHolder.chapterModel = model
        ReadViewModel.navigateToMangaReader(
            navController,
            infoModel.title,
            model.url,
            model.sourceUrl
        )
    }

    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit {
        return super<GenericSharedManga>.composeCustomPreferences() + super<GenericInfo>.composeCustomPreferences()
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