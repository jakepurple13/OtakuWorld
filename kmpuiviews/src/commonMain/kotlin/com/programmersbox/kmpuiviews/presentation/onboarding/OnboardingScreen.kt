package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StoryStepsScaffold
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.ColorSweepHost
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalColorSweep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.CatalogInfo
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.doneStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.layoutStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.lookStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.rememberOnboardingCatalogs
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.sourcesStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.syncStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.tipsStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.updatesStep
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.welcomeStep
import com.programmersbox.kmpuiviews.presentation.tips.Tip
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import org.koin.compose.koinInject

/** Steps that get a summary row on Done, in this order ("app" only when the app contributes one). */
private val SummaryIds = listOf("look", "layout", "app", "sync", "updates")

@Composable
fun OnboardingScreen(
    navController: NavigationActions,
    customPreferences: ComposeSettingsDsl,
    dataStoreHandling: DataStoreHandling = koinInject(),
    appConfig: AppConfig = koinInject(),
    handling: NewSettingsHandling = koinInject(),
) {
    HideNavBarWhileOnScreen()

    OnboardingContent(
        navController = navController,
        customPreferences = customPreferences,
        dataStoreHandling = dataStoreHandling,
        appName = appConfig.appName,
        handling = handling,
        catalogs = { rememberOnboardingCatalogs() },
    )
}

/** The story-steps flow without Koin-only pieces, so tests can supply the catalogs, sync step and tips. */
@Composable
internal fun OnboardingContent(
    navController: NavigationActions,
    customPreferences: ComposeSettingsDsl,
    dataStoreHandling: DataStoreHandling,
    appName: String,
    handling: NewSettingsHandling,
    catalogs: @Composable () -> List<CatalogInfo>,
    sync: OnboardingScope.() -> Unit = { syncStep(navController) },
    tips: List<Tip>? = null,
) {
    val config = customPreferences.onboardingConfig

    // While a tip's practice view is held, the pager stops swiping so the demo gets the gestures.
    var practicingTip by remember { mutableStateOf(false) }

    val onboardingScope = rememberOnboardingScope {
        // Built once and re-added below, so Welcome's "Coming up" chips and Done's summaries read the same specs.
        val middle = buildOnboardingScope {
            lookStep(handling)
            layoutStep(handling, appName = appName)
            sourcesStep(appName, catalogs)
            apply(customPreferences.onboardingSettings)
            sync()
            updatesStep(handling, config.updateNoun)
        }
        val specs = (0 until middle.size).map { middle[it].spec }
        val chipLabels = mapOf(
            "look" to "Look",
            "layout" to "Layout",
            "sources" to "Sources",
            "app" to config.appStepTab,
            "sync" to "Sync",
            "updates" to "Updates",
        )

        welcomeStep(
            appName = appName,
            config = config,
            comingUp = specs.mapNotNull { spec -> chipLabels[spec.id]?.let { spec.icon to it } },
        )
        repeat(middle.size) { step(middle[it].spec, middle[it].content) }
        tipsStep(onPracticeChange = { practicingTip = it }, tips = tips)
        //This should ALWAYS be last
        doneStep(SummaryIds.mapNotNull { id -> specs.firstOrNull { it.id == id } })
    }

    var hasSeenOnboarding by dataStoreHandling
        .hasGoneThroughOnboarding
        .asState()

    val pagerState = rememberPagerState { onboardingScope.size }

    fun leave() {
        hasSeenOnboarding = true
        navController.popBackStack()
    }

    // The sweep host wraps the whole frame (progress, hero, buttons) and the Surface paints the background,
    // so the frozen frame covers everything. Steps pass ROOT coordinates; convert to host coordinates.
    var hostCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    ColorSweepHost { sweepFrom ->
        val sweepFromRoot: (Offset) -> Unit = remember(sweepFrom) {
            { root -> sweepFrom(root - (hostCoords?.positionInRoot() ?: Offset.Zero)) }
        }
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { hostCoords = it },
        ) {
            CompositionLocalProvider(LocalColorSweep provides sweepFromRoot) {
                StoryStepsScaffold(
                    scope = onboardingScope,
                    appName = appName,
                    startLabel = config.startLabel,
                    onFinish = ::leave,
                    onSkipConfirmed = ::leave,
                    userScrollEnabled = !practicingTip,
                    pagerState = pagerState,
                )
            }
        }
    }
}
