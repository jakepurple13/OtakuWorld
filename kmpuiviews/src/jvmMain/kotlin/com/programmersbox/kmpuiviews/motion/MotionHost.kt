package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Behind, then content wrapped in Modifier.app, then InFront. Content keeps the same slot whatever [motion] is. */
@Composable
fun MotionHost(
    motion: DesktopMotion,
    time: () -> Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        with(motion) { Behind(time) }
        Box(with(motion) { Modifier.app(time) }) { content() }
        with(motion) { InFront(time) }
    }
}
