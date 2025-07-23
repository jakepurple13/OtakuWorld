package com.programmersbox.mangaworld

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.util.fastForEach
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.GenericSharedManga
import com.programmersbox.manga.shared.downloads.DownloadRoute
import com.programmersbox.manga.shared.downloads.DownloadScreen
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.reader.ReadView
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.manga.shared.settings.ImageLoaderSettings
import com.programmersbox.manga.shared.settings.ImageLoaderSettingsRoute
import com.programmersbox.manga.shared.settings.ReaderSettings
import com.programmersbox.manga.shared.settings.ReaderSettingsScreen
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import com.programmersbox.mangaworld.reader.ReadActivity
import com.programmersbox.source_utilities.NetworkHelper
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.bindsGenericInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.io.File

val appModule = module {
    singleOf(::GenericManga) { bindsGenericInfo() }

    singleOf(::NetworkHelper)
    single { NotificationLogo(R.drawable.manga_world_round_logo) }
    singleOf(::ChapterHolder)
    singleOf(::MangaSettingsHandling)
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
}

//TODO: For multiplatform, maybe this becomes an open class that then the Android version overrides
// while ios and desktop just use the open class?
class GenericManga(
    val context: Context,
    val chapterHolder: ChapterHolder,
    mangaSettingsHandling: MangaNewSettingsHandling,
    settingsHandling: NewSettingsHandling,
    appConfig: AppConfig,
) : GenericSharedManga(
    mangaSettingsHandling = mangaSettingsHandling,
    settingsHandling = settingsHandling,
    appConfig = appConfig,
), GenericInfo {

    override val deepLinkUri: String get() = "mangaworld://"

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        chapterHolder.chapters = allChapters
        if (runBlocking { mangaSettingsHandling.useNewReader.flow.first() }) {
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

    private fun downloadFullChapter(model: KmpChapterModel, title: String) {
        //val fileLocation = runBlocking { context.folderLocationFlow.first() }
        val fileLocation = DOWNLOAD_FILE_PATH

        val direct = File("$fileLocation$title/${model.name}/")
        if (!direct.exists()) direct.mkdir()

        GlobalScope.launch {
            model.getChapterInfo()
                .dispatchIo()
                .map { it.mapNotNull(KmpStorage::link) }
                .map {
                    it.mapIndexed { index, s ->
                        //val location = "/$fileLocation/$title/${model.name}"

                        //val file = File(Environment.getExternalStorageDirectory().path + location, "${String.format("%03d", index)}.png")

                        DownloadManager.Request(s.toUri())
                            //.setDestinationUri(file.toUri())
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "MangaWorld/$title/${model.name}/${String.format("%03d", index)}"
                            )
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setAllowedOverRoaming(true)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                            .setMimeType("image/*")
                            .setTitle(model.name)
                            .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Gecko/20100101 Firefox/77")
                            .addRequestHeader("Accept-Language", "en-US,en;q=0.5")
                    }
                }
                .onEach { it.fastForEach(context.downloadManager::enqueue) }
                .collect()
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        /*activity.requestPermissions(
            *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            else arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) { p -> if (p.isGranted) downloadFullChapter(model, infoModel.title.ifBlank { infoModel.url }) }*/

        downloadFullChapter(model, infoModel.title.ifBlank { infoModel.url })
    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    context(navGraph: EntryProviderBuilder<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<ReadViewModel.MangaReader> {
            ReadView(
                viewModel = koinViewModel { parametersOf(it) }
            )
        }
    }

    context(navGraph: EntryProviderBuilder<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<DownloadRoute> {
            DownloadScreen()
        }

        navGraph.entry<ImageLoaderSettingsRoute> {
            ImageLoaderSettings(mangaSettingsHandling)
        }

        navGraph.entry<ReaderSettingsScreen> {
            ReaderSettings(
                mangaSettingsHandling = mangaSettingsHandling,
                settingsHandling = settingsHandling
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