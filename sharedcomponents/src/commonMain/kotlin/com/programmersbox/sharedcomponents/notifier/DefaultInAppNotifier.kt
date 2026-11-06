package com.programmersbox.sharedcomponents.notifier

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * [banners] changes synchronously on the calling thread. Timer bookkeeping lives in [scope]
 * and is only touched from coroutines launched there, so it needs no lock.
 *
 * [timeSource] measures elapsed time while a timer runs; tests pass the virtual-time source.
 */
class DefaultInAppNotifier(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : InAppNotifier {

    private class Timer(val banner: Banner, var remaining: Long) {
        var startedAt: ComparableTimeMark? = null
        var job: Job? = null
    }

    private val state = MutableStateFlow<List<Banner>>(emptyList())
    private val nextId = MutableStateFlow(0L)
    private val acting = MutableStateFlow<Set<BannerId>>(emptySet())

    override val banners: StateFlow<List<Banner>> = state.asStateFlow()

    // Only touched inside scope.launch blocks.
    private val timers = HashMap<BannerId, Timer>()
    private val pausedBy = MutableStateFlow<Set<PauseReason>>(emptySet())

    override val paused: StateFlow<Set<PauseReason>> = pausedBy.asStateFlow()

    @kotlin.concurrent.Volatile
    private var adjuster: (Long, Boolean) -> Long = { millis, _ -> millis }

    private fun post(
        kind: BannerKind,
        title: String,
        subtitle: String?,
        icon: ImageVector?,
        action: String?,
        key: String?,
        durationMillis: Long?,
        onAction: suspend () -> Unit,
        fraction: Float? = null,
    ): BannerId {
        val fresh = BannerId(nextId.getAndUpdate { it + 1 })
        var result = fresh
        val duration = if (kind == BannerKind.Progress) null else durationMillis ?: defaultMillis(kind, action != null)
        state.update { list ->
            val existing = if (key == null) -1 else list.indexOfFirst { it.key == key }
            result = if (existing >= 0) list[existing].id else fresh
            val banner = Banner(result, kind, title, subtitle, icon, action, key, fraction, duration, onAction)
            if (existing >= 0) {
                list.toMutableList().also { it[existing] = banner }
            } else {
                (listOf(banner) + list).take(BannerDefaults.MaxBanners)
            }
        }
        schedule(result)
        return result
    }

    private fun defaultMillis(kind: BannerKind, hasAction: Boolean): Long = when (kind) {
        BannerKind.Success -> if (hasAction) BannerDefaults.WithActionMillis else BannerDefaults.SuccessMillis
        BannerKind.Info -> if (hasAction) BannerDefaults.WithActionMillis else BannerDefaults.InfoMillis
        BannerKind.Error -> BannerDefaults.ErrorMillis
        BannerKind.Progress -> BannerDefaults.InfoMillis
    }

    override fun success(
        title: String, subtitle: String?, icon: ImageVector?, action: String?,
        key: String?, durationMillis: Long?, onAction: suspend () -> Unit,
    ) = post(BannerKind.Success, title, subtitle, icon, action, key, durationMillis, onAction)

    override fun info(
        title: String, subtitle: String?, icon: ImageVector?, action: String?,
        key: String?, durationMillis: Long?, onAction: suspend () -> Unit,
    ) = post(BannerKind.Info, title, subtitle, icon, action, key, durationMillis, onAction)

    override fun error(
        title: String, subtitle: String?, icon: ImageVector?, action: String?,
        key: String?, durationMillis: Long?, onAction: suspend () -> Unit,
    ) = post(BannerKind.Error, title, subtitle, icon, action, key, durationMillis, onAction)

    override fun progress(title: String, subtitle: String?, key: String?): ProgressHandle {
        val id = post(BannerKind.Progress, title, subtitle, null, null, key, null, {})
        return Handle(id)
    }

    override fun dismiss(id: BannerId) {
        state.update { list -> list.filterNot { it.id == id } }
        prune()
    }

    override fun dismissAll() {
        state.value = emptyList()
        prune()
    }

    override fun performAction(id: BannerId) {
        val banner = state.value.firstOrNull { it.id == id } ?: return
        // Claim atomically so two concurrent calls cannot both run the action.
        if (id in acting.getAndUpdate { it + id }) return
        // A concurrent call may have run and removed this banner after we read it.
        if (state.value.none { it === banner }) {
            acting.update { it - id }
            return
        }
        // Start before the banner leaves state: the action's synchronous prefix (e.g. the
        // "undone" flag showUndoable reads) must run before any collector sees the removal.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching { banner.onAction() }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
                .onFailure { println("InAppNotifier: action for '${banner.title}' failed: $it") }
        }
        // Remove only the acted-on instance: the action may have posted a keyed replacement under the same id.
        state.update { list -> list.filterNot { it === banner } }
        prune()
        acting.update { it - id }
    }

    override fun setPaused(reason: PauseReason, paused: Boolean) {
        pausedBy.update { if (paused) it + reason else it - reason }
        scope.launch {
            // Reconcile every timer against the latest pause state; both transitions are idempotent.
            timers.forEach { (id, timer) ->
                if (isHeld(timer.banner)) {
                    val started = timer.startedAt ?: return@forEach
                    timer.job?.cancel()
                    timer.job = null
                    timer.startedAt = null
                    timer.remaining = (timer.remaining - started.elapsedNow().inWholeMilliseconds).coerceAtLeast(0)
                } else {
                    start(id, timer)
                }
            }
        }
    }

    /**
     * Expanded holds every banner. Background holds only banners without an action, so
     * Undo/Retry banners keep counting down and their deferred work still commits.
     */
    private fun isHeld(banner: Banner): Boolean {
        val reasons = pausedBy.value
        return PauseReason.Expanded in reasons || (PauseReason.Background in reasons && banner.action == null)
    }

    override fun setTimeoutAdjuster(adjust: (millis: Long, hasAction: Boolean) -> Long) {
        adjuster = adjust
    }

    private fun prune() {
        scope.launch { pruneNow() }
    }

    private fun pruneNow() {
        val live = state.value.mapTo(HashSet()) { it.id }
        val iterator = timers.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in live) {
                entry.value.job?.cancel()
                iterator.remove()
            }
        }
    }

    /** (Re)starts the expiry timer for [id], if its banner expires. */
    private fun schedule(id: BannerId) {
        scope.launch {
            timers.remove(id)?.job?.cancel()
            pruneNow()
            val banner = state.value.firstOrNull { it.id == id } ?: return@launch
            val millis = banner.durationMillis ?: return@launch
            val timer = Timer(banner, adjuster(millis, banner.action != null))
            timers[id] = timer
            if (!isHeld(banner)) start(id, timer)
        }
    }

    private fun start(id: BannerId, timer: Timer) {
        if (timer.job != null) return
        timer.startedAt = timeSource.markNow()
        timer.job = scope.launch {
            delay(timer.remaining)
            // Only remove the exact banner this timer was scheduled for; a keyed replacement keeps the id.
            if (timers[id] === timer) timers.remove(id)
            state.update { list -> list.filterNot { it === timer.banner } }
        }
    }

    private inner class Handle(override val id: BannerId) : ProgressHandle {
        override fun update(title: String?, subtitle: String?, fraction: Float?) {
            state.update { list ->
                list.map {
                    if (it.id != id) it
                    else it.copy(title = title ?: it.title, subtitle = subtitle ?: it.subtitle, fraction = fraction ?: it.fraction)
                }
            }
        }

        override fun success(title: String, subtitle: String?, action: String?, onAction: suspend () -> Unit) =
            finish(BannerKind.Success, title, subtitle, action, onAction)

        override fun info(title: String, subtitle: String?, action: String?, onAction: suspend () -> Unit) =
            finish(BannerKind.Info, title, subtitle, action, onAction)

        override fun error(title: String, subtitle: String?, action: String?, onAction: suspend () -> Unit) =
            finish(BannerKind.Error, title, subtitle, action, onAction)

        override fun dismiss() = this@DefaultInAppNotifier.dismiss(id)

        /** Morphs this banner in place; if it is gone, the result is a new banner. */
        private fun finish(kind: BannerKind, title: String, subtitle: String?, action: String?, onAction: suspend () -> Unit) {
            var morphed = false
            var key: String? = null
            state.update { list ->
                morphed = false
                list.map {
                    if (it.id != id) it
                    else {
                        morphed = true
                        key = it.key
                        Banner(id, kind, title, subtitle, null, action, it.key, null, defaultMillis(kind, action != null), onAction)
                    }
                }
            }
            if (morphed) schedule(id) else post(kind, title, subtitle, null, action, key, null, onAction)
        }
    }
}
