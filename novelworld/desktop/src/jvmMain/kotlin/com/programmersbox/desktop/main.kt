package com.programmersbox.desktop

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.kmpuiviews.tabs.TabHandoff
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.tabs.DownloadsDestination
import com.programmersbox.kmpuiviews.tabs.NavKeySerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.core.qualifier.named
import com.programmersbox.kmpuiviews.tabs.TabKey
import com.programmersbox.kmpuiviews.tabs.TabKeyProvider
import com.programmersbox.kmpuiviews.tabs.TabLabel
import com.programmersbox.novel.shared.reader.ReadViewModel
import org.koin.dsl.bind
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.desktopSetup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.desktop.motion.novelMotionCatalog
import com.programmersbox.novel.shared.novelSharedModule
import dev.nucleusframework.systeminfo.SystemInfo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.navigation3.navigation

fun main(args: Array<String>) {
    desktopSetup(
        args = args,
        name = "NovelWorld",
        appConfig = {
            AppConfig(
                appName = "NovelWorld",
                buildType = BuildType.NoFirebase,
                isDebug = false,
                userName = SystemInfo.users().firstOrNull()?.name
            )
        },
        jvmAppLogo = { JvmAppLogo(Res.drawable.app_icon) },
        genericInfo = { singleOf(::GenericNovelDesktop) },
        appDirs = AppDirs {
            appName = "NovelWorld"
            appAuthor = "jakepurple13"
        },
        moduleBlock = {
            single { DownloadsDestination(null) }
            // Every page this app adds, so saved tabs showing them restore on the next launch.
            single(named("novelNavKeys")) {
                NavKeySerializers(
                    SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(ReadViewModel.NovelReader::class)
                            subclass(PlatformSettings::class)
                        }
                    },
                    // A streamed chapter needs the chapter its click handed over; a download reads its folder.
                    restorable = { it !is ReadViewModel.NovelReader || it.downloaded },
                )
            }
            single {
                object : TabKeyProvider {
                    override fun keyOf(key: NavKey): TabKey? = when (key) {
                        is ReadViewModel.NovelReader -> key.novelUrl?.let { TabKey.Chapter(it) }
                        PlatformSettings -> TabKey.Settings
                        else -> null
                    }

                    override fun labelOf(key: NavKey): TabLabel? = when (key) {
                        is ReadViewModel.NovelReader -> TabLabel(key.novelTitle ?: "Reader", null, null)
                        else -> null
                    }
                }
            } bind TabKeyProvider::class
            // Each reader tab keeps the chapter its click handed over.
            single {
                val holder = get<ChapterHolder>()
                object : TabHandoff {
                    override fun capture(): Any? = holder.chapterModel to holder.chapters

                    override fun restore(state: Any?) {
                        @Suppress("UNCHECKED_CAST")
                        val (model, chapters) = state as Pair<KmpChapterModel?, List<KmpChapterModel>?>
                        holder.chapterModel = model
                        holder.chapters = chapters
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

            single { novelMotionCatalog() }

            navigation<PlatformSettings> { JvmSettingsScreen() }

            includes(novelSharedModule())
        }
    )
}
