package com.programmersbox.uiviews.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.uiviews.presentation.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.utils.HideNavBarWhileOnScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    customPreferences: ComposeSettingsDsl,
    appLogo: AppLogo = koinInject(),
    dataStoreHandling: DataStoreHandling = koinInject(),
) {
    HideNavBarWhileOnScreen()

    val onboardingScope = remember {
        OnboardingScopeImpl {
            item { WelcomeContent(appLogo = appLogo) }

            item { ThemeContent() }

            item { AccountContent(navController = navController) }

            item { GeneralContent() }

            apply(customPreferences.onboardingSettings)

            //This should ALWAYS be last
            item { FinishContent() }
        }
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

    val appName = stringResource(R.string.app_name)

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { onboardingScope.intervals.size }

    NormalOtakuScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Welcome!") },
                    subtitle = { Text(appName) },
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

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth()
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color by animateColorAsState(
                                when {
                                    pagerState.currentPage == iteration -> MaterialTheme.colorScheme.primary
                                    pagerState.currentPage > iteration -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(16.dp)
                            )
                        }
                    }
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

        //TODO: add account info screen
        // maybe even see if we can get the account created when

        HorizontalPager(
            state = pagerState,
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) { page -> onboardingScope.intervals[page].value() }
    }
}