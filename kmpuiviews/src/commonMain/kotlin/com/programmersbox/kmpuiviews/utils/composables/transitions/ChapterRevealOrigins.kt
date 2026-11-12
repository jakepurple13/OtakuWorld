package com.programmersbox.kmpuiviews.utils.composables.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.hypot
import kotlin.math.max
import kotlin.time.Clock

/**
 * Hands the point a chapter was tapped at from Details to the reader it opens.
 *
 * The point is a fraction of the window size so it still lands in the right place after a rotation.
 * A record only counts for the chapter it was made for and only for [VALID_MS], so a reader opened
 * from anywhere else (Daily overview, a notification, Downloads) never reveals from a stale point.
 */
object ChapterRevealOrigins {
    private const val VALID_MS = 2_000L

    private var key: String? = null
    private var fraction: Offset? = null
    private var recordedAt = 0L

    private fun now() = Clock.System.now().toEpochMilliseconds()

    fun record(key: String, fraction: Offset, nowMs: Long = now()) {
        this.key = key
        this.fraction = fraction
        recordedAt = nowMs
    }

    /** The pending point, without clearing it, if it is still fresh. */
    fun peek(nowMs: Long = now()): Offset? = fraction?.takeIf { nowMs - recordedAt <= VALID_MS }

    /** The pending point for [key], cleared once returned. */
    fun take(key: String?, nowMs: Long = now()): Offset? {
        if (key == null || key != this.key) return null
        return peek(nowMs).also { clear() }
    }

    internal fun clear() {
        key = null
        fraction = null
        recordedAt = 0L
    }
}

/** Distance from [center] to the farthest corner of [size]: the radius that fully covers it. */
fun revealRadius(size: Size, center: Offset): Float = hypot(
    max(center.x, size.width - center.x),
    max(center.y, size.height - center.y),
)
