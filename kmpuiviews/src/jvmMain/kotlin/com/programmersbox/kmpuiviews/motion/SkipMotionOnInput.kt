package com.programmersbox.kmpuiviews.motion

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Ends [run] on the first pointer press or key press inside it. Never consumes the event. */
fun Modifier.skipMotionOnInput(run: MotionRun): Modifier =
    pointerInput(run) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed && !it.previousPressed }) run.skip()
            }
        }
    }.onPreviewKeyEvent {
        run.skip()
        false
    }
