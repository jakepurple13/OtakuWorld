package com.programmersbox.kmpuiviews.motion

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Ends [run] on the first pointer press inside it. Never consumes the event. Key presses are handled at window level. */
fun Modifier.skipMotionOnInput(run: MotionRun): Modifier =
    pointerInput(run) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed && !it.previousPressed }) run.skip()
            }
        }
    }
