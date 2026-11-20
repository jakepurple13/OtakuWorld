package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
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

    private fun androidx.compose.ui.test.ComposeUiTest.setSweep(
        state: ColorSweepState,
        reduced: Boolean = false,
    ): () -> ((Offset) -> Unit) {
        var trigger: ((Offset) -> Unit)? = null
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

    @Test
    fun second_sweep_starts_from_zero_progress() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state)

        runOnUiThread { sweepFrom()(Offset(10f, 10f)) }
        mainClock.advanceTimeBy(1_000)
        assertNull(state.origin, "first sweep should have ended")
        assertEquals(1f, state.progress, "progress is left at 1f after a sweep")

        runOnUiThread { sweepFrom()(Offset(10f, 10f)) }
        // Before any frame runs, the new sweep must already be at 0 (old frame fully covering).
        assertEquals(0f, state.progress)
        assertNotNull(state.origin)
        mainClock.advanceTimeByFrame()
        assertTrue(state.progress < 0.2f, "progress was ${state.progress}")
    }

    @Test
    fun repeat_sweep_from_same_offset_restarts() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state)

        runOnUiThread { sweepFrom()(Offset(5f, 5f)) }
        mainClock.advanceTimeBy(400)
        val mid = state.progress
        assertTrue(mid > 0.3f, "progress was $mid")

        runOnUiThread { sweepFrom()(Offset(5f, 5f)) }
        assertEquals(0f, state.progress)
        mainClock.advanceTimeBy(100)
        assertTrue(state.progress < mid, "restart should be behind, was ${state.progress} vs $mid")
        mainClock.advanceTimeBy(1_000)
        assertNull(state.origin)
    }

    @Test
    fun reduced_motion_is_noop() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ColorSweepState()
        val sweepFrom = setSweep(state, reduced = true)
        runOnUiThread { sweepFrom()(Offset(1f, 1f)) }
        mainClock.advanceTimeBy(100)
        assertNull(state.origin)
    }
}
