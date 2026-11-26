package com.programmersbox.kmpuiviews.tabs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Writes that must finish even when the screen, tab or ViewModel that asked for them is gone. They run in
 * an app-lifetime scope under [NonCancellable]; quit waits on [awaitAll] before the process exits.
 */
class PendingWrites(private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
    private val jobs = ConcurrentHashMap.newKeySet<Job>()

    fun launch(block: suspend () -> Unit) {
        val job = scope.launch(NonCancellable) { block() }
        jobs += job
        job.invokeOnCompletion { jobs -= job }
    }

    suspend fun awaitAll() = jobs.toList().joinAll()

    companion object {
        val App = PendingWrites()
    }
}

/**
 * Saves a note's text [debounceMs] after the last edit, and on [flush] (the panel leaving composition).
 * [debounceScope] only times the debounce; the write itself goes through [writes], so cancelling the
 * scope or clearing the ViewModel never drops it.
 */
class NoteDraftSaver(
    private val debounceScope: CoroutineScope,
    private val writes: PendingWrites,
    private val persist: suspend (String) -> Unit,
    private val debounceMs: Long = NoteSaveDebounceMs,
) {
    private var pending: String? = null
    private var debounce: Job? = null

    fun edit(text: String) {
        pending = text
        debounce?.cancel()
        debounce = debounceScope.launch {
            delay(debounceMs)
            flush()
        }
    }

    fun flush() {
        debounce?.cancel()
        val text = pending ?: return
        pending = null
        writes.launch { persist(text) }
    }

    /** Drops the pending edit, for when the note is deleted. */
    fun cancel() {
        debounce?.cancel()
        pending = null
    }
}

const val NoteSaveDebounceMs = 500L

/**
 * The panel's field state. [edited] means the user typed text that is not in the database yet; while it is
 * set the database does not overwrite the field, and once the database matches it clears again.
 */
data class NoteDraft(val text: String = "", val edited: Boolean = false) {
    fun typed(value: String) = NoteDraft(value, edited = true)

    fun onNote(content: String?): NoteDraft = when {
        edited && content == text -> copy(edited = false)
        !edited -> NoteDraft(content.orEmpty())
        else -> this
    }
}
