package com.programmersbox.koogintegration.embedding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Desktop scheduling: call [refreshOnStartup] once when the application
 * launches (e.g. from main() or the root composable's scope).
 */
class DesktopEmbeddingRefresher(
    private val repository: FavoritesEmbeddingRepository,
) {
    fun refreshOnStartup(scope: CoroutineScope): Job = scope.launch {
        runCatching { repository.refreshEmbeddings() }
    }
}
