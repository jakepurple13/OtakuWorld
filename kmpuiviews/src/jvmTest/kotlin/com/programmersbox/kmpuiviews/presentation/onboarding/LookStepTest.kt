package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.lookStep
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class LookStepTest {
    private fun ComposeUiTest.show(handling: NewSettingsHandling) {
        val scope = buildOnboardingScope { lookStep(handling) }
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
    fun amoled_row_hidden_in_light() = runComposeUiTest {
        show(createTestNewSettingsHandling())
        onNodeWithText("Light").performClick()
        waitForIdle()
        onNodeWithText("AMOLED black").assertDoesNotExist()
        onNodeWithText("Dark").performClick()
        waitForIdle()
        onNodeWithText("AMOLED black").assertExists()
    }

    @Test
    fun swatch_click_writes_theme_color() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        show(handling)
        onNodeWithText("Blue").performClick()
        // The preference write is asynchronous (DataStore IO), so poll the real flow.
        waitUntil(timeoutMillis = 5_000) {
            runBlocking { handling.preferences.data.first().themeColor } == ThemeColor.Blue
        }
        assertEquals(ThemeColor.Blue, runBlocking { handling.preferences.data.first().themeColor })
    }

    @Test
    fun dynamic_swatch_keeps_its_own_colors_when_another_color_is_picked() = runComposeUiTest {
        val handling = createTestNewSettingsHandling()
        show(handling)
        fun pixel(tag: String): Int {
            val img = onNodeWithTag(tag, useUnmergedTree = true).captureToImage()
            return img.toPixelMap()[3, img.height / 2].toArgb()
        }
        val dynamicBefore = pixel("swatch-Dynamic")
        onNodeWithText("Blue").performClick()
        waitUntil(timeoutMillis = 5_000) {
            runBlocking { handling.preferences.data.first().themeColor } == ThemeColor.Blue
        }
        waitForIdle()
        assertEquals(dynamicBefore, pixel("swatch-Dynamic"))
        assertNotEquals(pixel("swatch-Blue"), pixel("swatch-Dynamic"))
    }

    @Test
    fun swatches_exclude_custom_ugly_myeyes() = runComposeUiTest {
        show(createTestNewSettingsHandling())
        listOf("Dynamic", "Blue", "Red", "Green", "Yellow", "Cyan", "Magenta").forEach {
            onNodeWithText(it).assertExists()
        }
        listOf("Custom", "Ugly", "MyEyes").forEach { onNodeWithText(it).assertDoesNotExist() }
    }

    @Test
    fun more_options_sheet_has_extra_settings() = runComposeUiTest {
        show(createTestNewSettingsHandling())
        onNodeWithText("More look options").performClick()
        waitForIdle()
        onNodeWithText("Show Expressiveness").assertExists()
        onNodeWithText("Color Blindness").assertExists()
        onNodeWithText("Color details from cover").assertExists()
        onNodeWithText("Ugly").assertExists()
        onNodeWithText("MyEyes").assertExists()
    }
}
