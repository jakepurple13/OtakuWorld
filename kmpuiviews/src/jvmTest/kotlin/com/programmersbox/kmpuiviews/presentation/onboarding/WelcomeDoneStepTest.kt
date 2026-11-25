package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.doneStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.welcomeStep
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlin.test.Test

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class WelcomeDoneStepTest {
    private fun spec(id: String, label: String?, value: String?) = OnboardingStepSpec(
        id, id, "", Icons.Default.Star, HeroShape.Cookie,
        summaryLabel = label,
        summary = value?.let { v -> { v } },
    )

    private fun ComposeUiTest.show(scope: OnboardingScope) = setContent {
        MaterialExpressiveTheme {
            CompositionLocalProvider(
                LocalReducedMotionOverride provides true,
                LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
            ) { scope[0].content() }
        }
    }

    @Test
    fun welcome_shows_app_name_and_genres() = runComposeUiTest {
        val config = OnboardingAppConfig.Default
        show(
            buildOnboardingScope {
                welcomeStep("MangaWorld", config, listOf(Icons.Default.Palette to "Look"))
            }
        )
        onNodeWithText("Welcome to MangaWorld").assertExists()
        config.genres.forEach { onNodeWithText(it.name).assertExists() }
        onNodeWithText("Look").assertExists()
    }

    @Test
    fun done_lists_summaries() = runComposeUiTest {
        show(
            buildOnboardingScope {
                doneStep(listOf(spec("look", "Look", "Dark"), spec("layout", "Layout", "Grid")))
            }
        )
        onNodeWithText("You're all set").assertExists()
        onNodeWithText("Look").assertExists()
        onNodeWithText("Dark").assertExists()
        onNodeWithText("Layout").assertExists()
        onNodeWithText("Grid").assertExists()
    }

    @Test
    fun done_has_no_summary_for_sources() = runComposeUiTest {
        show(
            buildOnboardingScope {
                doneStep(
                    listOf(
                        spec("look", "Look", "Dark"),
                        OnboardingStepSpec(
                            "sources", "Plug in sources", "", Icons.Default.Star, HeroShape.Arch,
                            summaryLabel = null, summary = null,
                        ),
                    )
                )
            }
        )
        onNodeWithText("Look").assertExists()
        onNodeWithText("Plug in sources").assertDoesNotExist()
        onNodeWithText("sources").assertDoesNotExist()
    }
}
