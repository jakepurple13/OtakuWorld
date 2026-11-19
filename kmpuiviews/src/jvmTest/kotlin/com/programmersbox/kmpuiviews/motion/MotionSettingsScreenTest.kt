package com.programmersbox.kmpuiviews.motion

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MotionSettingsScreenTest {
    private val catalog = DesktopMotionCatalog(
        appName = "AWorld",
        launch = listOf(fake("one", "One"), fake("two", "Two")),
        quit = listOf(fake("one", "One")),
    )

    @Test
    fun listsOnlyBasicAndOwn() = runComposeUiTest {
        setContent {
            MaterialTheme { MotionSettingsContent(catalog, "", "", {}, {}) }
        }
        listOf("Off", "Fade", "One", "Two", "AWorld", "Basic").forEach {
            onNodeWithText(it).assertExists()
        }
        onNodeWithText("CRT power-on").assertDoesNotExist()
    }

    @Test
    fun clickSavesLaunchKey() = runComposeUiTest {
        var saved: String? = null
        setContent {
            MaterialTheme { MotionSettingsContent(catalog, "", "", { saved = it }, {}) }
        }
        onNodeWithText("Two").performClick()
        assertEquals("two", saved)
    }

    @Test
    fun quitTabSavesQuitKey() = runComposeUiTest {
        var saved: String? = null
        setContent {
            MaterialTheme { MotionSettingsContent(catalog, "", "", {}, { saved = it }) }
        }
        onNodeWithText("Quit").performClick()
        onNodeWithText("Off").performClick()
        assertEquals("off", saved)
    }

    private fun fake(k: String, t: String) = object : DesktopMotion {
        override val key = k
        override val title = t
        override val durationMillis = 1000
    }
}
