package com.programmersbox.manga.shared.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.rememberOnboardingScope
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.manga.shared.settings.workingImageLoaders
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class ReaderSetupStepTest {
    private fun handling(): MangaNewSettingsHandling {
        val file = File.createTempFile("manga-settings-test", ".preferences_pb").also {
            it.delete()
            it.deleteOnExit()
        }
        return MangaNewSettingsHandling(
            createProtobuf(serializer = MangaNewSettingsSerializer, fileName = file.absolutePath)
        )
    }

    private fun ComposeUiTest.show(handling: MangaNewSettingsHandling) = setContent {
        MaterialExpressiveTheme {
            CompositionLocalProvider(
                LocalReducedMotionOverride provides true,
                LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 3000.dp)),
            ) {
                val scope = rememberOnboardingScope { readerSetupStep(handling) }
                scope[0].content()
            }
        }
    }

    private fun MangaNewSettingsHandling.settings(): MangaSettings =
        runBlocking { preferences.data.first() }

    @Test
    fun padding_slider_range_0_to_16() = runComposeUiTest {
        show(handling())
        val range = onNodeWithTag(ReaderPaddingTag).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        assertEquals(0f..16f, range?.range)
    }

    @Test
    fun page_number_chips_list_all_styles() = runComposeUiTest {
        show(handling())
        listOf(
            "Classic", "Rolling pill", "Scrubber", "Edge rail",
            "Story segments", "Printed folio", "Morphing pill", "Line only",
        ).forEach { onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun pages_button_only_with_floating_bar() = runComposeUiTest {
        val h = handling()
        show(h)
        // Serializer default: floating bar off
        onNodeWithText("Pages button in the bar").assertDoesNotExist()
        onNodeWithText("Floating bottom bar").performClick()
        waitUntil(timeoutMillis = 5_000) { h.settings().useFloatingReaderBottomBar }
        onNodeWithText("Pages button in the bar").assertIsDisplayed()
    }

    @Test
    fun pages_button_inverts_hide_flag() = runComposeUiTest {
        val h = handling()
        show(h)
        onNodeWithText("Floating bottom bar").performClick()
        waitUntil(timeoutMillis = 5_000) { h.settings().useFloatingReaderBottomBar }
        onNodeWithText("Pages button in the bar").performClick()
        waitUntil(timeoutMillis = 5_000) { h.settings().hidePageNumberInBar }
        assertTrue(h.settings().hidePageNumberInBar)
    }

    @Test
    fun drawer_toggle_peeks_drawer() = runComposeUiTest {
        val h = handling()
        show(h)
        onNodeWithTag(ReaderDrawerTag).assertDoesNotExist()
        onNodeWithText("Swipe for chapter list").performClick()
        onNodeWithTag(ReaderDrawerTag).assertIsDisplayed()
        mainClock.advanceTimeBy(1600)
        onNodeWithTag(ReaderDrawerTag).assertDoesNotExist()
        waitUntil(timeoutMillis = 5_000) { h.settings().allowUserDrawerGesture }
    }

    @Test
    fun reader_type_saves() = runComposeUiTest {
        val h = handling()
        show(h)
        onNodeWithText("Pager").performClick()
        waitUntil(timeoutMillis = 5_000) { h.settings().readerType == ReaderType.Pager }
    }

    @Test
    fun image_loader_options() = runComposeUiTest {
        show(handling())
        // Same filtering as the Settings screen: only loaders that work are offered.
        listOf(ImageLoaderType.Kamel, ImageLoaderType.Glide, ImageLoaderType.Telephoto, ImageLoaderType.Panpf)
            .forEach { type ->
                val node = onNodeWithText(type.name)
                if (type in workingImageLoaders) node.assertExists() else node.assertDoesNotExist()
            }
    }
}
