package com.programmersbox.desktop

import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRoute
import com.programmersbox.anime.shared.videos.LibraryFoldersRoute
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.tabs.CoreTabKeyProvider
import com.programmersbox.kmpuiviews.tabs.TabKey
import com.programmersbox.kmpuiviews.tabs.TabKeys
import com.programmersbox.kmpuiviews.tabs.TabOpenMode
import com.programmersbox.kmpuiviews.tabs.TabPrefsSnapshot
import com.programmersbox.kmpuiviews.tabs.TabsController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnimeTabKeyProviderTest {
    @Test
    fun downloadsSubpagesAreNotSettingsKeys() {
        assertNull(AnimeTabKeyProvider.keyOf(LibraryFoldersRoute))
        assertNull(AnimeTabKeyProvider.keyOf(SubtitleSettingsRoute))
        assertEquals(TabKey.Settings, AnimeTabKeyProvider.keyOf(PlatformSettings))
        assertEquals(TabKey.Downloads, AnimeTabKeyProvider.keyOf(VideoViewerRoute))
    }

    @Test
    fun downloadsSubpagesStayInTheDownloadsTab() {
        val home = Navigation3Actions(Screen.RecentScreen)
        val controller = TabsController(
            homeNav = home,
            keys = TabKeys(listOf(AnimeTabKeyProvider, CoreTabKeyProvider)),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        )
        val main = controller.windows.single()
        // A Settings tab is open too, so a Settings key would be sent there.
        controller.open(Screen.Settings, TabOpenMode.Background, main.activeId.value)
        val downloads = controller.open(VideoViewerRoute, TabOpenMode.Foreground, main.activeId.value)
        val nav = main.tabs.first { it.id == downloads }.nav

        nav.navigate(LibraryFoldersRoute)
        nav.navigate(SubtitleSettingsRoute)

        assertEquals(downloads, main.activeId.value)
        assertEquals(listOf<NavKey>(VideoViewerRoute, LibraryFoldersRoute, SubtitleSettingsRoute), nav.backStack.toList())
        assertEquals(3, main.tabs.size)
    }
}
