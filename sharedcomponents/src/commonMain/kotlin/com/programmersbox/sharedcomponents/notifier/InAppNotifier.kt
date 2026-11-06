package com.programmersbox.sharedcomponents.notifier

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.StateFlow

interface InAppNotifier {
    /** Newest first. */
    val banners: StateFlow<List<Banner>>

    fun success(
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        action: String? = null,
        key: String? = null,
        durationMillis: Long? = null,
        onAction: suspend () -> Unit = {},
    ): BannerId

    fun info(
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        action: String? = null,
        key: String? = null,
        durationMillis: Long? = null,
        onAction: suspend () -> Unit = {},
    ): BannerId

    fun error(
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        action: String? = null,
        key: String? = null,
        durationMillis: Long? = null,
        onAction: suspend () -> Unit = {},
    ): BannerId

    fun progress(title: String, subtitle: String? = null, key: String? = null): ProgressHandle

    fun dismiss(id: BannerId)

    fun dismissAll()

    /** Runs the banner's action in the notifier scope, then dismisses it. */
    fun performAction(id: BannerId)

    fun setPaused(reason: PauseReason, paused: Boolean)

    fun setTimeoutAdjuster(adjust: (millis: Long, hasAction: Boolean) -> Long)
}

interface ProgressHandle {
    val id: BannerId

    fun update(title: String? = null, subtitle: String? = null, fraction: Float? = null)

    fun success(
        title: String,
        subtitle: String? = null,
        action: String? = null,
        onAction: suspend () -> Unit = {},
    )

    fun info(
        title: String,
        subtitle: String? = null,
        action: String? = null,
        onAction: suspend () -> Unit = {},
    )

    fun error(
        title: String,
        subtitle: String? = null,
        action: String? = null,
        onAction: suspend () -> Unit = {},
    )

    fun dismiss()
}
