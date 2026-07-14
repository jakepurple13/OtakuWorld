package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.novel.shared.GenericSharedNovel

class GenericNovelDesktop(
    chapterHolder: ChapterHolder,
    private val navigationActions: NavigationActions,
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
            segmentedListItem(
                content = { Text("Platform Settings") },
                leadingContent = { Icon(Icons.Default.DesktopMac, null) },
                onClick = { navigationActions.navigate(PlatformSettings) }
            )
        }
    }
}
