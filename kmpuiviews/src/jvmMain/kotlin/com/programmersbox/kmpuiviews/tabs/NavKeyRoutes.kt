package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.koin.compose.navigation3.EntryProviderInstaller
import org.koin.core.annotation.KoinExperimentalAPI
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private const val UnroutableMarker = "otaku.unroutable"

/**
 * Which keys have a page: the same Koin `navigation<Key>` entries `koinEntryProvider` builds from, asked outside
 * composition. A restored key with no page would crash the tab ("Unknown screen"), so the session drops it.
 */
@OptIn(KoinExperimentalAPI::class)
class NavKeyRoutes(installers: List<EntryProviderInstaller>) {
    private val provider: (Any) -> NavEntry<Any> = entryProvider(
        fallback = { NavEntry(it, metadata = mapOf(UnroutableMarker to true)) {} },
    ) { installers.forEach { it() } }

    private val known = ConcurrentHashMap<KClass<*>, Boolean>()

    /** Whether [key] has a registered entry. Entries match by class, so the answer is kept per class. */
    fun routable(key: NavKey): Boolean = known.getOrPut(key::class) {
        runCatching { provider(key).metadata[UnroutableMarker] != true }.getOrDefault(false)
    }
}

/** Shown instead of crashing for a page with no registered entry (see `Nav3(unknownKeyEntry)`). */
fun pageUnavailableEntry(key: NavKey): NavEntry<NavKey> = NavEntry(key) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Page unavailable", style = MaterialTheme.typography.titleMedium)
    }
}
