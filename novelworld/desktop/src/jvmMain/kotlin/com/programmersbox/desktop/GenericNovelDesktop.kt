package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.tabs.navigationItem
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.novel.shared.GenericSharedNovel

class GenericNovelDesktop(
    chapterHolder: ChapterHolder,
) : GenericSharedNovel(chapterHolder = chapterHolder), PlatformGenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }

    @Composable
    override fun ProfileIcon(): String = ""

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            navigationItem(PlatformSettings, Icons.Default.DesktopMac, "Platform Settings")
        }

        novelPreferences()
    }
}
