package com.programmersbox.kmpuiviews.tabs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Writes that must finish even when the screen, tab or ViewModel that asked for them is gone. One consumer in
 * an app-lifetime scope runs them under [NonCancellable], one at a time in the order they were queued, so an
 * older write can never land after a newer one. A failed write is logged and the queue carries on. Quit waits
 * on [awaitAll] before the process exits.
 */
class PendingWrites(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch(NonCancellable) {
            for (write in queue) {
                try {
                    write()
                } catch (e: Throwable) {
                    log.log(Level.WARNING, "Pending write failed", e)
                }
            }
        }
    }

    fun launch(block: suspend () -> Unit) {
        queue.trySend(block)
    }

    /** Returns once everything queued before this call has run. */
    suspend fun awaitAll() {
        val barrier = CompletableDeferred<Unit>()
        queue.trySend { barrier.complete(Unit) }
        barrier.await()
    }

    companion object {
        private val log = Logger.getLogger(PendingWrites::class.java.name)
        val App = PendingWrites()
    }
}

/**
 * Saves a note's text [debounceMs] after the last edit, and on [flush] (the panel leaving composition).
 * [debounceScope] only times the debounce; the write itself goes through [writes], so cancelling the
 * scope or clearing the ViewModel never drops it. [persist] gets `allowDelete = false` for debounced saves:
 * blank text mid-edit is kept as pending but never deletes the note, only the final flush may.
 */
class NoteDraftSaver(
    private val debounceScope: CoroutineScope,
    private val writes: PendingWrites,
    private val persist: suspend (text: String, allowDelete: Boolean) -> Unit,
    private val debounceMs: Long = NoteSaveDebounceMs,
) {
    private var pending: String? = null
    private var debounce: Job? = null

    fun edit(text: String) {
        pending = text
        debounce?.cancel()
        debounce = debounceScope.launch {
            delay(debounceMs)
            write(allowDelete = false)
        }
    }

    /** The final save: the only one that may delete, when the text ends up blank. */
    fun flush() {
        debounce?.cancel()
        write(allowDelete = true)
    }

    private fun write(allowDelete: Boolean) {
        val text = pending ?: return
        // Blank mid-edit: keep it pending so a later flush still sees the final text
        if (text.isBlank() && !allowDelete) return
        pending = null
        writes.launch { persist(text, allowDelete) }
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
