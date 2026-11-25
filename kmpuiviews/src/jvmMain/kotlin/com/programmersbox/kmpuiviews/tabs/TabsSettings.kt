@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsController
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.serialization.Serializable
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/** Desktop Settings → Tabs & browsing. */
@Serializable
data object TabsSettingsRoute : NavKey

/** "Tabs" header + the row that opens [TabsSettingsRoute], for each app's Desktop Settings page. */
fun LazyListScope.tabsSettingsGroup() {
    item(contentType = "tabsHeader") {
        Text(
            "Tabs",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .animateItem()
                .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )
    }
    item(contentType = "tabsRow") {
        val position by tabPositionPref.asState()
        val navActions = LocalNavActions.current
        SegmentedListItem(
            content = { Text("Tabs & browsing") },
            supportingContent = {
                Text(if (position == TabPosition.Side) "Tabs on the side" else "Tabs across the top")
            },
            leadingContent = { Icon(Icons.Default.Tab, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            onClick = { navActions.navigate(TabsSettingsRoute) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            modifier = Modifier.animateItem(),
        )
    }
}

private fun NewTabSection.label() = when (this) {
    NewTabSection.Search -> "Search box"
    NewTabSection.Continue -> "Continue reading"
    NewTabSection.Updates -> "Favorites with new chapters"
    NewTabSection.Closed -> "Recently closed"
    NewTabSection.Links -> "Quick links to Lists, Downloads, Settings and History"
}

/** Every setting of the desktop tabbed browser, in the spec's groups and order. Rows hide when they do not apply. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSettingsPage() {
    val koin = getKoin()
    val miniPlayerSupported = remember(koin) { koin.getOrNull<MiniPlayerSupported>()?.supported == true }
    val quickActions = koinInject<QuickActionsController>()
    val mod = if (isMacOs) "⌘" else "Ctrl+"
    val clickMod = if (isMacOs) "⌘" else "Ctrl"

    var position by tabPositionPref.asState()
    var clickOpens by tabClickOpensPref.asState()
    var modClick by tabModClickPref.asState()
    var newPlace by tabNewPlacePref.asState()
    var afterClose by tabAfterClosePref.asState()
    var showCovers by tabShowCoversPref.asState()
    var newTabShows by newTabShowsPref.asState()
    var sections by newTabSectionsPref.asState()
    var continueCount by newTabContinueCountPref.asState()
    var background by newTabBackgroundPref.asState()
    var focusSearch by newTabFocusSearchPref.asState()
    var onLaunch by tabsOnLaunchPref.asState()
    var closedKept by tabsClosedKeptPref.asState()
    var confirmQuit by tabsConfirmQuitPref.asState()
    var sleepAfter by tabsSleepAfterPref.asState()
    var miniPlayer by tabsMiniPlayerPref.asState()
    var corner by miniPlayerCornerPref.asState()
    var pauseUnfocused by miniPlayerPauseUnfocusedPref.asState()
    var tearOut by tabsTearOutPref.asState()
    var motion by tabMotionPref.asState()

    val tabs = listOf(
        choiceRow(
            "position", "Tab position",
            "Across the top like a browser, or down the side for long titles. Changing it animates the tabs into place.",
            Icons.Default.Tab, listOf(TabPosition.Top to "Top", TabPosition.Side to "Side"), position,
        ) { position = it },
        choiceRow(
            "clickOpens", "Clicking a title opens", "${clickMod}-click and middle-click always open a new tab.",
            Icons.Default.Mouse, listOf(ClickOpens.SameTab to "This tab", ClickOpens.NewTab to "New tab"), clickOpens,
        ) { clickOpens = it },
        choiceRow(
            "modClick", "⌘/Ctrl-click opens",
            "With \"And switch to it\", ${clickMod}-click opens a tab and switches to it, and ${clickMod}-Shift-click opens one " +
                "in the background. Middle-click does what ${clickMod}-click does.",
            Icons.Default.Mouse,
            listOf(ModClick.Background to "In background", ModClick.Foreground to "And switch to it"), modClick,
        ) { modClick = it },
        choiceRow(
            "newPlace", "Place new tabs",
            "Next to current puts a new tab right of the active tab, after the ones you already opened from it.",
            Icons.Default.AddBox,
            listOf(NewTabPlace.NextToCurrent to "Next to current", NewTabPlace.AtEnd to "At the end"), newPlace,
        ) { newPlace = it },
        choiceRow(
            "afterClose", "After closing a tab, go to",
            "Last used is the tab you were on most recently that is still open in that window.",
            Icons.Default.Close,
            listOf(AfterClose.Right to "Tab on the right", AfterClose.Left to "Tab on the left", AfterClose.LastUsed to "Last used"),
            afterClose,
        ) { afterClose = it },
        switchRow(
            "showCovers", "Show covers on tabs",
            "Small cover art as the tab icon. Off shows a plain icon: a book for titles and chapters, a play icon for episodes.",
            Icons.Default.Image, showCovers,
        ) { showCovers = it },
    )

    val newTabPage = listOfNotNull(
        menuRow(
            "newTabShows", "A new tab shows", "What you see after ${mod}T or the + button.", Icons.Default.AddBox,
            listOf(
                NewTabShows.Page to "New tab page",
                NewTabShows.Recent to "Recent (like Home)",
                NewTabShows.Continue to "Continue reading only",
                NewTabShows.Blank to "Blank page",
            ),
            newTabShows,
        ) { newTabShows = it },
        sectionsRow(
            "sections", "New tab page sections", "Turn sections on or off and set their order.",
            Icons.AutoMirrored.Filled.ViewList, sections, { it.label() },
        ) { sections = it }.takeIf { newTabShows == NewTabShows.Page },
        choiceRow(
            "continueCount", "Continue reading shows", "How many titles you're partway through.",
            Icons.AutoMirrored.Filled.ViewList, ContinueCountOptions.map { it to "$it titles" }, continueCount,
        ) { continueCount = it }.takeIf { newTabShows != NewTabShows.Recent && newTabShows != NewTabShows.Blank },
        choiceRow(
            "background", "Background", "Plain, or a soft blur of the cover you read last.", Icons.Default.Wallpaper,
            listOf(NewTabBackground.Plain to "Plain", NewTabBackground.LastCover to "Last-read cover"), background,
        ) { background = it }.takeIf { newTabShows != NewTabShows.Recent },
        switchRow(
            "focusSearch", "Put the cursor in search", "Start typing a title as soon as the new tab opens.",
            Icons.Default.Search, focusSearch,
        ) { focusSearch = it }.takeIf { newTabShows == NewTabShows.Page },
    )

    val startup = listOf(
        choiceRow(
            "onLaunch", "When the app opens", "Reopen my tabs brings back your windows and tabs as you left them.",
            Icons.Default.Restore,
            listOf(OnLaunch.Restore to "Reopen my tabs", OnLaunch.Home to "Home only", OnLaunch.NewTab to "New tab page"),
            onLaunch,
        ) { onLaunch = it },
        choiceRow(
            "closedKept", "Remember recently closed",
            "How many closed tabs ${if (isMacOs) "⌘⇧T" else "Ctrl+Shift+T"} and Recently closed can bring back.",
            Icons.Default.Restore, ClosedKeptOptions.map { it to "$it" }, closedKept,
        ) { closedKept = it },
        switchRow(
            "confirmQuit", "Ask before quitting with several tabs open",
            "Closing the main window quits the app. This asks first when 3 or more tabs are open.",
            Icons.Default.Close, confirmQuit,
        ) { confirmQuit = it },
    )

    val memory = listOf(
        menuRow(
            "sleepAfter", "Put unused tabs to sleep",
            "A sleeping tab frees its memory and reloads when you open it again. The active tab, a playing tab " +
                "and Home never sleep.",
            Icons.Default.Bedtime,
            listOf(
                SleepAfter.Never to "Never",
                SleepAfter.M15 to "After 15 minutes",
                SleepAfter.H1 to "After 1 hour",
                SleepAfter.H4 to "After 4 hours",
            ),
            sleepAfter,
        ) { sleepAfter = it },
    )

    val playback = if (!miniPlayerSupported) emptyList() else listOfNotNull(
        switchRow(
            "miniPlayer", "Mini player when leaving a playing tab", "Keeps the video going in a corner of the window.",
            Icons.Default.PictureInPicture, miniPlayer,
        ) { miniPlayer = it },
        cornerRow(
            "corner", "Mini player corner", "You can still drag it while it plays. Dragging doesn't change this setting.",
            Icons.Default.PictureInPicture, corner,
        ) { corner = it }.takeIf { miniPlayer },
        switchRow(
            "pauseUnfocused", "Pause when the window loses focus",
            "Pauses the mini player when you switch to another app. It doesn't resume on its own.",
            Icons.Default.PictureInPicture, pauseUnfocused,
        ) { pauseUnfocused = it }.takeIf { miniPlayer },
    )

    val windowsAndMotion = listOf(
        switchRow(
            "tearOut", "Drag tabs out into windows",
            "Off keeps everything in one window: dragging only reorders, and Open in new window and ${mod}N are hidden.",
            Icons.AutoMirrored.Filled.OpenInNew, tearOut,
        ) { tearOut = it },
        choiceRow(
            "motion", "Tab animations",
            "System follows your computer's reduce-motion setting. Reduced turns tab motion into short crossfades, " +
                "and Off makes tab changes instant. Covers tab and window motion only.",
            Icons.Default.Animation,
            listOf(
                TabMotionMode.System to "System",
                TabMotionMode.Full to "Full",
                TabMotionMode.Reduced to "Reduced",
                TabMotionMode.Off to "Off",
            ),
            motion,
        ) { motion = it },
        linkRow(
            "shortcuts", "Keyboard shortcuts",
            if (isMacOs) "⌘T, ⌘W, ⌃Tab, ⌘L and the rest." else "Ctrl+T, Ctrl+W, Ctrl+Tab, Ctrl+L and the rest.",
            Icons.Default.Keyboard, Icons.AutoMirrored.Filled.KeyboardArrowRight,
        ) { quickActions.openShortcuts() },
    )

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Tabs & browsing") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            settingsGroup("Tabs", tabs)
            settingsGroup("New tab page", newTabPage)
            settingsGroup("Startup", startup)
            settingsGroup("Memory", memory)
            settingsGroup("Playback", playback)
            settingsGroup("Windows & motion", windowsAndMotion)
        }
    }
}
