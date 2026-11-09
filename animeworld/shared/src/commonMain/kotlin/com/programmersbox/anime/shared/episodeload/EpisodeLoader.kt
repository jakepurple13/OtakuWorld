package com.programmersbox.anime.shared.episodeload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

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

private const val HANDOFF_TIMEOUT_MS = 4_000L

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
 * [load] suspends for the whole flow (including waiting on the user), so the caller's coroutine is its lifetime.
 */
class EpisodeLoader {
    var state: EpisodeLoadState? by mutableStateOf(null)
        private set

    private val decisions = Channel<EpisodeLoadDecision>(Channel.CONFLATED)

    private var activeJob: Job? = null
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
     * Only one load runs at a time; a call while another is active returns immediately.
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
        try {
            coroutineScope {
                val job = launch { run(action, model, infoModel, onResolved, onDownloadFailed) }
                activeJob = job
                job.join()
            }
        } finally {
            activeJob = null
            handoff = null
            busy.unlock()
        }
    }

    private suspend fun run(
        action: EpisodeAction,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        onResolved: suspend (KmpStorage) -> Unit,
        onDownloadFailed: suspend (KmpStorage?, Throwable?) -> Unit,
    ) {
        decisions.tryReceive()

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

        try {
            show(EpisodeLoadStep.FetchingSources)
            val sources = try {
                model.getChapterInfo().firstOrNull().orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }

            if (sources.isEmpty()) {
                if (action == EpisodeAction.Download) {
                    release()
                    onDownloadFailed(null, null)
                } else {
                    show(EpisodeLoadStep.Failed("No sources found for ${model.name}", null, false))
                    decisions.receive()
                }
                return
            }

            var storage: KmpStorage? = sources.singleOrNull()
            var label: String? = null
            while (true) {
                val current = storage ?: run {
                    show(EpisodeLoadStep.ChooseSource(sources))
                    val picked = (decisions.receive() as? EpisodeLoadDecision.Pick)?.storage ?: return
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
                        return
                    }
                    show(EpisodeLoadStep.Failed("Couldn't load ${model.name}", e.message, sources.size > 1))
                    when (decisions.receive()) {
                        EpisodeLoadDecision.Retry -> Unit
                        EpisodeLoadDecision.PickOther -> storage = null
                        else -> return
                    }
                    continue
                }
                if (action == EpisodeAction.Download) {
                    release()
                    onResolved(resolved)
                } else {
                    val wait = CompletableDeferred<Unit>().also { handoff = it }
                    show(EpisodeLoadStep.OpeningPlayer)
                    onResolved(resolved)
                    withTimeoutOrNull(HANDOFF_TIMEOUT_MS) { wait.await() }
                }
                return
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
