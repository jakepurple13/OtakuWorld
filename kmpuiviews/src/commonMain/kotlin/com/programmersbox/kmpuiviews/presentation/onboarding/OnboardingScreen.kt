package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.presentation.onboarding.composables.AccountContent
import com.programmersbox.kmpuiviews.presentation.onboarding.composables.FinishContent
import com.programmersbox.kmpuiviews.presentation.onboarding.composables.GeneralContent
import com.programmersbox.kmpuiviews.presentation.onboarding.composables.ThemeContent
import com.programmersbox.kmpuiviews.presentation.onboarding.composables.WelcomeContent
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    customPreferences: ComposeSettingsDsl,
    navigationBarSettings: @Composable () -> Unit,
    accountContent: @Composable () -> Unit,
    sourceUpdateCheckContent: @Composable () -> Unit,
    dataStoreHandling: DataStoreHandling = koinInject(),
    appConfig: AppConfig = koinInject(),
) {
    HideNavBarWhileOnScreen()

    val onboardingScope = rememberOnboardingScope {
        item { WelcomeContent() }

        item { ThemeContent() }

        item {
            AccountContent(
                navController = navController,
                cloudContent = accountContent
            )
        }

        item {
            GeneralContent(
                navigationBarSettings = navigationBarSettings,
                sourceUpdateCheckContent = sourceUpdateCheckContent
            )
        }

        apply(customPreferences.onboardingSettings)

        //This should ALWAYS be last
        item { FinishContent() }
    }

    var hasSeenOnboarding by dataStoreHandling
        .hasGoneThroughOnboarding
        .asState()

    LaunchedEffect(hasSeenOnboarding) {
        if (hasSeenOnboarding) {
            navController.navigate(Screen.RecentScreen) {
                popUpTo(Screen.OnboardingScreen) {
                    inclusive = true
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { onboardingScope.size }

    NormalOtakuScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Welcome!") },
                    //TODO: Put back when supported
                    //subtitle = { Text(appConfig.appName) },
                    actions = {
                        var skipOnboarding by remember { mutableStateOf(false) }

                        if (skipOnboarding) {
                            AlertDialog(
                                onDismissRequest = { skipOnboarding = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            hasSeenOnboarding = true
                                            skipOnboarding = false
                                        }
                                    ) { Text("Confirm") }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { skipOnboarding = false }
                                    ) { Text("Dismiss") }
                                },
                                title = { Text("Skip Onboarding?") },
                                text = { Text("Are you sure you want to skip onboarding?") }
                            )
                        }

                        TextButton(
                            onClick = { skipOnboarding = true }
                        ) { Text("Skip") }
                    }
                )
                HorizontalDivider()
                Spacer(Modifier.size(4.dp))
            }
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    TextButton(
                        onClick = {
                            if (pagerState.canScrollBackward) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = pagerState.canScrollBackward
                    ) { Text("Back") }

                    OnboardingIndicator(pagerState)
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (pagerState.canScrollForward) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                hasSeenOnboarding = true
                            }
                        }
                    ) {
                        if (pagerState.canScrollForward) {
                            Text("Next")
                        } else {
                            Text("Finish!")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Onboarding(
            onboardingScope = onboardingScope,
            state = pagerState,
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        )
    }
}