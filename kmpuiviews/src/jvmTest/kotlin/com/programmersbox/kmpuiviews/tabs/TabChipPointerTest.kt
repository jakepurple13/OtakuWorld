package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The tab strip sits in Nucleus's macOS title bar, whose hit-test handler (Main pass, on the parent) starts a native
 * window drag on the first move after an unconsumed press. A tab chip must consume its press so a tab drag isn't
 * turned into a window drag.
 */
@OptIn(ExperimentalTestApi::class)
class TabChipPointerTest {
    @Test
    fun chipPressIsConsumedSoTheTitleBarDoesNotStartAWindowDrag() = runComposeUiTest {
        val unconsumedPresses = mutableIntStateOf(0)
        val activations = mutableIntStateOf(0)
        setContent {
            // Stand-in for the title bar's hit-test handler: a parent reading the Main pass after its children.
            Box(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type == PointerEventType.Press && event.changes.any { !it.isConsumed }) {
                                unconsumedPresses.value++
                            }
                        }
                    }
                }
            ) {
                Box(
                    Modifier
                        .size(100.dp, 32.dp)
                        .testTag("chip")
                        .tabChipPointer(pinned = false, onActivate = { activations.value++ }, onClose = {})
                )
            }
        }

        onNodeWithTag("chip").performMouseInput {
            press()
            release()
        }
        waitForIdle()

        assertEquals(0, unconsumedPresses.value, "the title bar saw an unconsumed press and would drag the window")
        assertEquals(1, activations.value, "a plain click still activates the tab")
    }

    @Test
    fun aConsumedReleaseDoesNotActivate() = runComposeUiTest {
        val activations = mutableIntStateOf(0)
        setContent {
            Box(
                Modifier
                    .size(100.dp, 32.dp)
                    .testTag("chip")
                    // Stand-in for the tab drag source, which consumes the release of a drag on the Initial pass.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Release) event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .tabChipPointer(pinned = false, onActivate = { activations.value++ }, onClose = {})
            )
        }

        onNodeWithTag("chip").performMouseInput {
            press()
            moveBy(Offset(40f, 0f))
            release()
        }
        waitForIdle()

        assertEquals(0, activations.value)
    }
}
