package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.ColorSweepHost
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.ColorSweepState
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3ExpressiveApi::class)
class ColorSweepTest {

    private fun ComposeUiTest.setSweep(
        state: ColorSweepState,
        reduced: Boolean = false,
    ): () -> ((Offset, () -> Unit) -> Unit) {
        var trigger: ((Offset, () -> Unit) -> Unit)? = null
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalReducedMotionOverride provides reduced) {
                    ColorSweepHost(state) { sweepFrom ->
                        trigger = sweepFrom
                        Text("content")
                    }
                }
            }
        }
        waitForIdle()
        return { trigger!! }
    }

    /** Advances frames until the sweep has captured the old frame and started. */
    private fun ComposeUiTest.untilStarted(state: ColorSweepState) {
        repeat(10) {
            if (state.origin != null) return
            mainClock.advanceTimeByFrame()
        }
        assertNotNull(state.origin, "sweep never started")
    }

    @Test
    fun apply_runs_after_capture() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state)

        var applied = false
        var frameAtApply = false
        runOnUiThread {
            sweepFrom()(Offset(10f, 10f)) {
                applied = true
                frameAtApply = state.frame != null && state.origin != null
            }
        }
        assertTrue(!applied, "apply must wait for the capture")
        untilStarted(state)
        assertTrue(applied)
        assertTrue(frameAtApply, "the old frame must be captured before the change applies")
        mainClock.advanceTimeBy(1_000)
        assertNull(state.origin)
        assertNull(state.frame)
    }

    @Test
    fun second_sweep_starts_from_zero_progress() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state)

        runOnUiThread { sweepFrom()(Offset(10f, 10f)) {} }
        untilStarted(state)
        mainClock.advanceTimeBy(1_000)
        assertNull(state.origin, "first sweep should have ended")
        assertEquals(1f, state.progress, "progress is left at 1f after a sweep")

        runOnUiThread { sweepFrom()(Offset(10f, 10f)) {} }
        untilStarted(state)
        assertTrue(state.progress < 0.2f, "progress was ${state.progress}")
    }

    @Test
    fun repeat_sweep_from_same_offset_restarts() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state)

        runOnUiThread { sweepFrom()(Offset(5f, 5f)) {} }
        untilStarted(state)
        val firstId = state.id
        mainClock.advanceTimeBy(400)
        val mid = state.progress
        assertTrue(mid > 0.3f, "progress was $mid")

        var applied = 0
        runOnUiThread { sweepFrom()(Offset(5f, 5f)) { applied++ } }
        untilStarted(state)
        assertEquals(firstId + 1, state.id)
        assertEquals(1, applied)
        assertTrue(state.progress < mid, "restart should be behind, was ${state.progress} vs $mid")
        mainClock.advanceTimeBy(1_000)
        assertNull(state.origin)
    }

    @Test
    fun reduced_motion_applies_immediately() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state, reduced = true)
        var applied = false
        runOnUiThread { sweepFrom()(Offset(1f, 1f)) { applied = true } }
        assertTrue(applied, "reduced motion applies the change synchronously")
        mainClock.advanceTimeBy(100)
        assertNull(state.origin)
        assertNull(state.frame)
        assertEquals(0, state.id)
    }
}
