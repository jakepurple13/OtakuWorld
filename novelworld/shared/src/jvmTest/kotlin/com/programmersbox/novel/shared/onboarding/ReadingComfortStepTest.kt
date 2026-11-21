package com.programmersbox.novel.shared.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.rememberOnboardingScope
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.novel.shared.novelTextSize
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class ReadingComfortStepTest {
    @BeforeTest
    fun freshDataStore() {
        otakuDataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { File(createTempDirectory("novel-step").toFile(), "otaku.preferences_pb").toOkioPath() },
        )
    }

    private fun ComposeUiTest.show() = setContent {
        MaterialExpressiveTheme {
            CompositionLocalProvider(
                LocalReducedMotionOverride provides true,
                LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 3000.dp)),
            ) {
                val scope = rememberOnboardingScope { readingComfortStep(novelTextSize) }
                scope[0].content()
            }
        }
    }

    // The handler captured the original store; the step writes through the swapped global one.
    private fun saved(): Int =
        runBlocking { otakuDataStore.data.first()[intPreferencesKey("novel_text_size")] ?: 16 }

    private fun ComposeUiTest.sampleFontSize(): Float {
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithTag(ReadingSampleTag).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first().layoutInput.style.fontSize.value
    }

    @Test
    fun slider_range_12_to_24() = runComposeUiTest {
        show()
        val range = onNodeWithTag(ReadingSizeTag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(12f..24f, range.range)
        assertEquals(11, range.steps)
    }

    @Test
    fun slider_writes_text_size() = runComposeUiTest {
        show()
        onNodeWithTag(ReadingSizeTag).performScrollTo()
        onNodeWithTag(ReadingSizeTag).performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        waitUntil(timeoutMillis = 5_000) { saved() == 20 }
        assertEquals(20, saved())
    }

    @Test
    fun sample_uses_text_size() = runComposeUiTest {
        show()
        assertEquals(16f, sampleFontSize())
        onNodeWithTag(ReadingSizeTag).performScrollTo()
        onNodeWithTag(ReadingSizeTag).performSemanticsAction(SemanticsActions.SetProgress) { it(22f) }
        waitUntil(timeoutMillis = 5_000) { sampleFontSize() == 22f }
    }
}
