package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.json.JsonElement

/** How long tab, stack and window changes settle before the session is written. */
const val TAB_SESSION_SAVE_DEBOUNCE_MS = 1_000L

/**
 * Every window and tab. Home saves only its current top-level stack. A stack stops at the first key [encode] can't
 * save, and a tab whose first key can't be saved is left out.
 */
fun TabsController.toSession(encode: (NavKey) -> JsonElement?): TabSession = TabSession(
    windows = windows.map { window ->
        val saved = window.tabs.mapNotNull { tab ->
            val stack = if (tab.pinned) tab.nav.currentTopLevelStack() else tab.nav.backStack.toList()
            stack.decodablePrefix(encode)
                .takeIf { it.isNotEmpty() }
                ?.let { tab.id to TabSessionEntry(tab.pinned, it) }
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
            val page = restoredTab(listOf(NewTabRoute))
            main.tabs.add(page)
            activate(page.id)
        }
    }
}

/**
 * Restores [session]'s tabs and torn-out windows. Each stack keeps its decodable prefix; a tab with none is dropped,
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
            val stack = entry.backStack.decodablePrefix(decode)
            when {
                stack.isEmpty() -> null
                entry.pinned && saved.isMain -> home.also { it.nav.restoreTopLevelStack(stack) }
                entry.pinned -> null
                else -> restoredTab(stack)
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

/** The keys up to the first one [convert] can't handle. */
private fun <A, B : Any> List<A>.decodablePrefix(convert: (A) -> B?): List<B> {
    val out = mutableListOf<B>()
    for (item in this) out += convert(item) ?: break
    return out
}

/** The stack of the top-level destination Home is on now (Recent's own stack, or e.g. Settings and what's above it). */
private fun Navigation3Actions.currentTopLevelStack(): List<NavKey> {
    val keys = backStack.toList()
    val start = keys.lastIndexOf(navBackStack.topLevelKey).coerceAtLeast(0)
    return keys.subList(start, keys.size)
}

/** Puts [stack] back as Home's current top-level stack, above its root. */
private fun Navigation3Actions.restoreTopLevelStack(stack: List<NavKey>) {
    if (stack.first() != backStack.first()) navBackStack.addTopLevel(stack.first())
    stack.drop(1).forEach { navBackStack.add(it) }
}
