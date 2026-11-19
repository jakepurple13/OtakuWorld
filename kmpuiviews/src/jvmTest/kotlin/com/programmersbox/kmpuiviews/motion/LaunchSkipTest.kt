package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LaunchSkipTest {
    @Test
    fun pressSkipsLaunch() = runComposeUiTest {
        val run = MotionRun(FadeInMotion)
        setContent { Box(Modifier.size(100.dp).skipMotionOnInput(run).testTag("host")) }
        assertFalse(run.finished)
        onNodeWithTag("host").performMouseInput { press() }
        assertTrue(run.finished)
    }

    @Test
    fun keySkipsLaunch() = runComposeUiTest {
        val run = MotionRun(FadeInMotion)
        val focus = FocusRequester()
        setContent {
            Box(Modifier.size(100.dp).skipMotionOnInput(run).focusRequester(focus).focusable().testTag("host"))
        }
        runOnIdle { focus.requestFocus() }
        assertFalse(run.finished)
        onNodeWithTag("host").performKeyInput { pressKey(Key.A) }
        assertTrue(run.finished)
    }
}
