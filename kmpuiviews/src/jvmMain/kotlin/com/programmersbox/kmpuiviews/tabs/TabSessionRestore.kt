package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.json.JsonElement

/** How long tab, stack and window changes settle before the session is written. */
const val TAB_SESSION_SAVE_DEBOUNCE_MS = 1_000L

/**
 * Every window and tab, each with its whole history and where it is in it. Keys [encode] can't save are skipped, and a
 * tab with none left is left out.
 */
fun TabsController.toSession(encode: (NavKey) -> JsonElement?): TabSession = TabSession(
    windows = windows.map { window ->
        val saved = window.tabs.mapNotNull { tab ->
            val (entries, index) = keepConvertible(tab.nav.history.entries, tab.nav.history.index, encode)
            entries.takeIf { it.isNotEmpty() }
                ?.let { tab.id to TabSessionEntry(tab.pinned, it, index) }
        }
        WindowSession(
            bounds = window.bounds.value,
            isMain = window.isMain,
            activeIndex = saved.indexOfFirst { it.first == window.activeId.value }.coerceAtLeast(0),
            tabs = saved.map { it.second },
        )
    }
)

/**
 * Applies the "When the app opens" choice. Until onboarding is done only Home is shown, so nothing hides it. The main
 * window's position and size come back whatever the choice.
 */
fun TabsController.startSession(
    onLaunch: OnLaunch,
    onboardingDone: Boolean,
    session: TabSession?,
    decode: (JsonElement) -> NavKey?,
    onScreen: (WindowBounds) -> Boolean = ::isOnScreen,
) {
    val main = windows.first { it.isMain }
    session?.windows?.firstOrNull { it.isMain }?.bounds?.let { main.bounds.value = it }
    if (!onboardingDone) return
    when (onLaunch) {
        OnLaunch.Restore -> session?.let { restore(it, decode, onScreen) }
        OnLaunch.Home -> Unit
        OnLaunch.NewTab -> {
            val page = restoredTab(listOf(NewTabRoute), -1)
            main.tabs.add(page)
            activate(page.id)
        }
    }
}

/**
 * Restores [session]'s tabs and torn-out windows. Each history keeps its decodable entries; a tab with none is dropped,
 * and a torn-out window with no tabs left is not recreated. Restored tabs stay parked until first shown.
 */
fun TabsController.restore(
    session: TabSession,
    decode: (JsonElement) -> NavKey?,
    onScreen: (WindowBounds) -> Boolean = ::isOnScreen,
) {
    val main = windows.first { it.isMain }
    val home = main.tabs.first { it.pinned }
    var mainActive: TabState? = null

    session.windows.forEach { saved ->
        val restored = saved.tabs.map { entry ->
            val (entries, index) = keepConvertible(entry.backStack, entry.index, decode)
            when {
                entries.isEmpty() -> null
                entry.pinned && saved.isMain -> home.also {
                    it.nav.restoreHistory(entries, index)
                    markRestored(it)
                }
                entry.pinned -> null
                else -> restoredTab(entries, index)
            }
        }
        val active = restored.getOrNull(saved.activeIndex)
        val tabs = restored.filterNotNull()

        if (saved.isMain) {
            tabs.filter { it !== home }.forEach { main.tabs.add(it) }
            mainActive = active ?: home
        } else if (tabs.isNotEmpty()) {
            // The same check the main window gets: off-screen (a screen since unplugged) opens at the default spot.
            val bounds = saved.bounds?.takeIf(onScreen) ?: TabsController.DefaultWindowBounds
            val window = restoredWindow(tabs.first(), bounds)
            tabs.drop(1).forEach { window.tabs.add(it) }
            activate((active ?: tabs.first()).id)
        }
    }
    // Last, so the main window ends up focused.
    activate((mainActive ?: home).id)
}

/**
 * Queues the session on [writes] [TAB_SESSION_SAVE_DEBOUNCE_MS] after tabs, stacks or windows stop changing. Runs until
 * cancelled. The quit save goes on the same queue, so it always lands after any autosave, and quit waits for both.
 */
@OptIn(FlowPreview::class)
suspend fun TabSessionStore.autosave(controller: TabsController, writes: PendingWrites = PendingWrites.App) {
    snapshotFlow { controller.toSession(::encode) }
        .debounce(TAB_SESSION_SAVE_DEBOUNCE_MS)
        .collect { session -> writes.launch { write(session) } }
}

/**
 * The items [convert] can handle, and where [index] lands among them: the last kept item at or before it (0 if none).
 * A negative [index] means the last item.
 */
private fun <A, B : Any> keepConvertible(items: List<A>, index: Int, convert: (A) -> B?): Pair<List<B>, Int> {
    val at = if (index < 0) items.lastIndex else index
    val kept = mutableListOf<B>()
    var keptIndex = 0
    items.forEachIndexed { i, item ->
        val converted = convert(item) ?: return@forEachIndexed
        kept += converted
        if (i <= at) keptIndex = kept.lastIndex
    }
    return kept to keptIndex
}
