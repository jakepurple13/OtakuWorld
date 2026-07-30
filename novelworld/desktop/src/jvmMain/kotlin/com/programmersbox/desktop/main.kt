package com.programmersbox.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import com.programmersbox.novel.shared.novelSharedModule
import dev.nucleusframework.systeminfo.SystemInfo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import java.io.File

fun main(args: Array<String>) {
    val appDirs = AppDirs {
        appName = "NovelWorld"
        appAuthor = "jakepurple13"
    }

    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    val desktopViewModelStoreOwner = DesktopViewModelStoreOwner()
    application {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides desktopViewModelStoreOwner
        ) {
            BaseDesktopUi(
                title = "NovelWorld",
                moduleBlock = {
                    modules(
                        module {
                            single {
                                AppConfig(
                                    appName = "NovelWorld",
                                    buildType = BuildType.NoFirebase,
                                    isDebug = false,
                                    userName = SystemInfo.users().firstOrNull()?.name
                                )
                            }
                            single { JvmAppLogo(Res.drawable.app_icon) }
                            singleOf(::GenericNovelDesktop) { bindsGenericInfo() }
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
            )
        }
    }
}

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
