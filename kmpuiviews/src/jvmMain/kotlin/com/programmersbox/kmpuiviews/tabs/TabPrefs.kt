package com.programmersbox.kmpuiviews.tabs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.DataStoreHandlerNullable
import com.programmersbox.datastore.DataStoreHandlerObject
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/** Where the tab strip sits. */
enum class TabPosition {
    Top, Side;

    companion object {
        fun fromName(name: String?): TabPosition = entries.firstOrNull { it.name == name } ?: Top
    }
}

/** Whether a normal click on a link reuses the current tab or opens a new one. */
enum class ClickOpens {
    SameTab, NewTab;

    companion object {
        fun fromName(name: String?): ClickOpens = entries.firstOrNull { it.name == name } ?: SameTab
    }
}

/** What ⌘/Ctrl-click opens. [Foreground] swaps ⌘-click and ⌘⇧-click; middle-click follows ⌘-click. */
enum class ModClick {
    Background, Foreground;

    companion object {
        fun fromName(name: String?): ModClick = entries.firstOrNull { it.name == name } ?: Background
    }
}

/** Where a new tab goes: right of the active tab (after its background run), or at the end of the window. */
enum class NewTabPlace {
    NextToCurrent, AtEnd;

    companion object {
        fun fromName(name: String?): NewTabPlace = entries.firstOrNull { it.name == name } ?: NextToCurrent
    }
}

/** Which tab becomes active when the active one closes. */
enum class AfterClose {
    Right, Left, LastUsed;

    companion object {
        fun fromName(name: String?): AfterClose = entries.firstOrNull { it.name == name } ?: Right
    }
}

/** Tab and window motion only. [System] follows the OS reduce-motion setting. */
enum class TabMotionMode {
    System, Full, Reduced, Off;

    companion object {
        fun fromName(name: String?): TabMotionMode = entries.firstOrNull { it.name == name } ?: System
    }
}

/** What ⌘T and "+" show. */
enum class NewTabShows {
    Page, Recent, Continue, Blank;

    companion object {
        fun fromName(name: String?): NewTabShows = entries.firstOrNull { it.name == name } ?: Page
    }
}

enum class NewTabBackground {
    Plain, LastCover;

    companion object {
        fun fromName(name: String?): NewTabBackground = entries.firstOrNull { it.name == name } ?: Plain
    }
}

/** What the app shows when it opens. Replaces the `desktop_tabs_restore` switch. */
enum class OnLaunch {
    Restore, Home, NewTab;

    companion object {
        fun fromName(name: String?): OnLaunch = entries.firstOrNull { it.name == name } ?: Restore
    }
}

enum class SleepAfter {
    Never, M15, H1, H4;

    companion object {
        fun fromName(name: String?): SleepAfter = entries.firstOrNull { it.name == name } ?: Never
    }
}

enum class MiniPlayerCorner {
    TopLeft, TopRight, BottomLeft, BottomRight;

    companion object {
        fun fromName(name: String?): MiniPlayerCorner = entries.firstOrNull { it.name == name } ?: BottomRight
    }
}

/** The new tab page's sections, in their default order. */
enum class NewTabSection(val defaultOn: Boolean) {
    Search(true), Continue(true), Updates(true), Closed(true), Links(false)
}

data class SectionSetting(val section: NewTabSection, val on: Boolean)

/**
 * Reads `Search:1,Continue:1,...`. Unknown or malformed entries are dropped, a repeated section keeps its first
 * entry, and sections missing from [raw] are appended in their default order with their default flag.
 */
fun parseSections(raw: String?): List<SectionSetting> {
    val parsed = raw.orEmpty()
        .split(',')
        .mapNotNull { entry ->
            val parts = entry.trim().split(':')
            if (parts.size != 2) return@mapNotNull null
            val section = NewTabSection.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            when (parts[1]) {
                "1" -> SectionSetting(section, true)
                "0" -> SectionSetting(section, false)
                else -> null
            }
        }
        .distinctBy { it.section }
    val missing = NewTabSection.entries
        .filter { section -> parsed.none { it.section == section } }
        .map { SectionSetting(it, it.defaultOn) }
    return parsed + missing
}

fun formatSections(sections: List<SectionSetting>): String =
    sections.joinToString(",") { "${it.section.name}:${if (it.on) 1 else 0}" }

val ContinueCountOptions = listOf(4, 8, 12)
val ClosedKeptOptions = listOf(10, 25, 50)

fun continueCountOf(value: Int?): Int = value?.takeIf { it in ContinueCountOptions } ?: 4

fun closedKeptOf(value: Int?): Int = value?.takeIf { it in ClosedKeptOptions } ?: 25

/** The old "Reopen tabs on launch" switch: on (or never set) reopens tabs, off opens Home only. */
fun migrateOnLaunch(oldRestore: Boolean?): OnLaunch = if (oldRestore == false) OnLaunch.Home else OnLaunch.Restore

private fun <E : Enum<E>> enumPref(key: String, default: E, fromName: (String?) -> E) = DataStoreHandlerObject(
    key = stringPreferencesKey(key),
    defaultValue = default,
    mapToType = fromName,
    mapToKey = { it.name },
)

private fun boolPref(key: String, default: Boolean) = DataStoreHandlerObject(
    key = booleanPreferencesKey(key),
    defaultValue = default,
    mapToType = { it },
    mapToKey = { it },
)

val tabPositionPref = enumPref("desktop_tab_position", TabPosition.Top, TabPosition::fromName)
val tabClickOpensPref = enumPref("desktop_tab_click_opens", ClickOpens.SameTab, ClickOpens::fromName)

/** Superseded by [tabsOnLaunchPref]; only read once, by [migrateTabsOnLaunch]. */
val tabsRestorePref = boolPref("desktop_tabs_restore", true)

val tabsMiniPlayerPref = boolPref("desktop_tabs_mini_player", true)
val tabModClickPref = enumPref("desktop_tab_mod_click", ModClick.Background, ModClick::fromName)
val tabNewPlacePref = enumPref("desktop_tab_new_place", NewTabPlace.NextToCurrent, NewTabPlace::fromName)
val tabAfterClosePref = enumPref("desktop_tab_after_close", AfterClose.Right, AfterClose::fromName)
val tabShowCoversPref = boolPref("desktop_tab_show_covers", true)
val tabMotionPref = enumPref("desktop_tab_motion", TabMotionMode.System, TabMotionMode::fromName)
val newTabShowsPref = enumPref("desktop_new_tab_shows", NewTabShows.Page, NewTabShows::fromName)

val newTabSectionsPref = DataStoreHandlerObject(
    key = stringPreferencesKey("desktop_new_tab_sections"),
    defaultValue = parseSections(null),
    mapToType = { parseSections(it) },
    mapToKey = { formatSections(it) },
)

val newTabContinueCountPref = DataStoreHandlerObject(
    key = intPreferencesKey("desktop_new_tab_continue_count"),
    defaultValue = 4,
    mapToType = { continueCountOf(it) },
    mapToKey = { it },
)

val newTabBackgroundPref = enumPref("desktop_new_tab_background", NewTabBackground.Plain, NewTabBackground::fromName)
val newTabFocusSearchPref = boolPref("desktop_new_tab_focus_search", true)
val tabsOnLaunchPref = enumPref("desktop_tabs_on_launch", OnLaunch.Restore, OnLaunch::fromName)

val tabsClosedKeptPref = DataStoreHandlerObject(
    key = intPreferencesKey("desktop_tabs_closed_kept"),
    defaultValue = 25,
    mapToType = { closedKeptOf(it) },
    mapToKey = { it },
)

val tabsConfirmQuitPref = boolPref("desktop_tabs_confirm_quit", true)
val tabsSleepAfterPref = enumPref("desktop_tabs_sleep_after", SleepAfter.Never, SleepAfter::fromName)
val miniPlayerCornerPref = enumPref("desktop_mini_player_corner", MiniPlayerCorner.BottomRight, MiniPlayerCorner::fromName)
val miniPlayerPauseUnfocusedPref = boolPref("desktop_mini_player_pause_unfocused", false)
val tabsTearOutPref = boolPref("desktop_tabs_tear_out", true)

/**
 * Writes `desktop_tabs_on_launch` from the old `desktop_tabs_restore` switch the first time, when it is not set yet.
 * The old key stays in place.
 */
suspend fun migrateTabsOnLaunch() {
    if (DataStoreHandlerNullable(stringPreferencesKey("desktop_tabs_on_launch")).getOrNull() != null) return
    val old = DataStoreHandlerNullable(booleanPreferencesKey("desktop_tabs_restore")).getOrNull()
    tabsOnLaunchPref.set(migrateOnLaunch(old))
}

/** Registered only by apps with a mini player (AnimeWorld). Its rows in Tabs & browsing show only when true. */
data class MiniPlayerSupported(val supported: Boolean)

/**
 * Desktop tabs wiring. [TabsController] is created at start so it is already the Home tab's
 * navigation interceptor before the first navigation (the onboarding redirect).
 */
internal val desktopTabsModule: Module = module {
    single { CoreTabKeyProvider } bind TabKeyProvider::class
    single { TabKeys(getAll()) }
    single(createdAtStart = true) {
        TabsController(
            homeNav = get<Navigation3Actions>(),
            keys = get(),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
            handoffs = getAll(),
            downloadsKey = getOrNull<DownloadsDestination>()?.key,
        ).also { controller ->
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            scope.launch { migrateTabsOnLaunch() }
            // The saved position before the first frame, so a Side layout does not open as Top and glide over.
            controller.motion.switchLayout(runBlocking { tabPositionPref.get() }, animate = false)
            combine(
                tabClickOpensPref.asFlow(),
                tabModClickPref.asFlow(),
                tabNewPlacePref.asFlow(),
                tabAfterClosePref.asFlow(),
                tabsClosedKeptPref.asFlow(),
            ) { clickOpens, modClick, newPlace, afterClose, closedKept ->
                TabPrefsSnapshot(
                    clickOpensNewTab = clickOpens == ClickOpens.NewTab,
                    modClick = modClick,
                    newPlace = newPlace,
                    afterClose = afterClose,
                    closedKept = closedKept,
                )
            }
                .onEach { controller.prefs = it }
                .launchIn(scope)
            tabMotionPref.asFlow()
                .onEach { controller.motion.mode = it }
                .launchIn(scope)
        }
    }
    single {
        MiniPlayerRegistry(get()).also { registry ->
            // On the UI thread: turning the mini player off pauses and hides it.
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            tabsMiniPlayerPref.asFlow().onEach { registry.enabled = it }.launchIn(scope)
            miniPlayerPauseUnfocusedPref.asFlow().onEach { registry.pauseUnfocused = it }.launchIn(scope)
        }
    }
}
