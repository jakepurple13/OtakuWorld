package com.programmersbox.desktop

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.kmpuiviews.tabs.TabHandoff
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.tabs.DownloadsDestination
import com.programmersbox.kmpuiviews.tabs.NavKeySerializers
import com.programmersbox.manga.shared.settings.ImageLoaderSettingsRoute
import com.programmersbox.manga.shared.settings.ReaderSettingsScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import com.programmersbox.kmpuiviews.tabs.TabKey
import com.programmersbox.kmpuiviews.tabs.TabKeyProvider
import com.programmersbox.kmpuiviews.tabs.TabLabel
import com.programmersbox.manga.shared.downloads.DownloadRoute
import com.programmersbox.manga.shared.reader.ReadViewModel
import org.koin.dsl.bind
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.desktop.motion.mangaMotionCatalog
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.desktopSetup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import com.programmersbox.kmpuiviews.presentation.shortcuts.ShortcutSection
import com.programmersbox.manga.shared.mangaSharedModule
import com.programmersbox.manga.shared.reader.readerShortcutSection
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import com.programmersbox.mihon.mihonModule
import dev.nucleusframework.systeminfo.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.navigation3.navigation
import java.io.File

fun main(args: Array<String>) {
    desktopSetup(
        args = args,
        name = "MangaWorld",
        appConfig = {
            AppConfig(
                appName = "MangaWorld",
                buildType = BuildType.NoFirebase,
                isDebug = false,
                userName = SystemInfo.users().firstOrNull()?.name
            )
        },
        jvmAppLogo = { JvmAppLogo(Res.drawable.app_icon) },
        genericInfo = { singleOf(::GenericMangaDesktop) },
        appDirs = AppDirs {
            appName = "MangaWorld"
            appAuthor = "jakepurple13"
        },
        moduleBlock = {
            factoryOf(::DownloadedMediaHandler)
            single { DownloadsDestination(DownloadRoute) }
            // Every page this app adds, so saved tabs showing them restore on the next launch.
            single(named("mangaNavKeys")) {
                NavKeySerializers(
                    SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(ReadViewModel.MangaReader::class)
                            subclass(DownloadRoute::class)
                            subclass(ImageLoaderSettingsRoute::class)
                            subclass(ReaderSettingsScreen::class)
                            subclass(PlatformSettings::class)
                        }
                    }
                )
            }
            single {
                object : TabKeyProvider {
                    override fun keyOf(key: NavKey): TabKey? = when (key) {
                        is ReadViewModel.MangaReader -> key.mangaUrl?.let { TabKey.Chapter(it) }
                        DownloadRoute -> TabKey.Downloads
                        PlatformSettings -> TabKey.Settings
                        else -> null
                    }

                    override fun labelOf(key: NavKey): TabLabel? = when (key) {
                        is ReadViewModel.MangaReader -> TabLabel(key.mangaTitle ?: "Reader", null, null)
                        else -> null
                    }
                }
            } bind TabKeyProvider::class
            // Each reader tab keeps the chapter its click handed over.
            single {
                val holder = get<ChapterHolder>()
                object : TabHandoff {
                    override fun capture(): Any? = Triple(holder.chapterModel, holder.chapters, holder.downloadedChapterPaths)

                    override fun restore(state: Any?) {
                        @Suppress("UNCHECKED_CAST")
                        val (model, chapters, paths) =
                            state as Triple<KmpChapterModel?, List<KmpChapterModel>?, List<String>?>
                        holder.chapterModel = model
                        holder.chapters = chapters
                        holder.downloadedChapterPaths = paths
                    }
                }
            } bind TabHandoff::class
            single {
                ExtensionWatcher(
                    extensionsDir = get<MangaDesktopSettings>()
                        .extensionDirectory
                        .asFlow()
                )
            }
            single {
                MangaDownloadManager(
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    mangaDesktopSettings = get(),
                    trayState = get(),
                )
            }
            single {
                MangaNewSettingsHandling(
                    createProtobuf(
                        serializer = MangaNewSettingsSerializer,
                        fileName = File(
                            get<AppDirs>().getUserDataDir(),
                            "MangaSettings.preferences_pb"
                        ).absolutePath
                    )
                )
            }

            single<ShortcutSection>(named("mangaReaderShortcuts")) { readerShortcutSection() }
            single { mangaMotionCatalog() }

            navigation<PlatformSettings> { JvmSettingsScreen() }

            includes(mangaSharedModule())
            includes(mihonModule)
        }
    )
}
