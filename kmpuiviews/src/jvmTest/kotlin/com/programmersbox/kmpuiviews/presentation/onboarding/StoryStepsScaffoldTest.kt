package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.material3.TextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performTextInput
import kotlin.test.assertTrue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StoryStepsScaffold
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3ExpressiveApi::class)
class StoryStepsScaffoldTest {

    private fun spec(id: String, shape: HeroShape = HeroShape.Cookie) =
        OnboardingStepSpec(id, "Title $id", "Body $id", Icons.Default.Star, shape)

    private fun scope(count: Int = 4, expanded: Boolean = false, extra: @Composable (Int) -> Unit = {}) =
        buildOnboardingScope {
            repeat(count - 1) { i ->
                val sp = spec("s$i", if (i == 0) HeroShape.Cookie else HeroShape.Diamond)
                step(sp) { StepLayout(sp, expanded = expanded) { Text("content $i"); extra(i) } }
            }
            val sp = spec("done", HeroShape.Done)
            step(sp) { StepLayout(sp, expanded = expanded) { Text("content done") } }
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
                        expanded = false,
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

    @Test
    fun scaffold_does_not_draw_titles_itself_but_step_layout_does() = runComposeUiTest {
        setScaffold()
        onNodeWithText("Title s0").assertExists()
        onNodeWithText("Body s0").assertExists()
    }

    @Test
    fun slider_keeps_arrow_keys_and_page_does_not_change() = runComposeUiTest {
        var value = 0.5f
        val p = Probe()
        val sc = scope(extra = { i ->
            if (i == 0) {
                var v by remember { mutableFloatStateOf(0.5f) }
                Slider(v, { v = it; value = it }, modifier = Modifier.testTag("slider"))
            }
        })
        setScaffold(sc, p)
        onNodeWithTag("slider").requestFocus()
        waitForIdle()
        onNodeWithTag("slider").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals(0, p.pager.currentPage)
        assertTrue(value > 0.5f, "slider should have moved, was $value")
    }

    @Test
    fun width_change_keeps_page_and_text() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        lateinit var pager: PagerState
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalReducedMotionOverride provides true) {
                    pager = rememberPagerState { 4 }
                    val sc = remember {
                        buildOnboardingScope {
                            repeat(4) { i ->
                                val sp = spec("w$i")
                                step(sp) {
                                    var text by rememberSaveable { mutableStateOf("") }
                                    StepLayout(sp, expanded = expanded) {
                                        TextField(text, { text = it }, modifier = Modifier.testTag("field$i"))
                                    }
                                }
                            }
                        }
                    }
                    StoryStepsScaffold(sc, "TestApp", "Go", {}, {}, pagerState = pager, expanded = expanded)
                }
            }
        }
        waitForIdle()
        onNodeWithTag(OnboardingTags.SinglePane).assertExists()
        onNodeWithTag(OnboardingTags.Next).performClick()
        waitForIdle()
        onNodeWithTag(OnboardingTags.Next).performClick()
        waitForIdle()
        assertEquals(2, pager.currentPage)
        onNodeWithTag("field2").performTextInput("hello")
        waitForIdle()
        expanded = true
        waitForIdle()
        assertEquals(2, pager.currentPage)
        onNodeWithTag(OnboardingTags.TwoPane).assertExists()
        onNodeWithTag("field2").assertTextContains("hello")
    }

    @Test
    fun compact_hero_leaves_with_scroll() = runComposeUiTest {
        val p = Probe()
        val sc = scope(extra = { repeat(30) { Text("row $it") } })
        setScaffold(sc, p)
        val pane = onNodeWithTag(OnboardingTags.SinglePane).fetchSemanticsNode().boundsInRoot
        val before = onNodeWithTag(OnboardingTags.Hero).fetchSemanticsNode().boundsInRoot
        // The hero is inset 8 dp so the Done hero (scaled 1.15) is not clipped.
        val inset = with(density) { 8.dp.toPx() }
        assertEquals(pane.top + inset, before.top)

        onNodeWithTag(OnboardingTags.SinglePane).performTouchInput { swipeUp(startY = bottom - 10f, endY = top + 10f) }
        waitForIdle()
        val after = onNodeWithTag(OnboardingTags.Hero).fetchSemanticsNode().boundsInRoot
        assertTrue(after.bottom <= pane.top + 1f, "hero should be above the content, hero=$after pane=$pane")

        // Page change: the new page starts at scroll 0, so the hero comes back.
        onNodeWithTag(OnboardingTags.Next).performClick()
        waitForIdle()
        val next = onNodeWithTag(OnboardingTags.Hero).fetchSemanticsNode().boundsInRoot
        assertEquals(pane.top + inset, next.top)
    }
}
