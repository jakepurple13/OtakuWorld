package com.programmersbox.desktop

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.desktopSetup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
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
            single {
                ExtensionWatcher(
                    extensionsDir = get<MangaDesktopSettings>()
                        .extensionDirectory
                        .asFlow()
                )
            }

            navigation<PlatformSettings> { JvmSettingsScreen() }

            includes(novelSharedModule())
        }
    )
}
