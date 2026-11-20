package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StoryStepsScaffold
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3ExpressiveApi::class)
class StoryStepsScaffoldTest {

    private fun spec(id: String, shape: HeroShape = HeroShape.Cookie) =
        OnboardingStepSpec(id, "Title $id", "Body $id", Icons.Default.Star, shape)

    private fun scope(count: Int = 4) = buildOnboardingScope {
        repeat(count - 1) { i -> step(spec("s$i", if (i == 0) HeroShape.Cookie else HeroShape.Diamond)) { Text("content $i") } }
        step(spec("done", HeroShape.Done)) { Text("content done") }
    }

    private class Probe {
        var finished = 0
        var skipped = 0
        lateinit var pager: PagerState
    }

    private fun ComposeUiTest.setScaffold(scope: OnboardingScope = scope(), probe: Probe = Probe()): Probe {
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalReducedMotionOverride provides true) {
                    probe.pager = rememberPagerState { scope.size }
                    StoryStepsScaffold(
                        scope = scope,
                        appName = "TestApp",
                        startLabel = "Start reading",
                        onFinish = { probe.finished++ },
                        onSkipConfirmed = { probe.skipped++ },
                        pagerState = probe.pager,
                    )
                }
            }
        }
        waitForIdle()
        return probe
    }

    private fun ComposeUiTest.goTo(probe: Probe, page: Int) {
        while (probe.pager.currentPage < page) {
            onNodeWithTag(OnboardingTags.Next).performClick()
            waitForIdle()
        }
        assertEquals(page, probe.pager.currentPage)
    }

    @Test
    fun next_labels_follow_position() = runComposeUiTest {
        val p = setScaffold()
        onNodeWithTag(OnboardingTags.Next).assertTextEquals("Let's go")
        goTo(p, 1)
        onNodeWithTag(OnboardingTags.Next).assertTextEquals("Next")
        goTo(p, 2)
        onNodeWithTag(OnboardingTags.Next).assertTextEquals("Next")
        goTo(p, 3)
        onNodeWithTag(OnboardingTags.Next).assertTextEquals("Start reading")
    }

    @Test
    fun back_disabled_on_first_step() = runComposeUiTest {
        setScaffold()
        onNodeWithTag(OnboardingTags.Back).assertIsNotEnabled()
    }

    @Test
    fun skip_opens_dialog_and_cancel_keeps_page() = runComposeUiTest {
        val p = setScaffold()
        goTo(p, 1)
        onNodeWithTag(OnboardingTags.Skip).performClick()
        waitForIdle()
        onNodeWithText("Skip setup?").assertExists()
        onNodeWithText("TestApp will start with default settings. You can run setup again from Settings › About › View Onboarding Again.").assertExists()
        onNodeWithTag(OnboardingTags.SkipCancel).performClick()
        waitForIdle()
        onNodeWithText("Skip setup?").assertDoesNotExist()
        assertEquals(1, p.pager.currentPage)
        assertEquals(0, p.skipped)
    }

    @Test
    fun skip_confirm_calls_callback() = runComposeUiTest {
        val p = setScaffold()
        onNodeWithTag(OnboardingTags.Skip).performClick()
        waitForIdle()
        onNodeWithTag(OnboardingTags.SkipConfirm).performClick()
        waitForIdle()
        assertEquals(1, p.skipped)
    }

    @Test
    fun skip_hidden_on_done() = runComposeUiTest {
        val p = setScaffold()
        goTo(p, 3)
        onNodeWithTag(OnboardingTags.Skip).assertDoesNotExist()
    }

    @Test
    fun next_on_done_calls_finish() = runComposeUiTest {
        val p = setScaffold()
        goTo(p, 3)
        onNodeWithTag(OnboardingTags.Next).performClick()
        waitForIdle()
        assertEquals(1, p.finished)
    }

    @Test
    fun back_on_done_goes_to_tips() = runComposeUiTest {
        val p = setScaffold()
        goTo(p, 3)
        onNodeWithTag(OnboardingTags.Back).performClick()
        waitForIdle()
        assertEquals(2, p.pager.currentPage)

        goTo(p, 3)
        onNodeWithTag(OnboardingTags.Progress).performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        assertEquals(2, p.pager.currentPage)
    }

    @Test
    fun arrow_keys_change_step() = runComposeUiTest {
        val p = setScaffold()
        onNodeWithTag(OnboardingTags.Progress).performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals(1, p.pager.currentPage)
    }

    @Test
    fun flow_without_app_step_has_eight_segments() = runComposeUiTest {
        setScaffold(scope(8))
        onNodeWithTag(OnboardingTags.Progress).onChildren().assertCountEquals(8)
    }
}
