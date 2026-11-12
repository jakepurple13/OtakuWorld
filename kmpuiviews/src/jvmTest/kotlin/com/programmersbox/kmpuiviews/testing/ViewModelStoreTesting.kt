package com.programmersbox.kmpuiviews.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Clears the store and waits until every ViewModel's viewModelScope has finished cancelling.
 * Returns the cleared ViewModels.
 *
 * clear() only cancels the scopes; their coroutines finish on other threads and may still
 * dispatch to Dispatchers.Main. Calling resetMain while that happens fails with
 * "Dispatchers.Main is used concurrently with setting it", so call this before resetMain.
 */
fun ViewModelStore.clearAndJoin(): List<ViewModel> {
    val viewModels = keys().mapNotNull { get(it) }
    val jobs = viewModels.map { it.viewModelScope.coroutineContext.job }
    clear()
    runBlocking { withTimeout(5_000) { jobs.joinAll() } }
    return viewModels
}
