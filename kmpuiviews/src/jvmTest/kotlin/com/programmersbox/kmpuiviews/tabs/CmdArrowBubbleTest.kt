package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the facts behind the window's Cmd+Left/Right handling: a bubbling handler is not reached while nothing is
 * focused (so the window-level preview handler must act then), is reached from a focused non-text node, and is not
 * reached when a focused text field consumes the key.
 */
@OptIn(ExperimentalTestApi::class)
class CmdArrowBubbleTest {
    @Test
    fun bubbleHandlerReachedOnlyFromFocusedNonTextNodes() = runComposeUiTest {
        val seen = mutableIntStateOf(0)
        val plainRequester = FocusRequester()
        val fieldRequester = FocusRequester()
        setContent {
            Column(
                Modifier.testTag("root").onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) seen.value++
                    false
                }
            ) {
                Box(Modifier.size(10.dp).testTag("plain").focusRequester(plainRequester).focusable())
                BasicTextField(
                    rememberTextFieldState("hello"),
                    Modifier.testTag("field").focusRequester(fieldRequester),
                )
            }
        }

        fun press(tag: String) = onNodeWithTag(tag).performKeyInput {
            keyDown(Key.MetaLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.MetaLeft)
        }
        press("root")
        assertEquals(0, seen.value, "nothing focused")
        runOnIdle { plainRequester.requestFocus() }
        press("plain")
        assertEquals(1, seen.value, "focused non-text node")
        runOnIdle { fieldRequester.requestFocus() }
        press("field")
        assertEquals(1, seen.value, "focused text field consumes it")
    }
}
