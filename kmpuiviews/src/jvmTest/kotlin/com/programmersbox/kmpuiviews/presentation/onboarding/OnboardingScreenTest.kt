package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalColorSweep
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.syncStep
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class OnboardingScreenTest {
    private val calls = mutableListOf<String>()
    private val nav: NavigationActions = Proxy.newProxyInstance(
        NavigationActions::class.java.classLoader,
        arrayOf(NavigationActions::class.java),
    ) { proxy, method, args ->
        when (method.name) {
            // Compose compares parameters on recomposition; a null from equals would crash.
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "FakeNavigationActions"
            else -> {
                calls += method.name
                null
            }
        }
    } as NavigationActions

    private val order = listOf("welcome", "look", "layout", "sources", "app", "sync", "updates", "tips", "done")

    private fun ComposeUiTest.show(): DataStoreHandling {
        val dataStore = createTestDataStoreHandling()
        val handling = createTestNewSettingsHandling()
        val prefs = ComposeSettingsDsl().apply {
            onboardingSettings {
                step(
                    OnboardingStepSpec(
                        id = "app",
                        title = "Fake reader",
                        body = "",
                        icon = Icons.Default.Star,
                        shape = HeroShape.Clover,
                        summaryLabel = "Reader",
                        summary = { "Fake reader summary" },
                    )
                ) {
                    Box(Modifier.fillMaxSize().testTag(OnboardingTags.step("app"))) {
                        Text(if (LocalColorSweep.current != null) "sweep provided" else "no sweep")
                    }
                }
            }
        }
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(
                    LocalReducedMotionOverride provides true,
                    LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 1600.dp)),
                ) {
                    OnboardingContent(
                        navController = nav,
                        customPreferences = prefs,
                        dataStoreHandling = dataStore,
                        appName = "TestApp",
                        handling = handling,
                        catalogs = { emptyList() },
                        sync = { syncStep(nav, false, { { {} } }, { {} }, { false }) },
                        tips = emptyList(),
                    )
                }
            }
        }
        waitForIdle()
        return dataStore
    }

    private fun ComposeUiTest.awaitStep(id: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(androidx.compose.ui.test.hasTestTag(OnboardingTags.step(id))).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.pageToDone() {
        order.dropLast(1).forEach { id ->
            awaitStep(id)
            onNodeWithTag(OnboardingTags.Next).performClick()
            waitForIdle()
        }
        awaitStep("done")
    }

    @Test
    fun step_order_matches_spec() = runComposeUiTest {
        show()
        order.forEachIndexed { index, id ->
            awaitStep(id)
            onNodeWithTag(OnboardingTags.step(id)).assertExists()
            if (index < order.lastIndex) {
                onNodeWithTag(OnboardingTags.Next).performClick()
                waitForIdle()
            }
        }
    }

    @Test
    fun finish_sets_flag_and_pops() = runComposeUiTest {
        val dataStore = show()
        pageToDone()
        onNodeWithTag(OnboardingTags.Next).performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { dataStore.hasGoneThroughOnboarding.getOrNull() } == true }
        assertEquals(listOf("popBackStack"), calls)
    }

    @Test
    fun skip_confirm_sets_flag_and_pops() = runComposeUiTest {
        val dataStore = show()
        onNodeWithTag(OnboardingTags.Skip).performClick()
        onNodeWithTag(OnboardingTags.SkipConfirm).performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { dataStore.hasGoneThroughOnboarding.getOrNull() } == true }
        assertEquals(listOf("popBackStack"), calls)
    }

    @Test
    fun done_summaries_in_spec_order() = runComposeUiTest {
        show()
        pageToDone()
        val labels = listOf("Look", "Layout", "Reader", "Sync & privacy", "Updates")
        val tops = labels.map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops, "summary rows out of order: ${labels.zip(tops)}")
        onNodeWithText("Fake reader summary").assertExists()
    }

    @Test
    fun steps_get_screen_wide_color_sweep() = runComposeUiTest {
        show()
        order.take(4).forEach { id ->
            awaitStep(id)
            onNodeWithTag(OnboardingTags.Next).performClick()
            waitForIdle()
        }
        awaitStep("app")
        onNodeWithText("sweep provided").assertExists()
    }

    @Test
    fun welcome_coming_up_includes_app_tab() = runComposeUiTest {
        show()
        listOf("Look", "Layout", "Sources", "Reader", "Sync", "Updates").forEach {
            onNodeWithText(it).assertExists()
        }
        assertTrue(calls.isEmpty())
    }
}
