package com.programmersbox.uiviews.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.datastore.asState
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.skydoves.landscapist.rememberDrawablePainter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    viewModel: AccountViewModel = koinViewModel(),
    appLogo: AppLogo = koinInject(),
    navController: NavController,
    dataStoreHandling: DataStoreHandling = koinInject(),
) {
    HideSystemBarsWhileOnScreen()

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
    val pagerState = rememberPagerState { 3 }

    NormalOtakuScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Welcome!") },
                    subtitle = { Text(appName) }
                )
                HorizontalDivider()
            }
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    TextButton(
                        onClick = {
                            hasSeenOnboarding = true
                        }
                    ) { Text("Skip") }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = when {
                                pagerState.currentPage == iteration -> MaterialTheme.colorScheme.primary
                                pagerState.currentPage > iteration -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }

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

        //TODO: Add login screen
        // add account info screen
        // maybe even see if we can get the account created when

        HorizontalPager(
            state = pagerState,
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) { page ->
            when (page) {
                0 -> WelcomeContent(
                    appLogo = appLogo,
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    appLogo: AppLogo,
) {
    val appName = stringResource(R.string.app_name)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .size(150.dp)
        ) {
            Image(
                rememberDrawablePainter(appLogo.logo),
                null,
                modifier = Modifier.matchParentSize()
            )
        }

        HorizontalDivider()

        Text(
            stringResource(R.string.welcome_description, appName),
            modifier = Modifier
                .padding(16.dp)
        )
    }
}