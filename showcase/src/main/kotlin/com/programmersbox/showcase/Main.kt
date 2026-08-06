package com.programmersbox.showcase

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.baseDesktopSetup
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import dev.nucleusframework.systeminfo.SystemInfo
import org.koin.core.module.dsl.singleOf

fun main(args: Array<String>) {
    baseDesktopSetup(
        args = args,
        name = "Component Showcase",
        appConfig = {
            AppConfig(
                appName = "Showcase",
                buildType = BuildType.NoFirebase,
                isDebug = false,
                userName = SystemInfo.users().firstOrNull()?.name
            )
        },
        genericInfo = { singleOf(::ShowcaseInfo) },
        appDirs = AppDirs {
            appName = "Showcase"
            appAuthor = "jakepurple13"
        },
        moduleBlock = {

        },
        content = { App() }
    )
}

class ShowcaseInfo : PlatformGenericInfo {
    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = { null }

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {

    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {

    }

    @Composable
    override fun ComposeShimmerItem() {

    }

    @ExperimentalFoundationApi
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {

    }

    @Composable
    override fun ProfileIcon(): String = ""
}