package com.programmersbox.kmpuiviews.motion

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class MotionPhase { Launch, Quit }

interface DesktopMotion {
    val key: String
    val title: String
    val durationMillis: Int

    /** Transform/clip/alpha applied to the app content for the frame at [time] ms. */
    fun Modifier.app(time: () -> Int): Modifier = this

    @Composable
    fun BoxScope.Behind(time: () -> Int) {
    }

    @Composable
    fun BoxScope.InFront(time: () -> Int) {
    }
}

data class DesktopMotionCatalog(
    val appName: String,
    val launch: List<DesktopMotion>,
    val quit: List<DesktopMotion>,
)

/** Built-ins first ([OffMotion], then Fade for the [phase]), followed by the app's own motions. */
fun DesktopMotionCatalog.options(phase: MotionPhase): List<DesktopMotion> = when (phase) {
    MotionPhase.Launch -> listOf(OffMotion, FadeInMotion) + launch
    MotionPhase.Quit -> listOf(OffMotion, FadeOutMotion) + quit
}

/** Empty or unknown [key] falls back to the app's first own motion (or Fade when it has none). */
fun DesktopMotionCatalog.resolve(phase: MotionPhase, key: String): DesktopMotion {
    val options = options(phase)
    return options.firstOrNull { it.key == key }
        ?: options.getOrNull(2)
        ?: options[1]
}

/** Maps [time] within the mockup segment [[start], [end]] to eased 0..1, clamped. */
@Suppress("UnusedReceiverParameter")
fun DesktopMotion.progress(time: Int, start: Int, end: Int, easing: Easing): Float =
    easing.transform(((time - start).toFloat() / (end - start)).coerceIn(0f, 1f))
