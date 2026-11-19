package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
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
}
