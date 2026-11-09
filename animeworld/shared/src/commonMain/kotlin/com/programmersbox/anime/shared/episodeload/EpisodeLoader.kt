package com.programmersbox.anime.shared.episodeload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile

enum class EpisodeAction { Play, Download }

sealed interface EpisodeLoadStep {
    data object FetchingSources : EpisodeLoadStep
    data class ChooseSource(val sources: List<KmpStorage>) : EpisodeLoadStep

    /** `label == null` means a single source: show "Resolving stream". */
    data class Resolving(val label: String?) : EpisodeLoadStep
    data class Failed(val message: String, val detail: String?, val canPickOther: Boolean) : EpisodeLoadStep

    /** The stream is resolved and being handed to the player; the overlay stays up until [EpisodeLoader.finishHandoff]. */
    data object OpeningPlayer : EpisodeLoadStep
}

// A slow phone (debug build) took 6-8.5 s from here until the activity was recreated in landscape for the player.
private const val HANDOFF_TIMEOUT_MS = 10_000L

data class EpisodeLoadState(
    val action: EpisodeAction,
    val episodeUrl: String,
    val episodeName: String,
    val showTitle: String,
    val posterUrl: String?,
    val step: EpisodeLoadStep,
    /** Image request headers for [posterUrl]; the details screen sends the same ones. */
    val posterHeaders: Map<String, String> = emptyMap(),
)

sealed interface EpisodeLoadDecision {
    data class Pick(val storage: KmpStorage) : EpisodeLoadDecision
    data object Retry : EpisodeLoadDecision
    data object PickOther : EpisodeLoadDecision
    data object Cancel : EpisodeLoadDecision
}

/**
 * Drives fetching an episode's sources and resolving a stream, exposing [state] for the UI.
 * [load] suspends until the stream is resolved and handed over (including waiting on the user), so the caller's
 * coroutine is its lifetime up to that point. A Play's [EpisodeLoadStep.OpeningPlayer] wait runs in
 * [handoffScope] instead: the caller's scope (the details screen) can be cancelled while the player opens.
 */
class EpisodeLoader(
    private val handoffScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    var state: EpisodeLoadState? by mutableStateOf(null)
        private set

    private val decisions = Channel<EpisodeLoadDecision>(Channel.CONFLATED)

    // decide/finishHandoff run on Main while load runs on IO.
    @Volatile
    private var activeJob: Job? = null

    @Volatile
    private var handoff: CompletableDeferred<Unit>? = null

    /** Clears an [EpisodeLoadStep.OpeningPlayer] state once the player is up; no-op in any other step. */
    fun finishHandoff() {
        if (state?.step == EpisodeLoadStep.OpeningPlayer) {
            state = null
            handoff?.complete(Unit)
        }
    }

    /** [EpisodeLoadDecision.Cancel] ends the running load at any step; the rest feed the step waiting on the user. */
    fun decide(decision: EpisodeLoadDecision) {
        if (decision == EpisodeLoadDecision.Cancel) {
            // Cancelling during the hand-off must not cancel the navigation/cast running in onResolved.
            if (state?.step == EpisodeLoadStep.OpeningPlayer) {
                finishHandoff()
                return
            }
            // Once the state is released the stream is already handed off, so there is nothing left to cancel.
            if (state != null) activeJob?.cancel()
        } else {
            decisions.trySend(decision)
        }
    }

    private val busy = Mutex()

    /**
     * Only one load runs at a time; a call while another is active (including a Play's player hand-off) returns
     * immediately. A Play returns right after [onResolved]; the overlay is held until [finishHandoff] or a timeout.
     * [onDownloadFailed]'s `storage == null` means no sources were found.
     */
    suspend fun load(
        action: EpisodeAction,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        onResolved: suspend (KmpStorage) -> Unit,
        onDownloadFailed: suspend (KmpStorage?, Throwable?) -> Unit = { _, _ -> },
    ) {
        if (!busy.tryLock()) return
        var handedOff = false
        try {
            coroutineScope {
                // Lazy so a Cancel that arrives as soon as the first step shows still finds the job.
                val job = launch(start = CoroutineStart.LAZY) {
                    handedOff = run(action, model, infoModel, onResolved, onDownloadFailed)
                }
                activeJob = job
                job.start()
                job.join()
            }
        } finally {
            activeJob = null
            // After a hand-off, the hand-off job owns the state, the lock and [handoff].
            if (!handedOff) {
                handoff = null
                busy.unlock()
            }
        }
    }

    /** Holds [EpisodeLoadStep.OpeningPlayer] until [finishHandoff] or the timeout, then frees the loader. */
    private fun holdHandoff(wait: CompletableDeferred<Unit>) {
        handoffScope.launch { withTimeoutOrNull(HANDOFF_TIMEOUT_MS) { wait.await() } }
            // Runs even if the scope never starts the job, so the lock is always released.
            .invokeOnCompletion {
                if (state?.step == EpisodeLoadStep.OpeningPlayer) state = null
                handoff = null
                busy.unlock()
            }
    }

    private fun drainDecisions() {
        while (decisions.tryReceive().isSuccess) Unit
    }

    /** Returns true when a Play was handed to the player and [holdHandoff] now owns the rest. */
    private suspend fun run(
        action: EpisodeAction,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        onResolved: suspend (KmpStorage) -> Unit,
        onDownloadFailed: suspend (KmpStorage?, Throwable?) -> Unit,
    ): Boolean {
        drainDecisions()

        fun show(step: EpisodeLoadStep) {
            state = EpisodeLoadState(
                action = action,
                episodeUrl = model.url,
                episodeName = model.name,
                showTitle = infoModel.title,
                posterUrl = infoModel.imageUrl.takeIf { it.isNotBlank() },
                step = step,
                posterHeaders = infoModel.extras.mapValues { it.value.toString() },
            )
        }

        // Once released, a newer load may own the state, so the finally must not touch it.
        var owned = true
        fun release() {
            state = null
            owned = false
        }

        // Cancel never comes through the channel (it cancels the job), and a decision left over from a
        // double tap on a fading-out button is ignored instead of ending the load.
        suspend fun awaitDecision(accept: (EpisodeLoadDecision) -> Boolean): EpisodeLoadDecision {
            while (true) {
                val decision = decisions.receive()
                if (accept(decision)) return decision
            }
        }

        try {
            var sources: List<KmpStorage>
            while (true) {
                show(EpisodeLoadStep.FetchingSources)
                sources = try {
                    model.getChapterInfo().firstOrNull().orEmpty()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
                if (sources.isNotEmpty()) break

                if (action == EpisodeAction.Download) {
                    release()
                    onDownloadFailed(null, null)
                    return false
                }
                drainDecisions()
                show(EpisodeLoadStep.Failed("No sources found for ${model.name}", null, false))
                awaitDecision { it == EpisodeLoadDecision.Retry }
            }

            var storage: KmpStorage? = sources.singleOrNull()
            var label: String? = null
            while (true) {
                val current = storage ?: run {
                    drainDecisions()
                    show(EpisodeLoadStep.ChooseSource(sources))
                    val picked = (awaitDecision { it is EpisodeLoadDecision.Pick } as EpisodeLoadDecision.Pick).storage
                    label = listOfNotNull(picked.source, picked.quality)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { null }
                    picked
                }
                storage = current

                show(EpisodeLoadStep.Resolving(label))
                val resolved = try {
                    resolveStream(model, current)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (action == EpisodeAction.Download) {
                        release()
                        onDownloadFailed(current, e)
                        return false
                    }
                    val canPickOther = sources.size > 1
                    drainDecisions()
                    show(EpisodeLoadStep.Failed("Couldn't load ${model.name}", e.message, canPickOther))
                    val decision = awaitDecision {
                        it == EpisodeLoadDecision.Retry || (canPickOther && it == EpisodeLoadDecision.PickOther)
                    }
                    if (decision == EpisodeLoadDecision.PickOther) storage = null
                    continue
                }
                if (action == EpisodeAction.Download) {
                    release()
                    onResolved(resolved)
                    return false
                }
                val wait = CompletableDeferred<Unit>().also { handoff = it }
                show(EpisodeLoadStep.OpeningPlayer)
                onResolved(resolved)
                // No suspension from here on: the hand-off job takes over the state and the lock.
                owned = false
                holdHandoff(wait)
                return true
            }
        } finally {
            if (owned) state = null
        }
    }
}

/** Resolves [storage] via its source, carrying headers/extras over; a blank link throws. */
suspend fun resolveStream(model: KmpChapterModel, storage: KmpStorage): KmpStorage {
    val resolved = model.source.resolve(storage).also { r ->
        if (r !== storage) {
            if (r.headers.isEmpty()) r.headers.putAll(storage.headers)
            if (r.extras.isEmpty()) r.extras.putAll(storage.extras)
        }
    }
    if (resolved.link.isNullOrBlank()) throw IllegalStateException("No link")
    return resolved
}
