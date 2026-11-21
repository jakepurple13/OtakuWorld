package com.programmersbox.anime.shared.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.programmersbox.anime.shared.subtitles.InMemorySubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleEdge
import com.programmersbox.anime.shared.subtitles.SubtitleFont
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.rememberOnboardingScope
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.koin.compose.KoinApplication
import org.koin.dsl.module
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
class PlayerSetupStepTest {
    @BeforeTest
    fun freshDataStore() {
        // videoPlayerStyle / videoPlayerBlur read the process-wide DataStore.
        otakuDataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { File(createTempDirectory("player-step").toFile(), "otaku.preferences_pb").toOkioPath() },
        )
    }

    private fun settings(): NewSettingsHandling {
        val file = File.createTempFile("player-step-settings", ".preferences_pb").also {
            it.delete()
            it.deleteOnExit()
        }
        return NewSettingsHandling(createProtobuf(serializer = SettingsSerializer(), fileName = file.absolutePath))
    }

    private fun ComposeUiTest.show(repo: InMemorySubtitleSettingsRepository, handling: NewSettingsHandling = settings()) =
        setContent {
            MaterialExpressiveTheme {
                KoinApplication(application = { modules(module { single { handling } }) }) {
                    CompositionLocalProvider(
                        LocalReducedMotionOverride provides true,
                        LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 3000.dp)),
                    ) {
                        val scope = rememberOnboardingScope { playerSetupStep(repo, handling) }
                        scope[0].content()
                    }
                }
            }
        }

    private fun InMemorySubtitleSettingsRepository.current(): SubtitleStyle = runBlocking { style.first() }

    @Test
    fun size_slider_writes_percent() = runComposeUiTest {
        val repo = InMemorySubtitleSettingsRepository()
        show(repo)
        onNodeWithTag(PlayerSizeTag).performScrollTo()
        onNodeWithTag(PlayerSizeTag).performSemanticsAction(SemanticsActions.SetProgress) { it(137f) }
        waitUntil(timeoutMillis = 5_000) { repo.current().sizePercent == 140 }
        assertEquals(140, repo.current().sizePercent)
    }

    @Test
    fun edge_and_font_choices_write_style() = runComposeUiTest {
        val repo = InMemorySubtitleSettingsRepository()
        show(repo)
        onNodeWithText("Mono").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { repo.current().font == SubtitleFont.Mono }
        onNodeWithText("Raised").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { repo.current().edge == SubtitleEdge.Raised }
    }

    @Test
    fun languages_multi_select() = runComposeUiTest {
        val repo = InMemorySubtitleSettingsRepository()
        show(repo)
        onNodeWithText("Japanese").performScrollTo().performClick()
        onNodeWithText("English").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { repo.current().preferredLanguages == listOf("ja", "en") }
        onNodeWithText("Japanese").performClick()
        waitUntil(timeoutMillis = 5_000) { repo.current().preferredLanguages == listOf("en") }
    }

    @Test
    fun subtitle_sample_hidden_when_auto_off() = runComposeUiTest {
        val repo = InMemorySubtitleSettingsRepository()
        show(repo)
        onNodeWithTag(PlayerSubtitleSampleTag).assertIsDisplayed()
        onNodeWithText("Turn on automatically").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { !repo.current().autoEnable }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTagCount(PlayerSubtitleSampleTag) == 0
        }
    }

    @Test
    fun battery_hidden_off_android() = runComposeUiTest {
        show(InMemorySubtitleSettingsRepository())
        onNodeWithText("Gestures").performScrollTo().assertIsDisplayed()
        onNodeWithText("Battery alert").assertDoesNotExist()
        onNodeWithTag(PlayerBatteryTag).assertDoesNotExist()
    }

    @Test
    fun gesture_rows_are_not_clickable() = runComposeUiTest {
        show(InMemorySubtitleSettingsRepository())
        onNodeWithText("Brightness and volume").performScrollTo().assertHasNoClickAction()
        onNodeWithText("Play and pause").performScrollTo().assertHasNoClickAction()
    }

    private fun ComposeUiTest.onAllNodesWithTagCount(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().size
}
