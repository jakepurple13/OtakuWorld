package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/** A page's reload, as offered to a host such as the desktop tab toolbar. */
class PageRefresh(
    val refreshing: () -> Boolean,
    val refresh: () -> Unit,
    /** Null when the reload can't be cancelled. */
    val stop: (() -> Unit)?,
)

/** Takes a page's [PageRefresh]; the returned call removes it again. */
fun interface PageRefreshHost {
    fun register(page: PageRefresh): () -> Unit
}

/** The host pages register with. Null outside the desktop tab shell, so registering does nothing on mobile. */
val LocalPageRefreshHost: ProvidableCompositionLocal<PageRefreshHost?> = staticCompositionLocalOf { null }

/** Offers this page's reload to [LocalPageRefreshHost] for as long as the page is composed. */
@Composable
fun RegisterPageRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onStop: (() -> Unit)? = null,
) {
    val host = LocalPageRefreshHost.current ?: return
    val currentRefreshing by rememberUpdatedState(refreshing)
    val currentRefresh by rememberUpdatedState(onRefresh)
    val currentStop by rememberUpdatedState(onStop)
    val canStop = onStop != null
    DisposableEffect(host, canStop) {
        val unregister = host.register(
            PageRefresh(
                refreshing = { currentRefreshing },
                refresh = { currentRefresh() },
                stop = if (canStop) ({ currentStop?.invoke() }) else null,
            )
        )
        onDispose(unregister)
    }
}
