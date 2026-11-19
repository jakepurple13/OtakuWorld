package com.programmersbox.kmpuiviews.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

class MotionRun(val motion: DesktopMotion) {
    var timeMillis by mutableIntStateOf(0)
        private set

    var finished by mutableStateOf(motion.durationMillis <= 0)
        private set

    fun skip() {
        timeMillis = motion.durationMillis
        finished = true
    }

    internal fun setTime(millis: Int) {
        timeMillis = millis
    }
}

/** Advances [MotionRun.timeMillis] with withFrameNanos; after the last frame plus one, sets finished and calls [onEnd] once. */
@Composable
fun MotionRun.Play(onEnd: () -> Unit = {}) {
    val currentOnEnd by rememberUpdatedState(onEnd)
    LaunchedEffect(this) {
        if (!finished) {
            val start = withFrameNanos { it }
            while (!finished) {
                val elapsed = ((withFrameNanos { it } - start) / 1_000_000L).toInt()
                setTime(elapsed.coerceAtMost(motion.durationMillis))
                if (elapsed >= motion.durationMillis) break
            }
            withFrameNanos { }
        }
        skip()
        currentOnEnd()
    }
}
