package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3ExpressiveApi::class)
class StepLayoutTest {
    private val spec = OnboardingStepSpec("a", "Title", "Body", Icons.Default.Star, HeroShape.Cookie)

    @Test
    fun compact_uses_single_pane() = runComposeUiTest {
        setContent {
            MaterialExpressiveTheme {
                StepLayout(spec, expanded = false, preview = { Text("prev") }) { Text("ctl") }
            }
        }
        onNodeWithTag(OnboardingTags.SinglePane).assertExists()
        onNodeWithTag(OnboardingTags.TwoPane).assertDoesNotExist()
        onNodeWithText("Title").assertExists()
        onNodeWithText("Body").assertExists()
        onNodeWithText("prev").assertExists()
        onNodeWithText("ctl").assertExists()
    }

    @Test
    fun expanded_uses_two_panes() = runComposeUiTest {
        setContent {
            MaterialExpressiveTheme {
                StepLayout(spec, expanded = true, preview = { Text("prev") }) { Text("ctl") }
            }
        }
        onNodeWithTag(OnboardingTags.TwoPane).assertExists()
        onNodeWithTag(OnboardingTags.SinglePane).assertDoesNotExist()
        onNodeWithText("Title").assertExists()
        onNodeWithText("prev").assertExists()
        onNodeWithText("ctl").assertExists()
    }
}
