package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    @Test
    fun groupEmitsNothingWithoutCatalog() = runComposeUiTest {
        setContent {
            MaterialTheme { LazyColumn { motionSettingsGroup(null) } }
        }
        onNodeWithText("Motion").assertDoesNotExist()
        onNodeWithText("Launch & quit animations").assertDoesNotExist()
    }

    @Test
    fun clickedRowStaysPreviewedUntilSaved() = runComposeUiTest {
        var launchKey by mutableStateOf("one")
        setContent {
            // The save lands later: onLaunchKey does not update launchKey.
            MaterialTheme { MotionSettingsContent(catalog, launchKey, "", {}, {}) }
        }
        onNodeWithText("Previewing one").assertExists()
        onNodeWithText("Two").performClick()
        onNodeWithText("Previewing two").assertExists()
        launchKey = "two"
        onNodeWithText("Previewing two").assertExists()
        // Once caught up, the preview follows the saved choice again.
        launchKey = "one"
        onNodeWithText("Previewing one").assertExists()
    }

    private fun fake(k: String, t: String) = object : DesktopMotion {
        override val key = k
        override val title = t
        override val durationMillis = 1000

        @Composable
        override fun BoxScope.InFront(time: () -> Int) {
            Text("Previewing $k")
        }
    }
}
