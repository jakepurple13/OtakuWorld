package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.runtime.mutableStateMapOf
import com.programmersbox.kmpmodels.KmpRemoteSources
import kotlin.coroutines.cancellation.CancellationException

sealed class RemoteState

class RemoteViewState(
    val sources: List<KmpRemoteSources>,
) : RemoteState()

class RemoteErrorState(val message: String? = null) : RemoteState()

data object RemoteLoadingState : RemoteState()

/** Remote extension lists per catalog name, each independently loadable and retryable. */
class RemoteCatalogs {
    val states = mutableStateMapOf<String, RemoteState>()
    private val fetchers = mutableMapOf<String, suspend () -> List<KmpRemoteSources>>()

    /** Shows [RemoteLoadingState] only on first load, so reloads never blank a loaded shelf. */
    suspend fun load(name: String, fetch: suspend () -> List<KmpRemoteSources>) {
        fetchers[name] = fetch
        if (name !in states) states[name] = RemoteLoadingState
        states[name] = fetchState(fetch)
    }

    suspend fun retry(name: String) {
        val fetch = fetchers[name] ?: return
        states[name] = RemoteLoadingState
        states[name] = fetchState(fetch)
    }

    private suspend fun fetchState(fetch: suspend () -> List<KmpRemoteSources>): RemoteState = try {
        RemoteViewState(fetch())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        RemoteErrorState(e.message)
    }
}
