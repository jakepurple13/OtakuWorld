package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
/**
 * Cmd+Left in a focused text field must move the caret and never reach the main window's key handlers, whichever
 * layer hosts the field. A focusable Popup or Dialog is its own layer: the main root's preview handler (where
 * the window-level tab shortcuts live) is not called at all, so nothing takes the key. In-tree, preview runs first
 * and the window must defer (TabsController.contentHasFocus) so the field can use it.
 */
class PopupArrowTest {
    private fun probe(name: String, previewSeen: Int, host: @androidx.compose.runtime.Composable (@androidx.compose.runtime.Composable () -> Unit) -> Unit) =
        runComposeUiTest {
            val preview = mutableIntStateOf(0)
            val bubble = mutableIntStateOf(0)
            val requester = FocusRequester()
            val state = TextFieldState("hello")
            setContent {
                Box(
                    Modifier.size(100.dp).testTag("root")
                        .onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) preview.value++
                            false
                        }
                        .onKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) bubble.value++
                            false
                        }
                ) {
                    host {
                        BasicTextField(state, Modifier.testTag("field").focusRequester(requester))
                    }
                }
            }
            runOnIdle { requester.requestFocus() }
            waitForIdle()
            assertEquals(androidx.compose.ui.text.TextRange(5), state.selection, "$name: caret starts at the end")
            onNodeWithTag("field").performKeyInput {
                keyDown(Key.MetaLeft)
                pressKey(Key.DirectionLeft)
                keyUp(Key.MetaLeft)
            }
            assertEquals(previewSeen, preview.value, "$name: main preview calls")
            // Cmd+Left moves the caret only with the macOS key map; CI runs on Linux, where the field ignores it.
            if (isMacOs) {
                assertEquals(0, bubble.value, "$name: key not left over for the window")
                assertEquals(androidx.compose.ui.text.TextRange(0), state.selection, "$name: caret moved")
            }
        }

    @Test
    fun popup() = probe("popup", previewSeen = 0) { c -> Popup(properties = PopupProperties(focusable = true)) { c() } }

    @Test
    fun dialog() = probe("dialog", previewSeen = 0) { c -> Dialog(onDismissRequest = {}) { c() } }

    @Test
    fun inTree() = probe("inTree", previewSeen = 1) { c -> c() }
}
