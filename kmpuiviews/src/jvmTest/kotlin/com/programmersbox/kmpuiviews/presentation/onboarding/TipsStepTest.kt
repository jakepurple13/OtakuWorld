package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.down
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.tipsStep
import com.programmersbox.kmpuiviews.presentation.tips.Tip
import com.programmersbox.kmpuiviews.presentation.tips.TipGesture
import com.programmersbox.kmpuiviews.presentation.tips.buildTips
import com.programmersbox.kmpuiviews.presentation.tips.sharedTips
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class TipsStepTest {
    private val tips = buildTips {
        tip("Swipe it", "swipe", TipGesture.Swipe, demo = { Box(Modifier.testTag("swipeDemo")) })
        tip("Hold no demo", "nodemo", TipGesture.Hold)
        tip("Hold A", "a", TipGesture.Hold, demo = { onDone ->
            Box(Modifier.size(60.dp).testTag("holdA").clickable { onDone() })
        })
        tip("Hold B", "b", TipGesture.Hold, demo = { Box(Modifier.testTag("holdB")) })
    }

    private fun ComposeUiTest.show(
        onPractice: (Boolean) -> Unit = {},
        list: List<Tip> = tips,
    ) {
        val scope = buildOnboardingScope { tipsStep(onPractice, list) }
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(
                    LocalReducedMotionOverride provides true,
                    LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 1600.dp)),
                ) { scope[0].content() }
            }
        }
    }

    @Test
    fun shows_first_hold_tip_only() = runComposeUiTest {
        show()
        onNodeWithTag("holdA").assertExists()
        onNodeWithTag("holdB").assertDoesNotExist()
        onNodeWithTag("swipeDemo").assertDoesNotExist()
        onNodeWithText("Press and hold the cover.").assertExists()
        onNodeWithText("More in Settings › Discover › Tips & Gestures").assertExists()
    }

    @Test
    fun shared_hold_demo_instruction_shown_once() = runComposeUiTest {
        show(list = sharedTips)
        onAllNodesWithText("Press and hold the cover", substring = true).assertCountEquals(1)
    }

    @Test
    fun hint_changes_after_done() = runComposeUiTest {
        show()
        onNodeWithTag("holdA").performClick()
        waitForIdle()
        onNodeWithText("Nice. That menu works on every cover.").assertExists()
        onNodeWithText("Press and hold the cover.").assertDoesNotExist()
    }

    @Test
    fun practice_reports_change() = runComposeUiTest {
        val seen = mutableListOf<Boolean>()
        show(onPractice = { seen += it })
        onNodeWithTag("holdA").performTouchInput { down(center) }
        waitForIdle()
        assertEquals(true, seen.last())
        onNodeWithTag("holdA").performTouchInput { up() }
        waitForIdle()
        assertEquals(false, seen.last())
    }

    @Test
    fun no_hold_demo_keeps_footnote() = runComposeUiTest {
        show(list = buildTips { tip("Swipe", "s", TipGesture.Swipe, demo = { Box(Modifier.testTag("swipeDemo")) }) })
        onNodeWithTag("swipeDemo").assertDoesNotExist()
        onNodeWithText("More in Settings › Discover › Tips & Gestures").assertExists()
    }
}
