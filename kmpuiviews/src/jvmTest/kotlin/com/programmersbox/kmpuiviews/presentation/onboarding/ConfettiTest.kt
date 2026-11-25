package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.ConfettiBurst
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3ExpressiveApi::class)
class ConfettiTest {
    @Test
    fun confetti_skipped_when_reduced_motion() = runComposeUiTest {
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalReducedMotionOverride provides true) {
                    ConfettiBurst(trigger = 1, colors = listOf(Color.Red, Color.Blue), modifier = Modifier.testTag("confetti"))
                }
            }
        }
        waitForIdle()
        onNodeWithTag("confetti").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "particles=0"))
    }
}
