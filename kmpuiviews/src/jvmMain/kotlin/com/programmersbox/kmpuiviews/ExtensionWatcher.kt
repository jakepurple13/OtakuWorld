package com.programmersbox.kmpuiviews

import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import io.github.irgaly.kfswatch.KfsDirectoryWatcherEvent
import io.github.irgaly.kfswatch.KfsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/**
 * Watches the extensions directory for changes and emits events when extensions are added or removed.
 * Desktop-only implementation using Java WatchService.
 */
class ExtensionWatcher(
    private val extensionsDir: Flow<String>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    private val watcher = KfsDirectoryWatcher(scope)

    fun observeExtensionsDir() = extensionsDir
        .onEach {
            watcher.removeAll()
            watcher.add(it)
        }
        .flatMapLatest { watcher.onEventFlow }
        .onStart { emit(KfsDirectoryWatcherEvent("", "", KfsEvent.Create)) }
}
