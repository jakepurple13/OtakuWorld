package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.mutableStateListOf
import com.programmersbox.kmpuiviews.presentation.components.PageRefresh
import com.programmersbox.kmpuiviews.presentation.components.PageRefreshHost

/**
 * The reload a tab's current page offers to the toolbar and shortcuts. The page that registered last wins; when it
 * leaves composition the one before it (if still composed) takes over.
 */
class TabRefreshRegistry : PageRefreshHost {
    private val pages = mutableStateListOf<PageRefresh>()

    val page: PageRefresh? get() = pages.lastOrNull()
    val canRefresh: Boolean get() = page != null
    val refreshing: Boolean get() = page?.refreshing?.invoke() == true
    val canStop: Boolean get() = refreshing && page?.stop != null

    override fun register(page: PageRefresh): () -> Unit {
        pages += page
        return { pages.remove(page) }
    }
}

/** Reloads [id]'s current page, unless it is already reloading. */
fun TabsController.refresh(id: TabId) {
    val registry = registryOf(id) ?: return
    if (!registry.refreshing) registry.page?.refresh?.invoke()
}

/** Cancels [id]'s reload, when its page can. */
fun TabsController.stopRefresh(id: TabId) {
    val registry = registryOf(id) ?: return
    if (registry.canStop) registry.page?.stop?.invoke()
}

private fun TabsController.registryOf(id: TabId): TabRefreshRegistry? =
    windows.firstNotNullOfOrNull { window -> window.tabs.firstOrNull { it.id == id } }?.refresh

/** Whether Esc would stop a reload in this window's active tab. */
val BrowserWindowState.canStopRefresh: Boolean
    get() = tabs.firstOrNull { it.id == activeId.value }?.refresh?.canStop == true
