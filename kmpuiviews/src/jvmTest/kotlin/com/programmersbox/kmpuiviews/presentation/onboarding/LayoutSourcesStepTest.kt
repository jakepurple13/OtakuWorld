package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.CatalogInfo
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.layoutStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.middleOptions
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.onboardingCatalogs
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.sourcesStep
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class LayoutSourcesStepTest {
    private fun ComposeUiTest.show(scope: OnboardingScope) = setContent {
        MaterialExpressiveTheme {
            CompositionLocalProvider(
                LocalReducedMotionOverride provides true,
                LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 1600.dp)),
            ) { scope[0].content() }
        }
    }

    private fun catalog(title: String) = CatalogInfo(Icons.Default.Star, title, "sub of $title", builtIn = false)

    @Test
    fun grid_choice_reflows_preview() = runComposeUiTest {
        createTestDataStoreHandling()
        val handling = createTestNewSettingsHandling()
        show(buildOnboardingScope { layoutStep(handling) })
        onNodeWithText("Fixed").performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("onb_mini_grid") and hasStateDescription("cols=2"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(GridChoice.Fixed, runBlocking { handling.preferences.data.first().gridChoice })
        onNodeWithText("Full adaptive").performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("onb_mini_grid") and hasStateDescription("cols=4"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun middle_options_exclude_multiple() {
        assertFalse(MiddleNavigationAction.Multiple in middleOptions)
        assertEquals(
            listOf(
                MiddleNavigationAction.All,
                MiddleNavigationAction.Notifications,
                MiddleNavigationAction.Lists,
                MiddleNavigationAction.Favorites,
                MiddleNavigationAction.Search,
            ),
            middleOptions,
        )
    }

    @Test
    fun all_chip_first_and_selected_by_default() = runComposeUiTest {
        createTestDataStoreHandling()
        val handling = createTestNewSettingsHandling()
        show(buildOnboardingScope { layoutStep(handling) })
        onNodeWithText("All").assertIsSelected()
        onNodeWithText("Notifications").assertIsNotSelected()
    }

    @Test
    fun middle_chip_writes_setting() = runComposeUiTest {
        createTestDataStoreHandling()
        val handling: NewSettingsHandling = createTestNewSettingsHandling()
        show(buildOnboardingScope { layoutStep(handling) })
        onNodeWithText("Search").performClick()
        waitUntil(timeoutMillis = 5_000) {
            runBlocking { handling.preferences.data.first().middleNavigationAction } == MiddleNavigationAction.Search
        }
    }

    @Test
    fun sources_has_no_buttons() = runComposeUiTest {
        show(buildOnboardingScope { sourcesStep("MangaWorld", listOf(catalog("A"), catalog("B"))) })
        onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(OnboardingTags.step("sources"))))
            .assertCountEquals(0)
    }

    @Test
    fun sources_copy_matches_spec() = runComposeUiTest {
        show(buildOnboardingScope { sourcesStep("MangaWorld", listOf(catalog("A"))) })
        onNodeWithText(
            "MangaWorld doesn't come with any titles. Extensions connect it to sites that have them, and you choose which to add."
        ).assertExists()
        onNodeWithText("How it works").assertExists()
        onNodeWithText("Find").assertExists()
        onNodeWithText("Settings › Sources & Extensions › Get extensions lists what's available.").assertExists()
        onNodeWithText("Install").assertExists()
        onNodeWithText("Tap Install on one. Android may ask once to allow installs from this app.").assertExists()
        onNodeWithText("Browse").assertExists()
        onNodeWithText("Its titles show up in Recent, All and Search. Switch sources from the source picker.").assertExists()
        onNodeWithText("Where extensions come from").assertExists()
        onNodeWithText("You'll add extensions after setup, from Settings › Sources & Extensions.").assertExists()
    }

    @Test
    fun catalogs_mapped_from_registered_names() {
        val manga = onboardingCatalogs(listOf("Mihon"), hasCustomBridge = true).map { it.title }
        assertEquals(listOf("OtakuWorld catalog", "Mihon & Tachiyomi repos", "Custom repos"), manga)
        val anime = onboardingCatalogs(listOf("Aniyomi", "CloudStream"), hasCustomBridge = true).map { it.title }
        assertEquals(listOf("OtakuWorld catalog", "Aniyomi repos", "CloudStream", "Custom repos"), anime)
        val novel = onboardingCatalogs(emptyList(), hasCustomBridge = false)
        assertEquals(listOf("OtakuWorld catalog"), novel.map { it.title })
        assertEquals(true, novel.single().builtIn)
        val unknown = onboardingCatalogs(listOf("Fancy"), hasCustomBridge = false)
        assertEquals(listOf("OtakuWorld catalog", "Fancy"), unknown.map { it.title })
    }

    @Test
    fun custom_repos_only_with_bridge() = runComposeUiTest {
        show(buildOnboardingScope { sourcesStep("MangaWorld", onboardingCatalogs(listOf("Mihon"), hasCustomBridge = false)) })
        onNodeWithText("Custom repos").assertDoesNotExist()
        onNodeWithText("Mihon & Tachiyomi repos").assertExists()
        onNodeWithText("Built in").assertExists()
    }

    @Test
    fun custom_repos_shown_with_bridge() = runComposeUiTest {
        show(buildOnboardingScope { sourcesStep("MangaWorld", onboardingCatalogs(listOf("Mihon"), hasCustomBridge = true)) })
        onNodeWithText("Custom repos").assertExists()
        onNodeWithText("Paste a repo link").assertExists()
    }
}
