package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.MediaCheckerSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.updatesStep
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class UpdatesStepTest {
    private fun ComposeUiTest.show(handling: NewSettingsHandling) {
        val scope = buildOnboardingScope { updatesStep(handling, "chapter") }
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(
                    LocalReducedMotionOverride provides true,
                    LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 1600.dp)),
                ) { scope[0].content() }
            }
        }
    }

    private fun stored(handling: NewSettingsHandling) = runBlocking { handling.mediaCheckerSettings.get() }

    private fun ComposeUiTest.awaitStored(handling: NewSettingsHandling, check: (MediaCheckerSettings) -> Boolean) {
        waitUntil(timeoutMillis = 5_000) { check(stored(handling)) }
    }

    @Test
    fun master_off_hides_options() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        show(handling)
        onNodeWithTag(OnboardingTags.UpdatesOptions).assertExists()
        onNodeWithText("Check for updates").performClick()
        awaitStored(handling) { !it.shouldRun }
        waitForIdle()
        onNodeWithTag(OnboardingTags.UpdatesOptions).assertDoesNotExist()
        onNodeWithText(
            "You can still pull to refresh on Notifications. Turn checks on anytime in Settings › Notifications.",
        ).assertExists()
        assertFalse(stored(handling).shouldRun)
    }

    @Test
    fun master_toggle_keeps_interval_and_network() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        runBlocking {
            handling.mediaCheckerSettings.set(
                handling.mediaCheckerSettings.get().copy(
                    interval = 6,
                    networkType = MediaCheckerNetworkType.Unmetered,
                )
            )
        }
        show(handling)
        onNodeWithText("6h").assertExists()
        onNodeWithText("Check for updates").performClick()
        awaitStored(handling) { !it.shouldRun }
        onNodeWithText("Check for updates").performClick()
        awaitStored(handling) { it.shouldRun }
        waitForIdle()
        assertEquals(6L, stored(handling).interval)
        assertEquals(MediaCheckerNetworkType.Unmetered, stored(handling).networkType)
        onNodeWithText("6h").assertExists()
    }

    @Test
    fun network_choice_maps_to_proto() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        show(handling)
        onNodeWithText("Wi‑Fi only").performClick()
        awaitStored(handling) { it.networkType == MediaCheckerNetworkType.Unmetered }
        waitForIdle()
        onNodeWithText("Wi‑Fi only").assertIsSelected()
        onNodeWithText("Any network").performClick()
        awaitStored(handling) { it.networkType == MediaCheckerNetworkType.Connected }
        assertEquals(MediaCheckerNetworkType.Connected, stored(handling).networkType)
    }

    @Test
    fun metered_stored_selects_neither_chip_and_is_kept() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        runBlocking {
            handling.mediaCheckerSettings.set(
                handling.mediaCheckerSettings.get().copy(networkType = MediaCheckerNetworkType.Metered)
            )
        }
        show(handling)
        assertFalse(onNodeWithText("Any network").fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertFalse(onNodeWithText("Wi‑Fi only").fetchSemanticsNode().config[SemanticsProperties.Selected])
        onNodeWithText("Only when charging").performClick()
        awaitStored(handling) { it.requiresCharging }
        assertEquals(MediaCheckerNetworkType.Metered, stored(handling).networkType)
    }

    @Test
    fun interval_range_1_to_24() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        show(handling)
        val slider = onNodeWithTag("onb_updates_interval")
        val info = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(1f..24f, info.range)
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(24f) }
        awaitStored(handling) { it.interval == 24L }
        waitForIdle()
        onNodeWithText("24h").assertExists()
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        awaitStored(handling) { it.interval == 1L }
        assertTrue(stored(handling).interval in 1..24)
    }

    @Test
    fun summary_reflects_state() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        val scope = buildOnboardingScope { updatesStep(handling, "chapter") }
        var text = ""
        setContent {
            MaterialExpressiveTheme { text = scope[0].spec.summary!!() }
        }
        waitForIdle()
        assertEquals("Every 1h · any network", text)
    }
}
